package com.atlaskv.persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppendOnlyLog implements PersistenceStrategy {

    private final Path logPath;
    private boolean crashAfterPartialWrite = false;

    /**
     * Spring constructor.
     */
    @Autowired
    public AppendOnlyLog(@Value("${atlaskv.persistence.file}") String logFile) throws IOException {
        this(Path.of(logFile));
    }

    /**
     * Constructor used by unit tests.
     */
    public AppendOnlyLog(Path logPath) throws IOException {

        this.logPath = logPath;

        if (Files.notExists(logPath)) {
            Files.createFile(logPath);
        }
    }

    /**
     * Appends a PUT operation.
     */
    @Override
    public void appendPut(String key, String value) throws IOException {

        appendLine(
                "PUT " + escape(key) + " " + escape(value)
        );

    }

    /**
     * Appends a DELETE operation.
     */
    @Override
    public void appendDelete(String key) throws IOException {

        appendLine(
                "DELETE " + escape(key)
        );

    }

    /**
     * Writes a single line and flushes immediately.
     */
    private void appendLine(String line) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(logPath, StandardOpenOption.APPEND)) {

            if(crashAfterPartialWrite) {
                int midpoint = line.length() / 2;
                writer.write(line.substring(0, midpoint));
                writer.flush();
                throw new RuntimeException("Injected crash");
            }

            writer.write(line);
            writer.newLine();

            /*
             * Flush to the operating system.
             *
             * This guarantees durability against process crashes,
             * but NOT against power failures or OS crashes.
             */
            writer.flush();

        }

    }

    private String escape(String value) {

        return value.replace("\\", "\\\\")
                .replace(" ", "\\ ");

    }

    public Path getLogPath() {
        return logPath;
    }

    public void truncate() throws IOException {
        Files.newBufferedWriter(logPath, StandardOpenOption.TRUNCATE_EXISTING).close();
    }

    public void enableCrashAfterPartialWrite() {
        crashAfterPartialWrite = true;
    }

}