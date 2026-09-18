package com.ignitionai.application;

import com.ignitionai.obdgenerator.config.*;
import com.ignitionai.obdgenerator.generator.*;
import com.ignitionai.obdinput.storage.*;
import com.ignitionai.obdinput.schema.*;
import java.nio.file.*;
import java.util.*;

public final class NativeWorkflowTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        GeneratorConfig config = ScenarioFile.parse(ScenarioFile.example());
        ObdGenerator generator = new ObdGenerator(config);
        var first = generator.generate().getPublicStream();
        var second = generator.generate().getPublicStream();
        check(first.size() == second.size(), "Same generator must reset seed on each run");
        for (int i=0;i<first.size();i++) check(ObdJsonMapper.serialize(first.get(i)).equals(ObdJsonMapper.serialize(second.get(i))), "Replay differs");
        long rpm = first.stream().flatMap(m -> m.getSensorReadings().stream()).filter(r -> r.getSignalId().equals("engine_rpm")).count();
        check(rpm > 160 && rpm <= 180, "3Hz must not be rounded down to 0.3Hz: " + rpm);
        check(first.stream().anyMatch(m -> m.getDtcSnapshotCompleteness() == DtcSnapshotCompleteness.COMPLETE && m.getDtcObservations().isEmpty() && m.getReceivedAt().equals(first.get(0).getReceivedAt().plusSeconds(46))), "Recovery needs an empty complete DTC scan");
        Path dir = Files.createTempDirectory("ignition-workflow-");
        try {
            Path saved = dir.resolve("session.jsonl"); SessionFiles.write(saved, first);
            var replay = SessionFiles.read(saved);
            InspectionService service = new InspectionService(new java.io.File("virtual-sensors/src/main/resources/sensors"));
            var a = service.assess(first); var b = service.assess(replay);
            check(a.certificate.getCertificateId().equals(b.certificate.getCertificateId()), "Certificate replay differs");
            var changed = new ObdGenerator(ScenarioFile.parse(ScenarioFile.example().replace("seed=42", "seed=43"))).generate().getPublicStream();
            check(!a.certificate.getCertificateId().equals(service.assess(changed).certificate.getCertificateId()), "Different telemetry must not overwrite an identical-score saved session");
            String sustained = "vehicle=TEST\nduration_ms=40000\nseed=1\nscenario=CITY_DRIVING\nsignals=engine_rpm,vehicle_speed,coolant_temperature\nmissing_fraction=0\nfault_start_ms=15000\nfault_end_ms=30000\nengine_rpm.unit=rpm\nengine_rpm.points=0:2000,40000:2000\nengine_rpm.bias=1200\nvehicle_speed.unit=km/h\nvehicle_speed.points=0:50,40000:50\ncoolant_temperature.unit=Cel\ncoolant_temperature.points=0:90,40000:90\n";
            var fault = service.assess(new ObdGenerator(ScenarioFile.parse(sustained)).generate().getPublicStream());
            check(fault.episodes.stream().anyMatch(ep -> ep.getSubsystemId().equals("engine_rpm") && ep.getDurationMs() >= 10000), "Persistent fault was absorbed into nominal baseline");
            String tex = new com.ignitionai.phase7.latex.LatexGenerator().generateCertificate(a.certificate);
            check(tex.contains("SIMULATION") && tex.contains("Preliminary report"), "Report lost source or model limitation");
            check(a.certificate.getHealthAssessment().getOdometerKm() == null, "Invented mileage");
            check(!"CONFIRMED".equals(a.certificate.getHealthAssessment().getVinOrIdentityStatus()), "Invented identity");
            check(a.degradation.stream().allMatch(d -> d.getEventRiskEstimate() == null), "Uncalibrated probability published");
            check(!a.observations.isEmpty(), "Phase 3 not executed");
            check(a.certificate.getHealthAssessment().getVehicleHealthIndex() != null, "Preliminary report has no usable indicator score");
            check(a.certificate.getHealthAssessment().getDataQualityStatus().equals("PARTIAL"), "Preliminary score must disclose partial data quality");
            check(a.certificate.getHealthAssessment().getAssessmentTimestamp().contains("T"), "Timestamp not UTC");
            boolean rejected = false;
            try { MiniJson.parse("{\"x\":1} trailing"); } catch (RuntimeException e) { rejected=true; }
            check(rejected, "Trailing JSON accepted");
            String tricky = "Nexon\\\"\n\t\u0001";
            check(tricky.equals(MiniJson.parse(ObdJsonMapper.quote(tricky))), "JSON escaping broken");
            config.setSamplingRatesHz(Map.of("engine_rpm", 0.0)); rejected=false;
            try { new ObdGenerator(config).generate(); } catch (IllegalArgumentException e) { rejected=true; }
            check(rejected, "Zero sample rate accepted");
            Files.delete(saved);
        } finally { Files.delete(dir); }
        System.out.println("PASS NativeWorkflowTest: deterministic configurable simulation, 3Hz scheduling, DTC recovery, JSON replay, honest assessment and invalid input checks");
    }
}
