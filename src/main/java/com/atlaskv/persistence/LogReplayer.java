package com.atlaskv.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.atlaskv.exception.KeyNotFoundException;
import com.atlaskv.storage.StorageEngine;

@Component
public class LogReplayer {

    private final StorageEngine storageEngine;
    private final AppendOnlyLog appendOnlyLog;

    @Autowired
    public LogReplayer(StorageEngine storageEngine, AppendOnlyLog appendOnlyLog) {
        this.storageEngine = storageEngine;
        this.appendOnlyLog = appendOnlyLog;
    }

    public LogReplayer(StorageEngine storageEngine, AppendOnlyLog appendOnlyLog, boolean testMode) {
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

        if (parts.length == 0) {
            return;
        }

        switch (parts[0]) {

            case "PUT" -> {

                if (parts.length != 3) {
                    return;
                }

                storageEngine.put(
                        unescape(parts[1]),
                        unescape(parts[2])
                );
            }

            case "DELETE" -> {

                if (parts.length != 2) {
                    return;
                }

                try {
                    storageEngine.delete(
                            unescape(parts[1])
                    );
                } catch (KeyNotFoundException ignored) {
                }
            }

            default -> {
                // Ignore unknown records
            }
        }
    }

    private String unescape(String value) {

        return value
                .replace("\\ ", " ")
                .replace("\\\\", "\\");
    }
}