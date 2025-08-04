package com.aimatrix.amxlsp.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aimatrix.amxlsp.desktop.ui.screens.*
import com.aimatrix.amxlsp.desktop.viewmodel.AmxLspViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmxLspApp() {
    val viewModel = remember { AmxLspViewModel() }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("Project", Icons.Default.Home),
        TabItem("Tools", Icons.Default.Build),
        TabItem("Memory", Icons.Default.List),
        TabItem("Config", Icons.Default.Settings)
    )
    
    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.initialize()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aimatrix Master Agent") },
                actions = {
                    IconButton(onClick = { /* TODO: Open settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { /* TODO: Open help */ }) {
                        Icon(Icons.Default.Info, contentDescription = "Help")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status bar
            StatusBar(viewModel)
            
            // Main content based on selected tab
            when (selectedTab) {
                0 -> ProjectScreen(viewModel)
                1 -> ToolsScreen(viewModel)
                2 -> MemoryScreen(viewModel)
                3 -> ConfigScreen(viewModel)
            }
        }
    }
}

@Composable
fun StatusBar(viewModel: AmxLspViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Status: ${uiState.status}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Project: ${uiState.activeProject ?: "None"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Memory: ${uiState.memoryCount} items",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

data class TabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)