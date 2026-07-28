package com.atlaskv.service;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atlaskv.exception.KeyNotFoundException;
import com.atlaskv.storage.StorageEngine;

class StorageServiceStressTest {

    private StorageService storageService;
    private StorageEngine storageEngine;

    @BeforeEach
    void setUp() {
        storageEngine = new StorageEngine(100);
        storageService = new StorageService(storageEngine);
    }

    @Test
    void concurrentMixedOperationsMaintainInvariant() throws InterruptedException {

        int numberOfThreads = 100;
        int operationsPerThread = 500;

        ExecutorService executor = Executors.newFixedThreadPool(20);

        CountDownLatch startLatch = new CountDownLatch(1);

        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        try {
            for (int i = 0; i < numberOfThreads; i++) {

                executor.submit(() -> {
                    Random random = new Random();

                    try {
                        startLatch.await();

                        for (int j = 0; j < operationsPerThread; j++) {
                            int operation = random.nextInt(3);
                            String key = "key" + random.nextInt(200);
                            switch (operation) {
                                case 0 -> storageService.put(
                                        key,
                                        "value" + random.nextInt(1000)
                                );

                                case 1 -> {
                                    try {
                                        storageService.get(key);
                                    } catch (KeyNotFoundException ignored) {
                                    }
                                }

                                case 2 -> {
                                    try {
                                        storageService.delete(key);
                                    } catch (KeyNotFoundException ignored) {
                                    }
                                }
                            }

                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        fail("Thread interrupted.");
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(
                    finishLatch.await(60, TimeUnit.SECONDS),
                    "Stress test timed out."
            );

            storageEngine.validate();

        } finally {
            shutdownExecutor(executor);
        }
    }

    @Test
    void concurrentEvictionNeverExceedsCapacity() throws InterruptedException {

        int capacity = 25;
        storageEngine = new StorageEngine(capacity);
        storageService = new StorageService(storageEngine);

        int numberOfThreads = 50;
        int insertsPerThread = 200;

        ExecutorService executor = Executors.newFixedThreadPool(20);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        try {
            for (int i = 0; i < numberOfThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < insertsPerThread; j++) {
                            String key = "T" + threadId + "-K" + j;
                            storageService.put(key, "value");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        fail("Thread interrupted.");
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(
                    finishLatch.await(60, TimeUnit.SECONDS),
                    "Stress test timed out."
            );

            storageEngine.validate();
            assertTrue(storageEngine.size() <= capacity);
        } finally {
            shutdownExecutor(executor);
        }
    }

    @Test
    void concurrentUpdatesToSameKeyRemainConsistent() throws InterruptedException {

        storageEngine = new StorageEngine(10);
        storageService = new StorageService(storageEngine);

        int numberOfThreads = 100;

        ExecutorService executor = Executors.newFixedThreadPool(20);

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
                        fail("Thread interrupted.");
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            assertTrue(
                    finishLatch.await(30, TimeUnit.SECONDS),
                    "Timed out waiting for workers."
            );
            storageEngine.validate();
            assertEquals(1, storageEngine.size());
            String result = storageService.get("counter");
            assertNotNull(result);
            assertTrue(result.matches("value\\d+"));
        } finally {
            shutdownExecutor(executor);
        }
    }

    private void shutdownExecutor(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        assertTrue(
                executor.awaitTermination(10, TimeUnit.SECONDS),
                "Executor did not terminate in time."
        );
    }
}