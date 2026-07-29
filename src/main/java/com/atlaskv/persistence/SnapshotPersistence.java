package com.atlaskv.persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.atlaskv.storage.StorageEngine;

@Component
public class SnapshotPersistence {

    private final Path snapshotPath;
    private final AppendOnlyLog appendOnlyLog;

    public SnapshotPersistence(@Value("${atlaskv.snapshot.file}") String snapshotFile, AppendOnlyLog appendOnlyLog) {
        this.snapshotPath = Path.of(snapshotFile);
        this.appendOnlyLog = appendOnlyLog;
    }

    public void createSnapshot(StorageEngine storageEngine) throws IOException {

        Map<String, String> snapshot = storageEngine.snapshot();

        try (BufferedWriter writer = Files.newBufferedWriter(snapshotPath)) {
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue());
                writer.newLine();
            }
        }
        // Snapshot successfully written
        appendOnlyLog.truncate();
    }

    public Path getSnapshotPath() {
        return snapshotPath;
    }
}