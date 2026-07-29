package com.atlaskv.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.stereotype.Component;

import com.atlaskv.exception.KeyNotFoundException;
import com.atlaskv.storage.StorageEngine;

@Component
public class LogReplayer {

    private final StorageEngine storageEngine;
    private final AppendOnlyLog appendOnlyLog;

    public LogReplayer(
            StorageEngine storageEngine,
            AppendOnlyLog appendOnlyLog
    ) {
        this.storageEngine = storageEngine;
        this.appendOnlyLog = appendOnlyLog;
    }

    public int replayLog() throws IOException {

        if (Files.notExists(appendOnlyLog.getLogPath())) return 0;

        int replayed = 0;
        List<String> lines = Files.readAllLines(appendOnlyLog.getLogPath());

        for (String line : lines) {
            if (line.isBlank()) continue;
            replayLine(line);
            replayed++;
        }

        return replayed;
    }

    private void replayLine(String line) {

        String[] parts = line.split(" ", 3);

        switch (parts[0]) {

            case "PUT" -> {

                String key = unescape(parts[1]);
                String value = unescape(parts[2]);

                storageEngine.put(key, value);
            }

            case "DELETE" -> {

                String key = unescape(parts[1]);

                try {
                    storageEngine.delete(key);
                } catch (KeyNotFoundException ignored) {
                    // Ignore deletes for keys that don't exist.
                }
            }

            default ->
                    throw new IllegalArgumentException(
                            "Unknown log entry: " + line
                    );
        }
    }

    private String unescape(String value) {

        return value
                .replace("\\ ", " ")
                .replace("\\\\", "\\");
    }
}