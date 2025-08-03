package com.aimatrix.amxlsp.mcp

import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.amxlsp.tools.ToolRegistry
import com.aimatrix.solidlsp.protocol.JsonRpcProtocol
import kotlinx.serialization.json.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class AmxLspMCPTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var agent: AmxLspAgent
    private lateinit var protocol: MCPProtocolHandler
    private lateinit var jsonRpc: JsonRpcProtocol
    
    @BeforeEach
    fun setUp() {
        agent = AmxLspAgent()
        protocol = MCPProtocolHandler(agent)
        jsonRpc = JsonRpcProtocol()
    }
    
    @Test
    fun `initialize returns correct server info`() {
        val params = mapOf(
            "protocolVersion" to "2024-11-05",
            "clientInfo" to mapOf(
                "name" to "test-client",
                "version" to "1.0.0"
            )
        )
        
        val result = protocol.handleInitialize(params)
        
        assertEquals("2024-11-05", result["protocolVersion"])
        
        val serverInfo = result["serverInfo"] as Map<*, *>
        assertEquals("amxlsp-mcp-server", serverInfo["name"])
        assertEquals("0.1.3", serverInfo["version"])
        
        val capabilities = result["capabilities"] as Map<*, *>
        assertTrue(capabilities.containsKey("tools"))
        assertTrue(capabilities.containsKey("logging"))
        assertTrue(capabilities.containsKey("prompts"))
        assertTrue(capabilities.containsKey("resources"))
    }
    
    @Test
    fun `tools list returns registered tools`() {
        val result = protocol.handleToolsList()
        val tools = result["tools"] as List<*>
        
        assertTrue(tools.isNotEmpty())
        
        val firstTool = tools.first() as Map<*, *>
        assertTrue(firstTool.containsKey("name"))
        assertTrue(firstTool.containsKey("description"))
        assertTrue(firstTool.containsKey("inputSchema"))
        
        val inputSchema = firstTool["inputSchema"] as Map<*, *>
        assertEquals("object", inputSchema["type"])
        assertTrue(inputSchema.containsKey("properties"))
        assertTrue(inputSchema.containsKey("required"))
    }
    
    @Test
    fun `tool call with valid tool returns result`() {
        // Try to call a tool that should exist (GetConfigTool)
        val result = protocol.handleToolCall("GetConfigTool", emptyMap())
        
        assertTrue(result.containsKey("content"))
        assertTrue(result.containsKey("isError"))
        assertEquals(false, result["isError"])
        
        val content = result["content"] as List<*>
        assertTrue(content.isNotEmpty())
        
        val firstContent = content.first() as Map<*, *>
        assertEquals("text", firstContent["type"])
        assertTrue(firstContent.containsKey("text"))
    }
    
    @Test
    fun `tool call with invalid tool returns error`() {
        val result = protocol.handleToolCall("NonExistentTool", emptyMap())
        
        assertTrue(result.containsKey("content"))
        assertTrue(result.containsKey("isError"))
        assertEquals(true, result["isError"])
        
        val content = result["content"] as List<*>
        val errorContent = content.first() as Map<*, *>
        val errorText = errorContent["text"] as String
        assertTrue(errorText.contains("Error"))
    }
    
    @Test
    fun `json rpc message parsing`() {
        val requestJson = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05"
                }
            }
        """.trimIndent()
        
        val message = jsonRpc.parseMessage(requestJson)
        assertNotNull(message)
        assertTrue(message is com.aimatrix.solidlsp.protocol.JsonRpcMessage.Request)
        
        val request = message as com.aimatrix.solidlsp.protocol.JsonRpcMessage.Request
        assertEquals("initialize", request.request.method)
        assertEquals(JsonPrimitive(1), request.request.id)
    }
    
    @Test
    fun `json rpc response serialization`() {
        val responseData = mapOf(
            "protocolVersion" to "2024-11-05",
            "serverInfo" to mapOf(
                "name" to "test-server",
                "version" to "1.0.0"
            )
        )
        
        val response = jsonRpc.serializeResponse(JsonPrimitive(1), responseData)
        assertNotNull(response)
        assertTrue(response.contains("\"jsonrpc\":\"2.0\""))
        assertTrue(response.contains("\"id\":1"))
        assertTrue(response.contains("protocolVersion"))
    }
    
    @Test
    fun `stdio server factory creates server`() {
        val factory = AmxLspMCPFactory()
        val server = factory.createStdioServer()
        
        assertNotNull(server)
        assertTrue(server is StdioMCPServer)
    }
    
    @Test
    fun `websocket server factory creates server`() {
        val factory = AmxLspMCPFactory()
        val server = factory.createWebSocketServer("localhost", 8080)
        
        assertNotNull(server)
        assertTrue(server is WebSocketMCPServer)
    }
    
    @Test
    fun `single process factory creates servers`() {
        val factory = AmxLspMCPFactorySingleProcess()
        val stdioServer = factory.createStdioServer()
        val wsServer = factory.createWebSocketServer("localhost", 8080)
        
        assertNotNull(stdioServer)
        assertNotNull(wsServer)
    }
    
    @Test
    fun `error response creation`() {
        val errorResponse = jsonRpc.createErrorResponse(
            JsonPrimitive(1),
            -32600,
            "Invalid Request"
        )
        
        assertTrue(errorResponse.contains("\"error\""))
        assertTrue(errorResponse.contains("\"code\":-32600"))
        assertTrue(errorResponse.contains("Invalid Request"))
    }
    
    @Test
    fun `notification handling`() {
        val notificationJson = """
            {
                "jsonrpc": "2.0",
                "method": "initialized"
            }
        """.trimIndent()
        
        val message = jsonRpc.parseMessage(notificationJson)
        assertNotNull(message)
        assertTrue(message is com.aimatrix.solidlsp.protocol.JsonRpcMessage.Notification)
        
        val notification = message as com.aimatrix.solidlsp.protocol.JsonRpcMessage.Notification
        assertEquals("initialized", notification.notification.method)
    }
}