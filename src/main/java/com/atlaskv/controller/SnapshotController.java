package com.atlaskv.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atlaskv.persistence.SnapshotPersistence;
import com.atlaskv.storage.StorageEngine;

@RestController
@RequestMapping("/snapshot")
public class SnapshotController {

    private final SnapshotPersistence snapshotPersistence;
    private final StorageEngine storageEngine;

    public SnapshotController(
            SnapshotPersistence snapshotPersistence,
            StorageEngine storageEngine
    ) {
        this.snapshotPersistence = snapshotPersistence;
        this.storageEngine = storageEngine;
    }

    @PostMapping
    public ResponseEntity<String> snapshot() throws IOException {

        snapshotPersistence.createSnapshot(storageEngine);

        return ResponseEntity.ok("Snapshot created");
    }
}