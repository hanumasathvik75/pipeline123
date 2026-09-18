package com.ignitionai.obdgenerator.generator;

import com.ignitionai.obdgenerator.config.GeneratorConfig;
import com.ignitionai.obdgenerator.config.ScenarioType;
import com.ignitionai.obdinput.schema.*;
import java.time.Instant;
import java.util.*;

public class ObdGenerator {

    private final GeneratorConfig config;
    private final Random random;

    public ObdGenerator(GeneratorConfig config) {
        this.config = config;
        this.random = new Random(config.getRandomSeed());
    }

    public GenerationResult generate() {
        if (config.getVehicleId() == null || config.getVehicleId().isBlank()) throw new IllegalArgumentException("Vehicle ID required");
        if (config.getDurationMs() <= 0 || config.getDurationMs() > 3_600_000) throw new IllegalArgumentException("Duration must be within one hour");
        if (!Double.isFinite(config.getMissingnessProbability()) || config.getMissingnessProbability() < 0 || config.getMissingnessProbability() > 1)
            throw new IllegalArgumentException("Missing fraction must be within 0..1");
        for (String signal : config.getSupportedSignals()) {
            double rate = config.getSamplingRatesHz().getOrDefault(signal, 1.0);
            if (!Double.isFinite(rate) || rate <= 0 || rate > 100) throw new IllegalArgumentException("Sampling rate must be >0 and <=100 Hz");
        }
        random.setSeed(config.getRandomSeed());
        Map<String, Double> nextDue = new HashMap<>();
        List<ObdMessage> publicStream = new ArrayList<>();
        List<GroundTruthRecord> groundTruth = new ArrayList<>();
        
        long currentTimeMs = 0;
        Instant startTime = Instant.parse("2026-09-17T10:00:00Z");
        String sessionId = "SIM-" + UUID.nameUUIDFromBytes(String.valueOf(config.getRandomSeed()).getBytes()).toString();
        
        long sequence = 1;
        while (currentTimeMs < config.getDurationMs()) {
            
            double coolant = 25.0;
            double rpm = 0.0;
            double speed = 0.0;
            double load = 0.0;
            double throttle = 0.0;
            
            if (config.getScenarioType() == ScenarioType.CITY_DRIVING) {
                coolant = Math.min(90.0, 25.0 + (currentTimeMs / 2000.0));
                if (currentTimeMs % 20000 < 10000) {
                    rpm = 800.0; speed = 0.0; load = 20.0; throttle = 5.0;
                } else {
                    rpm = 1800.0; speed = 30.0; load = 35.0; throttle = 12.0;
                }
            } else if (config.getScenarioType() == ScenarioType.HIGHWAY_CRUISE) {
                coolant = Math.min(90.0, 40.0 + (currentTimeMs / 1500.0));
                rpm = 2500.0; speed = 100.0; load = 50.0; throttle = 20.0;
            } else if (config.getScenarioType() == ScenarioType.HARD_ACCELERATION_WITH_FAULT) {
                coolant = Math.min(95.0, 50.0 + (currentTimeMs / 1000.0));
                if (currentTimeMs < 15000) {
                    rpm = 4000.0; speed = 60.0; load = 80.0; throttle = 50.0;
                } else {
                    rpm = 1500 + random.nextGaussian() * 300; // Fluctuating RPM due to misfire
                    speed = 40.0; load = 60.0; throttle = 30.0;
                }
            }

            List<SensorReading> readings = new ArrayList<>();
            Map<String, Double> trueValues = new HashMap<>();
            
            for (String signal : config.getSupportedSignals()) {
                double rateHz = config.getSamplingRatesHz().getOrDefault(signal, 1.0);
                double intervalMs = 1000.0 / rateHz;
                
                if (currentTimeMs + 0.00001 >= nextDue.getOrDefault(signal, 0.0)) {
                    nextDue.put(signal, nextDue.getOrDefault(signal, 0.0) + intervalMs);
                    double trueValue = 0.0;
                    String unit = "unknown";
                    
                    switch (signal) {
                        case "engine_rpm": trueValue = rpm; unit = "rpm"; break;
                        case "vehicle_speed_kph":
                        case "vehicle_speed": trueValue = speed; unit = "km/h"; break;
                        case "engine_coolant_temperature":
                        case "coolant_temperature": trueValue = coolant; unit = "Cel"; break;
                        case "calculated_engine_load": trueValue = load; unit = "%"; break;
                        case "throttle_position": trueValue = throttle; unit = "%"; break;
                        default:
                            if (!config.getSignalPrograms().containsKey(signal)) throw new IllegalArgumentException("Provide a trajectory and unit for " + signal);
                    }
                    if (config.getSignalPrograms().containsKey(signal)) {
                        trueValue = config.getSignalPrograms().get(signal).valueAt(currentTimeMs);
                        unit = config.getSignalPrograms().get(signal).getUnit();
                    }
                    
                    trueValues.put(signal, trueValue);
                    
                    if (random.nextDouble() < config.getMissingnessProbability()) {
                        continue;
                    }
                    
                    double noisedValue = trueValue;
                    
                    boolean faultActive = currentTimeMs >= config.getFaultStartMs() && currentTimeMs < config.getFaultEndMs();
                    if (faultActive && config.getStuckValues() != null && config.getStuckValues().containsKey(signal)) {
                        noisedValue = config.getStuckValues().get(signal);
                    } else {
                        if (faultActive && config.getBias() != null && config.getBias().containsKey(signal)) {
                            noisedValue += config.getBias().get(signal);
                        }
                        if (faultActive && config.getDriftPerSecond() != null && config.getDriftPerSecond().containsKey(signal)) {
                            noisedValue += config.getDriftPerSecond().get(signal) * ((currentTimeMs - config.getFaultStartMs()) / 1000.0);
                        }
                        if (config.getNoiseVariance() != null && config.getNoiseVariance().containsKey(signal)) {
                            noisedValue += random.nextGaussian() * Math.sqrt(config.getNoiseVariance().get(signal));
                        }
                    }

                    readings.add(SensorReading.builder()
                        .signalId(signal)
                        .value(noisedValue)
                        .unit(unit)
                        .measurementTimeBasis(MeasurementTimeBasis.SIMULATION)
                        .monotonicMs(currentTimeMs)
                        .receivedAt(startTime.plusMillis(currentTimeMs))
                        .quality(new Quality(QualityStatus.VALID, null, 0))
                        .provenance(new SignalProvenance("sim", "ref"))
                        .build());
                }
            }

            List<DtcObservation> dtcs = new ArrayList<>();
            List<String> activeDtcs = new ArrayList<>();
            if (config.getInjectedDtcs() != null && currentTimeMs >= config.getFaultStartMs() && currentTimeMs < config.getFaultEndMs()) {
                activeDtcs.addAll(config.getInjectedDtcs());
            }
            if (config.getScenarioType() == ScenarioType.HARD_ACCELERATION_WITH_FAULT && currentTimeMs >= 15000) {
                if (!activeDtcs.contains("P0301")) activeDtcs.add("P0301");
            }
            
            if (!activeDtcs.isEmpty()) {
                for (String dtcCode : activeDtcs) {
                     dtcs.add(DtcObservation.builder()
                        .code(dtcCode)
                        .status(DtcStatus.CONFIRMED)
                        .observationType(ObservationType.SNAPSHOT)
                        .observedAt(startTime.plusMillis(currentTimeMs))
                        .monotonicMs(currentTimeMs)
                        .provenance(new DtcProvenance("sim", "ref"))
                        .build());
                }
            }

            boolean dtcPoll = currentTimeMs % 1000 == 0;
            if (!readings.isEmpty() || dtcPoll) {
                ObdMessage.Builder builder = ObdMessage.builder()
                    .messageId("MSG-" + sequence)
                    .sessionId(sessionId)
                    .sequence(sequence++)
                    .messageType(MessageType.TELEMETRY)
                    .sourceType(SourceType.SIMULATION)
                    .emittedAt(startTime.plusMillis(currentTimeMs))
                    .receivedAt(startTime.plusMillis(currentTimeMs))
                    .vehicleRef(new VehicleRef(config.getVehicleId(), null, IdentityStatus.UNKNOWN))
                    .sensorReadings(readings)
                    .rawProvenance(new Provenance("generator", "1.0", "ref", config.getRandomSeed()));
                    
                if (dtcPoll) {
                    builder.dtcObservations(dtcs);
                    builder.dtcSnapshotCompleteness(DtcSnapshotCompleteness.COMPLETE);
                }
                    
                publicStream.add(builder.build());
                
                groundTruth.add(new GroundTruthRecord(currentTimeMs, trueValues, new ArrayList<>(activeDtcs)));
            }

            currentTimeMs += 10; // 10ms simulation tick
        }

        return new GenerationResult(publicStream, groundTruth);
    }
}
