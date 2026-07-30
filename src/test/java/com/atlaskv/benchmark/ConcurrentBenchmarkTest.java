package com.atlaskv.benchmark;

import com.atlaskv.exception.KeyNotFoundException;
import com.atlaskv.storage.StorageEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentBenchmarkTest {

    private static final int THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 2_000;

    @Test
    void benchmarkConcurrentLoad() throws Exception {

        StorageEngine engine = new StorageEngine(6_000);

        ExecutorService executor =
                Executors.newFixedThreadPool(THREADS);

        AtomicInteger puts = new AtomicInteger();
        AtomicInteger gets = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();

        int putsPerThread = OPERATIONS_PER_THREAD / 3;

        for (int t = 0; t < THREADS; t++) {

            final int threadId = t;

            tasks.add(() -> {

                Random random = new Random();

                try {

                    // -------------------------
                    // PUT Phase (deterministic)
                    // -------------------------

                    for (int i = 0; i < putsPerThread; i++) {

                        String key = "T" + threadId + "-P" + i;
                        String value = "value-" + threadId + "-" + i;

                        engine.put(key, value);

                        puts.incrementAndGet();
                    }

                    // -------------------------
                    // GET Phase
                    // -------------------------

                    for (int i = 0; i < OPERATIONS_PER_THREAD - putsPerThread; i++) {

                        String key =
                                "T" + random.nextInt(THREADS)
                                        + "-P"
                                        + random.nextInt(putsPerThread);

                        try {
                            engine.get(key);
                        }
                        catch (KeyNotFoundException ignored) {
                            // Another thread may not have inserted
                            // the key yet.
                        }

                        gets.incrementAndGet();
                    }

                }
                catch (Exception e) {
                    failures.incrementAndGet();
                    System.err.println("Thread " + threadId + " failed:");
                    e.printStackTrace();
                }

                return null;
            });
        }

        long start = System.nanoTime();

        executor.invokeAll(tasks);

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long end = System.nanoTime();

        long totalOps = (long) THREADS * OPERATIONS_PER_THREAD;

        double seconds = (end - start) / 1_000_000_000.0;

        double opsPerSecond = totalOps / seconds;

        double avgLatencyMicros =
                (seconds * 1_000_000) / totalOps;

        // -------------------------
        // Correctness Checks
        // -------------------------

        assertEquals(0, failures.get());

        assertTrue(engine.isWithinCapacity());

        assertTrue(engine.isLruConsistent());

        assertFalse(engine.hasCycle());

        assertEquals(
                engine.size(),
                engine.lruNodeCount()
        );

        assertEquals(
                puts.get(),
                engine.size(),
                "Some writes were lost."
        );

        int lostWrites = puts.get() - engine.size();

        // -------------------------
        // Report
        // -------------------------

        System.out.println();
        System.out.println("====================================");
        System.out.println("AtlasKV Concurrent Benchmark");
        System.out.println("====================================");

        System.out.println("Correctness");
        System.out.println("----------------------");
        System.out.println("Successful PUTs  : " + puts.get());
        System.out.println("Final Cache Size : " + engine.size());
        System.out.println("Lost Writes      : " + lostWrites);
        System.out.println("Failures         : " + failures.get());
        System.out.println("Capacity OK      : " + engine.isWithinCapacity());
        System.out.println("LRU OK           : " + engine.isLruConsistent());
        System.out.println("Cycle Free       : " + !engine.hasCycle());

        System.out.println();

        System.out.println("Performance");
        System.out.println("----------------------");
        System.out.println("Threads          : " + THREADS);
        System.out.println("PUT Operations   : " + puts.get());
        System.out.println("GET Operations   : " + gets.get());
        System.out.println("Total Operations : " + totalOps);
        System.out.printf("Elapsed          : %.3f s%n", seconds);
        System.out.printf("Throughput       : %.0f ops/sec%n", opsPerSecond);
        System.out.printf("Average Latency  : %.2f µs/op%n", avgLatencyMicros);

        System.out.println("====================================");
    }
}