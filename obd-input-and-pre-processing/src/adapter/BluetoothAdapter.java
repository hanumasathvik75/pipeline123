package com.ignitionai.obdinput.adapter;

import com.ignitionai.obdinput.schema.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class BluetoothAdapter implements ObdAdapter {
    
    private final String adapterId;
    private final String vehicleId;
    private final InputStream in;
    private final OutputStream out;
    
    private boolean sessionActive = false;
    private String sessionId;
    private long sessionStartTime;
    private long sessionSequenceCounter;
    
    private final List<String> supportedPids = new ArrayList<>();
    
    public BluetoothAdapter(String adapterId, String vehicleId, InputStream in, OutputStream out) {
        this.adapterId = adapterId;
        this.vehicleId = vehicleId;
        this.in = in;
        this.out = out;
    }

    @Override
    public void startSession(String sessionId) {
        this.sessionId = sessionId;
        this.sessionActive = true;
        
        try {
            // ELM327 Initialization sequence
            sendCommand("ATZ");   // Reset
            sendCommand("ATE0");  // Echo off
            sendCommand("ATL0");  // Linefeeds off
            sendCommand("ATS0");  // Spaces off
            sendCommand("ATH0");  // Headers off; ECU-specific diagnostics require a separate capture profile
            
            // Capability probe
            String capResp = sendCommand("0100");
            if (capResp.contains("41 00") || capResp.contains("4100")) {
                supportedPids.addAll(Arrays.asList("010C", "010D", "0105", "0104", "0111"));
            }
            
            sessionStartTime = System.nanoTime();
            sessionSequenceCounter = 1;
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Bluetooth adapter", e);
        }
    }

    @Override
    public Optional<AdapterResult> pollNextMessage() {
        if (!sessionActive) return Optional.empty();
        
        try {
            List<SensorReading> readings = new ArrayList<>();
            List<DtcObservation> dtcs = new ArrayList<>();
            StringBuilder rawTranscript = new StringBuilder();
            
            long msgMonotonicMs = (System.nanoTime() - sessionStartTime) / 1_000_000L;
            
            // 1. RPM
            String rpmResp = sendCommand("010C");
            rawTranscript.append("010C:").append(rpmResp).append(";");
            if (rpmResp.contains("41 0C") || rpmResp.contains("410C")) {
                String clean = rpmResp.replace(" ", "");
                int idx = clean.indexOf("410C");
                if (idx != -1 && clean.length() >= idx + 8) {
                    double rpm = ((Integer.parseInt(clean.substring(idx+4, idx+6), 16) * 256.0) + Integer.parseInt(clean.substring(idx+6, idx+8), 16)) / 4.0;
                    readings.add(buildReading("engine_rpm", "0C", rpm, "rpm", msgMonotonicMs, rpmResp));
                }
            }
            
            // 2. Speed
            String speedResp = sendCommand("010D");
            rawTranscript.append("010D:").append(speedResp).append(";");
            if (speedResp.contains("41 0D") || speedResp.contains("410D")) {
                String clean = speedResp.replace(" ", "");
                int idx = clean.indexOf("410D");
                if (idx != -1 && clean.length() >= idx + 6) {
                    double speed = Integer.parseInt(clean.substring(idx+4, idx+6), 16);
                    readings.add(buildReading("vehicle_speed", "0D", speed, "km/h", msgMonotonicMs, speedResp));
                }
            }
            
            // 3. Coolant
            String coolantResp = sendCommand("0105");
            rawTranscript.append("0105:").append(coolantResp).append(";");
            if (coolantResp.contains("41 05") || coolantResp.contains("4105")) {
                String clean = coolantResp.replace(" ", "");
                int idx = clean.indexOf("4105");
                if (idx != -1 && clean.length() >= idx + 6) {
                    double coolant = Integer.parseInt(clean.substring(idx+4, idx+6), 16) - 40.0;
                    readings.add(buildReading("coolant_temperature", "05", coolant, "Cel", msgMonotonicMs, coolantResp));
                }
            }
            
            // 4. Load
            String loadResp = sendCommand("0104");
            rawTranscript.append("0104:").append(loadResp).append(";");
            if (loadResp.contains("41 04") || loadResp.contains("4104")) {
                String clean = loadResp.replace(" ", "");
                int idx = clean.indexOf("4104");
                if (idx != -1 && clean.length() >= idx + 6) {
                    double load = Integer.parseInt(clean.substring(idx+4, idx+6), 16) * 100.0 / 255.0;
                    readings.add(buildReading("calculated_engine_load", "04", load, "%", msgMonotonicMs, loadResp));
                }
            }
            
            // 5. Throttle
            String throttleResp = sendCommand("0111");
            rawTranscript.append("0111:").append(throttleResp).append(";");
            if (throttleResp.contains("41 11") || throttleResp.contains("4111")) {
                String clean = throttleResp.replace(" ", "");
                int idx = clean.indexOf("4111");
                if (idx != -1 && clean.length() >= idx + 6) {
                    double throttle = Integer.parseInt(clean.substring(idx+4, idx+6), 16) * 100.0 / 255.0;
                    readings.add(buildReading("throttle_position", "11", throttle, "%", msgMonotonicMs, throttleResp));
                }
            }
            
            // 6. DTCs
            String dtcResp = sendCommand("03");
            rawTranscript.append("03:").append(dtcResp).append(";");
            boolean completeDtcScan = false;
            String cleanedDtc = dtcResp.replaceAll("\\s+", "");
            if (cleanedDtc.startsWith("43") && cleanedDtc.substring(2).matches("(?:[0-9A-Fa-f]{4})*")) {
                completeDtcScan = true;
                for (int i=2; i+4<=cleanedDtc.length(); i+=4) {
                    int packed = Integer.parseInt(cleanedDtc.substring(i,i+4),16);
                    if (packed == 0) continue;
                    String code = "PCBU".charAt((packed >> 14) & 3) + String.format(Locale.ROOT, "%04X", packed & 0x3fff);
                    dtcs.add(DtcObservation.builder().code(code).status(DtcStatus.CONFIRMED)
                        .observationType(ObservationType.SNAPSHOT).observedAt(Instant.now()).monotonicMs(msgMonotonicMs)
                        .provenance(new DtcProvenance("03", dtcResp)).build());
                }
            }

            if (readings.isEmpty() && dtcs.isEmpty() && rawTranscript.toString().contains("NO DATA")) {
                return Optional.empty(); // Device stopped responding
            }
            
            ObdMessage.Builder builder = ObdMessage.builder()
                .messageId("BLU-" + UUID.randomUUID().toString())
                .sessionId(sessionId)
                .sequence(sessionSequenceCounter++)
                .messageType(MessageType.TELEMETRY)
                .sourceType(SourceType.VEHICLE)
                .emittedAt(Instant.now())
                .receivedAt(Instant.now())
                .vehicleRef(new VehicleRef(vehicleId, null, IdentityStatus.UNKNOWN))
                .rawProvenance(new Provenance(adapterId, "1.0", "multi-poll", null));
                
            if (!readings.isEmpty()) builder.sensorReadings(readings);
            if (completeDtcScan) {
                builder.dtcObservations(dtcs);
                builder.dtcSnapshotCompleteness(DtcSnapshotCompleteness.COMPLETE);
            }
                
            ObdMessage msg = builder.build();
            String rawPayload = "{\"transcript\":" + com.ignitionai.obdinput.storage.ObdJsonMapper.quote(rawTranscript.toString()) + "}";
            return Optional.of(new AdapterResult(msg, rawPayload));
            
        } catch (Exception e) {
            System.err.println("Adapter poll error: " + e.getMessage());
        }
        
        return Optional.empty();
    }

    @Override
    public void endSession() {
        this.sessionActive = false;
    }
    
    private String sendCommand(String cmd) throws IOException {
        out.write((cmd + "\r").getBytes());
        out.flush();
        
        StringBuilder sb = new StringBuilder();
        int b;
        boolean prompt = false;
        while ((b = in.read()) != -1) {
            char c = (char) b;
            if (c == '>') {
                prompt = true; // ELM327 prompt
                break;
            }
            if (sb.length() > 16384) throw new IOException("Adapter response exceeds limit");
            if (Thread.currentThread().isInterrupted()) throw new IOException("Capture cancelled");
            if (c != '\r' && c != '\n') {
                sb.append(c);
            }
        }
        if (!prompt) throw new IOException("Adapter disconnected before response prompt");
        return sb.toString().trim();
    }
    
    private SensorReading buildReading(String signalId, String pid, double value, String unit, long monotonicMs, String rawResp) {
        return SensorReading.builder()
            .signalId(signalId)
            .pidOrDid(pid)
            .value(value)
            .unit(unit)
            .measuredAt(null)
            .measurementTimeBasis(MeasurementTimeBasis.PHONE_RECEIVE)
            .monotonicMs((System.nanoTime() - sessionStartTime) / 1_000_000L)
            .receivedAt(Instant.now())
            .quality(new Quality(QualityStatus.VALID, rawResp, 0))
            .provenance(new SignalProvenance(pid, rawResp))
            .build();
    }
}
