package com.atlaskv.storage.mutation;

import com.atlaskv.lru.LRUCache;
import com.atlaskv.lru.Node;

public class PutMutation extends StorageMutation {

    private boolean newInsert;

    private Node insertedNode;

    private Node existingNode;

    private String oldValue;

    private Node evictedNode;

    private LRUCache.NodePosition evictedPosition;
    private LRUCache.NodePosition existingPosition;

    public boolean isNewInsert() {
        return newInsert;
    }

    public void setNewInsert(boolean newInsert) {
        this.newInsert = newInsert;
    }

    public Node getInsertedNode() {
        return insertedNode;
    }

    public void setInsertedNode(Node insertedNode) {
        this.insertedNode = insertedNode;
    }

    public Node getExistingNode() {
        return existingNode;
    }

    public void setExistingNode(Node existingNode) {
        this.existingNode = existingNode;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public Node getEvictedNode() {
        return evictedNode;
    }

    public void setEvictedNode(Node evictedNode) {
        this.evictedNode = evictedNode;
    }

    public LRUCache.NodePosition getEvictedPosition() {
        return evictedPosition;
    }

    public void setEvictedPosition(LRUCache.NodePosition evictedPosition) {
        this.evictedPosition = evictedPosition;
    }

    public LRUCache.NodePosition getExistingPosition() {
        return existingPosition;
    }

    public void setExistingPosition(LRUCache.NodePosition existingPosition) {
        this.existingPosition = existingPosition;
    }
}