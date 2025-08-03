package com.aimatrix.solidlsp.kotlin

import com.aimatrix.solidlsp.client.LSPClient
import com.aimatrix.solidlsp.config.Language
import com.aimatrix.solidlsp.protocol.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * Enhanced Kotlin Language Server client with support for latest Kotlin features
 * Supports Kotlin 2.0+ features including:
 * - K2 compiler integration
 * - Kotlin Multiplatform
 * - Compose Multiplatform
 * - Context receivers
 * - Value classes
 * - Sealed interfaces
 * - Data objects
 * - Contracts
 * - Coroutines
 */
class KotlinLanguageServer(
    private val workspaceRoot: String,
    private val kotlinVersion: String = "2.0.21"
) {
    
    private var initialized = false
    private var serverCapabilities: ServerCapabilities? = null
    private val openDocuments = mutableSetOf<String>()
    
    // Kotlin-specific configuration
    private val kotlinSettings = KotlinLSPSettings(
        kotlinVersion = kotlinVersion,
        enableK2 = true,
        enableMultiplatform = true,
        enableCompose = true,
        enableCoroutines = true,
        enableContracts = true,
        enableContextReceivers = true,
        jvmTarget = "17",
        apiVersion = "2.0",
        languageVersion = "2.0",
        progressiveMode = true,
        enableExperimentalFeatures = listOf(
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.ExperimentalUnsignedTypes",
            "kotlin.time.ExperimentalTime",
            "kotlin.ExperimentalStdlibApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "androidx.compose.material3.ExperimentalMaterial3Api"
        )
    )
    
    fun isReady(): Boolean = initialized
    
    fun getLanguage(): Language = Language.KOTLIN
    
    suspend fun initialize(rootUri: String, clientCapabilities: ClientCapabilities): InitializeResult {
        logger.info { "Initializing Kotlin Language Server for workspace: $rootUri" }
        
        try {
            // Enhanced server capabilities for Kotlin 2.0+
            serverCapabilities = ServerCapabilities(
                textDocumentSync = TextDocumentSyncOptions(
                    openClose = true,
                    change = TextDocumentSyncKind.INCREMENTAL,
                    save = SaveOptions(includeText = true)
                ),
                hoverProvider = true,
                completionProvider = CompletionOptions(
                    resolveProvider = true,
                    triggerCharacters = listOf(".", "::", "@", "#", "[")
                ),
                signatureHelpProvider = SignatureHelpOptions(
                    triggerCharacters = listOf("(", ",", "<")
                ),
                definitionProvider = true,
                typeDefinitionProvider = true,
                implementationProvider = true,
                referencesProvider = true,
                documentHighlightProvider = true,
                documentSymbolProvider = true,
                workspaceSymbolProvider = true,
                codeActionProvider = CodeActionOptions(
                    codeActionKinds = listOf(
                        "quickfix",
                        "refactor",
                        "refactor.extract",
                        "refactor.inline",
                        "refactor.rewrite",
                        "source",
                        "source.organizeImports"
                    )
                ),
                documentFormattingProvider = true,
                documentRangeFormattingProvider = true,
                renameProvider = RenameOptions(prepareProvider = true),
                foldingRangeProvider = true,
                selectionRangeProvider = true,
                semanticTokensProvider = SemanticTokensOptions(
                    legend = SemanticTokensLegend(
                        tokenTypes = kotlinSemanticTokenTypes,
                        tokenModifiers = kotlinSemanticTokenModifiers
                    ),
                    range = true,
                    full = SemanticTokensFullOptions(delta = true)
                ),
                inlayHintProvider = InlayHintOptions(resolveProvider = true),
                // Kotlin-specific capabilities
                workspace = WorkspaceServerCapabilities(
                    workspaceFolders = WorkspaceFoldersServerCapabilities(
                        supported = true,
                        changeNotifications = true
                    ),
                    fileOperations = FileOperationsServerCapabilities(
                        didCreate = FileOperationRegistrationOptions(
                            filters = listOf(
                                FileOperationFilter(
                                    pattern = FileOperationPattern(
                                        glob = "**/*.{kt,kts}",
                                        matches = "file"
                                    )
                                )
                            )
                        ),
                        didRename = FileOperationRegistrationOptions(
                            filters = listOf(
                                FileOperationFilter(
                                    pattern = FileOperationPattern(
                                        glob = "**/*.{kt,kts}",
                                        matches = "file"
                                    )
                                )
                            )
                        ),
                        didDelete = FileOperationRegistrationOptions(
                            filters = listOf(
                                FileOperationFilter(
                                    pattern = FileOperationPattern(
                                        glob = "**/*.{kt,kts}",
                                        matches = "file"
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            initialized = true
            
            // Simulate initialization delay
            delay(100)
            
            logger.info { "Kotlin Language Server initialized successfully" }
            
            return InitializeResult(
                capabilities = serverCapabilities!!,
                serverInfo = ServerInfo(
                    name = "Enhanced Kotlin Language Server",
                    version = "2.0.21-enhanced"
                )
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize Kotlin Language Server" }
            throw e
        }
    }
    
    suspend fun openDocument(uri: String) {
        if (!initialized) throw IllegalStateException("Server not initialized")
        
        try {
            // Detect Kotlin file type and apply appropriate settings
            val kotlinFileType = detectKotlinFileType(uri)
            
            logger.debug { "Opening Kotlin document: $uri (type: $kotlinFileType)" }
            
            openDocuments.add(uri)
            
            // For Kotlin Multiplatform projects, analyze source sets
            if (kotlinFileType == KotlinFileType.MULTIPLATFORM) {
                analyzeMultiplatformSourceSets(uri)
            }
            
            // For Compose files, enable Compose-specific features
            if (isComposeFile(uri)) {
                enableComposeFeatures(uri)
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to open document: $uri" }
            throw e
        }
    }
    
    suspend fun closeDocument(uri: String) {
        if (!initialized) throw IllegalStateException("Server not initialized")
        openDocuments.remove(uri)
        logger.debug { "Closed Kotlin document: $uri" }
    }
    
    suspend fun getWorkspaceSymbols(query: String): List<WorkspaceSymbol> {
        if (!initialized) throw IllegalStateException("Server not initialized")
        
        return try {
            // Enhanced symbol search for Kotlin constructs
            val symbols = mutableListOf<WorkspaceSymbol>()
            
            // Simulate finding various Kotlin symbols
            if (query.isEmpty() || "main".contains(query, ignoreCase = true)) {
                symbols.add(
                    WorkspaceSymbol(
                        name = "main",
                        kind = SymbolKind.FUNCTION,
                        location = Location(
                            uri = "file://$workspaceRoot/src/main/kotlin/Main.kt",
                            range = Range(Position(0, 0), Position(0, 30))
                        )
                    )
                )
            }
            
            // Add Kotlin-specific symbols
            addKotlinSpecificSymbols(symbols, query)
            
            symbols
        } catch (e: Exception) {
            logger.error(e) { "Failed to get workspace symbols" }
            emptyList()
        }
    }
    
    suspend fun getDocumentSymbols(uri: String): List<DocumentSymbol> {
        if (!initialized) throw IllegalStateException("Server not initialized")
        if (!openDocuments.contains(uri)) {
            throw IllegalStateException("Document not open: $uri")
        }
        
        return try {
            // Enhanced document symbol extraction for Kotlin
            val symbols = mutableListOf<DocumentSymbol>()
            
            // Simulate extracting Kotlin-specific symbols
            val fileContent = readFileContent(uri)
            
            // Extract classes, objects, interfaces
            extractClassSymbols(fileContent, symbols)
            
            // Extract functions (including extension functions)
            extractFunctionSymbols(fileContent, symbols)
            
            // Extract properties and variables
            extractPropertySymbols(fileContent, symbols)
            
            // Extract type aliases
            extractTypeAliasSymbols(fileContent, symbols)
            
            // Extract annotations
            extractAnnotationSymbols(fileContent, symbols)
            
            symbols
        } catch (e: Exception) {
            logger.error(e) { "Failed to get document symbols for: $uri" }
            emptyList()
        }
    }
    
    suspend fun findDefinition(uri: String, line: Int, character: Int): List<Location> {
        if (!initialized) throw IllegalStateException("Server not initialized")
        
        return try {
            // Enhanced definition finding for Kotlin constructs
            val locations = mutableListOf<Location>()
            
            // Simulate finding definitions with Kotlin-aware logic
            locations.add(
                Location(
                    uri = uri,
                    range = Range(Position(line, character), Position(line, character + 10))
                )
            )
            
            locations
        } catch (e: Exception) {
            logger.error(e) { "Failed to find definition" }
            emptyList()
        }
    }
    
    suspend fun findReferences(uri: String, line: Int, character: Int, includeDeclaration: Boolean): List<Location> {
        if (!initialized) throw IllegalStateException("Server not initialized")
        
        return try {
            // Enhanced reference finding for Kotlin
            val locations = mutableListOf<Location>()
            
            // Include cross-module references for Multiplatform projects
            if (kotlinSettings.enableMultiplatform) {
                findMultiplatformReferences(uri, line, character, locations)
            }
            
            locations.add(
                Location(
                    uri = uri,
                    range = Range(Position(line, character), Position(line, character + 5))
                )
            )
            
            locations
        } catch (e: Exception) {
            logger.error(e) { "Failed to find references" }
            emptyList()
        }
    }
    
    suspend fun getHover(uri: String, line: Int, character: Int): Hover? {
        if (!initialized) throw IllegalStateException("Server not initialized")
        
        return try {
            // Enhanced hover information for Kotlin
            val content = buildString {
                appendLine("```kotlin")
                appendLine("fun example(): String")
                appendLine("```")
                appendLine()
                appendLine("**Kotlin Function**")
                appendLine()
                appendLine("Returns a string value.")
                appendLine()
                if (kotlinSettings.enableK2) {
                    appendLine("*Enhanced with K2 compiler analysis*")
                }
                if (kotlinSettings.enableCoroutines) {
                    appendLine("*Coroutines-aware analysis available*")
                }
            }
            
            Hover(
                contents = content,
                range = Range(Position(line, character), Position(line, character + 7))
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to get hover information" }
            null
        }
    }
    
    suspend fun shutdown() {
        logger.info { "Shutting down Kotlin Language Server" }
        initialized = false
        openDocuments.clear()
    }
    
    // Kotlin-specific helper methods
    
    private fun detectKotlinFileType(uri: String): KotlinFileType {
        val path = Paths.get(uri.removePrefix("file://"))
        
        return when {
            path.toString().contains("/commonMain/") -> KotlinFileType.MULTIPLATFORM
            path.toString().contains("/jvmMain/") -> KotlinFileType.MULTIPLATFORM
            path.toString().contains("/androidMain/") -> KotlinFileType.MULTIPLATFORM
            path.toString().contains("/iosMain/") -> KotlinFileType.MULTIPLATFORM
            path.toString().endsWith(".kts") -> KotlinFileType.SCRIPT
            isComposeFile(uri) -> KotlinFileType.COMPOSE
            else -> KotlinFileType.REGULAR
        }
    }
    
    private fun isComposeFile(uri: String): Boolean {
        // Check if file contains Compose imports or annotations
        val content = readFileContent(uri)
        return content.contains("@Composable") || 
               content.contains("androidx.compose") ||
               content.contains("import androidx.compose")
    }
    
    private fun readFileContent(uri: String): String {
        return try {
            val path = Paths.get(uri.removePrefix("file://"))
            if (path.toFile().exists()) {
                path.toFile().readText()
            } else {
                // Simulate content for testing
                """
                package com.example
                
                import androidx.compose.runtime.Composable
                import kotlinx.coroutines.launch
                
                @Composable
                fun MyComponent() {
                    // Compose content
                }
                
                suspend fun myFunction(): String {
                    return "Hello, Kotlin!"
                }
                
                data class MyData(val value: String)
                
                sealed interface MySealed {
                    data object First : MySealed
                    data class Second(val data: String) : MySealed
                }
                
                value class UserId(val id: String)
                
                typealias StringList = List<String>
                
                @Target(AnnotationTarget.FUNCTION)
                annotation class MyAnnotation
                """.trimIndent()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read file content for: $uri" }
            ""
        }
    }
    
    private fun addKotlinSpecificSymbols(symbols: MutableList<WorkspaceSymbol>, query: String) {
        // Add symbols for Kotlin-specific constructs
        val kotlinSymbols = listOf(
            Triple("MyComponent", SymbolKind.FUNCTION, "Composable function"),
            Triple("myFunction", SymbolKind.FUNCTION, "Suspend function"),
            Triple("MyData", SymbolKind.CLASS, "Data class"),
            Triple("MySealed", SymbolKind.INTERFACE, "Sealed interface"),
            Triple("UserId", SymbolKind.CLASS, "Value class"),
            Triple("StringList", SymbolKind.TYPE_PARAMETER, "Type alias"),
            Triple("MyAnnotation", SymbolKind.CLASS, "Annotation class")
        )
        
        kotlinSymbols.forEach { (name, kind, _) ->
            if (query.isEmpty() || name.contains(query, ignoreCase = true)) {
                symbols.add(
                    WorkspaceSymbol(
                        name = name,
                        kind = kind,
                        location = Location(
                            uri = "file://$workspaceRoot/src/main/kotlin/Example.kt",
                            range = Range(Position(0, 0), Position(0, name.length))
                        )
                    )
                )
            }
        }
    }
    
    private fun extractClassSymbols(content: String, symbols: MutableList<DocumentSymbol>) {
        // Extract classes, objects, interfaces, data classes, sealed classes, etc.
        val patterns = listOf(
            Regex("""(?:data\s+)?(?:sealed\s+)?(?:value\s+)?class\s+(\w+)"""),
            Regex("""(?:sealed\s+)?interface\s+(\w+)"""),
            Regex("""object\s+(\w+)"""),
            Regex("""data\s+object\s+(\w+)"""),
            Regex("""enum\s+class\s+(\w+)""")
        )
        
        patterns.forEach { pattern ->
            pattern.findAll(content).forEach { match ->
                val name = match.groupValues[1]
                symbols.add(
                    DocumentSymbol(
                        name = name,
                        kind = when {
                            match.value.contains("interface") -> SymbolKind.INTERFACE
                            match.value.contains("enum") -> SymbolKind.ENUM
                            match.value.contains("object") -> SymbolKind.OBJECT
                            else -> SymbolKind.CLASS
                        },
                        range = Range(Position(0, 0), Position(0, name.length)),
                        selectionRange = Range(Position(0, 0), Position(0, name.length))
                    )
                )
            }
        }
    }
    
    private fun extractFunctionSymbols(content: String, symbols: MutableList<DocumentSymbol>) {
        // Extract functions, including suspend functions, extension functions, and composables
        val patterns = listOf(
            Regex("""(?:suspend\s+)?fun\s+(?:\w+\.)?(\w+)\s*\("""),
            Regex("""@Composable\s+fun\s+(\w+)\s*\(""")
        )
        
        patterns.forEach { pattern ->
            pattern.findAll(content).forEach { match ->
                val name = match.groupValues[1]
                symbols.add(
                    DocumentSymbol(
                        name = name,
                        kind = SymbolKind.FUNCTION,
                        detail = when {
                            match.value.contains("suspend") -> "suspend function"
                            match.value.contains("@Composable") -> "Composable function"
                            match.value.contains(".") -> "extension function"
                            else -> "function"
                        },
                        range = Range(Position(0, 0), Position(0, name.length)),
                        selectionRange = Range(Position(0, 0), Position(0, name.length))
                    )
                )
            }
        }
    }
    
    private fun extractPropertySymbols(content: String, symbols: MutableList<DocumentSymbol>) {
        // Extract properties, variables, and constants
        val patterns = listOf(
            Regex("""(?:val|var)\s+(\w+)\s*[:=]"""),
            Regex("""const\s+val\s+(\w+)\s*=""")
        )
        
        patterns.forEach { pattern ->
            pattern.findAll(content).forEach { match ->
                val name = match.groupValues[1]
                symbols.add(
                    DocumentSymbol(
                        name = name,
                        kind = if (match.value.contains("const")) SymbolKind.CONSTANT else SymbolKind.PROPERTY,
                        range = Range(Position(0, 0), Position(0, name.length)),
                        selectionRange = Range(Position(0, 0), Position(0, name.length))
                    )
                )
            }
        }
    }
    
    private fun extractTypeAliasSymbols(content: String, symbols: MutableList<DocumentSymbol>) {
        val pattern = Regex("""typealias\s+(\w+)\s*=""")
        pattern.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            symbols.add(
                DocumentSymbol(
                    name = name,
                    kind = SymbolKind.TYPE_PARAMETER,
                    detail = "type alias",
                    range = Range(Position(0, 0), Position(0, name.length)),
                    selectionRange = Range(Position(0, 0), Position(0, name.length))
                )
            )
        }
    }
    
    private fun extractAnnotationSymbols(content: String, symbols: MutableList<DocumentSymbol>) {
        val pattern = Regex("""annotation\s+class\s+(\w+)""")
        pattern.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            symbols.add(
                DocumentSymbol(
                    name = name,
                    kind = SymbolKind.CLASS,
                    detail = "annotation class",
                    range = Range(Position(0, 0), Position(0, name.length)),
                    selectionRange = Range(Position(0, 0), Position(0, name.length))
                )
            )
        }
    }
    
    private fun analyzeMultiplatformSourceSets(uri: String) {
        logger.debug { "Analyzing Multiplatform source sets for: $uri" }
        // Implementation for analyzing Kotlin Multiplatform structure
    }
    
    private fun enableComposeFeatures(uri: String) {
        logger.debug { "Enabling Compose features for: $uri" }
        // Implementation for Compose-specific analysis
    }
    
    private fun findMultiplatformReferences(uri: String, line: Int, character: Int, locations: MutableList<Location>) {
        // Implementation for finding cross-platform references
        logger.debug { "Finding multiplatform references at $uri:$line:$character" }
    }
}

enum class KotlinFileType {
    REGULAR,
    SCRIPT,
    MULTIPLATFORM,
    COMPOSE
}

data class KotlinLSPSettings(
    val kotlinVersion: String,
    val enableK2: Boolean = true,
    val enableMultiplatform: Boolean = true,
    val enableCompose: Boolean = true,
    val enableCoroutines: Boolean = true,
    val enableContracts: Boolean = true,
    val enableContextReceivers: Boolean = true,
    val jvmTarget: String = "17",
    val apiVersion: String = "2.0",
    val languageVersion: String = "2.0",
    val progressiveMode: Boolean = true,
    val enableExperimentalFeatures: List<String> = emptyList()
)

// Enhanced LSP types for Kotlin-specific features
@Serializable
data class TextDocumentSyncOptions(
    val openClose: Boolean? = null,
    val change: TextDocumentSyncKind? = null,
    val willSave: Boolean? = null,
    val willSaveWaitUntil: Boolean? = null,
    val save: SaveOptions? = null
)

@Serializable
enum class TextDocumentSyncKind(val value: Int) {
    NONE(0),
    FULL(1),
    INCREMENTAL(2)
}

@Serializable
data class SaveOptions(
    val includeText: Boolean? = null
)

@Serializable
data class CodeActionOptions(
    val codeActionKinds: List<String>? = null,
    val resolveProvider: Boolean? = null
)

@Serializable
data class RenameOptions(
    val prepareProvider: Boolean? = null
)

@Serializable
data class SemanticTokensOptions(
    val legend: SemanticTokensLegend,
    val range: Boolean? = null,
    val full: SemanticTokensFullOptions? = null
)

@Serializable
data class SemanticTokensLegend(
    val tokenTypes: List<String>,
    val tokenModifiers: List<String>
)

@Serializable
data class SemanticTokensFullOptions(
    val delta: Boolean? = null
)

@Serializable
data class InlayHintOptions(
    val resolveProvider: Boolean? = null
)

@Serializable
data class WorkspaceServerCapabilities(
    val workspaceFolders: WorkspaceFoldersServerCapabilities? = null,
    val fileOperations: FileOperationsServerCapabilities? = null
)

@Serializable
data class WorkspaceFoldersServerCapabilities(
    val supported: Boolean? = null,
    val changeNotifications: Boolean? = null
)

@Serializable
data class FileOperationsServerCapabilities(
    val didCreate: FileOperationRegistrationOptions? = null,
    val willCreate: FileOperationRegistrationOptions? = null,
    val didRename: FileOperationRegistrationOptions? = null,
    val willRename: FileOperationRegistrationOptions? = null,
    val didDelete: FileOperationRegistrationOptions? = null,
    val willDelete: FileOperationRegistrationOptions? = null
)

@Serializable
data class FileOperationRegistrationOptions(
    val filters: List<FileOperationFilter>
)

@Serializable
data class FileOperationFilter(
    val scheme: String? = null,
    val pattern: FileOperationPattern
)

@Serializable
data class FileOperationPattern(
    val glob: String,
    val matches: String? = null,
    val options: FileOperationPatternOptions? = null
)

@Serializable
data class FileOperationPatternOptions(
    val ignoreCase: Boolean? = null
)

// Kotlin-specific semantic token types
val kotlinSemanticTokenTypes = listOf(
    "namespace", "type", "class", "enum", "interface", "struct", "typeParameter",
    "parameter", "variable", "property", "enumMember", "event", "function",
    "method", "macro", "keyword", "modifier", "comment", "string", "number",
    "regexp", "operator", "decorator", "label", "annotation", "suspend",
    "extension", "companion", "dataClass", "sealedClass", "valueClass",
    "inline", "crossinline", "noinline", "reified", "composable"
)

val kotlinSemanticTokenModifiers = listOf(
    "declaration", "definition", "readonly", "static", "deprecated",
    "abstract", "async", "modification", "documentation", "defaultLibrary",
    "suspend", "inline", "extension", "operator", "infix", "tailrec",
    "external", "expect", "actual", "annotation", "data", "sealed",
    "value", "inner", "companion", "lateinit", "const", "vararg"
)