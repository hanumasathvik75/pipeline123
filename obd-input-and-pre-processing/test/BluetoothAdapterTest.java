package com.ignitionai.obdinput.test;

import com.ignitionai.obdinput.adapter.BluetoothAdapter;
import com.ignitionai.obdinput.adapter.AdapterResult;
import com.ignitionai.obdinput.schema.SensorReading;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;

public class BluetoothAdapterTest {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;
        
        System.out.println("=== BluetoothAdapter Tests ===");
        
        if (test_elm327HandshakeAndTelemetry()) passed++; else failed++;
        
        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean test_elm327HandshakeAndTelemetry() {
        try {
            // Simulate the ELM327 device responding to:
            // 1. ATZ -> ELM327 v1.5
            // 2. ATE0 -> OK
            // 3. ATL0 -> OK
            // 4. ATS0 -> OK
            // 5. ATH1 -> OK
            // 6. 0100 -> 41 00 BE 3E B8 13
            // 7. 010C -> 41 0C 1A F8 (RPM = (26 * 256 + 248) / 4 = 1726)
            // 8. 010D -> 41 0D 32 (Speed = 50)
            // 9. 0105 -> 41 05 5A (Coolant = 90 - 40 = 50)
            // 10. 0104 -> 41 04 66 (Load = 102 * 100 / 255 = 40%)
            // 11. 0111 -> 41 11 19 (Throttle = 25 * 100 / 255 = 9.8%)
            // 12. 03 -> 43 03 01 00 00 00 00 (P0301)
            String simulatedDeviceResponses = 
                "ELM327 v1.5>" +
                "OK>" +
                "OK>" +
                "OK>" +
                "OK>" +
                "41 00 BE 3E B8 13>" +
                "41 0C 1A F8>" +
                "41 0D 32>" +
                "41 05 5A>" +
                "41 04 66>" +
                "41 11 19>" +
                "43 03 01 00 00 00 00>";
                
            InputStream mockIn = new ByteArrayInputStream(simulatedDeviceResponses.getBytes());
            ByteArrayOutputStream mockOut = new ByteArrayOutputStream();
            
            BluetoothAdapter adapter = new BluetoothAdapter("ELM327-01", "VEH-1", mockIn, mockOut);
            
            adapter.startSession("S-1");
            
            Optional<AdapterResult> res = adapter.pollNextMessage();
            if (res.isEmpty()) throw new Exception("Expected a message, got empty");
            
            AdapterResult adapterResult = res.get();
            if (adapterResult.getNormalizedMessage().getSensorReadings().size() != 5) throw new Exception("Expected 5 readings, got " + adapterResult.getNormalizedMessage().getSensorReadings().size());
            
            SensorReading rpmReading = adapterResult.getNormalizedMessage().getSensorReadings().get(0);
            if (!rpmReading.getSignalId().equals("engine_rpm")) throw new Exception("Wrong signal id");
            if (rpmReading.getValue() != 1726.0) throw new Exception("Wrong RPM value: " + rpmReading.getValue() + " expected 1726.0");
            
            if (adapterResult.getNormalizedMessage().getDtcObservations().size() != 1) throw new Exception("Expected 1 DTC");
            if (!adapterResult.getNormalizedMessage().getDtcObservations().get(0).getCode().equals("P0301")) throw new Exception("Expected P0301");
            
            if (adapterResult.getNormalizedMessage().getSequence() != 1) throw new Exception("Expected sequence 1");
            
            System.out.println("  PASS  test_elm327HandshakeAndTelemetry");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("  FAIL  test_elm327HandshakeAndTelemetry: " + e.getMessage());
            return false;
        }
    }
}
