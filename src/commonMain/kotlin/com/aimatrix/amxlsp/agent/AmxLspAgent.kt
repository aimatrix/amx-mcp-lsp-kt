package com.aimatrix.amxlsp.agent

import com.aimatrix.amxlsp.config.AmxLspConfig
import com.aimatrix.amxlsp.config.context.AmxLspAgentContext
import com.aimatrix.amxlsp.config.context.AmxLspAgentMode
import com.aimatrix.amxlsp.project.Project
import com.aimatrix.amxlsp.prompt.DefaultPromptFactory
import com.aimatrix.amxlsp.prompt.PromptFactory
import com.aimatrix.amxlsp.tools.Tool
import com.aimatrix.solidlsp.SolidLanguageServer
import mu.KotlinLogging
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

// Placeholder interfaces for agent components
interface LinesRead {
    fun addLine(line: String)
    fun getLines(): List<String>
}

interface MemoriesManager {
    fun store(key: String, value: String)
    fun retrieve(key: String): String?
    fun search(query: String): List<String>
}

class AmxLspAgent(
    var config: AmxLspConfig = AmxLspConfig.load()
) {
    var activeProject: Project? = null
        private set
    
    var languageServer: SolidLanguageServer? = null
        private set
    
    var memoriesManager: MemoriesManager? = null
    
    var linesRead: LinesRead? = null
        private set
    
    val promptFactory: PromptFactory = DefaultPromptFactory()
    
    private val activeTools = mutableSetOf<KClass<out Tool>>()
    private var context: AmxLspAgentContext? = null
    private val modes = mutableListOf<AmxLspAgentMode>()
    
    fun getProjectRoot(): String {
        return activeProject?.projectRoot
            ?: throw IllegalStateException("No active project")
    }
    
    fun getActiveProjectOrRaise(): Project {
        return activeProject
            ?: throw IllegalStateException("No active project. Use ActivateProject to select or create a project first.")
    }
    
    fun isUsingLanguageServer(): Boolean {
        return languageServer != null
    }
    
    fun toolIsActive(toolClass: KClass<out Tool>): Boolean {
        return toolClass in activeTools
    }
    
    fun getActiveToolNames(): List<String> {
        return activeTools.map { Tool.getNameFromClass(it) }.sorted()
    }
    
    fun activateProject(projectName: String) {
        val projectConfig = config.projects[projectName]
            ?: throw IllegalArgumentException("Project '$projectName' not found")
        
        activeProject = Project(
            projectRoot = projectConfig.rootPath,
            projectConfig = projectConfig
        )
        
        logger.info { "Activated project: $projectName" }
    }
    
    fun setContext(context: AmxLspAgentContext) {
        this.context = context
        updateActiveTools()
    }
    
    fun addMode(mode: AmxLspAgentMode) {
        modes.add(mode)
        updateActiveTools()
    }
    
    fun removeMode(mode: AmxLspAgentMode) {
        modes.remove(mode)
        updateActiveTools()
    }
    
    private fun updateActiveTools() {
        activeTools.clear()
        
        // Add tools from context
        context?.let { ctx ->
            activeTools.addAll(ctx.getEnabledTools())
        }
        
        // Add/remove tools from modes
        modes.forEach { mode ->
            activeTools.addAll(mode.getEnabledTools())
            activeTools.removeAll(mode.getDisabledTools().toSet())
        }
    }
    
    fun initializeLanguageServer() {
        val project = getActiveProjectOrRaise()
        languageServer = project.createLanguageServer()
        logger.info { "Initialized language server for project: ${project.projectName}" }
    }
    
    suspend fun shutdown() {
        languageServer?.stop()
        languageServer = null
        activeProject = null
        logger.info { "AmxLSP agent shutdown complete" }
    }
}