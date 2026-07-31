package com.atlaskv.service;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Service;

import com.atlaskv.persistence.PersistenceStrategy;
import com.atlaskv.storage.StorageEngine;
import com.atlaskv.storage.mutation.PutMutation;

@Service
public class StorageService {
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

    private final StorageEngine storageEngine;
    private final PersistenceStrategy persistenceStrategy;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public StorageService(StorageEngine storageEngine, PersistenceStrategy persistenceStrategy) {
        this.storageEngine = storageEngine;
        this.persistenceStrategy = persistenceStrategy;
    }

    public void put(String key, String value) {
        lock.writeLock().lock();
        PutMutation mutation = storageEngine.put(key, value);
        try {
            persistenceStrategy.appendPut(key, value);
        }
        catch(IOException e){
            storageEngine.rollback(mutation);
            throw new RuntimeException(
                    "Failed to persist PUT operation.",
                    e
            );
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
            persistenceStrategy.appendDelete(key);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist DELETE operation.", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}