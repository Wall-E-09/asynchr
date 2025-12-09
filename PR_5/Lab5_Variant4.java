package PR_5;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Lab5_Variant4 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("=== TASK 1: Combine Results ===");
        runTask1();

        System.out.println("\n------------------------------------------------\n");

        System.out.println("=== TASK 2: Software Selection ===");
        runTask2();
    }

    private static void runTask1() throws ExecutionException, InterruptedException {
        System.out.println("Starting two independent calculations...");

        CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
            printLog("Calculating sum of 1..100");
            simulateDelay(500);
            int sum = 0;
            for (int i = 1; i <= 100; i++) sum += i;
            printLog("Sum calculated: " + sum);
            return sum;
        });

        CompletableFuture<Integer> task2 = CompletableFuture.supplyAsync(() -> {
            printLog("Calculating sum of 101..200");
            simulateDelay(800);
            int sum = 0;
            for (int i = 101; i <= 200; i++) sum += i;
            printLog("Sum calculated: " + sum);
            return sum;
        });

        CompletableFuture<Integer> combinedFuture = task1.thenCombine(task2, (result1, result2) -> {
            printLog("Combining results: " + result1 + " + " + result2);
            return result1 + result2;
        });

        System.out.println("Final Combined Result: " + combinedFuture.get());
    }

    private static void runTask2() throws ExecutionException, InterruptedException {
        List<String> softwareNames = List.of("SuperSoft", "MegaCode", "UltraDev", "BasicTool");
        List<CompletableFuture<Software>> futures = new ArrayList<>();

        System.out.println("Fetching data for " + softwareNames.size() + " software products in parallel...");
        long start = System.currentTimeMillis();

        for (String name : softwareNames) {
            CompletableFuture<Software> future = CompletableFuture.supplyAsync(() -> fetchSoftwareData(name));
            futures.add(future);
        }

        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        CompletableFuture<Software> bestOptionFuture = allDone.thenApply(v -> {
            List<Software> softwareList = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            System.out.println("\nAll data received:");
            softwareList.forEach(System.out::println);

            return softwareList.stream()
                    .max(Comparator.comparingDouble(Software::calculateScore))
                    .orElseThrow();
        });

        Software bestSoftware = bestOptionFuture.get();
        long end = System.currentTimeMillis();

        System.out.println("\n>>> ANALYSIS COMPLETE <<<");
        System.out.println("Best Choice: " + bestSoftware.name);
        System.out.println("Score: " + String.format("%.2f", bestSoftware.calculateScore()));
        System.out.println("Total execution time: " + (end - start) + " ms");
    }

    private static Software fetchSoftwareData(String name) {
        printLog("Requesting data for " + name);
        Random r = ThreadLocalRandom.current();
        
        simulateDelay(r.nextInt(500, 1500));

        double price = 50 + r.nextDouble() * 200;
        int functionality = r.nextInt(1, 11);
        int support = r.nextInt(1, 6);

        return new Software(name, price, functionality, support);
    }

    static class Software {
        String name;
        double price;
        int functionality;
        int support;

        public Software(String name, double price, int functionality, int support) {
            this.name = name;
            this.price = price;
            this.functionality = functionality;
            this.support = support;
        }

        public double calculateScore() {
            return (functionality * 10) + (support * 5) - (price * 0.1);
        }

        @Override
        public String toString() {
            return String.format("[%s] Price: $%.2f | Func: %d/10 | Supp: %d/5 | Score: %.2f", 
                    name, price, functionality, support, calculateScore());
        }
    }

    private static void printLog(String message) {
        System.out.println("   [Thread-" + Thread.currentThread().getId() + "] " + message);
    }

    private static void simulateDelay(int ms) {
        try { TimeUnit.MILLISECONDS.sleep(ms); } catch (InterruptedException e) { throw new RuntimeException(e); }
    }
}
