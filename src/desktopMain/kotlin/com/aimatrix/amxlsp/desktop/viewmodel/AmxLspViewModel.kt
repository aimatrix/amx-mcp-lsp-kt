package com.aimatrix.amxlsp.desktop.viewmodel

import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.amxlsp.config.ProjectConfig
import com.aimatrix.amxlsp.config.AmxLspConfig
import com.aimatrix.amxlsp.memory.FileBasedMemoryManager
import com.aimatrix.amxlsp.memory.Memory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

data class AmxLspUiState(
    val status: String = "Initializing...",
    val activeProject: String? = null,
    val projects: List<ProjectConfig> = emptyList(),
    val memoryCount: Int = 0,
    val memories: List<Memory> = emptyList(),
    val toolOutput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class AmxLspViewModel {
    private val agent = AmxLspAgent()
    
    private val _uiState = MutableStateFlow(AmxLspUiState())
    val uiState: StateFlow<AmxLspUiState> = _uiState.asStateFlow()
    
    suspend fun initialize() {
        try {
            updateStatus("Loading configuration...")
            
            // Load configuration
            val config = AmxLspConfig.load()
            agent.config = config
            
            updateStatus("Ready")
            updateProjects()
            updateMemoryCount()
            
            logger.info { "AmxLSP ViewModel initialized successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize ViewModel" }
            updateError("Failed to initialize: ${e.message}")
        }
    }
    
    fun activateProject(projectName: String) {
        try {
            agent.activateProject(projectName)
            _uiState.value = _uiState.value.copy(
                activeProject = projectName,
                error = null
            )
            updateMemoryCount()
            appendOutput("Activated project: $projectName\n")
        } catch (e: Exception) {
            logger.error(e) { "Failed to activate project: $projectName" }
            updateError("Failed to activate project: ${e.message}")
        }
    }
    
    fun createProject(name: String, path: String, language: String?, activate: Boolean = true) {
        try {
            var config = agent.config
            
            if (name in config.projects) {
                updateError("Project '$name' already exists")
                return
            }
            
            val lang = language?.let { 
                com.aimatrix.solidlsp.config.Language.valueOf(it.uppercase()) 
            }
            
            val projectConfig = ProjectConfig(
                projectName = name,
                rootPath = path,
                language = lang
            )
            
            config.projects[name] = projectConfig
            
            if (activate || config.projects.size == 1) {
                config.activeProject = name
            }
            
            config.save()
            
            if (activate) {
                activateProject(name)
            }
            
            updateProjects()
            appendOutput("Successfully created project '$name'\n")
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to create project: $name" }
            updateError("Failed to create project: ${e.message}")
        }
    }
    
    suspend fun executeTool(toolName: String, arguments: Map<String, Any> = emptyMap()) {
        try {
            updateLoading(true)
            appendOutput("> $toolName ${arguments.entries.joinToString(" ") { "${it.key}=${it.value}" }}\n")
            
            // This would use the actual tool registry
            val result = "Tool execution result for $toolName with args: $arguments"
            appendOutput("$result\n\n")
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to execute tool: $toolName" }
            appendOutput("Error: ${e.message}\n\n")
        } finally {
            updateLoading(false)
        }
    }
    
    fun storeMemory(key: String, content: String, tags: List<String> = emptyList()) {
        try {
            val memoryManager = agent.memoriesManager
            if (memoryManager is FileBasedMemoryManager) {
                memoryManager.storeWithTags(key, content, tags)
                updateMemoryCount()
                updateMemories()
                appendOutput("Stored memory with key: $key\n")
            } else {
                updateError("Memory manager not available")
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to store memory: $key" }
            updateError("Failed to store memory: ${e.message}")
        }
    }
    
    fun searchMemories(query: String): List<Memory> {
        return try {
            val memoryManager = agent.memoriesManager
            if (memoryManager is FileBasedMemoryManager) {
                val searchResults = memoryManager.search(query)
                searchResults.mapNotNull { result ->
                    val key = result.substringBefore(":")
                    memoryManager.getAllMemories().find { it.key == key }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to search memories" }
            emptyList()
        }
    }
    
    fun deleteMemory(key: String) {
        try {
            val memoryManager = agent.memoriesManager
            if (memoryManager is FileBasedMemoryManager) {
                val deleted = memoryManager.deleteMemory(key)
                if (deleted) {
                    updateMemoryCount()
                    updateMemories()
                    appendOutput("Deleted memory: $key\n")
                } else {
                    updateError("Memory not found: $key")
                }
            } else {
                updateError("Memory manager not available")
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete memory: $key" }
            updateError("Failed to delete memory: ${e.message}")
        }
    }
    
    private fun updateStatus(status: String) {
        _uiState.value = _uiState.value.copy(status = status, error = null)
    }
    
    private fun updateError(error: String) {
        _uiState.value = _uiState.value.copy(error = error)
    }
    
    private fun updateLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }
    
    private fun updateProjects() {
        val projects = agent.config.projects.values.toList()
        val activeProject = agent.config.activeProject
        
        _uiState.value = _uiState.value.copy(
            projects = projects,
            activeProject = activeProject
        )
    }
    
    private fun updateMemoryCount() {
        try {
            val memoryManager = agent.memoriesManager
            val count = if (memoryManager is FileBasedMemoryManager) {
                memoryManager.getAllMemories().size
            } else {
                0
            }
            
            _uiState.value = _uiState.value.copy(memoryCount = count)
        } catch (e: Exception) {
            logger.error(e) { "Failed to update memory count" }
        }
    }
    
    private fun updateMemories() {
        try {
            val memoryManager = agent.memoriesManager
            val memories = if (memoryManager is FileBasedMemoryManager) {
                memoryManager.getAllMemories()
            } else {
                emptyList()
            }
            
            _uiState.value = _uiState.value.copy(memories = memories)
        } catch (e: Exception) {
            logger.error(e) { "Failed to update memories" }
        }
    }
    
    private fun appendOutput(text: String) {
        val currentOutput = _uiState.value.toolOutput
        _uiState.value = _uiState.value.copy(toolOutput = currentOutput + text)
    }
    
    fun clearOutput() {
        _uiState.value = _uiState.value.copy(toolOutput = "")
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}