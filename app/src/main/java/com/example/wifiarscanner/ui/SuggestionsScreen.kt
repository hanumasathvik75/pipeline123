package com.example.wifiarscanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wifiarscanner.AiState
import com.example.wifiarscanner.ScanViewModel
import com.example.wifiarscanner.ai.Suggestion
import com.example.wifiarscanner.ui.theme.Background
import com.example.wifiarscanner.ui.theme.SurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val offlineSuggestions by viewModel.offlineSuggestions.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val geminiResponse by viewModel.geminiResponse.collectAsState()
    val geminiError by viewModel.geminiError.collectAsState()
    val apiKey by viewModel.geminiApiKey.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Suggestions", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            // Offline suggestions header
            item {
                SectionHeader("📡 Offline Analysis", "Based on your actual scan data")
            }

            // Offline suggestion cards
            items(offlineSuggestions) { suggestion ->
                SuggestionCard(suggestion)
            }

            if (offlineSuggestions.isEmpty()) {
                item {
                    Text("No scan data available.", color = Color.White.copy(0.4f),
                        modifier = Modifier.padding(8.dp))
                }
            }

            // Gemini section divider
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("✨ Gemini AI Enhanced", "Deeper AI-powered insights")
            }

            // Gemini interaction
            item {
                GeminiPanel(
                    aiState = aiState,
                    geminiResponse = geminiResponse,
                    geminiError = geminiError,
                    hasApiKey = apiKey.isNotBlank(),
                    onAskGemini = { viewModel.requestGeminiSuggestions() },
                    onGoToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.White.copy(0.5f), fontSize = 12.sp)
    }
}

@Composable
private fun SuggestionCard(suggestion: Suggestion) {
    val priorityColor = when (suggestion.priority) {
        1 -> Color(0xFFFF6B6B)
        2 -> Color(0xFFFFBB33)
        else -> Color(0xFF4FC3F7)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Priority indicator stripe
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(priorityColor, RoundedCornerShape(2.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(suggestion.emoji, fontSize = 18.sp)
                Text(suggestion.title, color = Color.White, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            Text(suggestion.detail, color = Color.White.copy(0.72f), fontSize = 13.sp,
                lineHeight = 18.sp)
        }
    }
}

@Composable
private fun GeminiPanel(
    aiState: AiState,
    geminiResponse: String?,
    geminiError: String?,
    hasApiKey: Boolean,
    onAskGemini: () -> Unit,
    onGoToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            !hasApiKey -> {
                Text(
                    "🔑 Add your Gemini API key in Settings to unlock enhanced AI suggestions.",
                    color = Color.White.copy(0.7f),
                    fontSize = 13.sp
                )
                Button(
                    onClick = onGoToSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open Settings", color = Color(0xFF0A0E1A), fontWeight = FontWeight.SemiBold)
                }
            }

            aiState == AiState.IDLE -> {
                Text(
                    "Get richer, conversational suggestions from Gemini AI based on your scan.",
                    color = Color.White.copy(0.7f), fontSize = 13.sp
                )
                Button(
                    onClick = onAskGemini,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("✨ Ask Gemini", color = Color(0xFF0A0E1A), fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp)
                }
            }

            aiState == AiState.LOADING -> {
                GeminiLoadingIndicator()
            }

            aiState == AiState.DONE && geminiResponse != null -> {
                GeminiResponseCard(geminiResponse)
                TextButton(onClick = onAskGemini) {
                    Text("🔄 Refresh", color = Color(0xFF4FC3F7))
                }
            }

            aiState == AiState.ERROR -> {
                Text(
                    "⚠️ ${geminiError ?: "Failed to get Gemini response"}",
                    color = Color(0xFFFF6B6B), fontSize = 13.sp
                )
                Button(
                    onClick = onAskGemini,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Retry", color = Color(0xFF0A0E1A))
                }
            }
        }
    }
}

@Composable
private fun GeminiLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("✨ Gemini is analyzing your scan...", color = Color.White.copy(alpha),
            fontSize = 14.sp, fontWeight = FontWeight.Medium)
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF4FC3F7),
            trackColor = Color.White.copy(0.1f)
        )
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .alpha(alpha * 0.5f)
                    .background(Color.White.copy(0.12f), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun GeminiResponseCard(response: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✨", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text("Gemini's Analysis", color = Color(0xFF4FC3F7),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            response,
            color = Color.White.copy(0.88f),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}
