package com.ignitionai.bms;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses one "processed" BMS CSV export (the format seen under processed_data/&lt;month&gt;) into
 * an ordered list of {@link BmsReading}. Column position is resolved from the header row every
 * time rather than assumed, since the June file has ~112 columns and the April file has an extra
 * block of placeholder Var96..Var120 columns the others don't -- position-based parsing would
 * silently misread one against the other.
 *
 * Known, deliberately NOT worked around here: the "timestamp" column is corrupted to a
 * "1900-01-01" placeholder in most of the April and all of the May files. Rows where it fails to
 * parse keep timestamp == null; callers that need real chronological order (the cross-session
 * trend) must skip or otherwise handle those files rather than trust a fabricated date.
 */
public final class BmsCsvReader {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private BmsCsvReader() {}

    public static List<BmsReading> read(Path csvFile) throws IOException {
        List<BmsReading> out = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) return out;
            Map<String, Integer> col = indexHeader(headerLine);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                out.add(parseRow(parts, col));
            }
        }
        return out;
    }

    private static Map<String, Integer> indexHeader(String headerLine) {
        String[] headers = headerLine.split(",", -1);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headers.length; i++) col.put(headers[i].trim(), i);
        return col;
    }

    private static BmsReading parseRow(String[] parts, Map<String, Integer> col) {
        LocalDateTime timestamp = parseTimestamp(field(parts, col, "timestamp"));
        Double[] cellVoltages = new Double[14];
        for (int i = 0; i < 14; i++) cellVoltages[i] = parseDouble(field(parts, col, "Cell " + (i + 1) + " Voltage (V)"));
        return new BmsReading(
            timestamp,
            parseDouble(field(parts, col, "Pack Voltage (V)")),
            parseDouble(field(parts, col, "Pack Current (A)")),
            cellVoltages,
            parseDouble(field(parts, col, "Cell Temp 1 (C)")),
            parseDouble(field(parts, col, "Cell Temp 2 (C)")),
            parseDouble(field(parts, col, "Cell Temp 3 (C)")),
            parseDouble(field(parts, col, "FET Temp (C)")),
            parseDouble(field(parts, col, "Derated Charge Current (A)")),
            parseDouble(field(parts, col, "Derated Discharge Current (A)")),
            parseDouble(field(parts, col, "SoC EKF (%)")),
            parseDouble(field(parts, col, "SoC Coulomb Count (%)")),
            parseDouble(field(parts, col, "Voltage Imbalance (V)")),
            parseDouble(field(parts, col, "Pack SoC (%)")),
            parseDouble(field(parts, col, "Pack SoH (%)")),
            parseDouble(field(parts, col, "Motor Speed (rpm)")),
            parseDouble(field(parts, col, "Motor Temp (C)"))
        );
    }

    private static String field(String[] parts, Map<String, Integer> col, String name) {
        Integer idx = col.get(name);
        if (idx == null || idx >= parts.length) return null;
        String v = parts[idx].trim();
        return v.isEmpty() ? null : v;
    }

    private static Double parseDouble(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDateTime parseTimestamp(String s) {
        if (s == null) return null;
        try { return LocalDateTime.parse(s, TIMESTAMP_FORMAT); } catch (Exception e) { return null; }
    }
}
