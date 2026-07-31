package com.atlaskv.storage.mutation;

import com.atlaskv.lru.LRUCache;
import com.atlaskv.lru.Node;

public class DeleteMutation extends StorageMutation {

    private Node deletedNode;

    private LRUCache.NodePosition deletedPosition;

    public Node getDeletedNode() {
        return deletedNode;
    }

    public void setDeletedNode(Node deletedNode) {
        this.deletedNode = deletedNode;
    }

    public LRUCache.NodePosition getDeletedPosition() {
        return deletedPosition;
    }

    public void setDeletedPosition(LRUCache.NodePosition deletedPosition) {
        this.deletedPosition = deletedPosition;
    }
}