package com.ignitionai.virtualsensors.index;

import com.ignitionai.obdinput.storage.MiniJson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads the versioned, editable numeric-threshold policy for every diagnostic index and the
 * legacy monitored-indicator baselines from config/diagnostic-index-config.json (a classpath
 * resource). No index-specific numeric threshold should be a Java literal anywhere in this
 * codebase; every plugin reads its constants from here so they can be reviewed, versioned and
 * revised without recompiling, and every value's rationale is recorded next to it in the file
 * instead of in a scattered code comment.
 */
public final class DiagnosticIndexConfig {
    private static final DiagnosticIndexConfig INSTANCE = new DiagnosticIndexConfig();

    private final Map<String, Object> root;
    private final Map<String, Object> indices;
    private final Map<String, Object> monitoredIndicators;

    private DiagnosticIndexConfig() {
        try (InputStream in = DiagnosticIndexConfig.class.getClassLoader().getResourceAsStream("config/diagnostic-index-config.json")) {
            if (in == null) throw new IllegalStateException("config/diagnostic-index-config.json not found on classpath");
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            root = castMap(MiniJson.parse(text));
            indices = castMap(root.get("indices"));
            monitoredIndicators = castMap(root.get("monitored_indicators"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load config/diagnostic-index-config.json", e);
        }
    }

    public static DiagnosticIndexConfig get() { return INSTANCE; }

    public String configVersion() { return (String) root.get("config_version"); }

    /** A named numeric threshold for one index, e.g. number("combustion", "window_samples"). */
    public double number(String indexId, String key) {
        Map<String, Object> section = castMap(indices.get(indexId));
        if (section == null) throw new IllegalStateException("Missing config section indices." + indexId);
        if (!section.containsKey(key)) throw new IllegalStateException("Missing config value indices." + indexId + "." + key);
        return ((Number) section.get(key)).doubleValue();
    }

    public int integer(String indexId, String key) { return (int) number(indexId, key); }

    public String rationale(String indexId) {
        Map<String, Object> section = castMap(indices.get(indexId));
        return section == null ? null : (String) section.get("rationale");
    }

    /** Per-signal measurement-noise floor for the legacy raw-signal monitoring pipeline. */
    public Map<String, Double> monitoredIndicatorNoiseFloors() {
        return numericMap(castMap(monitoredIndicators.get("signal_noise_floor")));
    }

    /** Per-signal internal weight for the legacy raw-signal VHI aggregation. */
    public Map<String, Double> monitoredIndicatorWeights() {
        return numericMap(castMap(monitoredIndicators.get("weight")));
    }

    /** Per-signal sample count that counts as "full" data-coverage confidence -- each signal has
     * its own bar sized to its own typical rate (see monitored_indicators.confidence_rationale). */
    public Map<String, Double> monitoredIndicatorFullConfidenceSamples() {
        return numericMap(castMap(monitoredIndicators.get("confidence_full_at_samples")));
    }

    /** A scalar (non-map) value under monitored_indicators, e.g. "confidence_ceiling". */
    public double monitoredIndicatorsNumber(String key) {
        if (!monitoredIndicators.containsKey(key)) throw new IllegalStateException("Missing config value monitored_indicators." + key);
        return ((Number) monitoredIndicators.get(key)).doubleValue();
    }

    /** A scalar value under episode_quality, e.g. "full_confidence_at_samples" -- backs the anomaly
     * episode quality-confidence proxy (see InspectionService.episodeQualityConfidence). */
    public double episodeQualityNumber(String key) {
        Map<String, Object> section = castMap(root.get("episode_quality"));
        if (section == null || !section.containsKey(key)) throw new IllegalStateException("Missing config value episode_quality." + key);
        return ((Number) section.get(key)).doubleValue();
    }

    /** signal_quality.expected_min_hz -- backs SignalQualityScorePlugin's sample-rate consistency check. */
    public Map<String, Double> signalQualityExpectedMinHz() {
        return numericMap(castMap(castMap(indices.get("signal_quality")).get("expected_min_hz")));
    }

    /** signal_quality.plausible_ranges -- backs SignalQualityScorePlugin's implausible-value check. */
    public Map<String, double[]> signalQualityPlausibleRanges() {
        Map<String, Object> raw = castMap(castMap(indices.get("signal_quality")).get("plausible_ranges"));
        Map<String, double[]> out = new TreeMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            @SuppressWarnings("unchecked")
            List<Object> pair = (List<Object>) e.getValue();
            out.put(e.getKey(), new double[]{ ((Number) pair.get(0)).doubleValue(), ((Number) pair.get(1)).doubleValue() });
        }
        return out;
    }

    private static Map<String, Double> numericMap(Map<String, Object> raw) {
        Map<String, Double> out = new TreeMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) out.put(e.getKey(), ((Number) e.getValue()).doubleValue());
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) { return (Map<String, Object>) o; }
}
