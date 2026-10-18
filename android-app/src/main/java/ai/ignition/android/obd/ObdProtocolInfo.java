package ai.ignition.android.obd;

/**
 * Stores vehicle OBD protocol information retrieved from the ELM327 adapter.
 */
public class ObdProtocolInfo {
    public final String rawProtocolName;     // From ATDP
    public final String rawProtocolNumber;   // From ATDPN
    public final String normalizedName;

    public ObdProtocolInfo(String rawProtocolName, String rawProtocolNumber) {
        this.rawProtocolName = rawProtocolName != null ? rawProtocolName.trim() : "UNSET";
        this.rawProtocolNumber = rawProtocolNumber != null ? rawProtocolNumber.trim() : "UNSET";
        this.normalizedName = normalize(this.rawProtocolName, this.rawProtocolNumber);
    }

    private static String normalize(String name, String num) {
        String n = name.toUpperCase();
        if (n.contains("AUTO") || n.contains("SEARCHING")) {
            return "AUTO-DETECTED (" + name + ")";
        }
        if (n.contains("ISO 15765-4") || n.contains("CAN")) {
            return "ISO 15765-4 (CAN)";
        }
        if (n.contains("ISO 9141-2")) {
            return "ISO 9141-2";
        }
        if (n.contains("ISO 14230-4") || n.contains("KWP")) {
            return "ISO 14230-4 (KWP2000)";
        }
        if (n.contains("SAE J1850 PWM")) {
            return "SAE J1850 PWM";
        }
        if (n.contains("SAE J1850 VPW")) {
            return "SAE J1850 VPW";
        }
        return name.isEmpty() ? "UNKNOWN" : name;
    }

    @Override
    public String toString() {
        return normalizedName + " [Number=" + rawProtocolNumber + ", Raw='" + rawProtocolName + "']";
    }
}
