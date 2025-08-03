package com.aimatrix.amxlsp.tools

import com.aimatrix.amxlsp.agent.AmxLspAgent
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@ToolDescription("Read the contents of a file")
class ReadFileTool(agent: AmxLspAgent) : Tool(agent) {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val path = kwargs["path"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: path")
        
        val projectRoot = agent.getProjectRoot()
        val filePath = Paths.get(projectRoot, path)
        
        if (!filePath.exists()) {
            return "Error: File not found: $path"
        }
        
        if (!filePath.isRegularFile()) {
            return "Error: Path is not a file: $path"
        }
        
        return try {
            filePath.readText()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }
}

@ToolDescription("Write content to a file")
class WriteFileTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerCanEdit {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val path = kwargs["path"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: path")
        val content = kwargs["content"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: content")
        
        val projectRoot = agent.getProjectRoot()
        val filePath = Paths.get(projectRoot, path)
        
        // Ensure parent directory exists
        filePath.parent?.createDirectories()
        
        return try {
            filePath.writeText(content)
            SUCCESS_RESULT
        } catch (e: Exception) {
            "Error writing file: ${e.message}"
        }
    }
}

@ToolDescription("List files and directories")
class ListFilesTool(agent: AmxLspAgent) : Tool(agent) {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val path = kwargs["path"] as? String ?: "."
        val recursive = kwargs["recursive"] as? Boolean ?: false
        
        val projectRoot = agent.getProjectRoot()
        val dirPath = Paths.get(projectRoot, path)
        
        if (!dirPath.exists()) {
            return "Error: Directory not found: $path"
        }
        
        if (!dirPath.isDirectory()) {
            return "Error: Path is not a directory: $path"
        }
        
        return try {
            val files = if (recursive) {
                Files.walk(dirPath).use { paths ->
                    paths.map { projectRoot.let { root -> Paths.get(root).relativize(it) } }
                        .filter { it.toString().isNotEmpty() }
                        .sorted()
                        .map { it.toString() }
                        .toList()
                }
            } else {
                dirPath.listDirectoryEntries()
                    .map { projectRoot.let { root -> Paths.get(root).relativize(it) } }
                    .sorted()
                    .map { it.toString() }
            }
            
            files.joinToString("\n")
        } catch (e: Exception) {
            "Error listing files: ${e.message}"
        }
    }
}

@ToolDescription("Create a directory")
class CreateDirectoryTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerCanEdit {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val path = kwargs["path"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: path")
        
        val projectRoot = agent.getProjectRoot()
        val dirPath = Paths.get(projectRoot, path)
        
        return try {
            dirPath.createDirectories()
            SUCCESS_RESULT
        } catch (e: Exception) {
            "Error creating directory: ${e.message}"
        }
    }
}

@ToolDescription("Delete a file or empty directory")
class DeleteFileTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerCanEdit {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val path = kwargs["path"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: path")
        
        val projectRoot = agent.getProjectRoot()
        val filePath = Paths.get(projectRoot, path)
        
        if (!filePath.exists()) {
            return "Error: Path not found: $path"
        }
        
        return try {
            if (filePath.isDirectory() && filePath.listDirectoryEntries().isNotEmpty()) {
                "Error: Directory is not empty: $path"
            } else {
                filePath.deleteExisting()
                SUCCESS_RESULT
            }
        } catch (e: Exception) {
            "Error deleting path: ${e.message}"
        }
    }
}

// Register file tools
fun registerFileTools() {
    ToolRegistry.register(ReadFileTool::class)
    ToolRegistry.register(WriteFileTool::class)
    ToolRegistry.register(ListFilesTool::class)
    ToolRegistry.register(CreateDirectoryTool::class)
    ToolRegistry.register(DeleteFileTool::class)
}