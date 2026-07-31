package com.atlaskv.service;

import com.atlaskv.persistence.PersistenceStrategy;
import com.atlaskv.storage.StorageEngine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageServiceDeleteRollbackTest {

    @Test
    void deleteFailureShouldRollbackDeletion() throws Exception {

        StorageEngine engine = new StorageEngine(2);

        PersistenceStrategy persistence =
                mock(PersistenceStrategy.class);

        StorageService service =
                new StorageService(engine, persistence);

        service.put("A", "Apple");
        service.put("B", "Banana");

        // Initial LRU order:
        // HEAD -> B -> A -> TAIL

        doThrow(new IOException("Disk failure"))
                .when(persistence)
                .appendDelete("A");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.delete("A")
        );

        assertEquals(
                "Failed to persist DELETE operation.",
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

        assertTrue(engine.isWithinCapacity());

        assertTrue(engine.isLruConsistent());

        assertFalse(engine.hasCycle());

        verify(persistence).appendDelete("A");
    }
}