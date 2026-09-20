package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.List;

/**
 * A composite, session-level diagnostic index (e.g. "Combustion", "Thermal").
 * Unlike {@link com.ignitionai.virtualsensors.VirtualSensorPlugin}, which produces one
 * derived signal value per timestamp, a DiagnosticIndexPlugin evaluates the whole capture
 * session at once and returns a health/confidence/state/evidence summary.
 *
 * Auto-discovered via java.util.ServiceLoader. To add a new index: implement this interface,
 * package it inside the virtual-sensors module (or on the classpath), and list its fully
 * qualified class name in
 * virtual-sensors/src/main/resources/META-INF/services/com.ignitionai.virtualsensors.index.DiagnosticIndexPlugin
 * No existing file needs to change.
 */
public interface DiagnosticIndexPlugin {
    /** Stable lowercase identifier, e.g. "combustion". Used as the evidence/report row key. */
    String getIndexId();

    /** Human-readable name shown on the certificate, e.g. "Combustion". */
    String getDisplayName();

    /** Version string for this plugin's method, e.g. "rpm-residual-std.v1". Recorded on every result. */
    String getPluginVersion();

    /** Canonical signal IDs (see SignalIds.canonical) this index needs. Used to skip evaluation when unsupported. */
    List<String> getRequiredSignalIds();

    /** Evaluate over the full normalized session. Must not throw; return UNSUPPORTED/INSUFFICIENT_DATA instead. */
    DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context);
}
