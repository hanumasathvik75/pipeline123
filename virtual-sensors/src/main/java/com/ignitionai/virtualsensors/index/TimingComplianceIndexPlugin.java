package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Ignition-timing compensation compliance: at a given RPM/load operating point, the ECU commands
 * ignition timing advance from a largely repeatable internal map. If the ECU's trusted inputs to
 * that map (coolant/intake-air temperature, MAP) are themselves drifting, the commanded timing at
 * a REVISITED operating point becomes erratic even though the drifting input sensor's own raw
 * reading can still sit inside its static OBD pass/fail band — the timing behaviour "masks" the
 * drift. This bins the session by (RPM, load) and checks how repeatable timing advance is within
 * each revisited bin.
 *
 * Adapted from Komorska, Wolczynski & Borczuch 2019 ("Diagnosis of sensor faults in a combustion
 * engine control system with the artificial neural network", Diagnostyka 20(4)), which trained a
 * 12-class ANN on chassis-dynamometer fault-injection data to classify WHICH sensor drifted from
 * the resulting compensation fingerprint (99.6% accuracy on real injected faults the vehicle's own
 * OBD never flagged). IgnitionAI has no equivalent labelled fault-injection dataset to train that
 * classifier, so this implementation keeps the paper's underlying signal (timing-advance
 * repeatability at a revisited operating point) as a detection-only index: it flags that
 * compensation looks erratic, without attempting to name which specific input sensor caused it —
 * see the authoring guide for how a future labelled dataset could extend this to fault
 * classification. Thresholds and rationale live in config/diagnostic-index-config.json under
 * "timing_compliance".
 */
public final class TimingComplianceIndexPlugin implements DiagnosticIndexPlugin {
    private static final String ID = "timing_compliance";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Timing Compliance"; }
    @Override public String getPluginVersion() { return "operating-point-timing-repeatability.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("timing_advance", "engine_rpm", "calculated_engine_load"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        double rpmBinWidth = cfg.number(ID, "rpm_bin_width");
        double loadBinWidth = cfg.number(ID, "load_bin_width");
        double minSamplesPerBin = cfg.number(ID, "min_samples_per_bin");
        double minBinsForConfidence = cfg.number(ID, "min_bins_for_confidence");
        double binStdNormalDeg = cfg.number(ID, "bin_std_normal_deg");
        double binStdFlagDeg = cfg.number(ID, "bin_std_flag_deg");

        List<SeriesUtil.Sample> timing = SeriesUtil.series(session, "timing_advance");
        List<SeriesUtil.Sample> rpm = SeriesUtil.series(session, "engine_rpm");
        List<SeriesUtil.Sample> load = SeriesUtil.series(session, "calculated_engine_load");
        if (timing.isEmpty() || rpm.isEmpty() || load.isEmpty()) {
            return DiagnosticIndexResult.unsupported(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "timing_advance, engine_rpm or calculated_engine_load not present in session", getPluginVersion());
        }

        Map<String, List<Double>> bins = new TreeMap<>();
        for (SeriesUtil.Sample s : timing) {
            SeriesUtil.Sample r = SeriesUtil.nearest(rpm, s.t);
            SeriesUtil.Sample l = SeriesUtil.nearest(load, s.t);
            if (r == null || l == null || Math.abs(r.t - s.t) > 3000 || Math.abs(l.t - s.t) > 3000) continue;
            String key = ((long) (r.v / rpmBinWidth)) + "/" + ((long) (l.v / loadBinWidth));
            bins.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(s.v);
        }

        List<Double> qualifyingBinStds = new java.util.ArrayList<>();
        double worstBinStd = 0;
        int samplesUsed = 0;
        for (List<Double> values : bins.values()) {
            if (values.size() < minSamplesPerBin) continue;
            double std = SeriesUtil.stddev(values);
            qualifyingBinStds.add(std);
            worstBinStd = Math.max(worstBinStd, std);
            samplesUsed += values.size();
        }
        if (qualifyingBinStds.size() < minBinsForConfidence) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than " + (int) minBinsForConfidence + " revisited RPM/load operating-point bins with " + (int) minSamplesPerBin + "+ samples each", getPluginVersion());
        }
        double meanBinStd = SeriesUtil.mean(qualifyingBinStds);

        double health = 100.0;
        boolean flagged = false;
        if (meanBinStd > binStdFlagDeg) { health -= 40; flagged = true; }
        else if (meanBinStd > binStdNormalDeg) { health -= 15; }
        health = Math.max(0, Math.min(100, health));
        double confidence = 100.0 * SeriesUtil.clamp01(qualifyingBinStds.size() / minBinsForConfidence) * 0.85;

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("qualifying_bins", String.valueOf(qualifyingBinStds.size()));
        evidence.put("samples_used", String.valueOf(samplesUsed));
        evidence.put("mean_bin_std_deg", String.format(Locale.ROOT, "%.2f", meanBinStd));
        evidence.put("worst_bin_std_deg", String.format(Locale.ROOT, "%.2f", worstBinStd));
        evidence.put("rpm_bin_width", String.format(Locale.ROOT, "%.0f", rpmBinWidth));
        evidence.put("load_bin_width", String.format(Locale.ROOT, "%.0f", loadBinWidth));
        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), health, confidence,
            flagged ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }
}
