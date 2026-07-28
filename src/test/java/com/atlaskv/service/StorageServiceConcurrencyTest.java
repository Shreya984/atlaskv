package com.atlaskv.service;

import com.atlaskv.storage.StorageEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceConcurrencyTest {

    private StorageService storageService;
    private StorageEngine storageEngine;

    @BeforeEach
    void setUp() {
        storageEngine = new StorageEngine();
        storageService = new StorageService(storageEngine);
    }

    @Test
    void concurrentPutOperations_shouldStoreAllDistinctKeys() throws InterruptedException {

        int numberOfThreads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        try {

            for (int i = 0; i < numberOfThreads; i++) {

                final int index = i;

                executor.submit(() -> {
                    try {
                        // Wait until all worker threads are ready.
                        startLatch.await();

                        storageService.put("key" + index, "value" + index);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        fail("Thread interrupted");
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            // Release all threads simultaneously.
            startLatch.countDown();

            assertTrue(
                    finishLatch.await(5, TimeUnit.SECONDS),
                    "Timed out waiting for worker threads."
            );

            assertEquals(numberOfThreads, storageEngine.size());

            for (int i = 0; i < numberOfThreads; i++) {
                assertEquals(
                        "value" + i,
                        storageService.get("key" + i)
                );
            }

        } finally {
            shutdownExecutor(executor);
        }
    }

    @Test
    void concurrentWritesToSameKey_shouldLeaveValidFinalValue() throws InterruptedException {

        int numberOfThreads = 100;

        ExecutorService executor = Executors.newFixedThreadPool(10);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        try {

            for (int i = 0; i < numberOfThreads; i++) {

                final int value = i;

                executor.submit(() -> {
                    try {

                        startLatch.await();

                        storageService.put("counter", "value" + value);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        fail("Thread interrupted");
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown();

            assertTrue(
                    finishLatch.await(5, TimeUnit.SECONDS),
                    "Timed out waiting for worker threads."
            );

            String result = storageService.get("counter");

            assertNotNull(result);
            assertTrue(result.matches("value\\d+"));

            int number = Integer.parseInt(result.substring(5));

            assertTrue(number >= 0 && number < numberOfThreads);

        } finally {
            shutdownExecutor(executor);
        }
    }

    @Test
    void concurrentGetOperations_shouldAlwaysReturnStoredValue() throws InterruptedException {

        storageService.put("user", "Alice");

        int numberOfThreads = 50;

        ExecutorService executor = Executors.newFixedThreadPool(10);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        try {

            for (int i = 0; i < numberOfThreads; i++) {

                executor.submit(() -> {
                    try {

                        startLatch.await();

                        assertEquals(
                                "Alice",
                                storageService.get("user")
                        );

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        fail("Thread interrupted");
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown();

            assertTrue(
                    finishLatch.await(5, TimeUnit.SECONDS),
                    "Timed out waiting for worker threads."
            );

        } finally {
            shutdownExecutor(executor);
        }
    }

    private void shutdownExecutor(ExecutorService executor)
            throws InterruptedException {

        executor.shutdown();

        assertTrue(
                executor.awaitTermination(5, TimeUnit.SECONDS),
                "Executor did not terminate in time."
        );
    }
}