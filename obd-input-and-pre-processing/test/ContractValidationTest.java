package com.ignitionai.obdinput.test;

import com.ignitionai.obdinput.schema.*;
import com.ignitionai.obdinput.storage.ObdJsonMapper;
import com.ignitionai.obdinput.storage.MiniJson;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ContractValidationTest {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;
        
        System.out.println("=== Contract Validation Tests ===");
        
        if (test_strictContractValidation()) passed++; else failed++;
        
        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean test_strictContractValidation() {
        try {
            Instant now = Instant.parse("2026-09-17T10:00:00Z");
            
            CapabilitySnapshot cap = CapabilitySnapshot.builder()
                .supportedSignals(List.of("engine_rpm"))
                .supportedDtcServices(List.of("03"))
                .ecuIds(List.of("ECM"))
                .requestedRateHz(Map.of("engine_rpm", 1.0))
                .observedRateHz(Map.of("engine_rpm", 0.98))
                .capabilitySource(CapabilitySource.VEHICLE_QUERY)
                .observedAt(now)
                .build();
            
            SensorReading reading = SensorReading.builder()
                .signalId("engine_rpm")
                .pidOrDid("0C")
                .ecuId("ECM")
                .value(812.0)
                .unit("rpm")
                .measuredAt(now)
                .measurementTimeBasis(MeasurementTimeBasis.PHONE_RECEIVE)
                .monotonicMs(1250)
                .receivedAt(now)
                .quality(new Quality(QualityStatus.VALID, "response", 8) {
                    @Override public boolean isInterpolated() { return true; }
                })
                .provenance(new SignalProvenance("01", "ref1"))
                .build();
                
            DtcObservation dtc = DtcObservation.builder()
                .code("P0301")
                .ecuId("ECM")
                .status(DtcStatus.CONFIRMED)
                .observationType(ObservationType.SNAPSHOT)
                .observedAt(now)
                .monotonicMs(1250)
                .firstSeenAt(now)
                .lastSeenAt(now)
                .occurrenceCount(1)
                .freezeFrameRef("FF-0001")
                .rawStatusByte(15)
                .provenance(new DtcProvenance("03", "ref2"))
                .build();

            ObdMessage msg = ObdMessage.builder()
                .messageId("MSG-FULL")
                .sessionId("SES-FULL")
                .sequence(1)
                .messageType(MessageType.TELEMETRY)
                .sourceType(SourceType.VEHICLE)
                .emittedAt(now)
                .receivedAt(now)
                .dtcSnapshotCompleteness(DtcSnapshotCompleteness.COMPLETE)
                .vehicleRef(new VehicleRef("VEH-1", "VIN123", IdentityStatus.VERIFIED))
                .capabilitySnapshot(cap)
                .sensorReadings(List.of(reading))
                .dtcObservations(List.of(dtc))
                .rawProvenance(new Provenance("adapter", "1.0", "raw_ref", 12345L))
                .build();

            String jsonString = ObdJsonMapper.serialize(msg);
            Map<String, Object> json = (Map<String, Object>) MiniJson.parse(jsonString);
            
            // Check top level
            if (!json.containsKey("schema_version") || !json.get("schema_version").equals("obd-input.v1")) throw new Exception("schema_version missing/wrong");
            if (!json.get("dtc_snapshot_completeness").equals("complete")) throw new Exception("dtc_snapshot_completeness missing/wrong");
            if (!json.containsKey("capability_snapshot")) throw new Exception("capability_snapshot missing");
            
            // Check capabilities
            Map<String, Object> capJson = (Map<String, Object>) json.get("capability_snapshot");
            if (!capJson.get("capability_source").equals("vehicle_query")) throw new Exception("capability_source wrong");
            
            // Check reading
            List<Map<String, Object>> readings = (List<Map<String, Object>>) json.get("sensor_readings");
            if (readings.isEmpty()) throw new Exception("sensor_readings empty");
            Map<String, Object> readingJson = readings.get(0);
            if (!readingJson.get("measurement_time_basis").equals("phone_receive")) throw new Exception("measurement_time_basis wrong");
            Map<String, Object> qualityJson = (Map<String, Object>) readingJson.get("quality");
            if (qualityJson.get("interpolated") == null || !(Boolean)qualityJson.get("interpolated")) throw new Exception("quality.interpolated wrong");
            
            // Check DTC
            List<Map<String, Object>> dtcs = (List<Map<String, Object>>) json.get("dtc_observations");
            if (dtcs.isEmpty()) throw new Exception("dtc_observations empty");
            Map<String, Object> dtcJson = dtcs.get(0);
            if (!dtcJson.get("first_seen_at").equals("2026-09-17T10:00:00Z")) throw new Exception("first_seen_at wrong");
            if (((Number)dtcJson.get("raw_status_byte")).intValue() != 15) throw new Exception("raw_status_byte wrong");
            
            System.out.println("  PASS  test_strictContractValidation");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("  FAIL  test_strictContractValidation: " + e.getMessage());
            return false;
        }
    }
}
