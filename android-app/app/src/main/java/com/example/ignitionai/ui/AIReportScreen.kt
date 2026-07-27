package com.example.ignitionai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.ignitionai.theme.*
import com.example.ignitionai.data.NvidiaApiClient
import com.example.ignitionai.data.ChatRequest
import com.example.ignitionai.data.Message
import com.example.ignitionai.data.Config

@Composable
fun AIReportScreen() {
    var aiResponse by remember { mutableStateOf("Press 'Generate' to fetch the AI narrative using Nvidia API.") }
    var isGenerating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("AI Diagnosis", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = AccentBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Generating professional plain-English narrative...", color = TextSecondary)
                } else {
                    Text(text = aiResponse, color = TextSecondary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                isGenerating = true
                coroutineScope.launch {
                    try {
                        val prompt = "Generate a plain english repair narrative for DTC P0300 (Multiple Cylinder Misfire). Make it professional."
                        val request = ChatRequest(
                            model = Config.NVIDIA_MODEL_NAME,
                            messages = listOf(Message(role = "user", content = prompt))
                        )
                        val response = NvidiaApiClient.retrofitService.generateContent(request)
                        aiResponse = response.choices.firstOrNull()?.message?.content ?: "No response from AI."
                    } catch (e: Exception) {
                        aiResponse = "Error: ${e.message}"
                    } finally {
                        isGenerating = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text("Generate AI Report")
        }
    }
}
