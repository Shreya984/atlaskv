package com.atlaskv.lru;

public interface EvictionPolicy {

    void onGet(Node node);

    void onPut(Node node);

    Node evict();
}