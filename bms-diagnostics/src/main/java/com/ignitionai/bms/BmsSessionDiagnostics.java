package com.ignitionai.bms;

import java.util.ArrayList;
import java.util.List;

/**
 * Real, single-session BMS checks -- no cross-session history required. Three independently
 * meaningful signals, each already present or directly derivable from the raw telemetry:
 *
 * 1. Cell voltage imbalance -- the source data already computes this per row ("Voltage Imbalance
 *    (V)"); we take the session max/mean of it rather than recomputing from the 14 cell voltages,
 *    since the source's own computation is authoritative and cheaper to trust than re-deriving.
 * 2. SoC estimator disagreement -- SoC-EKF and SoC-Coulomb-Count are two independently computed
 *    state-of-charge estimates already in the data; a real BMS runs both specifically so they can
 *    cross-check each other. Their disagreement is honest evidence of estimator drift, not
 *    something invented for this project.
 * 3. Thermal spread across the pack's 3 cell-temperature sensors -- uneven cooling/airflow shows
 *    up as spread between sensors before it shows up as an absolute over-temperature.
 *
 * Thresholds live in config/bms-diagnostics-config.json, not as literals here.
 */
public final class BmsSessionDiagnostics {
    private BmsSessionDiagnostics() {}

    public static BmsSessionDiagnosticsResult evaluate(String sessionId, List<BmsReading> readings) {
        BmsDiagnosticsConfig cfg = BmsDiagnosticsConfig.get();
        List<Double> imbalances = new ArrayList<>();
        List<Double> socDisagreements = new ArrayList<>();
        List<Double> thermalSpreads = new ArrayList<>();
        List<Double> maxCellTemps = new ArrayList<>();
        List<Double> fetTemps = new ArrayList<>();
        List<Double> deratedDischarge = new ArrayList<>();
        Double representativeSoh = null;

        for (BmsReading r : readings) {
            if (r.voltageImbalanceV != null) imbalances.add(r.voltageImbalanceV);
            if (r.socEkfPct != null && r.socCoulombPct != null) socDisagreements.add(Math.abs(r.socEkfPct - r.socCoulombPct));
            Double t1 = r.cellTemp1C, t2 = r.cellTemp2C, t3 = r.cellTemp3C;
            if (t1 != null && t2 != null && t3 != null) {
                double max = Math.max(t1, Math.max(t2, t3)), min = Math.min(t1, Math.min(t2, t3));
                thermalSpreads.add(max - min);
                maxCellTemps.add(max);
            }
            if (r.fetTempC != null) fetTemps.add(r.fetTempC);
            if (!maxCellTemps.isEmpty() && r.deratedDischargeCurrentA != null) deratedDischarge.add(r.deratedDischargeCurrentA);
            if (representativeSoh == null && r.packSohPct != null) representativeSoh = r.packSohPct;
        }

        Double meanImbalance = mean(imbalances), maxImbalance = max(imbalances);
        Double meanSocDis = mean(socDisagreements), maxSocDis = max(socDisagreements);
        Double maxSpread = max(thermalSpreads), maxCellTemp = max(maxCellTemps), maxFet = max(fetTemps);
        Double deratingCorrelation = (maxCellTemps.size() == deratedDischarge.size() && maxCellTemps.size() > 5)
            ? pearson(maxCellTemps, deratedDischarge) : null;

        boolean cellImbalanceFlagged = maxImbalance != null && maxImbalance > cfg.number("cell_imbalance_flag_v");
        boolean socDisagreementFlagged = maxSocDis != null && maxSocDis > cfg.number("soc_disagreement_flag_pct");
        boolean thermalSpreadFlagged = maxSpread != null && maxSpread > cfg.number("thermal_spread_flag_c");

        double confidence = Math.max(0.0, Math.min(1.0, readings.size() / cfg.number("min_readings_for_session_confidence")));

        return new BmsSessionDiagnosticsResult(sessionId, readings.size(), meanImbalance, maxImbalance,
            meanSocDis, maxSocDis, maxSpread, maxCellTemp, maxFet, deratingCorrelation,
            cellImbalanceFlagged, socDisagreementFlagged, thermalSpreadFlagged, representativeSoh, confidence);
    }

    private static Double mean(List<Double> xs) {
        if (xs.isEmpty()) return null;
        double s = 0; for (double x : xs) s += x; return s / xs.size();
    }

    private static Double max(List<Double> xs) {
        if (xs.isEmpty()) return null;
        double m = xs.get(0); for (double x : xs) m = Math.max(m, x); return m;
    }

    private static double pearson(List<Double> a, List<Double> b) {
        double ma = mean(a), mb = mean(b), num = 0, da = 0, db = 0;
        for (int i = 0; i < a.size(); i++) {
            double xa = a.get(i) - ma, xb = b.get(i) - mb;
            num += xa * xb; da += xa * xa; db += xb * xb;
        }
        return (da == 0 || db == 0) ? 0 : num / Math.sqrt(da * db);
    }
}
