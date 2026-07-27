package com.example.ignitionai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.example.ignitionai.Results
import com.example.ignitionai.theme.*
import com.example.ignitionai.data.Simulator

@Composable
fun ScanScreen(onNavigate: (NavKey) -> Unit) {
    var dtcs by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedScenario by remember { mutableStateOf("Select a Simulator Scenario") }
    var simulatedSensors by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Scan Vehicle", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Simulator Dropdown
        Box {
            OutlinedTextField(
                value = selectedScenario,
                onValueChange = {},
                readOnly = true,
                label = { Text("OBD Simulator") },
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = TextPrimary,
                    disabledBorderColor = AccentBlue,
                    disabledLabelColor = TextSecondary
                )
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Simulator.SIMULATION_SCENARIOS.forEach { scenario ->
                    DropdownMenuItem(
                        text = { Text(scenario.name) },
                        onClick = {
                            selectedScenario = scenario.name
                            expanded = false
                            val result = Simulator.simulateScan(scenario.name)
                            dtcs = (result["dtcs"] as? List<*>)?.joinToString(", ") ?: ""
                            simulatedSensors = result["sensors"] as? Map<String, Float> ?: emptyMap()
                        }
                    )
                }
            }
            // Transparent overlay to catch clicks
            Spacer(modifier = Modifier.matchParentSize().clickable { expanded = true })
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = dtcs,
            onValueChange = { dtcs = it },
            label = { Text("Fault Codes (comma separated)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = TextSecondary,
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (simulatedSensors.isNotEmpty()) {
            Text("Simulated Sensor Data:", color = TextSecondary)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(simulatedSensors.toList()) { (key, value) ->
                    Text("$key: ${String.format("%.2f", value)}", color = TextPrimary)
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        Button(
            onClick = { onNavigate(Results) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text("Run Analysis")
        }
    }
}
