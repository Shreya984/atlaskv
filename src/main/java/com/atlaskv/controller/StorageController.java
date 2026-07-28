package com.atlaskv.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atlaskv.model.PutRequest;
import com.atlaskv.model.ValueResponse;
import com.atlaskv.service.StorageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/store")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PutMapping
    public ResponseEntity<Void> put(@Valid @RequestBody PutRequest request) {

        storageService.put(request.getKey(), request.getValue());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<ValueResponse> get(@PathVariable String key) {

        String value = storageService.get(key);

        return ResponseEntity.ok(new ValueResponse(value));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {

        storageService.delete(key);

        return ResponseEntity.noContent().build();
    }
}