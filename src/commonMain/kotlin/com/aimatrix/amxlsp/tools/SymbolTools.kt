package com.aimatrix.amxlsp.tools

import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.solidlsp.protocol.DocumentSymbol
import com.aimatrix.solidlsp.protocol.Location
import com.aimatrix.solidlsp.protocol.WorkspaceSymbol
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import java.net.URI

@ToolDescription("Find symbol definitions in the project")
class FindSymbolTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerSymbolicRead {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val symbolName = kwargs["symbol_name"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: symbol_name")
        
        val languageServer = agent.languageServer
            ?: return "Error: Language server not available"
        
        if (!languageServer.isReady()) {
            return "Error: Language server not ready"
        }
        
        return runBlocking {
            try {
                val symbols = languageServer.getWorkspaceSymbols(symbolName)
                
                if (symbols.isEmpty()) {
                    "No symbols found matching '$symbolName'"
                } else {
                    formatWorkspaceSymbols(symbols)
                }
            } catch (e: Exception) {
                "Error finding symbols: ${e.message}"
            }
        }
    }
    
    private fun formatWorkspaceSymbols(symbols: List<WorkspaceSymbol>): String {
        return buildString {
            appendLine("Found ${symbols.size} symbol(s):")
            appendLine()
            
            symbols.forEach { symbol ->
                appendLine("Symbol: ${symbol.name}")
                appendLine("  Kind: ${symbol.kind}")
                appendLine("  Location: ${formatLocation(symbol.location)}")
                symbol.containerName?.let { container ->
                    appendLine("  Container: $container")
                }
                appendLine()
            }
        }
    }
    
    private fun formatLocation(location: Location): String {
        val uri = location.uri
        val path = if (uri.startsWith("file://")) {
            uri.substring(7) // Remove "file://" prefix
        } else {
            uri
        }
        
        val range = location.range
        return "$path:${range.start.line + 1}:${range.start.character + 1}"
    }
}

@ToolDescription("Get symbols in a specific file")
class GetFileSymbolsTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerSymbolicRead {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val filePath = kwargs["file_path"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: file_path")
        
        val languageServer = agent.languageServer
            ?: return "Error: Language server not available"
        
        if (!languageServer.isReady()) {
            return "Error: Language server not ready"
        }
        
        val projectRoot = agent.getProjectRoot()
        val fullPath = Paths.get(projectRoot, filePath).toString()
        
        return runBlocking {
            try {
                // Open the document first
                languageServer.openDocument(fullPath)
                
                val symbols = languageServer.getDocumentSymbols(fullPath)
                
                if (symbols.isEmpty()) {
                    "No symbols found in file: $filePath"
                } else {
                    formatDocumentSymbols(symbols, filePath)
                }
            } catch (e: Exception) {
                "Error getting file symbols: ${e.message}"
            }
        }
    }
    
    private fun formatDocumentSymbols(symbols: List<DocumentSymbol>, filePath: String): String {
        return buildString {
            appendLine("Symbols in $filePath:")
            appendLine()
            
            symbols.forEach { symbol ->
                appendSymbol(symbol, 0)
            }
        }
    }
    
    private fun StringBuilder.appendSymbol(symbol: DocumentSymbol, indent: Int) {
        val prefix = "  ".repeat(indent)
        
        appendLine("${prefix}${symbol.name} (${symbol.kind})")
        
        symbol.detail?.let { detail ->
            appendLine("$prefix  Detail: $detail")
        }
        
        val range = symbol.range
        appendLine("$prefix  Range: ${range.start.line + 1}:${range.start.character + 1} - ${range.end.line + 1}:${range.end.character + 1}")
        
        symbol.children?.forEach { child ->
            appendSymbol(child, indent + 1)
        }
    }
}

@ToolDescription("Find definition of a symbol at a specific location")
class GoToDefinitionTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerSymbolicRead {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val filePath = kwargs["file_path"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: file_path")
        val line = kwargs["line"] as? Int
            ?: throw IllegalArgumentException("Missing required parameter: line")
        val character = kwargs["character"] as? Int ?: 0
        
        val languageServer = agent.languageServer
            ?: return "Error: Language server not available"
        
        if (!languageServer.isReady()) {
            return "Error: Language server not ready"
        }
        
        val projectRoot = agent.getProjectRoot()
        val fullPath = Paths.get(projectRoot, filePath).toString()
        
        return runBlocking {
            try {
                // Open the document first
                languageServer.openDocument(fullPath)
                
                val definitions = languageServer.findDefinition(fullPath, line - 1, character) // Convert to 0-based
                
                if (definitions.isEmpty()) {
                    "No definitions found at $filePath:$line:$character"
                } else {
                    formatDefinitions(definitions)
                }
            } catch (e: Exception) {
                "Error finding definition: ${e.message}"
            }
        }
    }
    
