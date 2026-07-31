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
import com.atlaskv.storage.mutation.PutMutation;
import com.atlaskv.storage.mutation.DeleteMutation;

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
    public PutMutation put(String key, String value) {
        PutMutation mutation = new PutMutation();
        Node existing = storage.get(key);
        // Existing key
        if (existing != null) {
            mutation.setExistingNode(existing);
            mutation.setOldValue(existing.getValue());

            mutation.setExistingPosition(lruCache.positionOf(existing));

            existing.setValue(value);
            lruCache.moveToFront(existing);

            return mutation;
        }

        // Cache full
        if (storage.size() >= capacity) {

            Node victim = lruCache.leastRecentlyUsed();

            if (victim != null) {

                mutation.setEvictedNode(victim);

                mutation.setEvictedPosition(lruCache.positionOf(victim));

                lruCache.remove(victim);

                storage.remove(victim.getKey());
            }
        }

        Node node = new Node(key, value);

        mutation.setNewInsert(true);
        mutation.setInsertedNode(node);
        storage.put(key, node);
        lruCache.addFirst(node);
        return mutation;
    }

    public void rollback(PutMutation mutation) {

        if (mutation == null) return;

        if (mutation.isNewInsert()) {

            Node inserted = mutation.getInsertedNode();

            if (inserted != null) {
                lruCache.remove(inserted);
                storage.remove(inserted.getKey());
            }

            if (mutation.getEvictedNode() != null) {

                Node victim = mutation.getEvictedNode();
                storage.put(victim.getKey(), victim);

                LRUCache.NodePosition position = mutation.getEvictedPosition();

                lruCache.restoreBetween(
                        position.previous(),
                        victim,
                        position.next()
                );
            }

            return;
        }

        if (mutation.getExistingNode() != null) {

            Node existing = mutation.getExistingNode();

            existing.setValue(mutation.getOldValue());

            lruCache.remove(existing);

            LRUCache.NodePosition position = mutation.getExistingPosition();

            if (position == null) {
                throw new IllegalStateException(
                        "Existing node position was not recorded."
                );
            }

            lruCache.restoreBetween(
                    position.previous(),
                    existing,
                    position.next()
            );
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
        Node node = storage.get(key);
        if (node == null) throw new KeyNotFoundException(key);
        lruCache.moveToFront(node);
        return node.getValue();
    }

    /**
     * Delete a key.
     */
    public DeleteMutation delete(String key) {
        DeleteMutation mutation = new DeleteMutation();
        Node node = storage.get(key);

        if (node == null) throw new KeyNotFoundException(key);

        mutation.setDeletedNode(node);

        mutation.setDeletedPosition(lruCache.positionOf(node));

        lruCache.remove(node);

        storage.remove(key);

        return mutation;
    }

    public void rollback(DeleteMutation mutation) {

        if (mutation == null) return;

        Node deleted = mutation.getDeletedNode();

        if (deleted == null) return;

        storage.put(
                deleted.getKey(),
                deleted
        );

        LRUCache.NodePosition position =
                mutation.getDeletedPosition();

        if (position == null) {
            throw new IllegalStateException(
                    "Deleted node position was not recorded."
            );
        }

        lruCache.restoreBetween(
                position.previous(),
                deleted,
                position.next()
        );
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
            throw new IllegalStateException("Map size and LRU size differ.");
        }

        if (storage.size() > capacity) {
            throw new IllegalStateException("Cache exceeded capacity.");
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

    public List<String> keysInOrder() {
        return lruCache.keysInOrder();
    }
}