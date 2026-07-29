package com.atlaskv.persistence;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.atlaskv.storage.StorageEngine;

import jakarta.annotation.PostConstruct;

@Component
public class RecoveryManager {

    private final SnapshotLoader snapshotLoader;
    private final LogReplayer logReplayer;
    private final StorageEngine storageEngine;

    public RecoveryManager(
            SnapshotLoader snapshotLoader,
            LogReplayer logReplayer,
            StorageEngine storageEngine
    ) {
        this.snapshotLoader = snapshotLoader;
        this.logReplayer = logReplayer;
        this.storageEngine = storageEngine;
    }

    @PostConstruct
    public void recover() throws IOException {

        int snapshotKeys = snapshotLoader.loadSnapshot();
        int replayedOperations = logReplayer.replayLog();

        System.out.println("Recovery complete:");
        System.out.println("  Snapshot keys loaded   : " + snapshotKeys);
        System.out.println("  WAL operations replayed: " + replayedOperations);
        System.out.println("  Final cache size       : " + storageEngine.size());
    }
}