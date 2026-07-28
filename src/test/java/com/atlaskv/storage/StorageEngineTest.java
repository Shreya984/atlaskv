package com.atlaskv.storage;

import com.atlaskv.exception.KeyNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageEngineTest {

    private StorageEngine storage;

    @BeforeEach
    void setUp() {
        storage = new StorageEngine(3);
    }

    @Test
    void putThenGetReturnsStoredValue() {

        storage.put("A", "Apple");

        assertEquals("Apple", storage.get("A"));

        storage.validate();
    }

    @Test
    void updatingExistingKeyDoesNotIncreaseSize() {

        storage.put("A", "Apple");
        storage.put("A", "Apricot");

        assertEquals(1, storage.size());
        assertEquals("Apricot", storage.get("A"));

        storage.validate();
    }

    @Test
    void leastRecentlyUsedEntryGetsEvicted() {

        storage.put("A", "Apple");
        storage.put("B", "Banana");
        storage.put("C", "Cherry");

        // A becomes most recently used
        storage.get("A");

        // Should evict B
        storage.put("D", "Durian");

        assertTrue(storage.containsKey("A"));
        assertFalse(storage.containsKey("B"));
        assertTrue(storage.containsKey("C"));
        assertTrue(storage.containsKey("D"));

        storage.validate();
    }

    @Test
    void deleteRemovesKey() {

        storage.put("A", "Apple");

        storage.delete("A");

        assertFalse(storage.containsKey("A"));
        assertEquals(0, storage.size());

        assertThrows(
                KeyNotFoundException.class,
                () -> storage.get("A")
        );

        storage.validate();
    }

    @Test
    void cacheNeverExceedsCapacity() {

        storage.put("A", "Apple");
        storage.put("B", "Banana");
        storage.put("C", "Cherry");
        storage.put("D", "Durian");
        storage.put("E", "Eggfruit");
        storage.put("F", "Fig");

        assertEquals(3, storage.size());

        storage.validate();
    }

    @Test
    void getMissingKeyThrowsException() {

        assertThrows(
                KeyNotFoundException.class,
                () -> storage.get("Missing")
        );
    }

    @Test
    void deleteMissingKeyThrowsException() {

        assertThrows(
                KeyNotFoundException.class,
                () -> storage.delete("Missing")
        );
    }

    @Test
    void updatingExistingKeyRefreshesRecency() {

        storage.put("A", "Apple");
        storage.put("B", "Banana");
        storage.put("C", "Cherry");

        // Updating A should make it most recently used.
        storage.put("A", "Apricot");

        // Should evict B.
        storage.put("D", "Durian");

        assertTrue(storage.containsKey("A"));
        assertFalse(storage.containsKey("B"));
        assertTrue(storage.containsKey("C"));
        assertTrue(storage.containsKey("D"));

        storage.validate();
    }

    @Test
    void repeatedGetOperationsDoNotChangeSize() {

        storage.put("A", "Apple");

        storage.get("A");
        storage.get("A");
        storage.get("A");

        assertEquals(1, storage.size());

        storage.validate();
    }
}