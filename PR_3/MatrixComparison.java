package pr_3;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

// ЧАСТИНА 1: Клас задачі для Work Stealing (Fork/Join) 
class MatrixRecursiveTask extends RecursiveAction {
    private static final int THRESHOLD = 20; 
    private final int[][] matrix;
    private final int startCol;
    private final int endCol;
    
    private final long[] results;

    public MatrixRecursiveTask(int[][] matrix, int startCol, int endCol, long[] results) {
        this.matrix = matrix;
        this.startCol = startCol;
        this.endCol = endCol;
        this.results = results;
    }

    @Override
    protected void compute() {
        if ((endCol - startCol) <= THRESHOLD) {
            for (int j = startCol; j < endCol; j++) {
                long sum = 0;
                for (int[] row : matrix) {
                    sum += row[j];
                }
                results[j] = sum;
            }
        } else {
            int mid = startCol + (endCol - startCol) / 2;
            invokeAll(
                new MatrixRecursiveTask(matrix, startCol, mid, results),
                new MatrixRecursiveTask(matrix, mid, endCol, results)
            );
        }
    }
}

// ЧАСТИНА 2: Головний клас 
public class MatrixComparison {
    
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        System.out.println("I am starting the matrix column sum calculation to compare Work Stealing and Work Dealing approaches.");
        
        int rows = getValidInput(scanner, "Please enter the number of rows (e.g. 2000): ");
        int cols = getValidInput(scanner, "Please enter the number of columns (e.g. 2000): ");
        
        System.out.print("Enter the minimum element value (e.g. 1): ");
        int minVal = scanner.nextInt();
        System.out.print("Enter the maximum element value (e.g. 100): ");
        int maxVal = scanner.nextInt();
        
        System.out.println("\nI am generating the random matrix now...");
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random.nextInt(maxVal - minVal + 1) + minVal;
            }
        }
        
        if (rows <= 20 && cols <= 20) {
            System.out.println("\nHere is the generated matrix:");
            printMatrix(matrix);
        } else {
            System.out.println("\nThe matrix is too large to show on the screen, so I will skip printing it.");
        }
        System.out.println();

        
        System.out.println("Starting the Work Stealing approach using Fork/Join...");
        runWorkStealing(matrix, cols);

        System.out.println("Starting the Work Dealing approach using ExecutorService...");
        runWorkDealing(matrix, cols);
    }

    // Реалізація Work Stealing (Fork/Join) ---
    private static void runWorkStealing(int[][] matrix, int cols) {
        long[] results = new long[cols];
        ForkJoinPool pool = new ForkJoinPool(); 
        
        long start = System.nanoTime();
        
        pool.invoke(new MatrixRecursiveTask(matrix, 0, cols, results));
        
        long end = System.nanoTime();
        
        double duration = (end - start) / 1_000_000.0;
        System.out.printf("Work Stealing execution time was %.4f ms%n", duration);
        
        printFirstResults(results);
    }

    // Реалізація Work Dealing (ExecutorService)
    private static void runWorkDealing(int[][] matrix, int cols) throws InterruptedException {
        long[] results = new long[cols];
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        
        CountDownLatch latch = new CountDownLatch(threads);

        long start = System.nanoTime();

        int chunkSize = (int) Math.ceil((double) cols / threads);

        for (int i = 0; i < threads; i++) {
            final int startCol = i * chunkSize;
            final int endCol = Math.min(startCol + chunkSize, cols);

            if (startCol < cols) {
                executor.submit(() -> {
                    try {
                        for (int j = startCol; j < endCol; j++) {
                            long sum = 0;
                            for (int[] row : matrix) {
                                sum += row[j];
                            }
                            results[j] = sum;
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            } else {
                latch.countDown();
            }
        }

        latch.await();
        executor.shutdown();
        
        long end = System.nanoTime();
        
        double duration = (end - start) / 1_000_000.0;
        System.out.printf("Work Dealing execution time was %.4f ms%n", duration);
        
        printFirstResults(results);
    }

    private static int getValidInput(Scanner scanner, String message) {
        System.out.print(message);
        while (!scanner.hasNextInt()) {
            System.out.print("That is not a valid number. Please enter an integer: ");
            scanner.next();
        }
        return scanner.nextInt();
    }

    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int val : row) {
                System.out.printf("%4d", val);
            }
            System.out.println();
        }
    }

    private static void printFirstResults(long[] results) {
        System.out.print("Here are the first 5 column sums: ");
        for (int i = 0; i < Math.min(5, results.length); i++) {
            System.out.print(results[i] + " ");
        }
        System.out.println("\n");
    }
}