package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Discovers every {@link DiagnosticIndexPlugin} on the classpath via ServiceLoader and runs it
 * against the current session. Adding a new plugin never requires editing this class.
 */
public final class DiagnosticIndexRuntime {

    public List<DiagnosticIndexResult> evaluateAll(List<ObdMessage> session, VehicleContext context) {
        List<DiagnosticIndexResult> results = new ArrayList<>();
        for (DiagnosticIndexPlugin plugin : ServiceLoader.load(DiagnosticIndexPlugin.class)) {
            try {
                results.add(plugin.evaluate(session, context));
            } catch (Exception e) {
                results.add(DiagnosticIndexResult.unsupported(plugin.getIndexId(), plugin.getDisplayName(),
                    plugin.getRequiredSignalIds(), "Plugin threw: " + e.getMessage(), plugin.getPluginVersion()));
            }
        }
        return results;
    }
}
