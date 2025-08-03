package com.aimatrix.solidlsp.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

// Basic LSP types
@Serializable
data class Position(
    val line: Int,
    val character: Int
)

@Serializable
data class Range(
    val start: Position,
    val end: Position
)

@Serializable
data class Location(
    val uri: String,
    val range: Range
)

@Serializable
data class TextDocumentIdentifier(
    val uri: String
)

@Serializable
data class VersionedTextDocumentIdentifier(
    val uri: String,
    val version: Int?
)

@Serializable
data class TextDocumentItem(
    val uri: String,
    val languageId: String,
    val version: Int,
    val text: String
)

@Serializable
data class TextEdit(
    val range: Range,
    val newText: String
)

@Serializable
data class WorkspaceEdit(
    val changes: Map<String, List<TextEdit>>? = null,
    val documentChanges: List<TextDocumentEdit>? = null
)

@Serializable
data class TextDocumentEdit(
    val textDocument: VersionedTextDocumentIdentifier,
    val edits: List<TextEdit>
)

// Symbol kinds
@Serializable
enum class SymbolKind(val value: Int) {
    FILE(1),
    MODULE(2),
    NAMESPACE(3),
    PACKAGE(4),
    CLASS(5),
    METHOD(6),
    PROPERTY(7),
    FIELD(8),
    CONSTRUCTOR(9),
    ENUM(10),
    INTERFACE(11),
    FUNCTION(12),
    VARIABLE(13),
    CONSTANT(14),
    STRING(15),
    NUMBER(16),
    BOOLEAN(17),
    ARRAY(18),
    OBJECT(19),
    KEY(20),
    NULL(21),
    ENUM_MEMBER(22),
    STRUCT(23),
    EVENT(24),
    OPERATOR(25),
    TYPE_PARAMETER(26)
}

@Serializable
data class SymbolInformation(
    val name: String,
    val kind: SymbolKind,
    val location: Location,
    val containerName: String? = null,
    val deprecated: Boolean? = null
)

@Serializable
data class DocumentSymbol(
    val name: String,
    val detail: String? = null,
    val kind: SymbolKind,
    val deprecated: Boolean? = null,
    val range: Range,
    val selectionRange: Range,
    val children: List<DocumentSymbol>? = null
)

// Completion types
@Serializable
enum class CompletionItemKind(val value: Int) {
    TEXT(1),
    METHOD(2),
    FUNCTION(3),
    CONSTRUCTOR(4),
    FIELD(5),
    VARIABLE(6),
    CLASS(7),
    INTERFACE(8),
    MODULE(9),
    PROPERTY(10),
    UNIT(11),
    VALUE(12),
    ENUM(13),
    KEYWORD(14),
    SNIPPET(15),
    COLOR(16),
    FILE(17),
    REFERENCE(18),
    FOLDER(19),
    ENUM_MEMBER(20),
    CONSTANT(21),
    STRUCT(22),
    EVENT(23),
    OPERATOR(24),
    TYPE_PARAMETER(25)
}

@Serializable
data class CompletionItem(
    val label: String,
    val kind: CompletionItemKind? = null,
    val detail: String? = null,
    val documentation: String? = null,
    val sortText: String? = null,
    val filterText: String? = null,
    val insertText: String? = null,
    val textEdit: TextEdit? = null
)

@Serializable
data class CompletionList(
    val isIncomplete: Boolean,
    val items: List<CompletionItem>
)

// Hover types
@Serializable
data class Hover(
    val contents: String,
    val range: Range? = null
)

// Diagnostic types
@Serializable
enum class DiagnosticSeverity(val value: Int) {
    ERROR(1),
    WARNING(2),
    INFORMATION(3),
    HINT(4)
}

@Serializable
data class Diagnostic(
    val range: Range,
    val severity: DiagnosticSeverity? = null,
    val code: String? = null,
    val source: String? = null,
    val message: String,
    val relatedInformation: List<DiagnosticRelatedInformation>? = null
)

@Serializable
data class DiagnosticRelatedInformation(
    val location: Location,
    val message: String
)

// Workspace symbols
@Serializable
data class WorkspaceSymbol(
    val name: String,
    val kind: SymbolKind,
    val location: Location,
    val containerName: String? = null
)

// Code action types
@Serializable
data class Command(
    val title: String,
    val command: String,
    val arguments: List<String>? = null
)

@Serializable
data class CodeAction(
    val title: String,
    val kind: String? = null,
    val diagnostics: List<Diagnostic>? = null,
    val edit: WorkspaceEdit? = null,
    val command: Command? = null
)

// References
@Serializable
data class ReferenceContext(
    val includeDeclaration: Boolean
)

// Formatting
@Serializable
data class FormattingOptions(
    val tabSize: Int,
    val insertSpaces: Boolean,
    val trimTrailingWhitespace: Boolean? = null,
    val insertFinalNewline: Boolean? = null,
    val trimFinalNewlines: Boolean? = null
)

