package com.ignitionai.obdgenerator.generator;

import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.schema.SensorReading;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Encodes generated telemetry as the raw AT-command/hex-response byte protocol a real ELM327-class
 * OBD-II adapter emits over its serial (USB/Bluetooth) link, per SAE J1979 (Mode 01 PID responses)
 * and SAE J2012 (Mode 03 DTC encoding). This does not replace the structured obd-input.v1 session
 * format the analytical pipeline consumes (see SessionFiles) -- it is a second, independent view of
 * the exact same generated values, built only so the simulator can show what a technician would
 * actually see scrolling in a real dongle's terminal, byte for byte. The PID formulas below are the
 * standard published SAE J1979 formulas, not something invented for this project.
 */
public final class Elm327FrameEncoder {
    private Elm327FrameEncoder() {}

    /** Canonical signal id -> Mode 01 PID (hex byte). Signals with no assigned PID are skipped. */
    private static final Map<String, Integer> PIDS = new LinkedHashMap<>();
    static {
        PIDS.put("calculated_engine_load", 0x04);
        PIDS.put("coolant_temperature", 0x05);
        PIDS.put("short_term_fuel_trim_b1", 0x06);
        PIDS.put("long_term_fuel_trim_b1", 0x07);
        PIDS.put("manifold_absolute_pressure", 0x0B);
        PIDS.put("engine_rpm", 0x0C);
        PIDS.put("vehicle_speed", 0x0D);
        PIDS.put("timing_advance", 0x0E);
        PIDS.put("intake_air_temperature", 0x0F);
        PIDS.put("mass_airflow_rate", 0x10);
        PIDS.put("throttle_position", 0x11);
        PIDS.put("barometric_pressure", 0x33);
        PIDS.put("control_module_voltage", 0x42);
        PIDS.put("oil_temperature", 0x5C);
    }

    public static Integer pidFor(String canonicalSignalId) { return PIDS.get(canonicalSignalId); }

    /** ">0100\r" style request line a scan tool would send for one PID. */
    public static String requestLine(int pid) { return String.format(Locale.ROOT, ">01%02X", pid); }

