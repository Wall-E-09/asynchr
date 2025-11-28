package pr_3;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

class DirectorySearchStealing extends RecursiveTask<Long> {
    private final File directory;
    private final long sizeThreshold;

    public DirectorySearchStealing(File directory, long sizeThreshold) {
        this.directory = directory;
        this.sizeThreshold = sizeThreshold;
    }

    @Override
    protected Long compute() {
        long count = 0;
        List<DirectorySearchStealing> subTasks = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    DirectorySearchStealing task = new DirectorySearchStealing(file, sizeThreshold);
                    task.fork();
                    subTasks.add(task);
                } else {
                    if (file.length() > sizeThreshold) {
                        count++;
                    }
                }
            }
        }

        for (DirectorySearchStealing task : subTasks) {
            count += task.join();
        }

        return count;
    }
}

public class FileSearchComparison {

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("TASK 2: File Search Comparison (Stealing vs Dealing)");

        System.out.print("Enter directory path: ");
        String path = scanner.nextLine();

        System.out.print("Enter min file size in bytes: ");
        while (!scanner.hasNextLong()) {
            System.out.print("Invalid number. Try again: ");
            scanner.next();
        }
        long sizeLimit = scanner.nextLong();

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Error: Invalid directory.");
            return;
        }
        System.out.println();

        System.out.println("Starting Work Stealing (Fork/Join)...");
        runWorkStealing(dir, sizeLimit);

        System.out.println("Starting Work Dealing (ExecutorService)...");
        runWorkDealing(dir, sizeLimit);
    }

    private static void runWorkStealing(File dir, long sizeLimit) {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        
        long start = System.currentTimeMillis();
        
        Long count = pool.invoke(new DirectorySearchStealing(dir, sizeLimit));
        
        long end = System.currentTimeMillis();

        System.out.println("Work Stealing found files: " + count);
        System.out.println("Work Stealing Time: " + (end - start) + " ms");
        System.out.println();
    }

    private static void runWorkDealing(File dir, long sizeLimit) throws InterruptedException {
        long startScan = System.currentTimeMillis();
        
        List<File> allDirectories = new ArrayList<>();
        collectDirectories(dir, allDirectories);
        
        int totalDirs = allDirectories.size();
        
        long startProcess = System.currentTimeMillis();

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        
        long[] globalCount = new long[1]; 
        Object lock = new Object();

        int chunkSize = (int) Math.ceil((double) totalDirs / threads);

        for (int i = 0; i < threads; i++) {
            final int startIdx = i * chunkSize;
            final int endIdx = Math.min(startIdx + chunkSize, totalDirs);

            if (startIdx < totalDirs) {
                executor.submit(() -> {
                    long localCount = 0;
                    try {
                        for (int j = startIdx; j < endIdx; j++) {
                            File folder = allDirectories.get(j);
                            File[] files = folder.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    if (!file.isDirectory() && file.length() > sizeLimit) {
                                        localCount++;
                                    }
                                }
                            }
                        }
                    } finally {
                        synchronized (lock) {
                            globalCount[0] += localCount;
                        }
                        latch.countDown();
                    }
                });
            } else {
                latch.countDown();
            }
        }

        latch.await();
        executor.shutdown();

        long end = System.currentTimeMillis();

        System.out.println("Work Dealing found files: " + globalCount[0]);
        System.out.println("Work Dealing Time (Total): " + (end - startScan) + " ms");
        System.out.println("   -> Scanning time: " + (startProcess - startScan) + " ms");
        System.out.println("   -> Processing time: " + (end - startProcess) + " ms");
        System.out.println();
    }

    private static void collectDirectories(File dir, List<File> list) {
        list.add(dir);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectDirectories(file, list);
                }
            }
        }
    }
}
