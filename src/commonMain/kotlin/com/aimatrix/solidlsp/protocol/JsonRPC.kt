package com.aimatrix.solidlsp.protocol

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

internal val logger = KotlinLogging.logger {}

// JSON-RPC 2.0 types
@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val method: String,
    val params: JsonElement? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class JsonRpcNotification(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: JsonElement? = null
)

// JSON-RPC error codes
object JsonRpcErrorCodes {
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
    const val SERVER_ERROR_START = -32099
    const val SERVER_ERROR_END = -32000
    
    // LSP-specific error codes
    const val SERVER_NOT_INITIALIZED = -32002
    const val UNKNOWN_ERROR_CODE = -32001
    const val REQUEST_CANCELLED = -32800
    const val CONTENT_MODIFIED = -32801
}

/**
 * JSON-RPC protocol handler for LSP communication
 */
class JsonRpcProtocol {
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }
    
    fun parseMessage(content: String): JsonRpcMessage? {
        return try {
            // Try to parse as a request first
            val jsonElement = json.parseToJsonElement(content)
            val jsonObject = jsonElement.jsonObject
            
            when {
                jsonObject.containsKey("id") && jsonObject.containsKey("method") -> {
                    JsonRpcMessage.Request(json.decodeFromJsonElement<JsonRpcRequest>(jsonElement))
                }
                jsonObject.containsKey("id") && (jsonObject.containsKey("result") || jsonObject.containsKey("error")) -> {
                    JsonRpcMessage.Response(json.decodeFromJsonElement<JsonRpcResponse>(jsonElement))
                }
                jsonObject.containsKey("method") -> {
                    JsonRpcMessage.Notification(json.decodeFromJsonElement<JsonRpcNotification>(jsonElement))
                }
                else -> {
                    logger.warn { "Unknown JSON-RPC message format: $content" }
                    null
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse JSON-RPC message: $content" }
            null
        }
    }
    
    fun serializeRequest(id: JsonElement?, method: String, params: Any?): String {
        val request = JsonRpcRequest(
            id = id,
            method = method,
            params = params?.let { json.encodeToJsonElement(it) }
        )
        return json.encodeToString(request)
    }
    
    fun serializeResponse(id: JsonElement?, result: Any? = null, error: JsonRpcError? = null): String {
        val response = JsonRpcResponse(
            id = id,
            result = result?.let { json.encodeToJsonElement(it) },
            error = error
        )
        return json.encodeToString(response)
    }
    
    fun serializeNotification(method: String, params: Any?): String {
        val notification = JsonRpcNotification(
            method = method,
            params = params?.let { json.encodeToJsonElement(it) }
        )
        return json.encodeToString(notification)
    }
    
    fun createErrorResponse(id: JsonElement?, code: Int, message: String, data: Any? = null): String {
        val error = JsonRpcError(
            code = code,
            message = message,
            data = data?.let { json.encodeToJsonElement(it) }
        )
        return serializeResponse(id, error = error)
    }
    
    internal inline fun <reified T> decodeParams(params: JsonElement?): T? {
        return params?.let {
            try {
                json.decodeFromJsonElement<T>(it)
            } catch (e: Exception) {
                logger.error(e) { "Failed to decode params as ${T::class.simpleName}" }
                null
            }
        }
    }
}

sealed class JsonRpcMessage {
    data class Request(val request: JsonRpcRequest) : JsonRpcMessage()
    data class Response(val response: JsonRpcResponse) : JsonRpcMessage()
    data class Notification(val notification: JsonRpcNotification) : JsonRpcMessage()
}

/**
 * LSP-specific protocol handler
 */
class LSPProtocol {
    internal val jsonRpc = JsonRpcProtocol()
    
    companion object {
        // LSP method names
        const val INITIALIZE = "initialize"
        const val INITIALIZED = "initialized"
        const val SHUTDOWN = "shutdown"
        const val EXIT = "exit"
        
        // Text document methods
        const val TEXT_DOCUMENT_DID_OPEN = "textDocument/didOpen"
        const val TEXT_DOCUMENT_DID_CHANGE = "textDocument/didChange"
        const val TEXT_DOCUMENT_DID_CLOSE = "textDocument/didClose"
        const val TEXT_DOCUMENT_DID_SAVE = "textDocument/didSave"
        
        // Language feature methods
        const val TEXT_DOCUMENT_COMPLETION = "textDocument/completion"
        const val TEXT_DOCUMENT_HOVER = "textDocument/hover"
        const val TEXT_DOCUMENT_SIGNATURE_HELP = "textDocument/signatureHelp"
        const val TEXT_DOCUMENT_DEFINITION = "textDocument/definition"
        const val TEXT_DOCUMENT_TYPE_DEFINITION = "textDocument/typeDefinition"
        const val TEXT_DOCUMENT_IMPLEMENTATION = "textDocument/implementation"
        const val TEXT_DOCUMENT_REFERENCES = "textDocument/references"
        const val TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT = "textDocument/documentHighlight"
        const val TEXT_DOCUMENT_DOCUMENT_SYMBOL = "textDocument/documentSymbol"
        const val TEXT_DOCUMENT_CODE_ACTION = "textDocument/codeAction"
        const val TEXT_DOCUMENT_FORMATTING = "textDocument/formatting"
        const val TEXT_DOCUMENT_RANGE_FORMATTING = "textDocument/rangeFormatting"
        const val TEXT_DOCUMENT_RENAME = "textDocument/rename"
        
        // Workspace methods
        const val WORKSPACE_SYMBOL = "workspace/symbol"
        const val WORKSPACE_DID_CHANGE_CONFIGURATION = "workspace/didChangeConfiguration"
        const val WORKSPACE_DID_CHANGE_WATCHED_FILES = "workspace/didChangeWatchedFiles"
        
        // Notifications from server
        const val TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS = "textDocument/publishDiagnostics"
    }
    
    fun parseMessage(content: String): JsonRpcMessage? = jsonRpc.parseMessage(content)
    
    fun createInitializeRequest(
        id: Int,
        processId: Int?,
        rootUri: String?,
        clientCapabilities: ClientCapabilities
    ): String {
        val params = mapOf(
            "processId" to processId,
            "rootUri" to rootUri,
            "capabilities" to clientCapabilities
        )
        return jsonRpc.serializeRequest(JsonPrimitive(id), INITIALIZE, params)
    }
    
    fun createInitializedNotification(): String {
        return jsonRpc.serializeNotification(INITIALIZED, emptyMap<String, Any>())
    }
    
    fun createShutdownRequest(id: Int): String {
        return jsonRpc.serializeRequest(JsonPrimitive(id), SHUTDOWN, null)
    }
    
    fun createExitNotification(): String {
        return jsonRpc.serializeNotification(EXIT, null)
    }
    
    fun createDidOpenNotification(textDocument: TextDocumentItem): String {
        val params = mapOf("textDocument" to textDocument)
        return jsonRpc.serializeNotification(TEXT_DOCUMENT_DID_OPEN, params)
    }
    
    fun createDidCloseNotification(textDocument: TextDocumentIdentifier): String {
        val params = mapOf("textDocument" to textDocument)
        return jsonRpc.serializeNotification(TEXT_DOCUMENT_DID_CLOSE, params)
    }
    
    fun createCompletionRequest(
        id: Int,
        textDocument: TextDocumentIdentifier,
        position: Position
    ): String {
        val params = mapOf(
            "textDocument" to textDocument,
            "position" to position
        )
        return jsonRpc.serializeRequest(JsonPrimitive(id), TEXT_DOCUMENT_COMPLETION, params)
    }
    
    fun createHoverRequest(
        id: Int,
        textDocument: TextDocumentIdentifier,
        position: Position
    ): String {
        val params = mapOf(
            "textDocument" to textDocument,
            "position" to position
        )
        return jsonRpc.serializeRequest(JsonPrimitive(id), TEXT_DOCUMENT_HOVER, params)
    }
    
    fun createDefinitionRequest(
        id: Int,
        textDocument: TextDocumentIdentifier,
        position: Position
    ): String {
        val params = mapOf(
            "textDocument" to textDocument,
            "position" to position
        )
        return jsonRpc.serializeRequest(JsonPrimitive(id), TEXT_DOCUMENT_DEFINITION, params)
    }
    
    fun createDocumentSymbolRequest(
        id: Int,
        textDocument: TextDocumentIdentifier
    ): String {
        val params = mapOf("textDocument" to textDocument)
        return jsonRpc.serializeRequest(JsonPrimitive(id), TEXT_DOCUMENT_DOCUMENT_SYMBOL, params)
    }
    
    fun createReferencesRequest(
        id: Int,
        textDocument: TextDocumentIdentifier,
        position: Position,
        includeDeclaration: Boolean = true
    ): String {
        val params = mapOf(
            "textDocument" to textDocument,
            "position" to position,
            "context" to ReferenceContext(includeDeclaration)
        )
        return jsonRpc.serializeRequest(JsonPrimitive(id), TEXT_DOCUMENT_REFERENCES, params)
    }
    
    fun createWorkspaceSymbolRequest(id: Int, query: String): String {
        val params = mapOf("query" to query)
        return jsonRpc.serializeRequest(JsonPrimitive(id), WORKSPACE_SYMBOL, params)
    }
    
    internal inline fun <reified T> decodeParams(params: JsonElement?): T? = jsonRpc.decodeParams(params)
    
    fun createErrorResponse(id: JsonElement?, code: Int, message: String, data: Any? = null): String {
        return jsonRpc.createErrorResponse(id, code, message, data)
    }
}