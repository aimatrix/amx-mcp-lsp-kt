package com.aimatrix.amxlsp.tools

import com.aimatrix.amxlsp.agent.AmxLspAgent
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Central registry for all available tools
 */
object ToolRegistry {
    private val toolClasses = mutableSetOf<KClass<out Tool>>()
    private val toolsByName = mutableMapOf<String, KClass<out Tool>>()
    
    init {
        // Register all built-in tools
        registerBuiltinTools()
    }
    
    fun register(toolClass: KClass<out Tool>) {
        toolClasses.add(toolClass)
        toolsByName[Tool.getNameFromClass(toolClass)] = toolClass
    }
    
    fun getAll(): Set<KClass<out Tool>> = toolClasses.toSet()
    
    fun getByName(name: String): KClass<out Tool>? = toolsByName[name]
    
    fun createTool(toolClass: KClass<out Tool>, agent: AmxLspAgent): Tool {
        val constructor = toolClass.primaryConstructor
            ?: throw IllegalArgumentException("Tool class must have a primary constructor")
        
        return constructor.call(agent)
    }
    
    fun getToolsForContext(contextName: String): Set<KClass<out Tool>> {
        // TODO: Return tools based on context configuration
        return when (contextName) {
            "desktop-app" -> getDesktopAppTools()
            "agent" -> getAgentTools()
            "ide-assistant" -> getIdeAssistantTools()
            else -> emptySet()
        }
    }
    
    private fun registerBuiltinTools() {
        // File tools
        registerFileTools()
        
        // TODO: Register other tool categories
        // registerSymbolTools()
        // registerMemoryTools()
        // registerConfigTools()
        // registerWorkflowTools()
        // registerCommandTools()
    }
    
    private fun getDesktopAppTools(): Set<KClass<out Tool>> {
        // Tools for desktop application context
        return setOf(
            ReadFileTool::class,
            WriteFileTool::class,
            ListFilesTool::class,
            CreateDirectoryTool::class,
            DeleteFileTool::class
            // Add more tools as implemented
        )
    }
    
    private fun getAgentTools(): Set<KClass<out Tool>> {
        // Tools for agent context (more comprehensive)
        return getDesktopAppTools() // Start with desktop tools
        // Add agent-specific tools
    }
    
    private fun getIdeAssistantTools(): Set<KClass<out Tool>> {
        // Tools for IDE assistant context
        return setOf(
            ReadFileTool::class,
            ListFilesTool::class
            // More restricted set for IDE context
        )
    }
}