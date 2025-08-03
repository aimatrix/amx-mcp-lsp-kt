package com.aimatrix.solidlsp.client

import com.aimatrix.solidlsp.config.Language
import com.aimatrix.solidlsp.config.LanguageServerConfig
import com.aimatrix.solidlsp.exceptions.*
import com.aimatrix.solidlsp.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * LSP client implementation that communicates with language servers
 */
class LSPClient(
    private val config: LanguageServerConfig,
    private val timeout: Long = 30000L
) {
    private val protocol = LSPProtocol()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val requestIdCounter = AtomicInteger(1)
    private val pendingRequests = ConcurrentHashMap<Int, CompletableDeferred<JsonRpcResponse>>()
    private val mutex = Mutex()
    
    private var process: Process? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var isInitialized = false
    
    // Document state
    private val openDocuments = ConcurrentHashMap<String, TextDocumentItem>()
    
    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        return@withContext mutex.withLock {
            if (process != null) {
                logger.warn { "Language server already started" }
                return@withLock true
            }
            
            try {
                val command = getLanguageServerCommand()
                logger.info { "Starting language server: ${command.joinToString(" ")}" }
                
                val processBuilder = ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                
                process = processBuilder.start()
                
                val processInstance = process!!
                writer = PrintWriter(OutputStreamWriter(processInstance.outputStream, Charsets.UTF_8), true)
                reader = BufferedReader(InputStreamReader(processInstance.inputStream, Charsets.UTF_8))
                
                // Start reading responses
                scope.launch {
                    readMessages()
                }
                
                // Initialize the server
                initialize()
                
                true
            } catch (e: Exception) {
                logger.error(e) { "Failed to start language server" }
                cleanup()
                false
            }
        }
    }
    
    suspend fun stop() {
        mutex.withLock {
            if (process == null) return@withLock
            
            try {
                if (isInitialized) {
                    // Send shutdown request
                    val shutdownId = requestIdCounter.getAndIncrement()
                    val shutdownRequest = protocol.createShutdownRequest(shutdownId)
                    sendMessage(shutdownRequest)
                    
                    // Send exit notification
                    val exitNotification = protocol.createExitNotification()
                    sendMessage(exitNotification)
                }
            } catch (e: Exception) {
                logger.warn(e) { "Error during shutdown" }
            } finally {
                cleanup()
            }
        }
    }
    
    private fun cleanup() {
        scope.cancel()
        writer?.close()
        reader?.close()
        process?.destroyForcibly()
        process = null
        writer = null
        reader = null
        isInitialized = false
        openDocuments.clear()
        
        // Complete all pending requests with error
        pendingRequests.values.forEach { deferred ->
            deferred.completeExceptionally(LanguageServerNotReadyException("Language server stopped"))
        }
        pendingRequests.clear()
    }
    
    private suspend fun initialize() {
        val initializeId = requestIdCounter.getAndIncrement()
        val clientCapabilities = createClientCapabilities()
        
        val initializeRequest = protocol.createInitializeRequest(
            id = initializeId,
            processId = ProcessHandle.current().pid().toInt(),
            rootUri = "file://${config.workspaceRoot}",
            clientCapabilities = clientCapabilities
        )
        
        val response = sendRequest(initializeRequest, initializeId)
        
        if (response.error != null) {
            throw LanguageServerInitException("Initialize failed: ${response.error.message}")
        }
        
        // Send initialized notification
        val initializedNotification = protocol.createInitializedNotification()
        sendMessage(initializedNotification)
        
        isInitialized = true
        logger.info { "Language server initialized successfully" }
    }
    
    private fun createClientCapabilities(): ClientCapabilities {
        return ClientCapabilities(
            workspace = WorkspaceClientCapabilities(
                applyEdit = true,
                workspaceEdit = WorkspaceEditClientCapabilities(documentChanges = true),
                symbol = WorkspaceSymbolClientCapabilities(dynamicRegistration = false)
            ),
            textDocument = TextDocumentClientCapabilities(
                synchronization = TextDocumentSyncClientCapabilities(
                    dynamicRegistration = false,
                    willSave = true,
                    willSaveWaitUntil = true,
                    didSave = true
                ),
                completion = CompletionClientCapabilities(
                    dynamicRegistration = false,
                    completionItem = CompletionItemClientCapabilities(snippetSupport = true)
                ),
                hover = HoverClientCapabilities(dynamicRegistration = false),
                signatureHelp = SignatureHelpClientCapabilities(dynamicRegistration = false),
                references = ReferencesClientCapabilities(dynamicRegistration = false),
                documentHighlight = DocumentHighlightClientCapabilities(dynamicRegistration = false),
                documentSymbol = DocumentSymbolClientCapabilities(dynamicRegistration = false),
                formatting = DocumentFormattingClientCapabilities(dynamicRegistration = false),
                rangeFormatting = DocumentRangeFormattingClientCapabilities(dynamicRegistration = false),
                onTypeFormatting = DocumentOnTypeFormattingClientCapabilities(dynamicRegistration = false),
                definition = DefinitionClientCapabilities(dynamicRegistration = false),
                codeAction = CodeActionClientCapabilities(dynamicRegistration = false),
                rename = RenameClientCapabilities(dynamicRegistration = false)
            )
        )
    }
    
    private fun getLanguageServerCommand(): List<String> {
        return when (config.language) {
            Language.PYTHON -> listOf("pyright-langserver", "--stdio")
            Language.KOTLIN -> listOf("kotlin-language-server")
            Language.JAVA -> listOf("jdtls")
            Language.TYPESCRIPT -> listOf("typescript-language-server", "--stdio")
            Language.JAVASCRIPT -> listOf("typescript-language-server", "--stdio")
            Language.GO -> listOf("gopls")
            Language.RUST -> listOf("rust-analyzer")
            Language.CSHARP -> listOf("omnisharp", "--languageserver")
            Language.PHP -> listOf("intelephense", "--stdio")
            Language.RUBY -> listOf("solargraph", "stdio")
            Language.ELIXIR -> listOf("elixir-ls")
            Language.CLOJURE -> listOf("clojure-lsp")
            Language.DART -> listOf("dart", "language-server")
            Language.TERRAFORM -> listOf("terraform-ls", "serve")
        }
    }
    
    private suspend fun readMessages() {
        val readerInstance = reader ?: return
        
        try {
            while (!scope.isActive) {
                val headers = mutableMapOf<String, String>()
                
                // Read headers
                while (true) {
                    val line = readerInstance.readLine() ?: break
                    if (line.isEmpty()) break // Empty line separates headers from content
                    
                    val parts = line.split(": ", limit = 2)
                    if (parts.size == 2) {
                        headers[parts[0]] = parts[1]
                    }
                }
                
                // Read content
                val contentLength = headers["Content-Length"]?.toIntOrNull()
                if (contentLength == null) {
                    logger.warn { "Missing Content-Length header" }
                    continue
                }
                
                val content = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = readerInstance.read(content, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                
                val messageContent = String(content)
                handleMessage(messageContent)
            }
        } catch (e: Exception) {
            if (scope.isActive) {
                logger.error(e) { "Error reading messages" }
            }
        }
    }
    
    private fun handleMessage(content: String) {
        val message = protocol.parseMessage(content) ?: return
        
        when (message) {
            is JsonRpcMessage.Response -> {
                val id = message.response.id?.let {
                    when (it) {
                        is JsonPrimitive -> it.content.toIntOrNull()
                        else -> null
                    }
                }
                
                id?.let { requestId ->
                    pendingRequests.remove(requestId)?.complete(message.response)
                }
            }
            is JsonRpcMessage.Notification -> {
                handleNotification(message.notification)
            }
            is JsonRpcMessage.Request -> {
                // Handle requests from server (rare)
                logger.debug { "Received request from server: ${message.request.method}" }
            }
        }
    }
    
    private fun handleNotification(notification: JsonRpcNotification) {
        when (notification.method) {
            LSPProtocol.TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS -> {
                val params = protocol.decodeParams<Map<String, Any>>(notification.params)
                logger.debug { "Received diagnostics: $params" }
            }
            else -> {
                logger.debug { "Received notification: ${notification.method}" }
            }
        }
    }
    
    private suspend fun sendRequest(message: String, requestId: Int): JsonRpcResponse {
        val deferred = CompletableDeferred<JsonRpcResponse>()
        pendingRequests[requestId] = deferred
        
        sendMessage(message)
        
        return withTimeout(timeout) {
            deferred.await()
        }
    }
    
    private fun sendMessage(message: String) {
        val writerInstance = writer ?: throw LanguageServerNotReadyException("Language server not started")
        
        val contentLength = message.toByteArray(Charsets.UTF_8).size
        val headers = "Content-Length: $contentLength\r\n\r\n"
        
        synchronized(writerInstance) {
            writerInstance.write(headers)
            writerInstance.write(message)
            writerInstance.flush()
        }
        
        logger.debug { "Sent message: $message" }
    }
    
    // Public API methods
    suspend fun openDocument(uri: String, languageId: String, version: Int, text: String) {
        if (!isInitialized) throw LanguageServerNotReadyException("Language server not initialized")
        
        val textDocument = TextDocumentItem(uri, languageId, version, text)
        openDocuments[uri] = textDocument
        
        val notification = protocol.createDidOpenNotification(textDocument)
        sendMessage(notification)
    }
    
    suspend fun closeDocument(uri: String) {
        if (!isInitialized) throw LanguageServerNotReadyException("Language server not initialized")
        
        openDocuments.remove(uri)
        val textDocument = TextDocumentIdentifier(uri)
        val notification = protocol.createDidCloseNotification(textDocument)
        sendMessage(notification)
    }
    
    suspend fun getCompletion(uri: String, position: Position): CompletionList? {
        if (!isInitialized) throw LanguageServerNotReadyException("Language server not initialized")
        
        val requestId = requestIdCounter.getAndIncrement()
        val textDocument = TextDocumentIdentifier(uri)
        val request = protocol.createCompletionRequest(requestId, textDocument, position)
        
        val response = sendRequest(request, requestId)
        
        return if (response.error == null && response.result != null) {
            protocol.decodeParams<CompletionList>(response.result)
        } else {
            null
        }
    }
    
    suspend fun getHover(uri: String, position: Position): Hover? {
        if (!isInitialized) throw LanguageServerNotReadyException("Language server not initialized")
        
        val requestId = requestIdCounter.getAndIncrement()
        val textDocument = TextDocumentIdentifier(uri)
        val request = protocol.createHoverRequest(requestId, textDocument, position)
        
        val response = sendRequest(request, requestId)
        
        return if (response.error == null && response.result != null) {
            protocol.decodeParams<Hover>(response.result)
        } else {
            null
        }
    }
    
    suspend fun getDefinition(uri: String, position: Position): List<Location>? {
        if (!isInitialized) throw LanguageServerNotReadyException("Language server not initialized")
        
        val requestId = requestIdCounter.getAndIncrement()
        val textDocument = TextDocumentIdentifier(uri)
        val request = protocol.createDefinitionRequest(requestId, textDocument, position)
        
        val response = sendRequest(request, requestId)
        
        return if (response.error == null && response.result != null) {
            protocol.decodeParams<List<Location>>(response.result)
        } else {
            null
        }
    }
    
    suspend fun getDocumentSymbols(uri: String): List<DocumentSymbol>? {
        if (!isInitialized) throw LanguageServerNotReadyException("Language server not initialized")
        
        val requestId = requestIdCounter.getAndIncrement()
        val textDocument = TextDocumentIdentifier(uri)
        val request = protocol.createDocumentSymbolRequest(requestId, textDocument)
        
        val response = sendRequest(request, requestId)
        
        return if (response.error == null && response.result != null) {
            protocol.decodeParams<List<DocumentSymbol>>(response.result)
        } else {
            null
        }
    }
    
    suspend fun getReferences(uri: String, position: Position, includeDeclaration: Boolean = true): List<Location>? {
        if (!isInitialized) throw LanguageServerNotReadyException("Language server not initialized")
        
        val requestId = requestIdCounter.getAndIncrement()
        val textDocument = TextDocumentIdentifier(uri)
        val request = protocol.createReferencesRequest(requestId, textDocument, position, includeDeclaration)
        
        val response = sendRequest(request, requestId)
        
        return if (response.error == null && response.result != null) {
            protocol.decodeParams<List<Location>>(response.result)
        } else {
            null
        }
    }
    
    suspend fun getWorkspaceSymbols(query: String): List<WorkspaceSymbol>? {
        if (!isInitialized) throw LanguageServerNotReadyException("Language server not initialized")
        
        val requestId = requestIdCounter.getAndIncrement()
        val request = protocol.createWorkspaceSymbolRequest(requestId, query)
        
        val response = sendRequest(request, requestId)
        
        return if (response.error == null && response.result != null) {
            protocol.decodeParams<List<WorkspaceSymbol>>(response.result)
        } else {
            null
        }
    }
    
    fun isReady(): Boolean = isInitialized
}