package com.example.pdfcompressor.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe in-memory holding area for generated/processed files.
 * Provides temporary token-based download storage with automatic
 * eviction after 30 minutes.
 */
@Service
public class FileStorageService {

    public record StoredFile(String filename, String contentType, byte[] data, Instant createdAt) {
    }

    private final Map<String, StoredFile> store = new ConcurrentHashMap<>();

    public FileStorageService() {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "file-storage-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::evictExpired, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Stores file bytes with a random UUID and returns the ID.
     */
    public String store(String filename, String contentType, byte[] data) {
        String id = UUID.randomUUID().toString();
        store.put(id, new StoredFile(filename, contentType, data, Instant.now()));
        return id;
    }

    /**
     * Retrieves stored file by token ID.
     */
    public StoredFile get(String id) {
        if (id == null) {
            return null;
        }
        return store.get(id);
    }

    /**
     * Removes file manually after download or on demand.
     */
    public StoredFile remove(String id) {
        return store.remove(id);
    }

    private void evictExpired() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(30));
        store.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
    }
}
