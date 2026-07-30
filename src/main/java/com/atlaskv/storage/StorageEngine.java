package com.atlaskv.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.atlaskv.exception.KeyNotFoundException;
import com.atlaskv.lru.LRUCache;
import com.atlaskv.lru.Node;

import java.util.concurrent.locks.ReentrantLock;

@Component
public class StorageEngine {

    private static final int DEFAULT_CAPACITY = 100;

    private final Map<String, Node> storage;
    private final LRUCache lruCache;
    private final int capacity;

    private final ReentrantLock lock = new ReentrantLock();

    public StorageEngine() {
        this(DEFAULT_CAPACITY);
    }

    public StorageEngine(int capacity) {
        this.capacity = capacity;
        this.storage = new HashMap<>();
        this.lruCache = new LRUCache();
    }

    /**
     * Insert a new key or update an existing key.
     */
    public void put(String key, String value) {

        lock.lock();

        try {
            Node existing = storage.get(key);

            // Existing key
            if (existing != null) {
                existing.setValue(value);
                lruCache.moveToFront(existing);
                return;
            }

            // Cache full
            if (storage.size() >= capacity) {

                Node victim = lruCache.removeLeastRecentlyUsed();

                if (victim != null) {
                    storage.remove(victim.getKey());
                }
            }

            Node node = new Node(key, value);

            storage.put(key, node);
            lruCache.addFirst(node);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Read a value.
     *
     * IMPORTANT:
     * GET is treated as a write operation because accessing a key
     * changes its recency in the LRU list.
     */
    public String get(String key) {

        lock.lock();

        try {
            Node node = storage.get(key);

            if (node == null) throw new KeyNotFoundException(key);

            lruCache.moveToFront(node);

            return node.getValue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Delete a key.
     */
    public void delete(String key) {

        lock.lock();

        try {
            Node node = storage.get(key);
            if (node == null) throw new KeyNotFoundException(key);
            lruCache.remove(node);
            storage.remove(key);
        } finally {
            lock.unlock();
        }
    }

    public boolean containsKey(String key) {
        lock.lock();
        try {
            
            return storage.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return storage.size();
        } finally {
            lock.unlock();
        }
    }

    public boolean isWithinCapacity() {
        lock.lock();
        try {
            return storage.size() <= capacity;
        } finally {
            lock.unlock();
        }
    }

    public int capacity() {
        return capacity;
    }

    /**
     * Development invariant check.
     * Used by unit tests.
     */
    public void validate() {
        lock.lock();
        try {
            if (storage.size() != lruCache.size()) {
                throw new IllegalStateException("Map size and LRU size differ.");
            }

            if (storage.size() > capacity) {
                throw new IllegalStateException("Cache exceeded capacity.");
            }
        } finally {
            lock.unlock();
        }
    }

    public Map<String, String> snapshot() {
        lock.lock();
        try {
            Map<String, String> copy = new HashMap<>();

            for (Node node : storage.values()) {
                copy.put(node.getKey(), node.getValue());
            }

            return copy;
        } finally {
            lock.unlock();
        }
    }

    public int lruNodeCount() {
        lock.lock();
        try {
            return lruCache.nodeCount();
        } finally {
            lock.unlock();
        }
    }

    public boolean hasCycle() {
        lock.lock();
        try {
            return lruCache.hasCycle();
        } finally {
            lock.unlock();
        }
    }

    public boolean isLruConsistent() {

        lock.lock();
        try {
            List<String> lruKeys = lruCache.keysInOrder();

            if (lruKeys.size() != storage.size()) return false;
            Set<String> uniqueKeys = new HashSet<>(lruKeys);

            if (uniqueKeys.size() != lruKeys.size()) return false; // duplicate node in LRU

            return storage.keySet().equals(uniqueKeys);
        } finally {
            lock.unlock();
        }
    }
}