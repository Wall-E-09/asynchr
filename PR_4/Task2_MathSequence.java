package PR_4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class Task2_MathSequence {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("Task 2 Math Sequence started");

        long globalStart = System.nanoTime();

        CompletableFuture<Void> mathChain = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread " + Thread.currentThread().getName() + " generating numbers");
            List<Double> sequence = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                double val = Math.round(ThreadLocalRandom.current().nextDouble(1.0, 10.0) * 100.0) / 100.0;
                sequence.add(val);
            }
            
            System.out.println("Generated sequence: " + sequence);
            return sequence;
        })
        .thenApplyAsync(sequence -> {
            System.out.println("Thread " + Thread.currentThread().getName() + " calculating sum of products");
            double sum = 0;
            for (int i = 0; i < sequence.size() - 1; i++) {
                double product = sequence.get(i) * sequence.get(i + 1);
                sum += product;
            }
            return sum;
        })
        .thenAcceptAsync(result -> {
            System.out.println("Thread " + Thread.currentThread().getName() + " printing result");
            double finalRes = Math.round(result * 100.0) / 100.0;
            System.out.println("Final Result (Sum of adjacent products): " + finalRes);
        })
        .thenRunAsync(() -> {
            long duration = (System.nanoTime() - globalStart) / 1_000_000;
            System.out.println("Total execution time for all async operations: " + duration + " ms");
        });

        mathChain.get();
    }
}