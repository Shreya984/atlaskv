package com.atlaskv.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.atlaskv.storage.StorageEngine;

@Component
public class SnapshotLoader {

    private final StorageEngine storageEngine;
    private final Path snapshotPath;

    public SnapshotLoader(
            StorageEngine storageEngine,
            @Value("${atlaskv.snapshot.file}") String snapshotFile
    ) {
        this.storageEngine = storageEngine;
        this.snapshotPath = Path.of(snapshotFile);
    }

    public int loadSnapshot() throws IOException {
        if (Files.notExists(snapshotPath)) return 0;
        int recovered = 0;

        try (BufferedReader reader = Files.newBufferedReader(snapshotPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(" ", 2);
                storageEngine.put(parts[0], parts[1]);
                recovered++;
            }
        }
        return recovered;
    }
}