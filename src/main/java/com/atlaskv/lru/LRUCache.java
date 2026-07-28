package com.atlaskv.lru;

public class LRUCache {

    private final Node head;
    private final Node tail;

    private int size;

    public LRUCache() {

        head = new Node("__HEAD__", "");
        tail = new Node("__TAIL__", "");

        head.setNext(tail);
        tail.setPrevious(head);

        size = 0;
    }

    /**
     * Inserts a new node immediately after the head sentinel,
     * making it the most recently used entry.
     */
    public void addFirst(Node node) {

        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null.");
        }

        linkAfterHead(node);
        size++;
    }

    /**
     * Removes a node from the linked list.
     */
    public void remove(Node node) {

        if (node == null) {
            return;
        }

        validateRealNode(node);

        unlink(node);
        size--;
    }

    /**
     * Moves an existing node to the front of the list.
     */
    public void moveToFront(Node node) {

        if (node == null) {
            return;
        }

        validateRealNode(node);

        // Already the most recently used node.
        if (head.getNext() == node) {
            return;
        }

        unlink(node);
        linkAfterHead(node);
    }

    /**
     * Removes and returns the least recently used node.
     */
    public Node removeLeastRecentlyUsed() {

        if (isEmpty()) {
            return null;
        }

        Node lru = tail.getPrevious();

        unlink(lru);
        size--;

        return lru;
    }

    /**
     * Disconnects a node from the linked list.
     * Does NOT modify the size.
     */
    private void unlink(Node node) {

        Node previous = node.getPrevious();
        Node next = node.getNext();

        previous.setNext(next);
        next.setPrevious(previous);

        node.setPrevious(null);
        node.setNext(null);
    }

    /**
     * Inserts a node immediately after the head sentinel.
     * Does NOT modify the size.
     */
    private void linkAfterHead(Node node) {

        Node first = head.getNext();

        node.setPrevious(head);
        node.setNext(first);

        head.setNext(node);
        first.setPrevious(node);
    }

    /**
     * Ensures callers never manipulate sentinel nodes.
     */
    private void validateRealNode(Node node) {

        if (node == head || node == tail) {
            throw new IllegalArgumentException(
                    "Cannot manipulate sentinel nodes."
            );
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }
}