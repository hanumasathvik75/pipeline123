package com.ignitionai.bms;

import com.ignitionai.obdinput.storage.MiniJson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Loads config/bms-diagnostics-config.json (classpath resource) -- same versioned,
 * reviewable-without-recompiling pattern used across the rest of this codebase. */
public final class BmsDiagnosticsConfig {
    private static final BmsDiagnosticsConfig INSTANCE = new BmsDiagnosticsConfig();
    private final Map<String, Object> root;

    private BmsDiagnosticsConfig() {
        try (InputStream in = BmsDiagnosticsConfig.class.getClassLoader().getResourceAsStream("config/bms-diagnostics-config.json")) {
            if (in == null) throw new IllegalStateException("config/bms-diagnostics-config.json not found on classpath");
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = (Map<String, Object>) MiniJson.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            root = parsed;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load config/bms-diagnostics-config.json", e);
        }
    }

    public static BmsDiagnosticsConfig get() { return INSTANCE; }

    public double number(String key) {
        if (!root.containsKey(key)) throw new IllegalStateException("Missing config value " + key);
        return ((Number) root.get(key)).doubleValue();
    }
}
