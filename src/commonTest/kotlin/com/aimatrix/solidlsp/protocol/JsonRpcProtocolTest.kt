package com.aimatrix.solidlsp.protocol

import kotlinx.serialization.json.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class JsonRpcProtocolTest {
    
    private lateinit var protocol: JsonRpcProtocol
    
    @BeforeEach
    fun setUp() {
        protocol = JsonRpcProtocol()
    }
    
    @Test
    fun `parse valid request message`() {
        val json = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05"
                }
            }
        """.trimIndent()
        
        val message = protocol.parseMessage(json)
        assertNotNull(message)
        assertTrue(message is JsonRpcMessage.Request)
        
        val request = message as JsonRpcMessage.Request
        assertEquals("initialize", request.request.method)
        assertEquals(JsonPrimitive(1), request.request.id)
        
        val params = request.request.params as JsonObject
        assertEquals("2024-11-05", params["protocolVersion"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun `parse valid notification message`() {
        val json = """
            {
                "jsonrpc": "2.0",
                "method": "initialized"
            }
        """.trimIndent()
        
        val message = protocol.parseMessage(json)
        assertNotNull(message)
        assertTrue(message is JsonRpcMessage.Notification)
        
        val notification = message as JsonRpcMessage.Notification
        assertEquals("initialized", notification.notification.method)
        assertNull(notification.notification.params)
    }
    
    @Test
    fun `parse valid response message`() {
        val json = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": {
                    "protocolVersion": "2024-11-05"
                }
            }
        """.trimIndent()
        
        val message = protocol.parseMessage(json)
        assertNotNull(message)
        assertTrue(message is JsonRpcMessage.Response)
        
        val response = message as JsonRpcMessage.Response
        assertEquals(JsonPrimitive(1), response.response.id)
        
        val result = response.response.result as JsonObject
        assertEquals("2024-11-05", result["protocolVersion"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun `parse error response message`() {
        val json = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "error": {
                    "code": -32600,
                    "message": "Invalid Request"
                }
            }
        """.trimIndent()
        
        val message = protocol.parseMessage(json)
        assertNotNull(message)
        assertTrue(message is JsonRpcMessage.Response)
        
        val response = message as JsonRpcMessage.Response
        assertEquals(JsonPrimitive(1), response.response.id)
        assertNotNull(response.response.error)
        
        val error = response.response.error!!
        assertEquals(-32600, error.code)
        assertEquals("Invalid Request", error.message)
    }
    
    @Test
    fun `parse invalid json returns null`() {
        val invalidJson = "{ invalid json }"
        val message = protocol.parseMessage(invalidJson)
        assertNull(message)
    }
    
    @Test
    fun `parse missing jsonrpc version returns null`() {
        val json = """
            {
                "id": 1,
                "method": "test"
            }
        """.trimIndent()
        
        val message = protocol.parseMessage(json)
        assertNull(message)
    }
    
    @Test
    fun `serialize response with result`() {
        val responseData = mapOf(
            "protocolVersion" to "2024-11-05",
            "serverInfo" to mapOf(
                "name" to "test-server",
                "version" to "1.0.0"
            )
        )
        
        val response = protocol.serializeResponse(JsonPrimitive(1), responseData)
        
        assertTrue(response.contains("\"jsonrpc\":\"2.0\""))
        assertTrue(response.contains("\"id\":1"))
        assertTrue(response.contains("protocolVersion"))
        assertTrue(response.contains("test-server"))
        
        // Verify it's valid JSON
        assertDoesNotThrow {
            Json.parseToJsonElement(response)
        }
    }
    
    @Test
    fun `serialize notification`() {
        val params = mapOf("uri" to "file:///test.kt")
        val notification = protocol.serializeNotification("textDocument/didOpen", params)
        
        assertTrue(notification.contains("\"jsonrpc\":\"2.0\""))
        assertTrue(notification.contains("\"method\":\"textDocument/didOpen\""))
        assertTrue(notification.contains("file:///test.kt"))
        assertFalse(notification.contains("\"id\"")) // Notifications don't have IDs
        
        // Verify it's valid JSON
        assertDoesNotThrow {
            Json.parseToJsonElement(notification)
        }
    }
    
    @Test
    fun `create error response`() {
        val errorResponse = protocol.createErrorResponse(
            JsonPrimitive(1),
            JsonRpcErrorCodes.INVALID_PARAMS,
            "Missing required parameter"
        )
        
        assertTrue(errorResponse.contains("\"jsonrpc\":\"2.0\""))
        assertTrue(errorResponse.contains("\"id\":1"))
        assertTrue(errorResponse.contains("\"error\""))
        assertTrue(errorResponse.contains("\"code\":${JsonRpcErrorCodes.INVALID_PARAMS}"))
        assertTrue(errorResponse.contains("Missing required parameter"))
        
        // Verify it's valid JSON
        assertDoesNotThrow {
            Json.parseToJsonElement(errorResponse)
        }
    }
    
    @Test
    fun `decode params with map`() {
        val params = JsonObject(mapOf(
            "key1" to JsonPrimitive("value1"),
            "key2" to JsonPrimitive(42),
            "key3" to JsonPrimitive(true)
        ))
        
        val decoded = protocol.decodeParams<Map<String, Any>>(params)
        assertNotNull(decoded)
        assertEquals("value1", decoded!!["key1"])
        assertEquals(42, decoded["key2"])
        assertEquals(true, decoded["key3"])
    }
    
    @Test
    fun `decode params with null returns null`() {
        val decoded = protocol.decodeParams<Map<String, Any>>(null)
        assertNull(decoded)
    }
    
    @Test
    fun `handle different id types`() {
        // String ID
        val stringIdJson = """
            {
                "jsonrpc": "2.0",
                "id": "string-id",
                "method": "test"
            }
        """.trimIndent()
        
        val stringIdMessage = protocol.parseMessage(stringIdJson)
        assertNotNull(stringIdMessage)
        assertTrue(stringIdMessage is JsonRpcMessage.Request)
        assertEquals(JsonPrimitive("string-id"), (stringIdMessage as JsonRpcMessage.Request).request.id)
        
        // Null ID (notification)
        val nullIdJson = """
            {
                "jsonrpc": "2.0",
                "method": "notification"
            }
        """.trimIndent()
        
        val nullIdMessage = protocol.parseMessage(nullIdJson)
        assertNotNull(nullIdMessage)
        assertTrue(nullIdMessage is JsonRpcMessage.Notification)
    }
    
    @Test
    fun `handle complex nested params`() {
        val json = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "complex",
                "params": {
                    "array": [1, 2, 3],
                    "nested": {
                        "deep": {
                            "value": "test"
                        }
                    },
                    "mixed": [
                        {"key": "value"},
                        "string",
                        42
                    ]
                }
            }
        """.trimIndent()
        
        val message = protocol.parseMessage(json)
        assertNotNull(message)
        assertTrue(message is JsonRpcMessage.Request)
        
        val request = message as JsonRpcMessage.Request
        val params = request.request.params as JsonObject
        
        val array = params["array"] as JsonArray
        assertEquals(3, array.size)
        assertEquals(1, array[0].jsonPrimitive.int)
        
        val nested = params["nested"] as JsonObject
        val deep = nested["deep"] as JsonObject
        assertEquals("test", deep["value"]?.jsonPrimitive?.content)
    }
}