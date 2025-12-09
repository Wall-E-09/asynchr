package PR_4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Task1_TextProcessing {

    private static final List<String> FILES = Arrays.asList("file1.txt", "file2.txt", "file3.txt");

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("Task 1 Text Processing started");

        CompletableFuture<Void> setupFuture = CompletableFuture.runAsync(() -> {
            long start = System.nanoTime();
            createDummyFiles();
            printTime("File creation", start);
        });

        setupFuture.join();

        CompletableFuture<Void> processingChain = CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            List<String> allSentences = new ArrayList<>();
            System.out.println("Thread " + Thread.currentThread().getName() + " reading files");
            
            for (String fileName : FILES) {
                try {
                    List<String> lines = Files.readAllLines(Paths.get(fileName));
                    allSentences.addAll(lines);
                } catch (IOException e) {
                    System.err.println("Error reading " + fileName);
                }
            }
            
            System.out.println("Initial sentences " + allSentences);
            printTime("Reading files", start);
            return allSentences;
        })
        .thenApplyAsync(sentences -> {
            long start = System.nanoTime();
            System.out.println("Thread " + Thread.currentThread().getName() + " removing letters");
            
            List<String> processed = sentences.stream()
                    .map(s -> s.replaceAll("[a-zA-Zа-яА-ЯіІїЇєЄ]", "")) 
                    .collect(Collectors.toList());
            
            printTime("Removing letters", start);
            return processed;
        })
        .thenAcceptAsync(finalList -> {
            long start = System.nanoTime();
            System.out.println("Thread " + Thread.currentThread().getName() + " printing result");
            System.out.println("Result list " + finalList);
            printTime("Output result", start);
        })
        .thenRunAsync(() -> {
            System.out.println("Task 1 finished completely");
        });

        processingChain.get(); 
    }

    private static void createDummyFiles() {
        try {
            Files.write(Paths.get("file1.txt"), Arrays.asList("Hello World 123", "Java Lab 4"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(Paths.get("file2.txt"), Arrays.asList("Async Programming is fun!", "No letters allowed 99%"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(Paths.get("file3.txt"), Arrays.asList("Keep digits: 2024", "End of text."), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printTime(String activity, long startTime) {
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        System.out.println("Time taken for " + activity + ": " + duration + " ms");
    }
}