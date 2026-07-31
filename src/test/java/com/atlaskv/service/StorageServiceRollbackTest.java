package com.atlaskv.service;

import com.atlaskv.exception.KeyNotFoundException;
import com.atlaskv.persistence.PersistenceStrategy;
import com.atlaskv.storage.StorageEngine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageServiceRollbackTest {

    @Test
    void putFailureShouldRollbackEviction() throws Exception {

        StorageEngine engine = new StorageEngine(2);

        PersistenceStrategy persistence =
                mock(PersistenceStrategy.class);

        StorageService service =
                new StorageService(engine, persistence);

        service.put("A", "Apple");
        service.put("B", "Banana");

        // Fail only the third PUT
        doThrow(new IOException("Disk failure"))
                .when(persistence)
                .appendPut("C", "Cherry");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.put("C", "Cherry")
        );

        assertEquals(
                "Failed to persist PUT operation.",
                exception.getMessage()
        );

        assertTrue(exception.getCause() instanceof IOException);

        assertEquals(
                List.of("B", "A"),
                engine.keysInOrder(),
                "LRU order should be restored exactly."
        );

        assertEquals(
                2,
                engine.size(),
                "Cache size should be restored."
        );

        assertEquals("Apple", engine.get("A"));
        assertEquals("Banana", engine.get("B"));

        assertThrows(
                KeyNotFoundException.class,
                () -> engine.get("C")
        );

        assertTrue(engine.isWithinCapacity());

        assertTrue(engine.isLruConsistent());

        assertFalse(engine.hasCycle());

        verify(persistence).appendPut("C", "Cherry");
    }
}