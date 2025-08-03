package com.aimatrix.amxlsp.tools

import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.amxlsp.config.ProjectConfig
import com.aimatrix.amxlsp.config.AmxLspConfig
import com.aimatrix.amxlsp.config.AmxLspPaths
import com.aimatrix.amxlsp.memory.FileBasedMemoryManager
import com.aimatrix.solidlsp.config.Language
import java.nio.file.Paths

@ToolDescription("Activate a project")
class ActivateProjectTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val projectName = kwargs["project_name"] as? String
        val projectPath = kwargs["project_path"] as? String
        
        if (projectName == null && projectPath == null) {
            throw IllegalArgumentException("Either 'project_name' or 'project_path' is required")
        }
        
        return try {
            when {
                projectName != null -> {
                    // Activate by name
                    var config = agent.config
                    if (projectName !in config.projects) {
                        "Error: Project '$projectName' not found. Available projects: ${config.projects.keys.joinToString(", ")}"
                    } else {
                        agent.activateProject(projectName)
                        
                        // Initialize language server if needed
                        val project = agent.getActiveProjectOrRaise()
                        if (project.language != null) {
                            agent.initializeLanguageServer()
                        }
                        
                        "Successfully activated project: $projectName"
                    }
                }
                projectPath != null -> {
                    // Activate by path - load or create project
                    val path = Paths.get(projectPath)
                    if (!path.toFile().exists()) {
                        "Error: Path does not exist: $projectPath"
                    } else {
                        val project = com.aimatrix.amxlsp.project.Project.load(path)
                        
                        // Add to agent's active project  
                        agent.activateProject(project.projectName)
                        
                        // Initialize language server if needed
                        if (project.language != null) {
                            agent.initializeLanguageServer()
                        }
                        
                        "Successfully activated project at: $projectPath"
                    }
                }
                else -> "Error: No project specified"
            }
        } catch (e: Exception) {
            "Error activating project: ${e.message}"
        }
    }
}

@ToolDescription("List all available projects")
class ListProjectsTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        return try {
            var config = agent.config
            
            if (config.projects.isEmpty()) {
                "No projects configured. Use CreateProject to create a new project."
            } else {
                buildString {
                    appendLine("Available projects:")
                    appendLine()
                    
                    config.projects.forEach { (name, projectConfig) ->
                        val marker = if (name == config.activeProject) " (active)" else ""
                        appendLine("- $name$marker")
                        appendLine("  Path: ${projectConfig.rootPath}")
                        projectConfig.language?.let { lang ->
                            appendLine("  Language: $lang")
                        }
                        appendLine()
                    }
                }
            }
        } catch (e: Exception) {
            "Error listing projects: ${e.message}"
        }
    }
}

