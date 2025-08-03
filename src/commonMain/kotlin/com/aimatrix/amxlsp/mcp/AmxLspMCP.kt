package com.aimatrix.amxlsp.mcp

import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.amxlsp.memory.FileBasedMemoryManager
import com.aimatrix.amxlsp.tools.*
import com.aimatrix.solidlsp.protocol.JsonRpcProtocol
import com.aimatrix.solidlsp.protocol.JsonRpcMessage
import com.aimatrix.solidlsp.protocol.JsonRpcErrorCodes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter

private val logger = KotlinLogging.logger {}

/**
 * Factory for creating MCP servers
 */
open class AmxLspMCPFactory {
    fun createStdioServer(): MCPServer {
        return StdioMCPServer()
    }
    
    fun createWebSocketServer(host: String, port: Int): MCPServer {
        return WebSocketMCPServer(host, port)
    }
}

/**
 * Single process version of the MCP factory
 */
class AmxLspMCPFactorySingleProcess : AmxLspMCPFactory() {
    // In single process mode, everything runs in the same process
}

/**
 * Base interface for MCP servers
 */
interface MCPServer {
    fun start()
    fun stop()
}

/**
 * MCP server that communicates via stdio
 */
class StdioMCPServer : MCPServer {
    private val agent = AmxLspAgent()
    private val protocol = MCPProtocolHandler(agent)
    private val jsonRpc = JsonRpcProtocol()
    private val reader = BufferedReader(InputStreamReader(System.`in`))
    private val writer = PrintWriter(OutputStreamWriter(System.out), true)
    
    override fun start() {
        logger.info { "Starting stdio MCP server" }
        
        // Initialize memory manager
        initializeMemoryManager()
        
        while (true) {
            val line = reader.readLine() ?: break
            if (line.trim().isEmpty()) continue
            
            try {
                val response = handleRequest(line)
                if (response.isNotEmpty()) {
                    writer.println(response)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error handling request" }
                val errorResponse = createErrorResponse(e, null)
                writer.println(errorResponse)
            }
        }
    }
    
    override fun stop() {
        kotlinx.coroutines.runBlocking {
            agent.shutdown()
        }
        logger.info { "Stdio MCP server stopped" }
    }
    
    private fun initializeMemoryManager() {
        val memoriesDir = com.aimatrix.amxlsp.config.AmxLspPaths.memoriesDir
        agent.memoriesManager = FileBasedMemoryManager(memoriesDir)
    }
    
    private fun handleRequest(request: String): String {
        val message = jsonRpc.parseMessage(request)
            ?: return createErrorResponse(IllegalArgumentException("Invalid JSON-RPC message"), null)
        
        return when (message) {
            is JsonRpcMessage.Request -> {
                try {
                    val result = when (message.request.method) {
                        "initialize" -> {
                            val params = jsonRpc.decodeParams<Map<String, Any>>(message.request.params) ?: emptyMap()
                            protocol.handleInitialize(params)
                        }
                        "tools/list" -> protocol.handleToolsList()
                        "tools/call" -> {
                            val params = jsonRpc.decodeParams<Map<String, Any>>(message.request.params)
                                ?: throw IllegalArgumentException("Missing parameters for tools/call")
                            val name = params["name"] as? String
                                ?: throw IllegalArgumentException("Missing tool name")
                            val arguments = params["arguments"] as? Map<String, Any> ?: emptyMap()
                            protocol.handleToolCall(name, arguments)
                        }
                        else -> throw IllegalArgumentException("Unknown method: ${message.request.method}")
                    }
                    
                    jsonRpc.serializeResponse(message.request.id, result)
                } catch (e: Exception) {
                    logger.error(e) { "Error handling method: ${message.request.method}" }
                    createErrorResponse(e, message.request.id)
                }
            }
            is JsonRpcMessage.Notification -> {
                // Handle notifications (no response needed)
                when (message.notification.method) {
                    "initialized" -> {
                        logger.info { "Client initialized" }
                    }
                    else -> {
                        logger.debug { "Received notification: ${message.notification.method}" }
                    }
                }
                "" // No response for notifications
            }
            is JsonRpcMessage.Response -> {
                logger.warn { "Received unexpected response message" }
                "" // No response needed
            }
        }
    }
    
    private fun createErrorResponse(error: Exception, id: JsonElement?): String {
        val code = when (error) {
            is IllegalArgumentException -> JsonRpcErrorCodes.INVALID_PARAMS
            else -> JsonRpcErrorCodes.INTERNAL_ERROR
        }
        return jsonRpc.createErrorResponse(id, code, error.message ?: "Unknown error")
    }
}

/**
 * MCP server that communicates via WebSocket
 */
class WebSocketMCPServer(
    private val host: String,
    private val port: Int
) : MCPServer {
    private val agent = AmxLspAgent()
    private val protocol = MCPProtocolHandler(agent)
    private val jsonRpc = JsonRpcProtocol()
    private var server: ApplicationEngine? = null
    
    override fun start() {
        logger.info { "Starting WebSocket MCP server on $host:$port" }
        
        // Initialize memory manager
        initializeMemoryManager()
        
        server = embeddedServer(Netty, port = port, host = host) {
            install(WebSockets)
            
            routing {
                webSocket("/mcp") {
                    logger.info { "WebSocket connection established" }
                    
                    try {
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                val request = frame.readText()
                                val response = handleRequest(request)
                                if (response.isNotEmpty()) {
                                    send(Frame.Text(response))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "WebSocket error" }
                    } finally {
                        logger.info { "WebSocket connection closed" }
                    }
                }
            }
        }.start(wait = true)
    }
    
    override fun stop() {
        server?.stop(1000, 5000)
        kotlinx.coroutines.runBlocking {
            agent.shutdown()
        }
        logger.info { "WebSocket MCP server stopped" }
    }
    
    private fun initializeMemoryManager() {
        val memoriesDir = com.aimatrix.amxlsp.config.AmxLspPaths.memoriesDir
        agent.memoriesManager = FileBasedMemoryManager(memoriesDir)
    }
    
    private fun handleRequest(request: String): String {
        val message = jsonRpc.parseMessage(request)
            ?: return createErrorResponse(IllegalArgumentException("Invalid JSON-RPC message"), null)
        
        return when (message) {
            is JsonRpcMessage.Request -> {
                try {
                    val result = when (message.request.method) {
                        "initialize" -> {
                            val params = jsonRpc.decodeParams<Map<String, Any>>(message.request.params) ?: emptyMap()
                            protocol.handleInitialize(params)
                        }
                        "tools/list" -> protocol.handleToolsList()
                        "tools/call" -> {
                            val params = jsonRpc.decodeParams<Map<String, Any>>(message.request.params)
                                ?: throw IllegalArgumentException("Missing parameters for tools/call")
                            val name = params["name"] as? String
                                ?: throw IllegalArgumentException("Missing tool name")
                            val arguments = params["arguments"] as? Map<String, Any> ?: emptyMap()
                            protocol.handleToolCall(name, arguments)
                        }
                        else -> throw IllegalArgumentException("Unknown method: ${message.request.method}")
                    }
                    
                    jsonRpc.serializeResponse(message.request.id, result)
                } catch (e: Exception) {
                    logger.error(e) { "Error handling method: ${message.request.method}" }
                    createErrorResponse(e, message.request.id)
                }
            }
            is JsonRpcMessage.Notification -> {
                // Handle notifications (no response needed)
                when (message.notification.method) {
                    "initialized" -> {
                        logger.info { "Client initialized" }
                    }
                    else -> {
                        logger.debug { "Received notification: ${message.notification.method}" }
                    }
                }
                "" // No response for notifications
            }
            is JsonRpcMessage.Response -> {
                logger.warn { "Received unexpected response message" }
                "" // No response needed
            }
        }
    }
    
    private fun createErrorResponse(error: Exception, id: JsonElement?): String {
        val code = when (error) {
            is IllegalArgumentException -> JsonRpcErrorCodes.INVALID_PARAMS
            else -> JsonRpcErrorCodes.INTERNAL_ERROR
        }
        return jsonRpc.createErrorResponse(id, code, error.message ?: "Unknown error")
    }
}

/**
 * MCP protocol handler
 */
class MCPProtocolHandler(private val agent: AmxLspAgent) {
    
