package com.atlaskv.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class StorageInvariantTest {

    @Test
    void cacheShouldNeverExceedCapacity() {

        StorageEngine engine = new StorageEngine(5);

        for (int i = 0; i < 100; i++) {
            engine.put("key" + i, "value" + i);
        }

        assertTrue(engine.isWithinCapacity());
    }

    @Test
    void mapSizeShouldMatchLruNodeCount() {

        StorageEngine engine = new StorageEngine(5);

        for (int i = 0; i < 100; i++) {
            engine.put("key" + i, "value" + i);
        }

        assertEquals(engine.size(), engine.lruNodeCount());
    }

    @Test
    void lruListShouldNotContainCycles() {

        StorageEngine engine = new StorageEngine(5);

        for (int i = 0; i < 100; i++) {
            engine.put("key" + i, "value" + i);
        }

        assertFalse(engine.hasCycle());
    }

    @Test
    void everyMapKeyShouldExistExactlyOnceInLruList() {

        StorageEngine engine = new StorageEngine(5);

        for (int i = 0; i < 100; i++) {
            engine.put("key" + i, "value" + i);
        }

        assertTrue(engine.isLruConsistent());
    }
}