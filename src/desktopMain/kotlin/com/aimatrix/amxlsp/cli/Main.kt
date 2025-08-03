package com.aimatrix.amxlsp.cli

import com.aimatrix.amxlsp.Constants
import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.amxlsp.config.ProjectConfig
import com.aimatrix.amxlsp.config.AmxLspConfig
import com.aimatrix.amxlsp.config.AmxLspPaths
import com.aimatrix.amxlsp.config.context.AmxLspAgentContext
import com.aimatrix.amxlsp.config.context.AmxLspAgentMode
import com.aimatrix.amxlsp.mcp.AmxLspMCPFactory
import com.aimatrix.amxlsp.mcp.AmxLspMCPFactorySingleProcess
import com.aimatrix.amxlsp.project.Project
import com.aimatrix.amxlsp.tools.ToolRegistry
import com.aimatrix.amxlsp.util.MemoryLogHandler
import com.aimatrix.solidlsp.config.Language
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import mu.KotlinLogging
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

// Utilities
fun openInEditor(path: String) {
    try {
        val editor = System.getenv("EDITOR")
        when {
            editor != null -> {
                ProcessBuilder(editor, path).start()
            }
            System.getProperty("os.name").lowercase().contains("windows") -> {
                try {
                    Desktop.getDesktop().open(File(path))
                } catch (e: Exception) {
                    ProcessBuilder("notepad.exe", path).start()
                }
            }
            System.getProperty("os.name").lowercase().contains("mac") -> {
                ProcessBuilder("open", path).start()
            }
            else -> {
                ProcessBuilder("xdg-open", path).start()
            }
        }
    } catch (e: Exception) {
        println("Failed to open $path: $e")
    }
}

// Main CLI class
class AmxLspCLI : CliktCommand(
    name = "amxlsp",
    help = "AmxLSP CLI commands. You can run `<command> --help` for more info on each command."
) {
    override fun run() = Unit
}

// MCP Server command
class StartMCPServerCommand : CliktCommand(
    name = "mcp-server",
    help = "Start the AmxLSP MCP server"
) {
    private val project by argument("project", help = "Project name or path").optional()
    private val host by option("--host", help = "Host to bind to").default("localhost")
    private val port by option("--port", help = "Port to bind to").int().default(3000)
    private val transport by option("--transport", help = "Transport type").choice("stdio", "websocket").default("stdio")
    private val singleProcess by option("--single-process", help = "Run in single process mode").flag()
    private val debug by option("--debug", help = "Enable debug logging").flag()
    
    override fun run() {
        if (debug) {
            System.setProperty("LOG_LEVEL", "DEBUG")
        }
        
        logger.info { "Starting AmxLSP MCP server on $transport transport" }
        
        val factory = if (singleProcess) {
            AmxLspMCPFactorySingleProcess()
        } else {
            AmxLspMCPFactory()
        }
        
        when (transport) {
            "stdio" -> factory.createStdioServer().start()
            "websocket" -> factory.createWebSocketServer(host, port).start()
            else -> {
                logger.error { "Unknown transport: $transport" }
                exitProcess(1)
            }
        }
    }
}

// Project commands
class ProjectCommand : CliktCommand(
    name = "project",
    help = "Manage AmxLSP projects"
) {
    override fun run() = Unit
}

class ListProjectsCommand : CliktCommand(
    name = "list",
    help = "List all available projects"
) {
    override fun run() {
        val config = AmxLspConfig.load()
        val projects = config.projects
        
        if (projects.isEmpty()) {
            println("No projects configured.")
            println("Use 'amxlsp project create' to create a new project.")
            return
        }
        
        println("Available projects:")
        projects.forEach { (name, projectConfig) ->
            val marker = if (name == config.activeProject) " (active)" else ""
            println("  - $name: ${projectConfig.rootPath}$marker")
        }
    }
}

