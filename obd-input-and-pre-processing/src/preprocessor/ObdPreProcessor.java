package com.ignitionai.obdinput.preprocessor;

import com.ignitionai.obdinput.schema.*;
import java.util.*;

public class ObdPreProcessor {

    private final Map<String, Long> lastSequence = new HashMap<>();
    private final Map<String, Map<String, Long>> lastMonotonicTimes = new HashMap<>();
    private final Map<String, Set<String>> seenSensorReadings = new HashMap<>();
    private final Map<String, Set<String>> knownSessionDtcs = new HashMap<>();

    public ProcessingResult process(ObdMessage message) {
        validateSchemaAndRequiredFields(message);
        
        String session = message.getSessionId();
        List<QualityEvent> qualityEvents = new ArrayList<>();
        
        long currentSeq = message.getSequence();
        Long lastSeq = lastSequence.get(session);
        if (lastSeq != null && currentSeq != lastSeq + 1) {
            qualityEvents.add(new SequenceGapEvent(session, System.currentTimeMillis(), lastSeq + 1, currentSeq));
        }
        lastSequence.put(session, currentSeq);

        Map<String, Long> sessionTimes = lastMonotonicTimes.computeIfAbsent(session, k -> new HashMap<>());
        Set<String> sessionSeenReadings = seenSensorReadings.computeIfAbsent(session, k -> new HashSet<>());
        
        List<SensorReading> processedReadings = new ArrayList<>();
        for (SensorReading reading : message.getSensorReadings()) {
            if (reading.getQuality().isInterpolated()) {
                throw new ValidationException("Interpolated data is not allowed at input boundary.");
            }
            
            String key = String.format("%s-%s-%s-%d-%f", 
                session, reading.getSignalId(), reading.getEcuId(), reading.getMonotonicMs(), reading.getValue());

            String duplicateCheckKey = String.format("%s-%s-%s-%d", 
                session, reading.getSignalId(), reading.getEcuId(), reading.getMonotonicMs());

            if (sessionSeenReadings.contains(key)) {
                continue;
            } else if (sessionSeenReadings.contains(duplicateCheckKey)) {
                processedReadings.add(flagAsConflicting(reading));
                continue;
            }
            sessionSeenReadings.add(key);
            sessionSeenReadings.add(duplicateCheckKey);

            long lastTime = sessionTimes.getOrDefault(reading.getSignalId(), -1L);
            if (reading.getMonotonicMs() < lastTime) {
                processedReadings.add(flagAsOutOfOrder(reading));
                // Do not update max monotonic time because this reading is old
            } else {
                processedReadings.add(reading);
                sessionTimes.put(reading.getSignalId(), reading.getMonotonicMs());
            }
        }

        Set<String> sessionDtcs = knownSessionDtcs.computeIfAbsent(session, k -> new HashSet<>());
        List<DtcObservation> processedDtcs = new ArrayList<>();

        for (DtcObservation dtc : message.getDtcObservations()) {
            processedDtcs.add(dtc);
            if (dtc.getObservationType() == ObservationType.SNAPSHOT || dtc.getObservationType() == ObservationType.ADDED) {
                sessionDtcs.add(dtc.getCode());
            } else if (dtc.getObservationType() == ObservationType.CLEARED) {
                sessionDtcs.remove(dtc.getCode());
            }
        }
        
        if (message.getDtcSnapshotCompleteness() == DtcSnapshotCompleteness.COMPLETE) {
            Set<String> codesInThisMessage = new HashSet<>();
            for (DtcObservation dtc : message.getDtcObservations()) {
                codesInThisMessage.add(dtc.getCode());
            }
            sessionDtcs.retainAll(codesInThisMessage);
        }

        ObdMessage processedMsg = ObdMessage.builder()
            .messageId(message.getMessageId())
            .sessionId(message.getSessionId())
            .sequence(message.getSequence())
            .messageType(message.getMessageType())
            .sourceType(message.getSourceType())
            .emittedAt(message.getEmittedAt())
            .receivedAt(message.getReceivedAt())
            .vehicleRef(message.getVehicleRef())
            .sensorReadings(processedReadings)
            .dtcObservations(processedDtcs)
            .capabilitySnapshot(message.getCapabilitySnapshot())
            .rawProvenance(message.getRawProvenance())
            .dtcSnapshotCompleteness(message.getDtcSnapshotCompleteness())
            .build();
            
        return new ProcessingResult(processedMsg, qualityEvents);
    }

    private void validateSchemaAndRequiredFields(ObdMessage message) {
        if (!"obd-input.v1".equals(message.getSchemaVersion())) {
            throw new ValidationException("Unsupported schema version: " + message.getSchemaVersion());
        }
        if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
            throw new ValidationException("message_id is required");
        }
        if (message.getSessionId() == null || message.getSessionId().isEmpty()) {
            throw new ValidationException("session_id is required");
        }
        for (SensorReading reading : message.getSensorReadings()) {
            if (reading.getSignalId() == null || reading.getSignalId().isEmpty()) {
                throw new ValidationException("signal_id is required");
            }
            if (reading.getUnit() == null || reading.getUnit().isEmpty()) {
                throw new ValidationException("unit is required");
            }
        }
    }

    private SensorReading flagAsConflicting(SensorReading reading) {
        Quality newQuality = new Quality(QualityStatus.INVALID, "CONFLICTING_DUPLICATE", reading.getQuality().getAgeMs());
        return copyWithNewQuality(reading, newQuality);
    }

    private SensorReading flagAsOutOfOrder(SensorReading reading) {
        Quality newQuality = new Quality(QualityStatus.STALE, "OUT_OF_ORDER", reading.getQuality().getAgeMs());
        return copyWithNewQuality(reading, newQuality);
    }

    private SensorReading copyWithNewQuality(SensorReading r, Quality q) {
        return SensorReading.builder()
            .signalId(r.getSignalId())
            .pidOrDid(r.getPidOrDid())
            .ecuId(r.getEcuId())
            .value(r.getValue())
            .unit(r.getUnit())
            .measuredAt(r.getMeasuredAt())
            .measurementTimeBasis(r.getMeasurementTimeBasis())
            .monotonicMs(r.getMonotonicMs())
            .receivedAt(r.getReceivedAt())
            .quality(q)
            .provenance(r.getProvenance())
            .build();
    }
    
    // For testing
    public Set<String> getKnownDtcs(String sessionId) {
        return knownSessionDtcs.getOrDefault(sessionId, Collections.emptySet());
    }
}
