package com.ignitionai.application;

import com.ignitionai.obdgenerator.config.*;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.obdinput.schema.*;
import com.ignitionai.phase5.DegradationOutput;
import java.nio.file.*;
import java.util.*;

/** Multi-visit causal test through actual persisted OBD files, including untrusted and duplicate history. */
public final class HistoryWorkflowTest {
    static final String SCENARIO = "vehicle=TEST\nduration_ms=40000\nseed=1\nscenario=CITY_DRIVING\nsignals=engine_rpm,vehicle_speed,coolant_temperature\nmissing_fraction=0\nfault_start_ms=15000\nfault_end_ms=30000\nengine_rpm.unit=rpm\nengine_rpm.points=0:2000,40000:2000\nengine_rpm.bias=1200\nvehicle_speed.unit=km/h\nvehicle_speed.points=0:50,40000:50\ncoolant_temperature.unit=Cel\ncoolant_temperature.points=0:90,40000:90\n";
    static List<ObdMessage> visit(String date) throws Exception {
        return new ObdGenerator(ScenarioFile.parse(SCENARIO + "start_time=" + date + "T10:00:00Z\n")).generate().getPublicStream();
    }
    static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    static DegradationOutput rpm(InspectionResult r) { return r.degradation.stream().filter(d -> d.getSubsystemId().equals("engine_rpm")).findFirst().orElseThrow(); }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("ignition-history-");
        try {
            var sensors = new java.io.File("virtual-sensors/src/main/resources/sensors");
            var service = new InspectionService(sensors);
            var current = visit("2026-09-18");
            var alone = service.assess(current);
            check(rpm(alone).getDataSufficiencyStatus() == DegradationOutput.DataSufficiency.INSUFFICIENT, "One session falsely sufficient");
            SessionFiles.write(dir.resolve("older.jsonl"), visit("2026-09-16"));
            SessionFiles.write(dir.resolve("previous.jsonl"), visit("2026-09-17"));
            var history = new AssessmentHistory(sensors, dir);
            var result = history.assess(current);
            check(rpm(result).getDataSufficiencyStatus() == DegradationOutput.DataSufficiency.SPARSE, "Previous visits not used");
            check(rpm(result).getEvidenceReferences().size() > rpm(alone).getEvidenceReferences().size(), "Historical evidence lost");
            check(!result.certificate.getCertificateId().equals(alone.certificate.getCertificateId()), "Certificate omits historical input");
            check(rpm(result).getEventRiskEstimate() == null, "History must not invent model calibration");
            String expected = result.certificate.getCertificateId();
            SessionFiles.write(dir.resolve("future.jsonl"), visit("2026-09-19"));
            SessionFiles.write(dir.resolve("duplicate.jsonl"), visit("2026-09-17"));
            SessionFiles.write(dir.resolve("current.jsonl"), current);
            GeneratorConfig other = ScenarioFile.parse(SCENARIO + "start_time=2026-09-15T10:00:00Z\n");
            other.setVehicleId("OTHER");
            SessionFiles.write(dir.resolve("other-vehicle.jsonl"), new ObdGenerator(other).generate().getPublicStream());
            check(history.assess(current).certificate.getCertificateId().equals(expected), "Future, duplicate, self or other vehicle contaminated history");
            Files.writeString(dir.resolve("broken.jsonl"), "broken JSON");
            check(history.assess(current).notes.stream().anyMatch(n -> n.contains("unreadable or invalid")), "Corrupt history silently ignored");
            var earliest = history.assess(visit("2026-09-16"));
            check(rpm(earliest).getDataSufficiencyStatus() == DegradationOutput.DataSufficiency.INSUFFICIENT, "Replaying old visit leaked future evidence");
            var shifted = service.assess(visit("2026-09-17"));
            List<ObdMessage> real = shifted.messages.stream().map(m -> ObdMessage.builder().messageId(m.getMessageId()).sessionId(m.getSessionId())
                .sequence(m.getSequence()).messageType(m.getMessageType()).sourceType(SourceType.VEHICLE).emittedAt(m.getEmittedAt()).receivedAt(m.getReceivedAt())
                .vehicleRef(m.getVehicleRef()).sensorReadings(m.getSensorReadings()).dtcObservations(m.getDtcObservations())
                .dtcSnapshotCompleteness(m.getDtcSnapshotCompleteness()).rawProvenance(m.getRawProvenance()).build()).collect(java.util.stream.Collectors.toList());
            check(!AssessmentHistory.eligible(current, real, AssessmentHistory.firstTime(current)), "Simulated and real captures mixed");
            System.out.println("PASS HistoryWorkflowTest: persisted multi-visit degradation, causal replay, provenance, deduplication, isolation, corrupt-file reporting");
        } finally {
            try (var files = Files.list(dir)) { for (Path p : files.collect(java.util.stream.Collectors.toList())) Files.delete(p); }
            Files.delete(dir);
        }
    }
}