// Server capabilities
@Serializable
data class ServerCapabilities(
    val textDocumentSync: @Contextual Any? = null,
    val hoverProvider: Boolean? = null,
    val completionProvider: CompletionOptions? = null,
    val signatureHelpProvider: SignatureHelpOptions? = null,
    val definitionProvider: Boolean? = null,
    val typeDefinitionProvider: Boolean? = null,
    val implementationProvider: Boolean? = null,
    val referencesProvider: Boolean? = null,
    val documentHighlightProvider: Boolean? = null,
    val documentSymbolProvider: Boolean? = null,
    val workspaceSymbolProvider: Boolean? = null,
    val codeActionProvider: @Contextual Any? = null,
    val documentFormattingProvider: Boolean? = null,
    val documentRangeFormattingProvider: Boolean? = null,
    val renameProvider: @Contextual Any? = null,
    val foldingRangeProvider: Boolean? = null,
    val selectionRangeProvider: Boolean? = null,
    val semanticTokensProvider: @Contextual Any? = null,
    val inlayHintProvider: @Contextual Any? = null,
    val workspace: @Contextual Any? = null
)

@Serializable
data class CompletionOptions(
    val resolveProvider: Boolean? = null,
    val triggerCharacters: List<String>? = null
)

@Serializable
data class SignatureHelpOptions(
    val triggerCharacters: List<String>? = null
)

// Initialize types
@Serializable
data class ClientCapabilities(
    val workspace: WorkspaceClientCapabilities? = null,
    val textDocument: TextDocumentClientCapabilities? = null,
    val experimental: Map<String, @Contextual Any>? = null
)

@Serializable
data class WorkspaceClientCapabilities(
    val applyEdit: Boolean? = null,
    val workspaceEdit: WorkspaceEditClientCapabilities? = null,
    val didChangeConfiguration: DynamicRegistrationCapabilities? = null,
    val didChangeWatchedFiles: DynamicRegistrationCapabilities? = null,
    val symbol: WorkspaceSymbolClientCapabilities? = null
)

@Serializable
data class TextDocumentClientCapabilities(
    val synchronization: TextDocumentSyncClientCapabilities? = null,
    val completion: CompletionClientCapabilities? = null,
    val hover: HoverClientCapabilities? = null,
    val signatureHelp: SignatureHelpClientCapabilities? = null,
    val references: ReferencesClientCapabilities? = null,
    val documentHighlight: DocumentHighlightClientCapabilities? = null,
    val documentSymbol: DocumentSymbolClientCapabilities? = null,
    val formatting: DocumentFormattingClientCapabilities? = null,
    val rangeFormatting: DocumentRangeFormattingClientCapabilities? = null,
    val onTypeFormatting: DocumentOnTypeFormattingClientCapabilities? = null,
    val definition: DefinitionClientCapabilities? = null,
    val codeAction: CodeActionClientCapabilities? = null,
    val rename: RenameClientCapabilities? = null
)

@Serializable
data class DynamicRegistrationCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class WorkspaceEditClientCapabilities(
    val documentChanges: Boolean? = null
)

@Serializable
data class WorkspaceSymbolClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class TextDocumentSyncClientCapabilities(
    val dynamicRegistration: Boolean? = null,
    val willSave: Boolean? = null,
    val willSaveWaitUntil: Boolean? = null,
    val didSave: Boolean? = null
)

@Serializable
data class CompletionClientCapabilities(
    val dynamicRegistration: Boolean? = null,
    val completionItem: CompletionItemClientCapabilities? = null
)

@Serializable
data class CompletionItemClientCapabilities(
    val snippetSupport: Boolean? = null
)

@Serializable
data class HoverClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class SignatureHelpClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class ReferencesClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class DocumentHighlightClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class DocumentSymbolClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class DocumentFormattingClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class DocumentRangeFormattingClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class DocumentOnTypeFormattingClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class DefinitionClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class CodeActionClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

@Serializable
data class RenameClientCapabilities(
    val dynamicRegistration: Boolean? = null
)

// Initialize request/response types
@Serializable
data class InitializeParams(
    val processId: Int? = null,
    val clientInfo: ClientInfo? = null,
    val rootPath: String? = null,
    val rootUri: String? = null,
    val capabilities: ClientCapabilities,
    val initializationOptions: @Contextual Any? = null,
    val trace: String? = null,
    val workspaceFolders: List<WorkspaceFolder>? = null
)

@Serializable
data class ClientInfo(
    val name: String,
    val version: String? = null
)

@Serializable
data class WorkspaceFolder(
    val uri: String,
    val name: String
)

@Serializable
data class InitializeResult(
    val capabilities: ServerCapabilities,
    val serverInfo: ServerInfo? = null
)

@Serializable
data class ServerInfo(
    val name: String,
    val version: String? = null
)

// Additional capability types  
@Serializable
data class WorkspaceFoldersCapabilities(
    val workspaceFolders: Boolean? = null
)