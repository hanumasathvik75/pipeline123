package com.ignitionai.application;

import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.storage.ObdJsonMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Portable newline-delimited obd-input.v1 records; no Java object deserialization. */
public final class SessionFiles {
    public static List<ObdMessage> read(Path path) throws IOException {
        if (Files.size(path) > 64L * 1024 * 1024) throw new IOException("Session exceeds the 64 MB import limit");
        List<ObdMessage> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line; int number = 0;
            while ((line = reader.readLine()) != null) {
                number++; if (line.isBlank()) continue;
                try { records.add(ObdJsonMapper.deserialize(line)); }
                catch (RuntimeException e) { throw new IOException("Invalid session record at line " + number + ": " + e.getMessage(), e); }
                if (records.size() > 200000) throw new IOException("Session exceeds 200,000 records");
            }
        }
        if (records.isEmpty()) throw new IOException("Session contains no records");
        return records;
    }
    public static void write(Path path, List<ObdMessage> records) throws IOException {
        Path target = path.toAbsolutePath(); Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".session-", ".tmp");
        try {
            try (BufferedWriter w = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (ObdMessage message : records) { w.write(ObdJsonMapper.serialize(message)); w.newLine(); }
            }
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
