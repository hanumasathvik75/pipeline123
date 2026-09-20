package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Air-intake cross-validation: manifold absolute pressure (MAP) and mass airflow (MAF) are two
 * independently-measured physical quantities that both track the same underlying intake airflow,
 * which is driven by engine RPM and load. Across a session that visits a range of operating
 * points, MAP, MAF and an RPM*load "load proxy" should all correlate strongly with each other; a
 * genuine sensor fault (obscuration, drift, an intake leak) breaks that expected co-variation even
 * while each individual reading can still sit inside its own static plausibility band — precisely
 * the class of fault a single-instant, single-PID OBD check cannot see.
 *
 * Method adapted from Komorska, Wolczynski & Borczuch 2018 ("Fault diagnostics in air intake
 * system of combustion engine using virtual sensors", Diagnostyka 19(1)) and Ahmed, Bhatti & Iqbal
 * 2011 ("Virtual Sensors for Automotive Engine Sensors Fault Diagnosis in Second-Order Sliding
 * Modes", IEEE Sensors J. 11(9)). Both papers build their expected-value model from a SEPARATE,
 * per-engine calibration sweep (a steady-state engine-map test, or an identified physical model)
 * and compare live readings against it. IgnitionAI has no such per-engine calibration data yet, so
 * this is a session-native adaptation: instead of a pre-calibrated lookup table, it checks whether
 * the expected MAP/MAF/load co-variation holds WITHIN the current session's own operating-point
 * spread. This is weaker evidence than the papers' pre-calibrated versions (see the authoring
 * guide for the upgrade path once multi-session calibration data exists) but requires no external
 * data and is still testing the same real physical relationship. Thresholds and rationale live in
 * config/diagnostic-index-config.json under "air_intake_consistency".
 */
public final class AirIntakeConsistencyIndexPlugin implements DiagnosticIndexPlugin {
    private static final String ID = "air_intake_consistency";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Air Intake"; }
    @Override public String getPluginVersion() { return "map-maf-load-correlation.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("manifold_absolute_pressure", "mass_airflow_rate", "engine_rpm", "calculated_engine_load"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        double minRpmSpan = cfg.number(ID, "min_rpm_span_for_evaluation");
        double strong = cfg.number(ID, "strong_correlation");
        double weak = cfg.number(ID, "weak_correlation");
        double minSamplesForConfidence = cfg.number(ID, "min_samples_for_confidence");

        List<SeriesUtil.Sample> map = SeriesUtil.series(session, "manifold_absolute_pressure");
        List<SeriesUtil.Sample> maf = SeriesUtil.series(session, "mass_airflow_rate");
        List<SeriesUtil.Sample> rpm = SeriesUtil.series(session, "engine_rpm");
        List<SeriesUtil.Sample> load = SeriesUtil.series(session, "calculated_engine_load");
        if (map.isEmpty() || maf.isEmpty() || rpm.isEmpty() || load.isEmpty()) {
            return DiagnosticIndexResult.unsupported(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "manifold_absolute_pressure, mass_airflow_rate, engine_rpm or calculated_engine_load not present in session", getPluginVersion());
        }
        double rpmSpan = rpm.stream().mapToDouble(s -> s.v).max().orElse(0) - rpm.stream().mapToDouble(s -> s.v).min().orElse(0);
        if (rpmSpan < minRpmSpan) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "RPM span " + String.format(Locale.ROOT, "%.0f", rpmSpan) + " below " + minRpmSpan + " needed to expose an operating-point spread", getPluginVersion());
        }

        List<Double> mapAligned = new java.util.ArrayList<>(), mafAligned = new java.util.ArrayList<>(), loadProxyAligned = new java.util.ArrayList<>();
        for (SeriesUtil.Sample r : rpm) {
            SeriesUtil.Sample nearestLoad = SeriesUtil.nearest(load, r.t);
            SeriesUtil.Sample nearestMap = SeriesUtil.nearest(map, r.t);
            SeriesUtil.Sample nearestMaf = SeriesUtil.nearest(maf, r.t);
            if (nearestLoad == null || nearestMap == null || nearestMaf == null) continue;
            if (Math.abs(nearestLoad.t - r.t) > 3000 || Math.abs(nearestMap.t - r.t) > 3000 || Math.abs(nearestMaf.t - r.t) > 3000) continue;
            mapAligned.add(nearestMap.v);
            mafAligned.add(nearestMaf.v);
            loadProxyAligned.add(r.v * nearestLoad.v);
        }
        if (mapAligned.size() < minSamplesForConfidence / 2) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than " + (int) (minSamplesForConfidence / 2) + " time-aligned MAP/MAF/RPM/load samples", getPluginVersion());
        }

        double corrMapLoad = SeriesUtil.pearson(mapAligned, loadProxyAligned);
        double corrMafLoad = SeriesUtil.pearson(mafAligned, loadProxyAligned);
        double corrMapMaf = SeriesUtil.pearson(mapAligned, mafAligned);
        double minCorr = Math.min(corrMapMaf, Math.min(corrMapLoad, corrMafLoad));

        double health = 100.0 * SeriesUtil.clamp01(minCorr / strong);
        double confidence = 100.0 * SeriesUtil.clamp01(mapAligned.size() / minSamplesForConfidence) * 0.85;
        DiagnosticIndexResult.State state = minCorr < strong ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL;

        String likelyChannel;
        if (corrMapLoad <= corrMafLoad && corrMapLoad <= corrMapMaf) likelyChannel = "manifold_absolute_pressure";
        else if (corrMafLoad <= corrMapLoad && corrMafLoad <= corrMapMaf) likelyChannel = "mass_airflow_rate";
        else likelyChannel = "none isolated (map/maf agree, both diverge from rpm*load)";

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("aligned_samples", String.valueOf(mapAligned.size()));
        evidence.put("rpm_span", String.format(Locale.ROOT, "%.0f", rpmSpan));
        evidence.put("corr_map_vs_load_proxy", String.format(Locale.ROOT, "%.3f", corrMapLoad));
        evidence.put("corr_maf_vs_load_proxy", String.format(Locale.ROOT, "%.3f", corrMafLoad));
        evidence.put("corr_map_vs_maf", String.format(Locale.ROOT, "%.3f", corrMapMaf));
        evidence.put("strong_correlation_threshold", String.format(Locale.ROOT, "%.2f", strong));
        if (state == DiagnosticIndexResult.State.FLAGGED) evidence.put("likely_channel", likelyChannel);
        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), health, confidence, state,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }

}
