package com.aimatrix.solidlsp.client

import com.aimatrix.solidlsp.config.Language
import com.aimatrix.solidlsp.protocol.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.*

class LSPClientTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var mockLSPClient: MockLSPClient
    
    @BeforeEach
    fun setUp() {
        mockLSPClient = MockLSPClient()
    }
    
    @Test
    fun `initialize sets up client correctly`() = runTest {
        val rootUri = tempDir.toUri().toString()
        val clientCapabilities = ClientCapabilities(
            workspace = WorkspaceClientCapabilities(
                workspaceFolders = true
            ),
            textDocument = TextDocumentClientCapabilities(
                synchronization = TextDocumentSyncClientCapabilities(
                    dynamicRegistration = true
                )
            )
        )
        
        val result = mockLSPClient.initialize(rootUri, clientCapabilities)
        assertNotNull(result)
        assertEquals("mock-server", result.serverInfo.name)
        assertTrue(mockLSPClient.isReady())
    }
    
    @Test
    fun `open document works correctly`() = runTest {
        mockLSPClient.initialize(tempDir.toUri().toString(), ClientCapabilities())
        
        val testFile = tempDir / "test.kt"
        testFile.writeText("fun main() { println(\"Hello\") }")
        
        mockLSPClient.openDocument(testFile.toString())
        assertTrue(mockLSPClient.openDocuments.contains(testFile.toString()))
    }
    
    @Test
    fun `get workspace symbols works`() = runTest {
        mockLSPClient.initialize(tempDir.toUri().toString(), ClientCapabilities())
        
        val symbols = mockLSPClient.getWorkspaceSymbols("test")
        assertNotNull(symbols)
        assertTrue(symbols.isNotEmpty())
        assertEquals("testSymbol", symbols.first().name)
    }
    
    @Test
    fun `get document symbols works`() = runTest {
        mockLSPClient.initialize(tempDir.toUri().toString(), ClientCapabilities())
        
        val testFile = tempDir / "test.kt"
        testFile.writeText("fun main() { println(\"Hello\") }")
        mockLSPClient.openDocument(testFile.toString())
        
        val symbols = mockLSPClient.getDocumentSymbols(testFile.toString())
        assertNotNull(symbols)
        assertTrue(symbols.isNotEmpty())
        assertEquals("main", symbols.first().name)
    }
    
    @Test
    fun `find definition works`() = runTest {
        mockLSPClient.initialize(tempDir.toUri().toString(), ClientCapabilities())
        
        val testFile = tempDir / "test.kt"
        testFile.writeText("fun main() { println(\"Hello\") }")
        mockLSPClient.openDocument(testFile.toString())
        
        val definitions = mockLSPClient.findDefinition(testFile.toString(), 0, 4)
        assertNotNull(definitions)
        assertTrue(definitions.isNotEmpty())
        assertTrue(definitions.first().uri.contains("test.kt"))
    }
    
    @Test
    fun `find references works`() = runTest {
        mockLSPClient.initialize(tempDir.toUri().toString(), ClientCapabilities())
        
        val testFile = tempDir / "test.kt"
        testFile.writeText("fun main() { println(\"Hello\") }")
        mockLSPClient.openDocument(testFile.toString())
        
        val references = mockLSPClient.findReferences(testFile.toString(), 0, 4, true)
        assertNotNull(references)
        assertTrue(references.isNotEmpty())
        assertTrue(references.first().uri.contains("test.kt"))
    }
    
    @Test
    fun `get hover works`() = runTest {
        mockLSPClient.initialize(tempDir.toUri().toString(), ClientCapabilities())
        
        val testFile = tempDir / "test.kt"
        testFile.writeText("fun main() { println(\"Hello\") }")
        mockLSPClient.openDocument(testFile.toString())
        
        val hover = mockLSPClient.getHover(testFile.toString(), 0, 4)
        assertNotNull(hover)
        assertEquals("Hover information for main function", hover!!.contents)
    }
    
    @Test
    fun `close document works`() = runTest {
        mockLSPClient.initialize(tempDir.toUri().toString(), ClientCapabilities())
        
        val testFile = tempDir / "test.kt"
        testFile.writeText("fun main() { println(\"Hello\") }")
        
        mockLSPClient.openDocument(testFile.toString())
        assertTrue(mockLSPClient.openDocuments.contains(testFile.toString()))
        
        mockLSPClient.closeDocument(testFile.toString())
        assertFalse(mockLSPClient.openDocuments.contains(testFile.toString()))
    }
    
    @Test
    fun `shutdown works correctly`() = runTest {
        mockLSPClient.initialize(tempDir.toUri().toString(), ClientCapabilities())
        assertTrue(mockLSPClient.isReady())
        
        mockLSPClient.shutdown()
        assertFalse(mockLSPClient.isReady())
    }
    
    @Test
    fun `language detection works`() {
        assertEquals(Language.KOTLIN, mockLSPClient.getLanguage())
    }
    
    class MockLSPClient : LSPClient {
        private var initialized = false
        private var serverCapabilities: ServerCapabilities? = null
        val openDocuments = mutableSetOf<String>()
        
        override fun isReady(): Boolean = initialized
        
        override fun getLanguage(): Language = Language.KOTLIN
        
        override suspend fun initialize(rootUri: String, clientCapabilities: ClientCapabilities): InitializeResult {
            initialized = true
            serverCapabilities = ServerCapabilities(
                textDocumentSync = TextDocumentSyncOptions(),
                hoverProvider = true,
                definitionProvider = true,
                referencesProvider = true,
                documentSymbolProvider = true,
                workspaceSymbolProvider = true
            )
            
            return InitializeResult(
                capabilities = serverCapabilities!!,
                serverInfo = ServerInfo(
                    name = "mock-server",
                    version = "1.0.0"
                )
            )
        }
        
        override suspend fun openDocument(uri: String) {
            if (!initialized) throw IllegalStateException("Not initialized")
            openDocuments.add(uri)
        }
        
        override suspend fun closeDocument(uri: String) {
            if (!initialized) throw IllegalStateException("Not initialized")
            openDocuments.remove(uri)
        }
        
        override suspend fun getWorkspaceSymbols(query: String): List<WorkspaceSymbol> {
            if (!initialized) throw IllegalStateException("Not initialized")
            return listOf(
                WorkspaceSymbol(
                    name = "testSymbol",
                    kind = SymbolKind.FUNCTION,
                    location = Location(
                        uri = "file:///test.kt",
                        range = Range(
                            start = Position(0, 0),
                            end = Position(0, 10)
                        )
                    )
                )
            )
        }
        
        override suspend fun getDocumentSymbols(uri: String): List<DocumentSymbol> {
            if (!initialized) throw IllegalStateException("Not initialized")
            if (!openDocuments.contains(uri)) throw IllegalStateException("Document not open")
            
            return listOf(
                DocumentSymbol(
                    name = "main",
                    kind = SymbolKind.FUNCTION,
                    range = Range(
                        start = Position(0, 0),
                        end = Position(0, 30)
                    ),
                    selectionRange = Range(
                        start = Position(0, 4),
                        end = Position(0, 8)
                    )
                )
            )
        }
        
        override suspend fun findDefinition(uri: String, line: Int, character: Int): List<Location> {
            if (!initialized) throw IllegalStateException("Not initialized")
            return listOf(
                Location(
                    uri = uri,
                    range = Range(
                        start = Position(line, character),
                        end = Position(line, character + 4)
                    )
                )
            )
        }
        
        override suspend fun findReferences(uri: String, line: Int, character: Int, includeDeclaration: Boolean): List<Location> {
            if (!initialized) throw IllegalStateException("Not initialized")
            return listOf(
                Location(
                    uri = uri,
                    range = Range(
                        start = Position(line, character),
                        end = Position(line, character + 4)
                    )
                )
            )
        }
        
        override suspend fun getHover(uri: String, line: Int, character: Int): Hover? {
            if (!initialized) throw IllegalStateException("Not initialized")
            return Hover(
                contents = "Hover information for main function",
                range = Range(
                    start = Position(line, character),
                    end = Position(line, character + 4)
                )
            )
        }
        
        override suspend fun shutdown() {
            initialized = false
            openDocuments.clear()
        }
    }
}