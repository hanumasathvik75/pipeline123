package com.ignitionai.obdgenerator.config;

import java.io.*;
import java.util.*;

/** Editable scenario format shared by the simulator UI and automated replay tests. */
public final class ScenarioFile {
    public static GeneratorConfig parse(String text) throws IOException {
        Properties p = new Properties(); p.load(new StringReader(text));
        GeneratorConfig c = new GeneratorConfig();
        c.setVehicleId(p.getProperty("vehicle", "SIM-NEXON"));
        if (p.containsKey("start_time")) c.setStartTime(java.time.Instant.parse(p.getProperty("start_time").trim()));
        c.setDurationMs(Long.parseLong(p.getProperty("duration_ms", "60000")));
        c.setRandomSeed(Long.parseLong(p.getProperty("seed", "42")));
        c.setScenarioType(ScenarioType.valueOf(p.getProperty("scenario", "CITY_DRIVING")));
        c.setMissingnessProbability(number(p, "missing_fraction", 0));
        c.setFaultWindow(Long.parseLong(p.getProperty("fault_start_ms", "20000")), Long.parseLong(p.getProperty("fault_end_ms", "45000")));
        List<String> signals = new ArrayList<>();
        Map<String, SignalProgram> programs = new TreeMap<>();
        for (String id : p.getProperty("signals", "engine_rpm,vehicle_speed,coolant_temperature,calculated_engine_load,throttle_position").split(",")) {
            id = id.trim();
            if (id.isEmpty() || signals.contains(id)) throw new IllegalArgumentException("Signal IDs must be nonempty and unique");
            signals.add(id);
            c.getSamplingRatesHz().put(id, number(p, id + ".hz", 1));
            if (p.containsKey(id + ".points")) {
                Map<Long, Double> points = new TreeMap<>();
                for (String point : p.getProperty(id + ".points").split(",")) {
                    String[] parts = point.trim().split(":");
                    if (parts.length != 2) throw new IllegalArgumentException("Use milliseconds:value for " + id);
                    if (points.put(Long.parseLong(parts[0].trim()), Double.parseDouble(parts[1].trim())) != null)
                        throw new IllegalArgumentException("Duplicate trajectory timestamp for " + id);
                }
                programs.put(id, new SignalProgram(p.getProperty(id + ".unit"), points));
            }
            for (String suffix : List.of("bias", "drift_per_second", "noise_variance", "stuck")) {
                if (!p.containsKey(id + "." + suffix)) continue;
                double value = number(p, id + "." + suffix, 0);
                if (suffix.equals("noise_variance") && value < 0) throw new IllegalArgumentException("Noise variance cannot be negative");
                Map<String, Double> target = suffix.equals("bias") ? c.getBias() : suffix.equals("drift_per_second") ? c.getDriftPerSecond() : suffix.equals("noise_variance") ? c.getNoiseVariance() : c.getStuckValues();
                target.put(id, value);
            }
        }
        c.setSupportedSignals(signals); c.setSignalPrograms(programs);
        String codes = p.getProperty("dtcs", "").trim();
        if (!codes.isEmpty()) {
            List<String> dtcs = new ArrayList<>();
            for (String code : codes.split(",")) {
                code = code.trim().toUpperCase(Locale.ROOT);
                if (!code.matches("[PCBU][0-3][0-9A-F]{3}")) throw new IllegalArgumentException("Invalid DTC " + code);
                dtcs.add(code);
            }
            c.setInjectedDtcs(dtcs);
        }
        return c;
    }
    private static double number(Properties p, String key, double fallback) {
        double n = Double.parseDouble(p.getProperty(key, Double.toString(fallback)));
        if (!Double.isFinite(n)) throw new IllegalArgumentException(key + " must be finite");
        return n;
    }
    public static String example() {
        return "# Times are milliseconds; values interpolate between points.\n"
            + "vehicle=SIM-NEXON\nstart_time=2026-09-17T10:00:00Z\nduration_ms=60000\nseed=42\nscenario=CITY_DRIVING\n"
            + "signals=engine_rpm,vehicle_speed,coolant_temperature,calculated_engine_load,throttle_position\n"
            + "missing_fraction=0.02\nfault_start_ms=20000\nfault_end_ms=45000\ndtcs=P0301\n"
            + "engine_rpm.hz=3\nengine_rpm.unit=rpm\nengine_rpm.points=0:800,10000:800,15000:2200,45000:2200,50000:800,60000:800\n"
            + "engine_rpm.noise_variance=100\nengine_rpm.bias=500\n"
            + "vehicle_speed.unit=km/h\nvehicle_speed.points=0:0,10000:0,15000:50,45000:50,50000:0\n"
            + "coolant_temperature.unit=Cel\ncoolant_temperature.points=0:85,60000:90\n"
            + "calculated_engine_load.unit=%\ncalculated_engine_load.points=0:20,15000:45,45000:45,50000:20\n"
            + "throttle_position.unit=%\nthrottle_position.points=0:5,15000:20,45000:20,50000:5\n";
    }
}
