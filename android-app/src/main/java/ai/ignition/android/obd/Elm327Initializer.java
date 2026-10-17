package ai.ignition.android.obd;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

/**
 * Executes standard ELM327 initialization and collects adapter identity and vehicle protocol.
 */
public class Elm327Initializer {

    private static final Duration INIT_TIMEOUT = Duration.ofSeconds(4);

    public static class InitResult {
        public final String resetResponse;      // ATZ
        public final String adapterIdentity;    // ATI
        public final String echoResponse;       // ATE0
        public final String linefeedResponse;   // ATL0
        public final String spacesResponse;     // ATS0
        public final boolean success;
        public final String errorMessage;

        public InitResult(String resetResponse, String adapterIdentity, String echoResponse,
                          String linefeedResponse, String spacesResponse, boolean success, String errorMessage) {
            this.resetResponse = resetResponse;
            this.adapterIdentity = adapterIdentity;
            this.echoResponse = echoResponse;
            this.linefeedResponse = linefeedResponse;
            this.spacesResponse = spacesResponse;
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Initializes the ELM327 adapter with AT commands sequentially.
     */
    public static InitResult initialize(ObdTransport transport) {
        String atz = "", ati = "", ate0 = "", atl0 = "", ats0 = "";
        try {
            // 1. ATZ Reset
            atz = transport.exchange("ATZ", Duration.ofSeconds(3));
            try {
                Thread.sleep(800); // Allow adapter micro-controller to complete internal reboot
            } catch (InterruptedException ignored) {}

            // Check if ATZ produced error
            if (isErrorToken(atz)) {
                return new InitResult(atz, ati, ate0, atl0, ats0, false, "Adapter reset failed: " + atz);
            }

            // 2. ATI Identification
            ati = transport.exchange("ATI", INIT_TIMEOUT);
            if (isErrorToken(ati)) {
                return new InitResult(atz, ati, ate0, atl0, ats0, false, "Adapter rejected identity query (ATI): " + ati);
            }

            // 3. ATE0 Echo off
            ate0 = transport.exchange("ATE0", INIT_TIMEOUT);

            // 4. ATL0 Linefeeds off
            atl0 = transport.exchange("ATL0", INIT_TIMEOUT);

            // 5. ATS0 Spaces off
            ats0 = transport.exchange("ATS0", INIT_TIMEOUT);

            return new InitResult(atz, ati, ate0, atl0, ats0, true, null);
        } catch (IOException e) {
            return new InitResult(atz, ati, ate0, atl0, ats0, false, "Initialization failed: " + e.getMessage());
        }
    }

    private static boolean isErrorToken(String resp) {
        if (resp == null || resp.isBlank()) return true;
        String u = resp.toUpperCase(Locale.ROOT);
        return u.equals("?") || u.contains("UNABLE TO CONNECT") || u.contains("BUS INIT: ... ERROR")
                || u.contains("CAN ERROR") || u.contains("ERR") || u.contains("STOPPED");
    }

    /**
     * Queries vehicle protocol via ATDP and ATDPN.
     * Should be called after at least one OBD query (such as 0100) has established vehicle bus sync.
     */
    public static ObdProtocolInfo queryProtocol(ObdTransport transport) {
        String atdp = "UNSET";
        String atdpn = "UNSET";
        try {
            atdp = transport.exchange("ATDP", INIT_TIMEOUT);
        } catch (Exception ignored) {}
        try {
            atdpn = transport.exchange("ATDPN", INIT_TIMEOUT);
        } catch (Exception ignored) {}

        return new ObdProtocolInfo(atdp, atdpn);
    }
}
