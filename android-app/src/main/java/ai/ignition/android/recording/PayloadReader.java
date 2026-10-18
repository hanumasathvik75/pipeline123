package ai.ignition.android.recording;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Paged, memory-bounded reader for session.jsonl files.
 * Streams lines without loading an entire multi-megabyte recording into memory.
 * Preserves raw JSONL records, null signals, and explicit NO_DATA / UNSUPPORTED / ERROR states.
 */
public final class PayloadReader {

    public static final int DEFAULT_PAGE_SIZE = 50;

    private PayloadReader() {}

    /**
     * Represents a single record in a JSONL file, containing both the exact raw JSON text
     * and the parsed SessionEvent model (or error information if malformed).
     */
    public static final class PayloadRecord {
        private final long lineNumber;
        private final String rawJson;
        private final SessionEvent event;
        private final boolean malformed;
        private final String parseError;

        public PayloadRecord(long lineNumber, String rawJson, SessionEvent event, boolean malformed, String parseError) {
            this.lineNumber = lineNumber;
            this.rawJson = rawJson;
            this.event = event;
            this.malformed = malformed;
            this.parseError = parseError;
        }

        public long getLineNumber() { return lineNumber; }
        public String getRawJson() { return rawJson; }
        public SessionEvent getEvent() { return event; }
        public boolean isMalformed() { return malformed; }
        public String getParseError() { return parseError; }

        public String getFormattedDisplay() {
            if (malformed) {
                return "[Line " + lineNumber + "] ⚠️ MALFORMED: " + parseError + "\nRaw: " + rawJson;
            }
            if (event == null) {
                return "[Line " + lineNumber + "] (Empty record)";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("#").append(event.getSequence()).append(" | ")
              .append(event.getEventType()).append(" | ")
              .append(event.getTimestamp());

            if (event.getEventType() == EventType.OBD_OBSERVATION) {
                sb.append("\n  Signal: ").append(event.getSignalName() != null ? event.getSignalName() : "Unknown");
                sb.append(" = ");
                if (event.getValue() != null) {
                    sb.append(event.getValue());
                    if (event.getUnit() != null) sb.append(" ").append(event.getUnit());
                } else {
                    sb.append("null");
                }
                sb.append(" [Status: ").append(event.getStatus() != null ? event.getStatus().name() : "NONE").append("]");
                if (event.getDetails() != null) {
                    if (event.getDetails().containsKey("raw_response")) {
                        sb.append(" [Raw: '").append(event.getDetails().get("raw_response")).append("']");
                    }
                    if (event.getDetails().containsKey("request_duration_ms")) {
                        sb.append(" [Latency: ").append(event.getDetails().get("request_duration_ms")).append("ms]");
                    }
                }
            } else if (event.getEventType() == EventType.CONNECTION_LOST || event.getEventType() == EventType.CONNECTION_RESTORED) {
                if (event.getDetails() != null && event.getDetails().containsKey("gap_duration_ms")) {
                    sb.append("\n  Gap Duration: ").append(event.getDetails().get("gap_duration_ms")).append(" ms");
                }
                if (event.getErrorMessage() != null) {
                    sb.append("\n  Reason: ").append(event.getErrorMessage());
                }
            } else if (event.getEventType() == EventType.CAPTURE_ERROR) {
                sb.append("\n  Category: ").append(event.getErrorCategory());
                sb.append("\n  Message: ").append(event.getErrorMessage());
            }

            return sb.toString();
        }
    }

    /**
     * Paged slice of records.
     */
    public static final class PayloadPage {
        private final int pageIndex;
        private final int pageSize;
        private final long totalLines;
        private final int totalPages;
        private final List<PayloadRecord> records;

        public PayloadPage(int pageIndex, int pageSize, long totalLines, List<PayloadRecord> records) {
            this.pageIndex = pageIndex;
            this.pageSize = pageSize;
            this.totalLines = totalLines;
            this.totalPages = (int) Math.max(1, (totalLines + pageSize - 1) / pageSize);
            this.records = (records != null) ? Collections.unmodifiableList(records) : Collections.emptyList();
        }

        public int getPageIndex() { return pageIndex; }
        public int getPageSize() { return pageSize; }
        public long getTotalLines() { return totalLines; }
        public int getTotalPages() { return totalPages; }
        public List<PayloadRecord> getRecords() { return records; }
        public boolean hasNextPage() { return pageIndex < totalPages - 1; }
        public boolean hasPreviousPage() { return pageIndex > 0; }
    }

    /**
     * Reads a specific page of records from a session.jsonl file.
     *
     * @param jsonlFile Source JSONL file.
     * @param pageIndex 0-indexed page number.
     * @param pageSize Number of records per page.
     * @return PayloadPage containing the records.
     * @throws IOException If file read fails.
     */
    public static PayloadPage readPage(File jsonlFile, int pageIndex, int pageSize) throws IOException {
        if (jsonlFile == null || !jsonlFile.exists() || !jsonlFile.isFile()) {
            return new PayloadPage(0, pageSize, 0, Collections.emptyList());
        }

        long totalLines = countLines(jsonlFile);
        if (totalLines == 0) {
            return new PayloadPage(0, pageSize, 0, Collections.emptyList());
        }

        int clampedPageIndex = Math.max(0, pageIndex);
        long startLine = (long) clampedPageIndex * pageSize;
        long endLine = startLine + pageSize;

        List<PayloadRecord> records = new ArrayList<>();
        long currentLineNum = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonlFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                currentLineNum++;
                if (currentLineNum <= startLine) {
                    continue;
                }
                if (currentLineNum > endLine) {
                    break;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    records.add(new PayloadRecord(currentLineNum, line, null, true, "Empty or blank line"));
                    continue;
                }

                try {
                    SessionEvent event = SessionEvent.fromJson(trimmed);
                    records.add(new PayloadRecord(currentLineNum, line, event, false, null));
                } catch (Exception e) {
                    records.add(new PayloadRecord(currentLineNum, line, null, true, e.getMessage()));
                }
            }
        }

        return new PayloadPage(clampedPageIndex, pageSize, totalLines, records);
    }

    /**
     * Counts the total number of lines in a file.
     */
    public static long countLines(File file) {
        if (file == null || !file.exists() || !file.isFile()) return 0;
        long lines = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                lines++;
            }
        } catch (IOException e) {
            return 0;
        }
        return lines;
    }
}
