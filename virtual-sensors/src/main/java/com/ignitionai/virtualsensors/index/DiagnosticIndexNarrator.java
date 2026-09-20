package com.ignitionai.virtualsensors.index;

import java.util.Locale;
import java.util.Map;

/**
 * One plain-English sentence per Diagnostic Index result, shared by every reader-facing surface
 * (certificate PDF, native desktop app) so there is exactly one place that defines what each
 * index's state means in plain language -- not one copy per UI that can quietly drift apart.
 * Falls back to a generic templated sentence for any index this class doesn't specifically
 * recognise, so a newly plugged-in DiagnosticIndexPlugin never regresses to a raw key=value dump
 * even before this class is extended for it.
 */
public final class DiagnosticIndexNarrator {
    private DiagnosticIndexNarrator() {}

    public static String plainEnglishFinding(DiagnosticIndexResult r) {
        Map<String, String> e = r.getEvidence();
        if (r.getState() == DiagnosticIndexResult.State.INSUFFICIENT_DATA) return "Not enough evidence in this drive to assess: " + (r.getReason() == null ? "insufficient data." : lower(r.getReason()) + ".");
        if (r.getState() == DiagnosticIndexResult.State.UNSUPPORTED) return "Not assessed: required signal(s) unavailable on this vehicle/session.";
        boolean flagged = r.getState() == DiagnosticIndexResult.State.FLAGGED;
        switch (r.getIndexId()) {
            case "combustion":
                return flagged
                    ? "Engine speed held unusually unsteady in " + pct(e, "flagged_fraction") + " of higher-load driving (" + e.get("flagged_windows") + " of " + e.get("total_windows") + " windows) — consistent with a mild misfire-like irregularity."
                    : "Engine speed held steady across the drive; no misfire-like irregularity detected.";
            case "fuel":
                return flagged
                    ? "The engine computer needed larger-than-normal fuel correction for " + pct(e, "exceedance_fraction") + " of the drive (average " + e.get("mean_abs_combined_trim_pct") + "%)."
                    : "Fuel-trim correction stayed within the normal band for essentially the whole drive (average " + e.get("mean_abs_combined_trim_pct") + "%).";
            case "thermal":
                if ("false".equals(e.get("operating_temperature_reached"))) return "Coolant never reached normal operating temperature during this drive.";
                return flagged
                    ? "Reached operating temperature, but coolant temperature swung by ±" + e.get("steady_state_std_c") + "°C afterwards — more than expected once warm."
                    : "Reached normal operating temperature and held it steady (±" + e.get("steady_state_std_c") + "°C) for the rest of the drive.";
            case "load_response":
                return flagged
                    ? "Engine speed took about " + e.get("estimated_lag_seconds") + "s to respond after a throttle input — slower than the " + e.get("max_lag_seconds") + "s expected for a healthy air path."
                    : "Engine speed responded to throttle input within " + e.get("estimated_lag_seconds") + "s, inside the expected range.";
            case "charging":
                return flagged
                    ? "Charging voltage averaged " + e.get("mean_voltage_v") + "V with higher-than-normal ripple — worth checking the alternator/regulator or a battery connection."
                    : "Charging voltage averaged " + e.get("mean_voltage_v") + "V, inside the normal 12.4–14.9V band, with low ripple.";
            case "air_intake_consistency":
                return flagged
                    ? "Manifold pressure and airflow did not track engine load as consistently as expected (correlation " + e.get("corr_map_vs_maf") + "), most likely from the " + e.getOrDefault("likely_channel", "affected") + " reading."
                    : "Manifold pressure and airflow both tracked engine load consistently across the drive, confirming the two sensors agree.";
            case "timing_compliance":
                return flagged
                    ? "Ignition timing varied unusually (±" + e.get("mean_bin_std_deg") + "°) each time the engine returned to the same speed/load condition — worth checking the sensors that inform ignition timing."
                    : "Ignition timing stayed consistent (±" + e.get("mean_bin_std_deg") + "°) each time the engine returned to the same speed/load condition.";
            case "driving_behavior":
                return "This inspection drive included " + e.get("hard_accel_events") + " firm-acceleration and " + e.get("hard_decel_events")
                    + " hard-braking event(s) — " + pct(e, "aggressive_fraction") + " of the drive. This describes usage, not a fault.";
            case "signal_quality":
                return flagged
                    ? "Data quality this session was below the usual bar (" + pct(e, "missingness_fraction") + " missing, " + pct(e, "implausible_fraction") + " implausible readings) — treat the other findings above with extra caution."
                    : "Data quality this session was good: " + pct(e, "missingness_fraction") + " missing and " + pct(e, "implausible_fraction") + " implausible readings, well within the expected range.";
            case "hybrid_battery_health":
                return flagged
                    ? "The vehicle reports " + e.get("mean_remaining_life_pct") + "% hybrid battery pack remaining life over OBD-II — below the " + e.get("flag_below_pct") + "% threshold, worth a closer look at the HV battery."
                    : "The vehicle reports " + e.get("mean_remaining_life_pct") + "% hybrid battery pack remaining life over OBD-II — within the normal range.";
            case "overall_condition":
                return flagged
                    ? "Combining every mechanical-condition index above (" + e.get("components_supported") + " supported this session), " + e.get("components_flagged") + " came back flagged — see those specific findings for what to check."
                    : "Combining every mechanical-condition index above (" + e.get("components_supported") + " supported this session), none came back flagged.";
            default:
                return (flagged ? "Flagged: " : "Normal: ") + String.join("; ", r.getContributingPids());
        }
    }

    private static String pct(Map<String, String> e, String key) {
        try { return String.format(Locale.ROOT, "%.1f%%", Double.parseDouble(e.get(key)) * 100); } catch (Exception ex) { return e.get(key); }
    }

    private static String lower(String s) { return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1); }
}
