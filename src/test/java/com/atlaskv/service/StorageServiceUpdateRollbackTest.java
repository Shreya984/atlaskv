package com.atlaskv.service;

import com.atlaskv.persistence.PersistenceStrategy;
import com.atlaskv.storage.StorageEngine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageServiceUpdateRollbackTest {

    @Test
    void updateFailureShouldRollbackValueAndLruOrder() throws Exception {

        // -------------------------
        // Arrange
        // -------------------------

        StorageEngine engine = new StorageEngine(2);

        PersistenceStrategy persistence =
                mock(PersistenceStrategy.class);

        StorageService service =
                new StorageService(engine, persistence);

        service.put("A", "Apple");

        // Fail when updating A
        doThrow(new IOException("Disk failure"))
                .when(persistence)
                .appendPut("A", "Apricot");

        // -------------------------
        // Act
        // -------------------------

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.put("A", "Apricot")
        );

        // -------------------------
        // Verify exception
        // -------------------------

        assertEquals(
                "Failed to persist PUT operation.",
                exception.getMessage()
        );

        assertTrue(exception.getCause() instanceof IOException);

        // -------------------------
        // Verify exact rollback state
        // -------------------------

        assertEquals(
                List.of("A"),
                engine.keysInOrder(),
                "LRU order should be restored exactly."
        );

        assertEquals(
                1,
                engine.size(),
                "Cache size should remain unchanged."
        );

        // -------------------------
        // Verify contents
        // -------------------------

        assertEquals(
                "Apple",
                engine.get("A"),
                "Original value should be restored."
        );

        // -------------------------
        // Verify invariants
        // -------------------------

        assertTrue(engine.isWithinCapacity());

        assertTrue(engine.isLruConsistent());

        assertFalse(engine.hasCycle());

        // -------------------------
        // Verify persistence interaction
        // -------------------------

        verify(persistence)
                .appendPut("A", "Apricot");
    }
}