    private fun formatDefinitions(definitions: List<Location>): String {
        return buildString {
            appendLine("Found ${definitions.size} definition(s):")
            appendLine()
            
            definitions.forEach { location ->
                appendLine(formatLocation(location))
            }
        }
    }
    
    private fun formatLocation(location: Location): String {
        val uri = location.uri
        val path = if (uri.startsWith("file://")) {
            uri.substring(7) // Remove "file://" prefix
        } else {
            uri
        }
        
        val range = location.range
        return "$path:${range.start.line + 1}:${range.start.character + 1}"
    }
}

@ToolDescription("Find all references to a symbol at a specific location")
class FindReferencesTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerSymbolicRead {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val filePath = kwargs["file_path"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: file_path")
        val line = kwargs["line"] as? Int
            ?: throw IllegalArgumentException("Missing required parameter: line")
        val character = kwargs["character"] as? Int ?: 0
        val includeDeclaration = kwargs["include_declaration"] as? Boolean ?: true
        
        val languageServer = agent.languageServer
            ?: return "Error: Language server not available"
        
        if (!languageServer.isReady()) {
            return "Error: Language server not ready"
        }
        
        val projectRoot = agent.getProjectRoot()
        val fullPath = Paths.get(projectRoot, filePath).toString()
        
        return runBlocking {
            try {
                // Open the document first
                languageServer.openDocument(fullPath)
                
                val references = languageServer.findReferences(fullPath, line - 1, character, includeDeclaration) // Convert to 0-based
                
                if (references.isEmpty()) {
                    "No references found at $filePath:$line:$character"
                } else {
                    formatReferences(references)
                }
            } catch (e: Exception) {
                "Error finding references: ${e.message}"
            }
        }
    }
    
    private fun formatReferences(references: List<Location>): String {
        return buildString {
            appendLine("Found ${references.size} reference(s):")
            appendLine()
            
            references.groupBy { location ->
                val uri = location.uri
                if (uri.startsWith("file://")) uri.substring(7) else uri
            }.forEach { (filePath, locationsInFile) ->
                appendLine("$filePath:")
                locationsInFile.forEach { location ->
                    val range = location.range
                    appendLine("  Line ${range.start.line + 1}, Column ${range.start.character + 1}")
                }
                appendLine()
            }
        }
    }
}

@ToolDescription("Get hover information for a symbol at a specific location")
class GetHoverInfoTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerSymbolicRead {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val filePath = kwargs["file_path"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: file_path")
        val line = kwargs["line"] as? Int
            ?: throw IllegalArgumentException("Missing required parameter: line")
        val character = kwargs["character"] as? Int ?: 0
        
        val languageServer = agent.languageServer
            ?: return "Error: Language server not available"
        
        if (!languageServer.isReady()) {
            return "Error: Language server not ready"
        }
        
        val projectRoot = agent.getProjectRoot()
        val fullPath = Paths.get(projectRoot, filePath).toString()
        
        return runBlocking {
            try {
                // Open the document first
                languageServer.openDocument(fullPath)
                
                val hover = languageServer.getHover(fullPath, line - 1, character) // Convert to 0-based
                
                if (hover == null) {
                    "No hover information available at $filePath:$line:$character"
                } else {
                    buildString {
                        appendLine("Hover information at $filePath:$line:$character:")
                        appendLine()
                        appendLine(hover.contents)
                        
                        hover.range?.let { range ->
                            appendLine()
                            appendLine("Range: ${range.start.line + 1}:${range.start.character + 1} - ${range.end.line + 1}:${range.end.character + 1}")
                        }
                    }
                }
            } catch (e: Exception) {
                "Error getting hover information: ${e.message}"
            }
        }
    }
}

// Register symbol tools
fun registerSymbolTools() {
    ToolRegistry.register(FindSymbolTool::class)
    ToolRegistry.register(GetFileSymbolsTool::class)
    ToolRegistry.register(GoToDefinitionTool::class)
    ToolRegistry.register(FindReferencesTool::class)
    ToolRegistry.register(GetHoverInfoTool::class)
}