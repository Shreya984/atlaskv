package com.atlaskv.service;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Service;

import com.atlaskv.storage.StorageEngine;

@Service
public class StorageService {

    private final StorageEngine storageEngine;

    /**
     * AtlasKV currently uses a single coarse-grained lock.
     *
     * IMPORTANT:
     * GET acquires the write lock instead of the read lock.
     *
     * Reason:
     * A successful GET will eventually update the LRU recency list by moving
     * the accessed node to the front. Although Day 5 does not yet implement
     * LRU, we preserve the locking contract now so later features do not
     * require changing the concurrency model.
     *
     * Correctness is intentionally prioritized over maximum read concurrency.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public StorageService(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
    }

    public void put(String key, String value) {

        lock.writeLock().lock();

        try {
            storageEngine.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String get(String key) {

        /*
         * Intentionally uses the WRITE lock.
         *
         * Future GET operations mutate LRU metadata, making GET a write.
         */
        lock.writeLock().lock();

        try {
            return storageEngine.get(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void delete(String key) {

        lock.writeLock().lock();

        try {
            storageEngine.delete(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
}