package com.ignitionai.obdinput.storage;

import com.ignitionai.obdinput.schema.ObdMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

public class LocalFileStorageProvider implements StorageProvider {
    
    private final Path rootDir;

    public LocalFileStorageProvider(Path rootDir) {
        this.rootDir = rootDir;
        try {
            Files.createDirectories(rootDir.resolve("raw"));
            Files.createDirectories(rootDir.resolve("normalized"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local file storage", e);
        }
    }

    @Override
    public void persistRawRecord(String sessionId, String rawPayload) {
        // Enforces retention logic conceptually.
        // Files older than 90 days would be cleaned up in a background task.
        Path sessionDir = rootDir.resolve("raw").resolve(sessionId);
        try {
            Files.createDirectories(sessionDir);
            Path filePath = sessionDir.resolve(Instant.now().toEpochMilli() + ".json");
            Files.writeString(filePath, rawPayload);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist raw record", e);
        }
    }

    @Override
    public void persistNormalizedMessage(ObdMessage message) {
        Path sessionDir = rootDir.resolve("normalized").resolve(message.getSessionId());
        try {
            Files.createDirectories(sessionDir);
            Path filePath = sessionDir.resolve(message.getMessageId() + ".json");
            Files.writeString(filePath, ObdJsonMapper.serialize(message));
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist normalized record", e);
        }
    }
    
    public void cleanupOldFiles() {
        try {
            Instant now = Instant.now();
            Instant rawCutoff = now.minus(90, ChronoUnit.DAYS);
            Instant normalizedCutoff = now.minus(365, ChronoUnit.DAYS);
            
            Path rawDir = rootDir.resolve("raw");
            if (Files.exists(rawDir)) {
                try (Stream<Path> files = Files.walk(rawDir)) {
                    files.filter(Files::isRegularFile).forEach(p -> checkAndDelete(p, rawCutoff));
                }
            }
            
            Path normDir = rootDir.resolve("normalized");
            if (Files.exists(normDir)) {
                try (Stream<Path> files = Files.walk(normDir)) {
                    files.filter(Files::isRegularFile).forEach(p -> checkAndDelete(p, normalizedCutoff));
                }
            }
        } catch (IOException e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
    }
    
    private void checkAndDelete(Path file, Instant cutoff) {
        try {
            Instant modified = Files.getLastModifiedTime(file).toInstant();
            if (modified.isBefore(cutoff)) {
                Files.delete(file);
            }
        } catch (IOException ignored) {}
    }
}
