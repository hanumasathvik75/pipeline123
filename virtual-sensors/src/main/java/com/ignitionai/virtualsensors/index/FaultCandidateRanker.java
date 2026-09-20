package com.ignitionai.virtualsensors.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Probabilistic fault graph: combines every piece of evidence this session produced (flagged
 * Diagnostic Index results AND stored DTCs) into one posterior probability per candidate root
 * cause, instead of ranking each flagged index independently. Two independent pieces of evidence
 * pointing at the same cause (e.g. a Combustion flag AND a matching misfire DTC) genuinely raise
 * that cause's probability above either alone -- that convergence is the actual point of a fault
 * graph, and the previous implementation (max(5, 100-health) per index, unrelated to anything else
 * found this session) did not do it.
 *
 * Method: a naive-Bayes-style odds update. Every named cause starts at the same small prior
 * (nothing is likely without evidence). Each corroborating piece of evidence multiplies that
 * cause's odds by a likelihood ratio derived from a fixed, documented "how strongly does this
 * evidence implicate that cause if true" association strength (0-1). This is a heuristic
 * combination rule, not a fitted/calibrated model -- the association strengths below are
 * automotive-diagnosis domain reasoning (which components a given symptom or DTC commonly
 * implicates), not learned from failure-outcome data, and the resulting probabilities are capped
 * well short of certainty for exactly that reason. Log-odds combination is the same principled
 * mechanism used by real fault-diagnosis Bayesian networks; what's missing for a production
 * version is calibrating the association strengths against real repair-outcome data, not the
 * underlying math.
 */
public final class FaultCandidateRanker {
    private FaultCandidateRanker() {}

    private static final double PRIOR_PROBABILITY = 0.04;
    private static final double MAX_LIKELIHOOD_RATIO_STRENGTH = 9.0;
    private static final double MIN_POSTERIOR_PCT = 3.0;
    private static final double MAX_POSTERIOR_PCT = 95.0;

    /** Diagnostic-index-id -> {candidate cause -> association strength 0-1}. driving_behavior and
     * signal_quality are deliberately absent: they characterise usage/data quality, not a fault. */
    private static final Map<String, Map<String, Double>> INDEX_EVIDENCE = new LinkedHashMap<>();
    static {
        INDEX_EVIDENCE.put("combustion", Map.of("Spark plug or ignition coil", 0.65, "Fuel injector (affected cylinder)", 0.45));
        INDEX_EVIDENCE.put("fuel", Map.of("Vacuum or intake leak", 0.55, "MAF sensor drift", 0.45, "Fuel pressure or injector imbalance", 0.5));
        INDEX_EVIDENCE.put("thermal", Map.of("Thermostat", 0.6, "Cooling fan", 0.4, "Coolant level or flow restriction", 0.5));
        INDEX_EVIDENCE.put("load_response", Map.of("Throttle body or actuator adaptation", 0.6, "Intake restriction", 0.4, "MAP or MAF plausibility", 0.5));
        INDEX_EVIDENCE.put("charging", Map.of("Alternator or voltage regulator", 0.65, "Battery or ground connection", 0.5));
        INDEX_EVIDENCE.put("air_intake_consistency", Map.of("MAF sensor drift", 0.55, "MAP sensor drift", 0.55, "Vacuum or intake leak", 0.4));
        INDEX_EVIDENCE.put("timing_compliance", Map.of("Knock sensor or ECU timing input", 0.5, "Coolant or intake-air temperature sensor drift", 0.4));
    }

    /** DTC pattern (regex) -> {candidate cause -> association strength 0-1}. */
    private static final Map<Pattern, Map<String, Double>> DTC_EVIDENCE = new LinkedHashMap<>();
    static {
        DTC_EVIDENCE.put(Pattern.compile("P010[1-4]|P017[14]"), Map.of("Vacuum or intake leak", 0.6, "MAF sensor drift", 0.5, "Fuel pressure or injector imbalance", 0.4));
        DTC_EVIDENCE.put(Pattern.compile("P017[25]"), Map.of("Fuel pressure or injector imbalance", 0.55, "MAF sensor drift", 0.4));
        DTC_EVIDENCE.put(Pattern.compile("P030[1-8]"), Map.of("Spark plug or ignition coil", 0.6, "Fuel injector (affected cylinder)", 0.45));
        DTC_EVIDENCE.put(Pattern.compile("P0128"), Map.of("Thermostat", 0.8));
        DTC_EVIDENCE.put(Pattern.compile("P0480|P0483"), Map.of("Cooling fan", 0.7));
        DTC_EVIDENCE.put(Pattern.compile("P0562"), Map.of("Battery or ground connection", 0.6, "Alternator or voltage regulator", 0.5));
        DTC_EVIDENCE.put(Pattern.compile("P0563"), Map.of("Alternator or voltage regulator", 0.7));
        DTC_EVIDENCE.put(Pattern.compile("P012[123]"), Map.of("Throttle body or actuator adaptation", 0.6));
        DTC_EVIDENCE.put(Pattern.compile("P0335|P0336"), Map.of("Crank position sensor", 0.7));
    }