    /** "41 0C 1A F8\r" style response line: mode 01 ECU reply carrying the PID and its data bytes. */
    public static String responseLine(String canonicalSignalId, double value) {
        Integer pid = PIDS.get(canonicalSignalId);
        if (pid == null) return null;
        int[] bytes = encodeBytes(canonicalSignalId, value);
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "41 %02X", pid));
        for (int b : bytes) sb.append(String.format(Locale.ROOT, " %02X", b));
        return sb.toString();
    }

    /** Inverse of the SAE J1979 decode formula for each supported PID -- clamped to the byte(s)'
     * valid range the same way real ECU firmware would saturate rather than overflow. */
    private static int[] encodeBytes(String signalId, double value) {
        switch (signalId) {
            case "calculated_engine_load":
            case "throttle_position":
                return new int[]{ clampByte(Math.round(value * 255.0 / 100.0)) };
            case "coolant_temperature":
            case "intake_air_temperature":
            case "oil_temperature":
                return new int[]{ clampByte(Math.round(value + 40)) };
            case "short_term_fuel_trim_b1":
            case "long_term_fuel_trim_b1":
                return new int[]{ clampByte(Math.round(value * 128.0 / 100.0 + 128)) };
            case "manifold_absolute_pressure":
            case "barometric_pressure":
                return new int[]{ clampByte(Math.round(value)) };
            case "engine_rpm": {
                int ab = clampWord(Math.round(value * 4));
                return new int[]{ ab >> 8, ab & 0xFF };
            }
            case "vehicle_speed":
                return new int[]{ clampByte(Math.round(value)) };
            case "timing_advance":
                return new int[]{ clampByte(Math.round((value + 64) * 2)) };
            case "mass_airflow_rate": {
                int ab = clampWord(Math.round(value * 100));
                return new int[]{ ab >> 8, ab & 0xFF };
            }
            case "control_module_voltage": {
                int ab = clampWord(Math.round(value * 1000));
                return new int[]{ ab >> 8, ab & 0xFF };
            }
            default:
                throw new IllegalArgumentException("No byte encoding for " + signalId);
        }
    }

    /** Canonical signal id for a Mode 01 PID, or null if unsupported (inverse of {@link #pidFor}). */
    public static String signalFor(int pid) {
        for (Map.Entry<String, Integer> e : PIDS.entrySet()) if (e.getValue() == pid) return e.getKey();
        return null;
    }

    /** SAE J1979 decode formula for each supported PID -- the exact inverse of {@link #encodeBytes},
     * used to turn a real (or emulated) adapter's raw response bytes back into a physical value. */
    public static double decodeValue(String signalId, int[] bytes) {
        int a = bytes[0];
        switch (signalId) {
            case "calculated_engine_load":
            case "throttle_position":
                return a * 100.0 / 255.0;
            case "coolant_temperature":
            case "intake_air_temperature":
            case "oil_temperature":
                return a - 40.0;
            case "short_term_fuel_trim_b1":
            case "long_term_fuel_trim_b1":
                return (a - 128) * 100.0 / 128.0;
            case "manifold_absolute_pressure":
            case "barometric_pressure":
                return a;
            case "engine_rpm":
                return ((256.0 * a) + bytes[1]) / 4.0;
            case "vehicle_speed":
                return a;
            case "timing_advance":
                return a / 2.0 - 64;
            case "mass_airflow_rate":
                return ((256.0 * a) + bytes[1]) / 100.0;
            case "control_module_voltage":
                return ((256.0 * a) + bytes[1]) / 1000.0;
            default:
                throw new IllegalArgumentException("No byte decoding for " + signalId);
        }
    }

    /** Unit string for a canonical signal id, matching what {@link ObdGenerator} scenarios use. */
    public static String unitFor(String signalId) {
        switch (signalId) {
            case "engine_rpm": return "rpm";
            case "vehicle_speed": return "km/h";
            case "coolant_temperature": case "intake_air_temperature": case "oil_temperature": return "Cel";
            case "calculated_engine_load": case "throttle_position":
            case "short_term_fuel_trim_b1": case "long_term_fuel_trim_b1": return "%";
            case "manifold_absolute_pressure": case "barometric_pressure": return "kPa";
            case "timing_advance": return "deg";
            case "mass_airflow_rate": return "g/s";
            case "control_module_voltage": return "V";
            default: throw new IllegalArgumentException("No unit for " + signalId);
        }
    }

    /** SAE J2012 DTC decoding: the inverse of {@link #encodeDtc}, turning 2 raw bytes back into a
     * 5-character code such as "P0301". */
    public static String decodeDtc(int highByte, int lowByte) {
        int packed = (highByte << 8) | lowByte;
        char category = "PCBU".charAt((packed >> 14) & 0x3);
        int digit2 = (packed >> 12) & 0x3;
        int digit3 = (packed >> 8) & 0xF;
        int digit4 = (packed >> 4) & 0xF;
        int digit5 = packed & 0xF;
        return String.format(Locale.ROOT, "%c%d%X%X%X", category, digit2, digit3, digit4, digit5);
    }

    private static int clampByte(long v) { return (int) Math.max(0, Math.min(255, v)); }
    private static int clampWord(long v) { return (int) Math.max(0, Math.min(65535, v)); }

    /** SAE J2012 DTC encoding: 5 characters (e.g. "P0301") pack into 2 bytes. First character's
     * category (P/C/B/U) occupies the top 2 bits, the rest are nibble-per-character hex digits. */
    public static int[] encodeDtc(String code) {
        if (code == null || code.length() != 5) throw new IllegalArgumentException("DTC must be 5 characters: " + code);
        int category = "PCBU".indexOf(Character.toUpperCase(code.charAt(0)));
        if (category < 0) throw new IllegalArgumentException("Unknown DTC category: " + code);
        int digit2 = Character.digit(code.charAt(1), 16);
        int digit3 = Character.digit(code.charAt(2), 16);
        int digit4 = Character.digit(code.charAt(3), 16);
        int digit5 = Character.digit(code.charAt(4), 16);
        int packed = (category << 14) | (digit2 << 12) | (digit3 << 8) | (digit4 << 4) | digit5;
        return new int[]{ (packed >> 8) & 0xFF, packed & 0xFF };
    }

    /** "43 01 03 01\r" style Mode 03 response: count byte followed by each DTC's 2 encoded bytes. */
    public static String dtcResponseLine(java.util.List<String> codes) {
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "43 %02X", codes.size()));
        for (String code : codes) { int[] b = encodeDtc(code); sb.append(String.format(Locale.ROOT, " %02X %02X", b[0], b[1])); }
        return sb.toString();
    }

    /** True if this message carries at least one signal with a known PID encoding. */
    public static boolean hasEncodableReadings(ObdMessage message) {
        for (SensorReading r : message.getSensorReadings()) if (PIDS.containsKey(r.getSignalId())) return true;
        return false;
    }
}
