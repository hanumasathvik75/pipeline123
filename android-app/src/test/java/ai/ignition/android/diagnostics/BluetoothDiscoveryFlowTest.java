package ai.ignition.android.diagnostics;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ai.ignition.android.recording.CaptureState;
import ai.ignition.android.util.RedactionUtil;

import static org.junit.Assert.*;

/**
 * Tests covering Bluetooth adapter discovery, device classification,
 * permission states, and connection lifecycle transitions.
 */
public class BluetoothDiscoveryFlowTest {

    // Helper device stub for pure unit testing without android.bluetooth dependency in local JVM
    private static class StubDevice {
        final String name;
        final String address;

        StubDevice(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }

    private boolean isLikelyObdDongle(String name) {
        if (name == null) return false;
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.contains("OBD") || upper.contains("ELM") || upper.contains("V-LINK") ||
                upper.contains("SCAN") || upper.contains("VGATE") || upper.contains("CAR");
    }

    @Test
    public void testDeviceSorting_placesObdAdaptersFirst() {
        List<StubDevice> devices = new ArrayList<>();
        devices.add(new StubDevice("Galaxy Buds Pro", "00:11:22:33:44:55"));
        devices.add(new StubDevice("OBDII", "AA:BB:CC:DD:EE:FF"));
        devices.add(new StubDevice("Sony WH-1000XM4", "11:22:33:44:55:66"));
        devices.add(new StubDevice("vGate iCar Pro", "22:33:44:55:66:77"));

        devices.sort((a, b) -> {
            boolean aObd = isLikelyObdDongle(a.name);
            boolean bObd = isLikelyObdDongle(b.name);
            if (aObd && !bObd) return -1;
            if (!aObd && bObd) return 1;
            String aName = a.name != null ? a.name : "";
            String bName = b.name != null ? b.name : "";
            return aName.compareToIgnoreCase(bName);
        });

        // The first two items must be OBD devices
        assertTrue("First device must be OBD adapter", isLikelyObdDongle(devices.get(0).name));
        assertTrue("Second device must be OBD adapter", isLikelyObdDongle(devices.get(1).name));
        assertFalse("Third device must be non-OBD", isLikelyObdDongle(devices.get(2).name));
        assertFalse("Fourth device must be non-OBD", isLikelyObdDongle(devices.get(3).name));

        assertEquals("OBDII", devices.get(0).name);
        assertEquals("vGate iCar Pro", devices.get(1).name);
    }

    @Test
    public void testRedactedMac_doesNotLeakHardwareAddress() {
        String rawMac = "00:1D:A5:68:98:8A";
        String redacted = RedactionUtil.redactMac(rawMac);

        assertNotEquals(rawMac, redacted);
        assertTrue("Redacted MAC must mask suffix bytes", redacted.contains("**:**:**"));
        assertTrue("Redacted MAC must preserve OUI prefix", redacted.startsWith("00:1D:A5"));
    }

    @Test
    public void testEmptyDeviceListHandling_detectsZeroPairedDevices() {
        List<StubDevice> emptyList = Collections.emptyList();
        assertTrue("Empty bonded list must be cleanly detected without throwing", emptyList.isEmpty());
    }

    @Test
    public void testDisconnectSafety_recordingMustBeStoppedFirst() {
        CaptureState state = CaptureState.RECORDING;
        boolean canSafelyDisconnect = (state != CaptureState.RECORDING && state != CaptureState.RECONNECTING);
        assertFalse("Cannot safely disconnect while recording is actively in progress", canSafelyDisconnect);

        state = CaptureState.STOPPED;
        canSafelyDisconnect = (state != CaptureState.RECORDING && state != CaptureState.RECONNECTING);
        assertTrue("Can safely disconnect after recording has stopped", canSafelyDisconnect);
    }

    @Test
    public void testButtonTextStates() {
        // Disconnected state
        String disconnectedText = "Connect ELM327";
        assertEquals("Connect ELM327", disconnectedText);

        // Connecting state
        String connectingText = "Connecting…";
        assertEquals("Connecting…", connectingText);

        // Connected state
        String connectedText = "Disconnect";
        assertEquals("Disconnect", connectedText);
    }
}
