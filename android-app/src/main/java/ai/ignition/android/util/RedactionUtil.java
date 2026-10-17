package ai.ignition.android.util;

import java.util.regex.Pattern;

/**
 * Utility for masking sensitive diagnostic data (full VIN, Bluetooth hardware MAC addresses)
 * in diagnostic logs and test reports.
 */
public final class RedactionUtil {

    private static final Pattern MAC_PATTERN = Pattern.compile("(?i)([0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2})[:-]([0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2})");

    private RedactionUtil() {}

    /**
     * Redacts a Bluetooth MAC address, e.g. "AA:BB:CC:11:22:33" -> "AA:BB:CC:**:**:**".
     */
    public static String redactMac(String mac) {
        if (mac == null || mac.isBlank()) return "UNSPECIFIED";
        return MAC_PATTERN.matcher(mac).replaceAll("$1:**:**:**");
    }

    /**
     * Pattern to match 17-character VINs within arbitrary sentences or error messages.
     * Preserves WMI (first 3) and last 4 characters, redacting the middle 10 characters.
     */
    private static final Pattern VIN_IN_TEXT_PATTERN = Pattern.compile("(?<![A-HJ-NPR-Za-hj-npr-z0-9])([A-HJ-NPR-Za-hj-npr-z0-9]{3})[A-HJ-NPR-Za-hj-npr-z0-9]{10}([A-HJ-NPR-Za-hj-npr-z0-9]{4})(?![A-HJ-NPR-Za-hj-npr-z0-9])");

    /**
     * Redacts any 17-character VIN appearing inside a sentence or message.
     */
    public static String redactVinInText(String text) {
        if (text == null || text.isBlank()) return text;
        return VIN_IN_TEXT_PATTERN.matcher(text).replaceAll("$1**********$2");
    }

    /**
     * Redacts a 17-character VIN, preserving the first 3 characters (WMI) and the last 4 characters,
     * masking the middle 10 characters: "1HGCR2F84HA123456" -> "1HG**********3456".
     */
    public static String redactVin(String vin) {
        if (vin == null || vin.isBlank()) return "UNAVAILABLE";
        String clean = vin.trim();
        if (clean.length() == 17) {
            return clean.substring(0, 3) + "**********" + clean.substring(13);
        }
        if (clean.length() > 6) {
            return clean.substring(0, 3) + "***" + clean.substring(clean.length() - 3);
        }
        return "REDACTED";
    }
}
