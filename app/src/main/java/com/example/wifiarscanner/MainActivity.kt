package com.example.wifiarscanner

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.wifiarscanner.ui.*
import com.example.wifiarscanner.ui.theme.Background
import com.example.wifiarscanner.ui.theme.WiFiARTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.foundation.layout.Box

// Simple navigation state
private enum class Screen {
    SCAN, RESULTS, SUGGESTIONS, SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WiFiARTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                ) {
                    // Build permissions list based on Android version
                    val permissions = buildList {
                        add(Manifest.permission.CAMERA)
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            add(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    }

                    val permissionsState = rememberMultiplePermissionsState(permissions)

                    if (!permissionsState.allPermissionsGranted) {
                        PermissionScreen(permissionsState = permissionsState)
                    } else {
                        AppNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(viewModel: ScanViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.SCAN) }

    when (currentScreen) {
        Screen.SCAN -> ScanScreen(
            viewModel = viewModel,
            onNavigateToResults = { currentScreen = Screen.RESULTS },
            onNavigateToSettings = { currentScreen = Screen.SETTINGS }
        )
        Screen.RESULTS -> HeatmapSummaryScreen(
            viewModel = viewModel,
            onNavigateToSuggestions = { currentScreen = Screen.SUGGESTIONS },
            onNavigateBack = { currentScreen = Screen.SCAN },
            onRescan = {
                viewModel.resetScan()
                currentScreen = Screen.SCAN
            }
        )
        Screen.SUGGESTIONS -> SuggestionsScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.RESULTS },
            onNavigateToSettings = { currentScreen = Screen.SETTINGS }
        )
        Screen.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.SUGGESTIONS }
        )
    }
}