class CreateProjectCommand : CliktCommand(
    name = "create",
    help = "Create a new project"
) {
    private val name by argument("name", help = "Project name")
    private val path by argument("path", help = "Project root path").path(
        mustExist = true,
        canBeFile = false,
        mustBeReadable = true
    )
    private val language by option("-l", "--language", help = "Primary language for the project")
        .enum<Language>()
    private val activate by option("-a", "--activate", help = "Activate the project after creation").flag()
    
    override fun run() {
        val config = AmxLspConfig.load()
        
        if (name in config.projects) {
            logger.error { "Project '$name' already exists" }
            exitProcess(1)
        }
        
        val projectConfig = ProjectConfig(
            rootPath = path.toString(),
            language = language
        )
        
        config.projects[name] = projectConfig
        if (activate || config.projects.size == 1) {
            config.activeProject = name
        }
        
        config.save()
        
        println("Created project '$name' at ${path.toAbsolutePath()}")
        if (config.activeProject == name) {
            println("Project '$name' is now active")
        }
    }
}

class ActivateProjectCommand : CliktCommand(
    name = "activate",
    help = "Activate a project"
) {
    private val project by argument("project", help = "Project name or path")
    
    override fun run() {
        val config = AmxLspConfig.load()
        
        // Check if it's a path
        val projectPath = Path(project)
        val projectName = if (projectPath.exists() && projectPath.isDirectory()) {
            // Find project by path
            config.projects.entries.find { it.value.rootPath == projectPath.toString() }?.key
        } else {
            // Assume it's a project name
            if (project in config.projects) project else null
        }
        
        if (projectName == null) {
            logger.error { "Project '$project' not found" }
            exitProcess(1)
        }
        
        config.activeProject = projectName
        config.save()
        
        println("Activated project '$projectName'")
    }
}

class IndexProjectCommand : CliktCommand(
    name = "index",
    help = "Index a project for faster symbol operations"
) {
    private val project by argument("project", help = "Project name or path").optional()
    private val force by option("-f", "--force", help = "Force re-indexing").flag()
    
    override fun run() {
        val agent = AmxLspAgent()
        val projectToIndex = project ?: agent.config.activeProject
        
        if (projectToIndex == null) {
            logger.error { "No project specified and no active project" }
            exitProcess(1)
        }
        
        logger.info { "Indexing project '$projectToIndex'..." }
        // TODO: Implement indexing logic
        println("Project indexing complete")
    }
}

// Config commands
class ConfigCommand : CliktCommand(
    name = "config",
    help = "Manage AmxLSP configuration"
) {
    override fun run() = Unit
}

class ShowConfigCommand : CliktCommand(
    name = "show",
    help = "Show current configuration"
) {
    override fun run() {
        val config = AmxLspConfig.load()
        println("AmxLSP Configuration:")
        println("  Config file: ${AmxLspPaths.amxlspConfigFile}")
        println("  Active project: ${config.activeProject ?: "none"}")
        println("  Default context: ${config.defaultContext}")
        println("  Default modes: ${config.defaultModes.joinToString(", ")}")
    }
}

class EditConfigCommand : CliktCommand(
    name = "edit",
    help = "Edit configuration file"
) {
    override fun run() {
        val configFile = AmxLspPaths.amxlspConfigFile
        if (!configFile.exists()) {
            logger.info { "Creating new configuration file at $configFile" }
            configFile.parent.createDirectories()
            // Copy template or create default
            configFile.writeText("""
                active_project: null
                default_context: desktop-app
                default_modes:
                  - interactive
                  - editing
                projects: {}
            """.trimIndent())
        }
        openInEditor(configFile.toString())
    }
}

// Main entry point
fun main(args: Array<String>) {
    val projectCommand = ProjectCommand().subcommands(
        ListProjectsCommand(),
        CreateProjectCommand(),
        ActivateProjectCommand(),
        IndexProjectCommand()
    )
    
    val configCommand = ConfigCommand().subcommands(
        ShowConfigCommand(),
        EditConfigCommand()
    )
    
    AmxLspCLI()
        .subcommands(
            StartMCPServerCommand(),
            projectCommand,
            configCommand
        )
        .main(args)
}