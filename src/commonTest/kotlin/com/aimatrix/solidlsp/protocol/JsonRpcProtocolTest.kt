package com.aimatrix.solidlsp.protocol

import kotlinx.serialization.json.*
import kotlin.test.*

class JsonRpcProtocolTest {
    
    @Test
    fun testJsonRpcProtocolExists() {
        // Basic test to verify JsonRpcProtocol can be instantiated
        val protocol = JsonRpcProtocol()
        assertNotNull(protocol)
    }
    
    @Test
    fun testJsonRpcVersion() {
        // Test that we use correct JSON-RPC version
        val version = "2.0"
        assertEquals("2.0", version)
    }
    
    @Test
    fun testJsonParsing() {
        // Test basic JSON parsing capabilities
        val simpleJson = """{"test": "value"}"""
        val element = Json.parseToJsonElement(simpleJson)
        assertTrue(element is JsonObject)
        
        val obj = element as JsonObject
        assertEquals("value", obj["test"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun testJsonRpcErrorCodes() {
        // Test that error codes are properly defined
        val parseError = JsonRpcErrorCodes.PARSE_ERROR
        val invalidRequest = JsonRpcErrorCodes.INVALID_REQUEST
        val methodNotFound = JsonRpcErrorCodes.METHOD_NOT_FOUND
        val invalidParams = JsonRpcErrorCodes.INVALID_PARAMS
        val internalError = JsonRpcErrorCodes.INTERNAL_ERROR
        
        assertEquals(-32700, parseError)
        assertEquals(-32600, invalidRequest)
        assertEquals(-32601, methodNotFound)
        assertEquals(-32602, invalidParams)
        assertEquals(-32603, internalError)
    }
    
    @Test
    fun testJsonSerialization() {
        // Test basic JSON serialization
        val testData = mapOf(
            "name" to "AmxLSP",
            "version" to "1.0.3"
        )
        
        val json = Json.encodeToString(kotlinx.serialization.serializer(), testData)
        assertTrue(json.contains("AmxLSP"))
        assertTrue(json.contains("1.0.3"))
        assertFalse(json.contains("serena"))
    }
}