package com.ignitionai.virtualsensors.index;

import com.ignitionai.obd.SignalIds;
import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.schema.QualityStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Shared helpers for pulling clean (timestamp, value) series out of a session for index plugins. */
public final class SeriesUtil {
    private SeriesUtil() {}

    public static final class Sample {
        public final long t;
        public final double v;
        public Sample(long t, double v) { this.t = t; this.v = v; }
    }

    /** Valid, finite, monotonically-ordered readings for one canonical signal id, across the whole session. */
    public static List<Sample> series(List<ObdMessage> session, String signalId) {
        String canonical = SignalIds.canonical(signalId);
        List<Sample> out = new ArrayList<>();
        long lastT = Long.MIN_VALUE;
        for (ObdMessage m : session) {
            for (var r : m.getSensorReadings()) {
                if (!SignalIds.canonical(r.getSignalId()).equals(canonical)) continue;
                if (r.getQuality().getStatus() != QualityStatus.VALID || !Double.isFinite(r.getValue())) continue;
                long t = r.getMonotonicMs();
                if (t < lastT) continue;
                out.add(new Sample(t, r.getValue()));
                lastT = t;
            }
        }
        return out;
    }

    public static boolean hasSignal(List<ObdMessage> session, String signalId) {
        String canonical = SignalIds.canonical(signalId);
        for (ObdMessage m : session) for (var r : m.getSensorReadings())
            if (SignalIds.canonical(r.getSignalId()).equals(canonical) && r.getQuality().getStatus() == QualityStatus.VALID) return true;
        return false;
    }

    public static java.util.Set<String> observedSignalIds(List<ObdMessage> session) {
        java.util.Set<String> ids = new TreeSet<>();
        for (ObdMessage m : session) for (var r : m.getSensorReadings())
            if (r.getQuality().getStatus() == QualityStatus.VALID) ids.add(SignalIds.canonical(r.getSignalId()));
        return ids;
    }

    public static double mean(List<Double> xs) {
        double s = 0; for (double x : xs) s += x; return xs.isEmpty() ? Double.NaN : s / xs.size();
    }

    public static double stddev(List<Double> xs) {
        if (xs.size() < 2) return Double.NaN;
        double m = mean(xs), s = 0;
        for (double x : xs) s += (x - m) * (x - m);
        return Math.sqrt(s / (xs.size() - 1));
    }

    public static double median(List<Double> xs) {
        if (xs.isEmpty()) return Double.NaN;
        List<Double> sorted = new ArrayList<>(xs); java.util.Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    public static double clamp01(double x) { return Math.max(0.0, Math.min(1.0, x)); }

    /** Nearest sample to timestamp t, or null if the series is empty. Series must be time-sorted (series() guarantees this). */
    public static Sample nearest(List<Sample> series, long t) {
        Sample best = null;
        long bestDist = Long.MAX_VALUE;
        for (Sample s : series) {
            long dist = Math.abs(s.t - t);
            if (dist < bestDist) { bestDist = dist; best = s; }
            if (s.t > t + 3000) break;
        }
        return best;
    }

    public static double pearson(List<Double> a, List<Double> b) {
        double ma = mean(a), mb = mean(b), num = 0, da = 0, db = 0;
        for (int i = 0; i < a.size(); i++) {
            double xa = a.get(i) - ma, xb = b.get(i) - mb;
            num += xa * xb; da += xa * xa; db += xb * xb;
        }
        return (da == 0 || db == 0) ? 0 : num / Math.sqrt(da * db);
    }
}
