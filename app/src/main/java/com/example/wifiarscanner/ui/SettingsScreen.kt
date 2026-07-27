package com.example.wifiarscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wifiarscanner.ScanViewModel
import com.example.wifiarscanner.ui.theme.Background
import com.example.wifiarscanner.ui.theme.SurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit
) {
    val currentKey by viewModel.geminiApiKey.collectAsState()
    var keyInput by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Gemini API key section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("✨ Gemini AI (Optional)",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Add a free Gemini API key to unlock enhanced AI-powered WiFi suggestions. " +
                        "Get one at aistudio.google.com/apikey — it's free.",
                    color = Color.White.copy(0.65f), fontSize = 13.sp, lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        saved = false
                    },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIza...", color = Color.White.copy(0.3f)) },
                    visualTransformation = if (showKey) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White.copy(0.5f)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        viewModel.saveApiKey(keyInput)
                        saved = true
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4FC3F7),
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        focusedLabelColor = Color(0xFF4FC3F7),
                        unfocusedLabelColor = Color.White.copy(0.4f),
                        cursorColor = Color(0xFF4FC3F7)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            keyboard?.hide()
                            viewModel.saveApiKey(keyInput)
                            saved = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Key", color = Color(0xFF0A0E1A), fontWeight = FontWeight.SemiBold)
                    }
                    if (keyInput.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                keyInput = ""
                                viewModel.saveApiKey("")
                                saved = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF6B6B)
                            )
                        ) {
                            Text("Clear")
                        }
                    }
                }

                if (saved) {
                    Text("✅ API key saved!", color = Color(0xFF00FF88), fontSize = 13.sp)
                }

                if (currentKey.isNotBlank() && !saved) {
                    Text(
                        "🔑 Key configured (${currentKey.take(8)}...)",
                        color = Color(0xFF4FC3F7), fontSize = 12.sp
                    )
                }
            }

            // About section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("ℹ️ About", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                InfoRow("📶", "WiFi Signal", "Reads live RSSI from connected network")
                InfoRow("📍", "Position", "Dead-reckoning via step detector + compass")
                InfoRow("🎨", "Heatmap", "Canvas-based spatial gradient overlay")
                InfoRow("🧠", "Offline AI", "Rule-based analysis — works without internet")
                InfoRow("✨", "Gemini AI", "Optional — requires API key + internet")
            }

            // Privacy note
            Text(
                "🔒 No data leaves your device unless you enable Gemini AI. " +
                    "Scan data is stored only in RAM and cleared on reset.",
                color = Color.White.copy(0.4f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun InfoRow(emoji: String, label: String, description: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(emoji, fontSize = 16.sp, modifier = Modifier.width(24.dp))
        Column {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = Color.White.copy(0.5f), fontSize = 12.sp)
        }
    }
}
