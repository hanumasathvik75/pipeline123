package com.example.ignitionai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.ignitionai.AIReport
import com.example.ignitionai.theme.*

@Composable
fun ResultsScreen(onNavigate: (NavKey) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Results", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Root Cause", color = TextSecondary, fontSize = 14.sp)
                Text("Multiple Cylinder Misfire", color = DangerRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Confidence: 85%", color = SuccessGreen, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Recommended Repair Steps", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("1. Inspect spark plugs and ignition coils.", color = TextSecondary)
        Text("2. Check for vacuum leaks.", color = TextSecondary)
        Text("3. Verify fuel pressure.", color = TextSecondary)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { onNavigate(AIReport) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text("Get AI Explanation")
        }
    }
}