@ToolDescription("Create a new project")
class CreateProjectTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val name = kwargs["name"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: name")
        val path = kwargs["path"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: path")
        val language = kwargs["language"] as? String
        val activate = kwargs["activate"] as? Boolean ?: true
        
        return try {
            var config = agent.config
            
            if (name in config.projects) {
                "Error: Project '$name' already exists"
            } else {
                val projectPath = Paths.get(path)
                if (!projectPath.toFile().exists()) {
                    "Error: Path does not exist: $path"
                } else {
                    val lang = language?.let { Language.valueOf(it.uppercase()) }
                    
                    val projectConfig = ProjectConfig(
                        projectName = name,
                        rootPath = projectPath.toString(),
                        language = lang
                    )
                    
                    config.projects[name] = projectConfig
                    
                    if (activate || config.projects.size == 1) {
                        config.activeProject = name
                    }
                    
                    config.save()
                    
                    buildString {
                        appendLine("Successfully created project '$name'")
                        appendLine("  Path: ${projectPath.toAbsolutePath()}")
                        lang?.let { appendLine("  Language: $it") }
                        
                        if (config.activeProject == name) {
                            appendLine("  Status: Active")
                            
                            // Activate the project in the agent
                            agent.activateProject(name)
                            
                            // Initialize language server if needed
                            if (lang != null) {
                                agent.initializeLanguageServer()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            "Error creating project: ${e.message}"
        }
    }
}

@ToolDescription("Get current configuration")
class GetConfigTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        return try {
            var config = agent.config
            
            buildString {
                appendLine("AmxLSP Configuration:")
                appendLine("  Config file: ${AmxLspPaths.amxlspConfigFile}")
                appendLine("  Active project: ${config.activeProject ?: "none"}")
                appendLine("  Default context: ${config.defaultContext}")
                appendLine("  Default modes: ${config.defaultModes.joinToString(", ")}")
                appendLine()
                
                appendLine("Projects (${config.projects.size}):")
                config.projects.forEach { (name, projectConfig) ->
                    val marker = if (name == config.activeProject) " (active)" else ""
                    appendLine("  - $name$marker")
                    appendLine("    Path: ${projectConfig.rootPath}")
                    projectConfig.language?.let { lang ->
                        appendLine("    Language: $lang")
                    }
                }
                
                appendLine()
                appendLine("Analytics:")
                appendLine("  Opt out: ${config.analytics.optOut}")
                
                appendLine()
                appendLine("Paths:")
                appendLine("  AmxLSP home: ${AmxLspPaths.amxlspHome}")
                appendLine("  Contexts: ${AmxLspPaths.contextsDir}")
                appendLine("  Modes: ${AmxLspPaths.modesDir}")
                appendLine("  Memories: ${AmxLspPaths.memoriesDir}")
            }
        } catch (e: Exception) {
            "Error getting configuration: ${e.message}"
        }
    }
}

@ToolDescription("Update configuration setting")
class UpdateConfigTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val setting = kwargs["setting"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: setting")
        val value = kwargs["value"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: value")
        
        return try {
            var config = agent.config
            var updated = false
            
            when (setting) {
                "default_context" -> {
                    config.defaultContext = value
                    updated = true
                }
                "analytics_opt_out" -> {
                    val optOut = value.toBooleanStrictOrNull()
                        ?: throw IllegalArgumentException("Invalid boolean value for analytics_opt_out: $value")
                    config.analytics.copy(optOut = optOut)
                    updated = true
                }
                else -> {
                    throw IllegalArgumentException("Unknown setting: $setting. Available settings: default_context, analytics_opt_out")
                }
            }
            
            if (updated) {
                config.save()
                "Successfully updated $setting to: $value"
            } else {
                "No changes made"
            }
        } catch (e: Exception) {
            "Error updating configuration: ${e.message}"
        }
    }
}

@ToolDescription("Get project information")
class GetProjectInfoTool(agent: AmxLspAgent) : Tool(agent) {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        return try {
            val project = agent.getActiveProjectOrRaise()
            
            buildString {
                appendLine("Active Project Information:")
                appendLine("  Name: ${project.projectName}")
                appendLine("  Root: ${project.projectRoot}")
                appendLine("  Language: ${project.language}")
                appendLine("  Encoding: ${project.projectConfig.encoding}")
                appendLine()
                
                appendLine("Configuration:")
                appendLine("  Ignore gitignore files: ${project.projectConfig.ignoreAllFilesInGitignore}")
                appendLine("  Ignored paths: ${project.projectConfig.ignoredPaths.size}")
                
                if (project.projectConfig.ignoredPaths.isNotEmpty()) {
                    appendLine("  Custom ignored paths:")
                    project.projectConfig.ignoredPaths.take(10).forEach { path ->
                        appendLine("    - $path")
                    }
                    if (project.projectConfig.ignoredPaths.size > 10) {
                        appendLine("    ... and ${project.projectConfig.ignoredPaths.size - 10} more")
                    }
                }
                
                appendLine()
                appendLine("Language Server:")
                val languageServer = agent.languageServer
                if (languageServer != null) {
                    appendLine("  Status: ${if (languageServer.isReady()) "Ready" else "Not ready"}")
                    appendLine("  Language: ${languageServer.getLanguage()}")
                } else {
                    appendLine("  Status: Not initialized")
                }
            }
        } catch (e: Exception) {
            "Error getting project information: ${e.message}"
        }
    }
}

@ToolDescription("Initialize memory manager for the current project")
class InitializeMemoryTool(agent: AmxLspAgent) : Tool(agent) {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        return try {
            val project = agent.getActiveProjectOrRaise()
            val memoriesDir = Paths.get(project.projectRoot, ".amxlsp", "memories")
            
            agent.memoriesManager = FileBasedMemoryManager(memoriesDir)
            
            "Successfully initialized memory manager for project: ${project.projectName}"
        } catch (e: Exception) {
            "Error initializing memory: ${e.message}"
        }
    }
}

// Register config tools
fun registerConfigTools() {
    ToolRegistry.register(ActivateProjectTool::class)
    ToolRegistry.register(ListProjectsTool::class)
    ToolRegistry.register(CreateProjectTool::class)
    ToolRegistry.register(GetConfigTool::class)
    ToolRegistry.register(UpdateConfigTool::class)
    ToolRegistry.register(GetProjectInfoTool::class)
    ToolRegistry.register(InitializeMemoryTool::class)
}