    private static final Map<String, String> CAUSE_DESCRIPTION = new TreeMap<>();
    static {
        CAUSE_DESCRIPTION.put("Spark plug or ignition coil", "Check spark plugs, ignition coils and their connections for the affected cylinder(s).");
        CAUSE_DESCRIPTION.put("Fuel injector (affected cylinder)", "Check fuel injector condition and flow balance for the affected cylinder(s).");
        CAUSE_DESCRIPTION.put("Vacuum or intake leak", "Inspect the intake manifold, vacuum hoses and gaskets for leaks.");
        CAUSE_DESCRIPTION.put("MAF sensor drift", "Check MAF sensor calibration, wiring and contamination.");
        CAUSE_DESCRIPTION.put("MAP sensor drift", "Check MAP sensor calibration, wiring and vacuum reference line.");
        CAUSE_DESCRIPTION.put("Fuel pressure or injector imbalance", "Check fuel pump pressure, fuel filter and injector flow balance.");
        CAUSE_DESCRIPTION.put("Thermostat", "Check thermostat opening temperature and operation.");
        CAUSE_DESCRIPTION.put("Cooling fan", "Check cooling fan operation and its control circuit.");
        CAUSE_DESCRIPTION.put("Coolant level or flow restriction", "Check coolant level, hoses and for flow restrictions or airlocks.");
        CAUSE_DESCRIPTION.put("Throttle body or actuator adaptation", "Check throttle body cleanliness, actuator response and adaptation values.");
        CAUSE_DESCRIPTION.put("Intake restriction", "Check air filter and intake tract for restriction.");
        CAUSE_DESCRIPTION.put("MAP or MAF plausibility", "Cross-check MAP/MAF sensor plausibility against RPM and load.");
        CAUSE_DESCRIPTION.put("Alternator or voltage regulator", "Check alternator output, voltage regulator and drive belt.");
        CAUSE_DESCRIPTION.put("Battery or ground connection", "Check battery condition, terminals and chassis/engine ground connections.");
        CAUSE_DESCRIPTION.put("Knock sensor or ECU timing input", "Check the knock sensor and the sensor inputs the ECU uses to adjust ignition timing.");
        CAUSE_DESCRIPTION.put("Coolant or intake-air temperature sensor drift", "Check coolant and intake-air temperature sensor readings for drift.");
        CAUSE_DESCRIPTION.put("Crank position sensor", "Check crankshaft position sensor, reluctor wheel and wiring.");
    }

    private static final Map<String, String> CAUSE_TOOLS = new TreeMap<>();
    static {
        CAUSE_TOOLS.put("Spark plug or ignition coil", "Spark plug socket, multimeter, coil-on-plug swap test");
        CAUSE_TOOLS.put("Fuel injector (affected cylinder)", "Fuel pressure gauge, injector flow tester or swap test");
        CAUSE_TOOLS.put("Vacuum or intake leak", "Smoke tester or carb-cleaner leak test, torque wrench");
        CAUSE_TOOLS.put("MAF sensor drift", "OBD live-data scan tool, MAF sensor cleaner");
        CAUSE_TOOLS.put("MAP sensor drift", "OBD live-data scan tool, vacuum pump");
        CAUSE_TOOLS.put("Fuel pressure or injector imbalance", "Fuel pressure gauge");
        CAUSE_TOOLS.put("Thermostat", "Infrared thermometer, coolant temperature gauge");
        CAUSE_TOOLS.put("Cooling fan", "Multimeter, fan relay test");
        CAUSE_TOOLS.put("Coolant level or flow restriction", "Visual inspection, cooling-system pressure tester");
        CAUSE_TOOLS.put("Throttle body or actuator adaptation", "OBD scan tool (throttle relearn procedure), throttle body cleaner");
        CAUSE_TOOLS.put("Intake restriction", "Visual inspection of air filter and intake tract");
        CAUSE_TOOLS.put("MAP or MAF plausibility", "OBD live-data scan tool (cross-plot MAP/MAF against RPM)");
        CAUSE_TOOLS.put("Alternator or voltage regulator", "Multimeter, battery/alternator load tester");
        CAUSE_TOOLS.put("Battery or ground connection", "Multimeter, battery load tester, ground-strap inspection");
        CAUSE_TOOLS.put("Knock sensor or ECU timing input", "OBD live-data scan tool, oscilloscope if available");
        CAUSE_TOOLS.put("Coolant or intake-air temperature sensor drift", "Multimeter (resistance check), OBD live-data comparison");
        CAUSE_TOOLS.put("Crank position sensor", "Multimeter, oscilloscope, timing light");
    }

