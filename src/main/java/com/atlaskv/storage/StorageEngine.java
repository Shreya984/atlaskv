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

@Component
public class StorageEngine {

    private static final int DEFAULT_CAPACITY = 100;

    private final Map<String, Node> storage;
    private final LRUCache lruCache;
    private final int capacity;

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
    }

    /**
     * Read a value.
     *
     * IMPORTANT:
     * GET is treated as a write operation because accessing a key
     * changes its recency in the LRU list.
     */
    public String get(String key) {

        Node node = storage.get(key);

        if (node == null) {
            throw new KeyNotFoundException(key);
        }

        lruCache.moveToFront(node);

        return node.getValue();
    }

    /**
     * Delete a key.
     */
    public void delete(String key) {

        Node node = storage.get(key);

        if (node == null) {
            throw new KeyNotFoundException(key);
        }

        lruCache.remove(node);
        storage.remove(key);
    }

    public boolean containsKey(String key) {
        return storage.containsKey(key);
    }

    public int size() {
        return storage.size();
    }

    public boolean isWithinCapacity() {
        return storage.size() <= capacity;
    }

    public int capacity() {
        return capacity;
    }

    /**
     * Development invariant check.
     * Used by unit tests.
     */
    public void validate() {

        if (storage.size() != lruCache.size()) {
            throw new IllegalStateException(
                    "Map size and LRU size differ."
            );
        }

        if (storage.size() > capacity) {
            throw new IllegalStateException(
                    "Cache exceeded capacity."
            );
        }
    }

    public Map<String, String> snapshot() {
        Map<String, String> copy = new HashMap<>();

        for (Node node : storage.values()) {
            copy.put(node.getKey(), node.getValue());
        }

        return copy;
    }

    public int lruNodeCount() {
        return lruCache.nodeCount();
    }

    public boolean hasCycle() {
        return lruCache.hasCycle();
    }

    public boolean isLruConsistent() {

    List<String> lruKeys = lruCache.keysInOrder();

        if (lruKeys.size() != storage.size()) return false;
        Set<String> uniqueKeys = new HashSet<>(lruKeys);

        if (uniqueKeys.size() != lruKeys.size()) return false; // duplicate node in LRU

        return storage.keySet().equals(uniqueKeys);
    }
}