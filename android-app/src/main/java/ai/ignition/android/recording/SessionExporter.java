package ai.ignition.android.recording;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Streams local drive session files (JSONL, manifest, or combined ZIP)
 * to Android Storage Access Framework (SAF) destinations or arbitrary output streams.
 *
 * Uses bounded buffer streaming (8 KB) to prevent memory exhaustion on large drive recordings.
 * Strictly read-only on source files: guarantees no modification or deletion of raw session data.
 */
public final class SessionExporter {

    private static final int BUFFER_SIZE = 8192;

    private SessionExporter() {}

    /**
     * Streams the session.jsonl file to the specified destination output stream.
     *
     * @param jsonlFile Source JSONL file.
     * @param destination Destination output stream.
     * @return Number of bytes written.
     * @throws IOException If file is missing or stream operations fail.
     */
    public static long exportJsonl(File jsonlFile, OutputStream destination) throws IOException {
        if (jsonlFile == null || !jsonlFile.exists() || !jsonlFile.isFile()) {
            throw new FileNotFoundException("Source session.jsonl does not exist: " +
                    (jsonlFile != null ? jsonlFile.getAbsolutePath() : "null"));
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination output stream cannot be null");
        }
        return copyStream(new FileInputStream(jsonlFile), destination);
    }

    /**
     * Streams the manifest.json file to the specified destination output stream.
     *
     * @param manifestFile Source manifest.json file.
     * @param destination Destination output stream.
     * @return Number of bytes written.
     * @throws IOException If file is missing or stream operations fail.
     */
    public static long exportManifest(File manifestFile, OutputStream destination) throws IOException {
        if (manifestFile == null || !manifestFile.exists() || !manifestFile.isFile()) {
            throw new FileNotFoundException("Source manifest.json does not exist: " +
                    (manifestFile != null ? manifestFile.getAbsolutePath() : "null"));
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination output stream cannot be null");
        }
        return copyStream(new FileInputStream(manifestFile), destination);
    }

    /**
     * Creates a standard ZIP archive containing the exact original session.jsonl and manifest.json
     * (and any other files within the session folder) and streams it to the destination.
     *
     * @param sessionDir Source directory for the session.
     * @param destination Destination output stream.
     * @return Total uncompressed bytes packed into the ZIP archive.
     * @throws IOException If source files cannot be read or ZIP writing fails.
     */
    public static long exportSessionZip(File sessionDir, OutputStream destination) throws IOException {
        if (sessionDir == null || !sessionDir.exists() || !sessionDir.isDirectory()) {
            throw new FileNotFoundException("Source session directory does not exist: " +
                    (sessionDir != null ? sessionDir.getAbsolutePath() : "null"));
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination output stream cannot be null");
        }

        File[] files = sessionDir.listFiles(f -> f.isFile() && !f.getName().endsWith(".tmp"));
        if (files == null || files.length == 0) {
            throw new FileNotFoundException("Session directory has no valid files to export: " + sessionDir.getAbsolutePath());
        }

        long totalBytesPacked = 0;
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(destination))) {
            byte[] buffer = new byte[BUFFER_SIZE];

            for (File file : files) {
                ZipEntry entry = new ZipEntry(file.getName());
                entry.setTime(file.lastModified());
                entry.setSize(file.length());
                zos.putNextEntry(entry);

                try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, read);
                        totalBytesPacked += read;
                    }
                }
                zos.closeEntry();
            }
            zos.finish();
            zos.flush();
        }

        return totalBytesPacked;
    }

    /**
     * SAF export helper: streams session.jsonl to a content URI.
     */
    public static long exportJsonlToUri(Context context, SessionRepository repo, String sessionId, Uri destinationUri) throws IOException {
        File file = repo.getSessionJsonlFile(sessionId);
        try (OutputStream out = context.getContentResolver().openOutputStream(destinationUri)) {
            if (out == null) throw new IOException("Could not open output stream for URI: " + destinationUri);
            return exportJsonl(file, out);
        }
    }

    /**
     * SAF export helper: streams manifest.json to a content URI.
     */
    public static long exportManifestToUri(Context context, SessionRepository repo, String sessionId, Uri destinationUri) throws IOException {
        File file = repo.getManifestFile(sessionId);
        try (OutputStream out = context.getContentResolver().openOutputStream(destinationUri)) {
            if (out == null) throw new IOException("Could not open output stream for URI: " + destinationUri);
            return exportManifest(file, out);
        }
    }

    /**
     * SAF export helper: streams session directory as ZIP to a content URI.
     */
    public static long exportSessionZipToUri(Context context, SessionRepository repo, String sessionId, Uri destinationUri) throws IOException {
        File dir = repo.getSessionDir(sessionId);
        try (OutputStream out = context.getContentResolver().openOutputStream(destinationUri)) {
            if (out == null) throw new IOException("Could not open output stream for URI: " + destinationUri);
            return exportSessionZip(dir, out);
        }
    }

    private static long copyStream(InputStream in, OutputStream out) throws IOException {
        long totalBytes = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream bin = new BufferedInputStream(in);
             OutputStream bout = new BufferedOutputStream(out)) {
            int read;
            while ((read = bin.read(buffer)) != -1) {
                bout.write(buffer, 0, read);
                totalBytes += read;
            }
            bout.flush();
        }
        return totalBytes;
    }
}
