package com.example.ignitionai.data

import kotlin.random.Random

object Simulator {

    data class Scenario(
        val name: String,
        val dtcs: List<String>,
        val description: String,
        val sensors: Map<String, Pair<Float, Float>>
    )

    val SIMULATION_SCENARIOS = listOf(
        Scenario(
            name = "Random Misfire Engine",
            dtcs = listOf("P0300", "P0301"),
            description = "Multiple cylinder misfires with poor ignition",
            sensors = mapOf(
                "engine_rpm" to Pair(650f, 820f),
                "crankshaft_position" to Pair(5f, 25f),
                "camshaft_position" to Pair(690f, 720f),
                "ignition_timing" to Pair(-15f, -5f)
            )
        ),
        Scenario(
            name = "Fuel Starvation (Lean)",
            dtcs = listOf("P0087", "P0171"),
            description = "Low fuel pressure causing lean condition",
            sensors = mapOf(
                "fuel_rail_pressure" to Pair(10f, 28f),
                "o2_sensor_b1s1" to Pair(0.1f, 0.3f),
                "short_term_fuel_trim_b1" to Pair(16f, 23f)
            )
        ),
        Scenario(
            name = "Engine Overheating",
            dtcs = listOf("P0115", "P0116"),
            description = "Cooling system fault / stuck thermostat",
            sensors = mapOf(
                "ect_sensor" to Pair(115f, 135f),
                "iat_sensor" to Pair(35f, 55f),
                "cooling_fan_duty" to Pair(90f, 100f)
            )
        )
    )

    fun simulateScan(scenarioName: String): Map<String, Any> {
        val scenario = SIMULATION_SCENARIOS.find { it.name == scenarioName }
            ?: return emptyMap()

        val sensorValues = mutableMapOf<String, Float>()
        for ((sensorName, range) in scenario.sensors) {
            val base = range.first + Random.nextFloat() * (range.second - range.first)
            // Add slight noise
            val noise = (Random.nextFloat() - 0.5f) * (range.second - range.first) * 0.05f
            sensorValues[sensorName] = base + noise
        }

        return mapOf(
            "dtcs" to scenario.dtcs,
            "description" to scenario.description,
            "sensors" to sensorValues
        )
    }
}