    init {
        // Register all tools
        registerAllTools()
    }
    
    private fun registerAllTools() {
        registerFileTools()
        registerSymbolTools()
        registerMemoryTools()
        registerConfigTools()
    }
    
    fun handleInitialize(params: Map<String, Any>): Map<String, Any> {
        logger.info { "Initializing MCP server with params: $params" }
        
        return mapOf(
            "protocolVersion" to "2024-11-05",
            "serverInfo" to mapOf(
                "name" to "amxlsp-mcp-server",
                "version" to "0.1.3"
            ),
            "capabilities" to mapOf(
                "tools" to mapOf(
                    "listChanged" to false
                ),
                "logging" to mapOf(),
                "prompts" to mapOf(
                    "listChanged" to false
                ),
                "resources" to mapOf(
                    "subscribe" to false,
                    "listChanged" to false
                )
            )
        )
    }
    
    fun handleToolsList(): Map<String, Any> {
        val tools = ToolRegistry.getAll().map { toolClass ->
            mapOf(
                "name" to Tool.getNameFromClass(toolClass),
                "description" to Tool.getToolDescription(toolClass),
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to getToolParameters(toolClass),
                    "required" to getRequiredParameters(toolClass)
                )
            )
        }
        
        return mapOf("tools" to tools)
    }
    
    fun handleToolCall(name: String, arguments: Map<String, Any>): Map<String, Any> {
        val toolClass = ToolRegistry.getByName(name)
            ?: throw IllegalArgumentException("Tool not found: $name")
        
        val tool = ToolRegistry.createTool(toolClass, agent)
        
        return try {
            val result = tool.applyEx(kwargs = arguments)
            mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to result
                    )
                ),
                "isError" to false
            )
        } catch (e: Exception) {
            logger.error(e) { "Error executing tool $name" }
            mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to "Error: ${e.message}"
                    )
                ),
                "isError" to true
            )
        }
    }
    
    private fun getToolParameters(toolClass: kotlin.reflect.KClass<out Tool>): Map<String, Any> {
        // In a full implementation, this would use reflection to analyze the apply method parameters
        // For now, return a generic schema
        return mapOf(
            "description" to mapOf(
                "type" to "string",
                "description" to "Tool parameters"
            )
        )
    }
    
    private fun getRequiredParameters(toolClass: kotlin.reflect.KClass<out Tool>): List<String> {
        // In a full implementation, this would analyze which parameters are required
        return emptyList()
    }
}