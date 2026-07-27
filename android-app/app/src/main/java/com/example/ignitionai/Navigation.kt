package com.example.ignitionai

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.ignitionai.ui.*
import com.example.ignitionai.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Dashboard)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.height(32.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDarkest
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BgDarkest) {
                val currentRoute = backStack.lastOrNull()
                NavigationBarItem(
                    selected = currentRoute == Dashboard,
                    onClick = { backStack.add(Dashboard) },
                    icon = { Text("Dash") }
                )
                NavigationBarItem(
                    selected = currentRoute == Scan,
                    onClick = { backStack.add(Scan) },
                    icon = { Text("Scan") }
                )
                NavigationBarItem(
                    selected = currentRoute == Results,
                    onClick = { backStack.add(Results) },
                    icon = { Text("Results") }
                )
                NavigationBarItem(
                    selected = currentRoute == AIReport,
                    onClick = { backStack.add(AIReport) },
                    icon = { Text("AI") }
                )
            }
        }
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Dashboard> {
                    DashboardScreen(onNavigate = { backStack.add(it) })
                }
                entry<Scan> {
                    ScanScreen(onNavigate = { backStack.add(it) })
                }
                entry<Results> {
                    ResultsScreen(onNavigate = { backStack.add(it) })
                }
                entry<AIReport> {
                    AIReportScreen()
                }
            },
            modifier = Modifier.padding(padding)
        )
    }
}
