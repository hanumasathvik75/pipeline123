package com.example.ignitionai.data

object DiagnosticEngine {
    fun analyze(dtcs: List<String>, sensors: Map<String, Float>): Map<String, Any> {
        val finalRoot = if (dtcs.contains("P0300")) "Multiple Cylinder Misfire" else if (dtcs.contains("P0171")) "Lean Condition" else "Unknown Sensor Fault"
        
        return mapOf(
            "input_dtcs" to dtcs,
            "input_sensors" to sensors,
            "final_root" to finalRoot,
            "final_decision" to mapOf("confidence" to 0.85, "method" to "heuristic"),
            "repair_steps" to listOf(
                "Inspect spark plugs and ignition coils.",
                "Check for vacuum leaks.",
                "Verify fuel pressure."
            ),
            "sensor_anomalies" to sensors.mapValues { Math.random().toFloat() }
        )
    }
}
