package com.aimatrix.solidlsp

import com.aimatrix.solidlsp.client.LSPClient
import com.aimatrix.solidlsp.config.LanguageServerConfig
import com.aimatrix.solidlsp.protocol.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.nio.file.Paths
import kotlin.io.path.readText
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * Unified wrapper around Language Server Protocol (LSP) implementations
 */
class SolidLanguageServer(
    private val config: LanguageServerConfig,
    private val timeout: Long = 30000L
) {
    private val client = LSPClient(config, timeout)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    suspend fun start(): Boolean {
        return client.start()
    }
    
    suspend fun stop() {
        client.stop()
        scope.cancel()
        logger.info { "SolidLanguageServer stopped" }
    }
    
    fun isReady(): Boolean = client.isReady()
    
    // Document management
    suspend fun openDocument(filePath: String): Boolean {
        return try {
            val path = Paths.get(filePath)
            val uri = path.toFile().toURI().toString()
            val text = path.readText()
            val languageId = config.language.languageId
            
            client.openDocument(uri, languageId, 1, text)
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to open document: $filePath" }
            false
        }
    }
    
    suspend fun closeDocument(filePath: String) {
        val uri = Paths.get(filePath).toFile().toURI().toString()
        client.closeDocument(uri)
    }
    
    // Symbol operations
    suspend fun getDocumentSymbols(filePath: String): List<DocumentSymbol> {
        val uri = Paths.get(filePath).toFile().toURI().toString()
        return client.getDocumentSymbols(uri) ?: emptyList()
    }
    
    suspend fun getWorkspaceSymbols(query: String): List<WorkspaceSymbol> {
        return client.getWorkspaceSymbols(query) ?: emptyList()
    }
    
    suspend fun findDefinition(filePath: String, line: Int, character: Int): List<Location> {
        val uri = Paths.get(filePath).toFile().toURI().toString()
        val position = Position(line, character)
        return client.getDefinition(uri, position) ?: emptyList()
    }
    
    suspend fun findReferences(filePath: String, line: Int, character: Int, includeDeclaration: Boolean = true): List<Location> {
        val uri = Paths.get(filePath).toFile().toURI().toString()
        val position = Position(line, character)
        return client.getReferences(uri, position, includeDeclaration) ?: emptyList()
    }
    
    // Code intelligence
    suspend fun getCompletion(filePath: String, line: Int, character: Int): CompletionList? {
        val uri = Paths.get(filePath).toFile().toURI().toString()
        val position = Position(line, character)
        return client.getCompletion(uri, position)
    }
    
    suspend fun getHover(filePath: String, line: Int, character: Int): Hover? {
        val uri = Paths.get(filePath).toFile().toURI().toString()
        val position = Position(line, character)
        return client.getHover(uri, position)
    }
    
    // Utility methods
    fun getLanguage() = config.language
    fun getWorkspaceRoot() = config.workspaceRoot
}

// Settings class
class SolidLSPSettings(
    val logLevel: String = "INFO",
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000,
    val timeout: Long = 30000L
)