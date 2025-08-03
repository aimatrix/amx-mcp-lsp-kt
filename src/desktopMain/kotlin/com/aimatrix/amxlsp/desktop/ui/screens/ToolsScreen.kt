package com.aimatrix.amxlsp.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aimatrix.amxlsp.desktop.viewmodel.AmxLspViewModel
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(viewModel: AmxLspViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Tools",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick tools section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Quick Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickTools) { tool ->
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    viewModel.executeTool(tool.name, tool.defaultArgs)
                                }
                            },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(tool.icon, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tool.displayName)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tool execution section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tool Execution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                var command by remember { mutableStateOf("") }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("Command") },
                        placeholder = { Text("e.g., GetConfigTool") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !uiState.isLoading
                    )
                    
                    Button(
                        onClick = {
                            if (command.isNotBlank()) {
                                scope.launch {
                                    viewModel.executeTool(command.trim())
                                }
                                command = ""
                            }
                        },
                        enabled = !uiState.isLoading && command.isNotBlank()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Execute")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Execute")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Output section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Output",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    TextButton(
                        onClick = { viewModel.clearOutput() }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    SelectionContainer {
                        Text(
                            text = uiState.toolOutput.ifEmpty { "No output yet. Execute a tool to see results here." },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.toolOutput.isEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

data class QuickTool(
    val name: String,
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val defaultArgs: Map<String, Any> = emptyMap()
)

private val quickTools = listOf(
    QuickTool("GetConfigTool", "Config", Icons.Default.Settings),
    QuickTool("ListProjectsTool", "Projects", Icons.Default.Home),
    QuickTool("GetMemoryStatsTool", "Memory Stats", Icons.Default.Info),
    QuickTool("GetProjectInfoTool", "Project Info", Icons.Default.Info),
    QuickTool("ListFilesTool", "List Files", Icons.Default.List),
    QuickTool("FindSymbolTool", "Find Symbol", Icons.Default.Search, mapOf("symbol_name" to "main")),
)