    /** Tools/equipment a technician needs to check the given cause, or a generic fallback for a
     * cause not in the static map (e.g. one only reachable via a future DTC_EVIDENCE addition). */
    public static String toolsFor(String cause) {
        return CAUSE_TOOLS.getOrDefault(cause, "Standard diagnostic tool set; consult service manual for this component.");
    }

    public static List<FaultCandidate> rank(List<DiagnosticIndexResult> results, List<String> dtcCodes) {
        double priorOdds = PRIOR_PROBABILITY / (1.0 - PRIOR_PROBABILITY);
        Map<String, Double> odds = new LinkedHashMap<>();
        Map<String, List<String>> contributions = new LinkedHashMap<>();
        Map<String, LinkedHashMap<String, String>> mergedEvidence = new LinkedHashMap<>();

        for (DiagnosticIndexResult r : results) {
            if (r.getState() != DiagnosticIndexResult.State.FLAGGED || r.getHealthPct() == null) continue;
            Map<String, Double> causes = INDEX_EVIDENCE.get(r.getIndexId());
            if (causes == null) continue;
            for (Map.Entry<String, Double> e : causes.entrySet()) {
                applyEvidence(odds, priorOdds, e.getKey(), e.getValue());
                contributions.computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                    .add(r.getDisplayName() + " index (flagged, " + String.format(Locale.ROOT, "%.1f%%", r.getHealthPct()) + " health)");
                mergedEvidence.computeIfAbsent(e.getKey(), k -> new LinkedHashMap<>())
                    .putAll(prefixKeys(r.getIndexId(), r.getEvidence()));
            }
        }
        for (String dtc : dtcCodes) {
            for (Map.Entry<Pattern, Map<String, Double>> entry : DTC_EVIDENCE.entrySet()) {
                if (!entry.getKey().matcher(dtc).matches()) continue;
                for (Map.Entry<String, Double> e : entry.getValue().entrySet()) {
                    applyEvidence(odds, priorOdds, e.getKey(), e.getValue());
                    contributions.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add("DTC " + dtc);
                }
            }
        }

        List<FaultCandidate> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : odds.entrySet()) {
            String cause = e.getKey();
            double posteriorPct = 100.0 * e.getValue() / (1.0 + e.getValue());
            posteriorPct = Math.max(MIN_POSTERIOR_PCT, Math.min(MAX_POSTERIOR_PCT, posteriorPct));
            String rootCause = CAUSE_DESCRIPTION.getOrDefault(cause, "Requires technician investigation.");
            String corroboration = String.join("; ", contributions.getOrDefault(cause, List.of()));
            Map<String, String> evidence = mergedEvidence.getOrDefault(cause, new LinkedHashMap<>());
            out.add(new FaultCandidate(cause, posteriorPct, rootCause, corroboration, corroboration, evidence));
        }
        out.sort(Comparator.comparingDouble(FaultCandidate::getLikelihoodPct).reversed());
        return out;
    }

    private static void applyEvidence(Map<String, Double> odds, double priorOdds, String cause, double strength) {
        double likelihoodRatio = 1.0 + strength * MAX_LIKELIHOOD_RATIO_STRENGTH;
        odds.merge(cause, priorOdds * likelihoodRatio, (existing, ignored) -> existing * likelihoodRatio);
    }

    private static Map<String, String> prefixKeys(String prefix, Map<String, String> evidence) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : evidence.entrySet()) out.put(prefix + "." + e.getKey(), e.getValue());
        return out;
    }
}
