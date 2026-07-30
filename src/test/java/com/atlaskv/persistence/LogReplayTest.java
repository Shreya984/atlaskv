package com.atlaskv.persistence;

import com.atlaskv.exception.KeyNotFoundException;
import com.atlaskv.storage.StorageEngine;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

class LogReplayTest {

    @Test
    void replayShouldReconstructSameState() throws Exception {

        Path logFile = Files.createTempFile("atlaskv", ".aof");

        StorageEngine original = new StorageEngine(10);
        StorageEngine recovered = new StorageEngine(10);

        AppendOnlyLog log = new AppendOnlyLog(logFile);

        // Execute operations
        original.put("user1", "Alice");
        log.appendPut("user1", "Alice");

        original.put("user2", "Bob");
        log.appendPut("user2", "Bob");

        original.delete("user1");
        log.appendDelete("user1");

        original.put("user3", "Charles");
        log.appendPut("user3", "Charles");

        // Recover into a fresh engine
        LogReplayer replayer = new LogReplayer(recovered, log, true);

        replayer.replayLog();

        assertEquals(original.snapshot(), recovered.snapshot());
    }

    @Test
    void replayShouldIgnorePartialLogRecord() throws Exception {

        Path logFile = Files.createTempFile("atlaskv", ".aof");

        Files.write(
                logFile,
                List.of(
                        "PUT user1 Alice",
                        "PUT user2 Bob",
                        "PUT user3"
                )
        );

        StorageEngine recovered = new StorageEngine(10);

        AppendOnlyLog log = new AppendOnlyLog(logFile);

        LogReplayer replayer =
                new LogReplayer(recovered, log, true);

        replayer.replayLog();

        assertEquals("Alice", recovered.get("user1"));
        assertEquals("Bob", recovered.get("user2"));

        assertThrows(
                KeyNotFoundException.class,
                () -> recovered.get("user3")
        );
    }
}