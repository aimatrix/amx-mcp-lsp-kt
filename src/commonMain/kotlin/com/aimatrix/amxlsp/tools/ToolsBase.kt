package com.aimatrix.amxlsp.tools

import com.aimatrix.amxlsp.agent.LinesRead
import com.aimatrix.amxlsp.agent.MemoriesManager
import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.amxlsp.code.CodeEditor
import com.aimatrix.amxlsp.code.JetBrainsCodeEditor
import com.aimatrix.amxlsp.code.LanguageServerCodeEditor
import com.aimatrix.amxlsp.project.Project
import com.aimatrix.amxlsp.prompt.PromptFactory
import com.aimatrix.amxlsp.symbol.LanguageServerSymbolRetriever
import com.aimatrix.solidlsp.exceptions.SolidLSPException
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions

private val logger = KotlinLogging.logger {}

const val SUCCESS_RESULT = "OK"
const val TOOL_DEFAULT_MAX_ANSWER_LENGTH = 200_000

// Component base class
abstract class Component(val agent: AmxLspAgent) {
    
    fun getProjectRoot(): String {
        return agent.getProjectRoot()
    }
    
    val promptFactory: PromptFactory
        get() = agent.promptFactory
    
    val memoriesManager: MemoriesManager
        get() = agent.memoriesManager ?: throw IllegalStateException("Memories manager not initialized")
    
    fun createLanguageServerSymbolRetriever(): LanguageServerSymbolRetriever {
        if (!agent.isUsingLanguageServer()) {
            throw Exception("Cannot create LanguageServerSymbolRetriever; agent is not in language server mode.")
        }
        val languageServer = agent.languageServer
            ?: throw IllegalStateException("Language server not initialized")
        return LanguageServerSymbolRetriever(languageServer, agent)
    }
    
    val project: Project
        get() = agent.getActiveProjectOrRaise()
    
    fun createCodeEditor(): CodeEditor {
        return if (agent.isUsingLanguageServer()) {
            LanguageServerCodeEditor(createLanguageServerSymbolRetriever(), agent)
        } else {
            JetBrainsCodeEditor(project, agent)
        }
    }
    
    val linesRead: LinesRead
        get() = agent.linesRead ?: throw IllegalStateException("Lines read not initialized")
}

// Tool marker interfaces
interface ToolMarker

interface ToolMarkerCanEdit : ToolMarker

interface ToolMarkerDoesNotRequireActiveProject : ToolMarker

interface ToolMarkerOptional : ToolMarker

interface ToolMarkerSymbolicRead : ToolMarker

interface ToolMarkerSymbolicEdit : ToolMarkerCanEdit

// Tool metadata data class
data class ToolMetadata(
    val name: String,
    val description: String,
    val parameters: Map<String, ParameterMetadata>
)

data class ParameterMetadata(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true,
    val defaultValue: Any? = null
)

// Main Tool class
abstract class Tool(agent: AmxLspAgent) : Component(agent) {
    
    companion object {
        fun getNameFromClass(cls: KClass<out Tool>): String {
            val name = cls.simpleName ?: throw IllegalArgumentException("Tool class must have a name")
            return name.removeSuffix("Tool")
                .replace(Regex("([A-Z])")) { "_${it.value.lowercase()}" }
                .removePrefix("_")
        }
        
        fun canEdit(cls: KClass<out Tool>): Boolean {
            return cls.isSubclassOf(ToolMarkerCanEdit::class)
        }
        
        fun getToolDescription(cls: KClass<out Tool>): String {
            val annotation = cls.findAnnotation<ToolDescription>()
            return annotation?.value?.trim() ?: ""
        }
        
        fun requiresActiveProject(cls: KClass<out Tool>): Boolean {
            return !cls.isSubclassOf(ToolMarkerDoesNotRequireActiveProject::class)
        }
        
        fun isOptional(cls: KClass<out Tool>): Boolean {
            return cls.isSubclassOf(ToolMarkerOptional::class)
        }
    }
    
    fun getName(): String = getNameFromClass(this::class)
    
    fun canEdit(): Boolean = canEdit(this::class)
    
    abstract fun apply(vararg args: Any, kwargs: Map<String, Any> = emptyMap()): String
    
    fun getToolDescription(): String = getToolDescription(this::class)
    
    fun isActive(): Boolean = agent.toolIsActive(this::class)
    
    private fun logToolApplication(params: Map<String, Any>) {
        logger.info { "${getName()}: $params" }
    }
    
    private fun limitLength(result: String, maxAnswerChars: Int): String {
        val nChars = result.length
        return if (nChars > maxAnswerChars) {
            "The answer is too long ($nChars characters). " +
            "Please try a more specific tool query or raise the max_answer_chars parameter."
        } else {
            result
        }
    }
    
    fun applyEx(
        logCall: Boolean = true,
        catchExceptions: Boolean = true,
        kwargs: Map<String, Any> = emptyMap()
    ): String {
        fun task(): String {
            try {
                if (!isActive()) {
                    return "Error: Tool '${getName()}' is not active. Active tools: ${agent.getActiveToolNames()}"
                }
            } catch (e: Exception) {
                return "RuntimeError while checking if tool ${getName()} is active: $e"
            }
            
            if (logCall) {
                logToolApplication(kwargs)
            }
            
            try {
                // Check whether the tool requires an active project
                if (!this::class.isSubclassOf(ToolMarkerDoesNotRequireActiveProject::class)) {
                    if (agent.activeProject == null) {
                        return "No active project. Use ActivateProject to select or create a project first."
                    }
                    
                    if (agent.isUsingLanguageServer() && agent.languageServer?.isReady() != true) {
                        return "Language server is not ready. Cannot execute tool ${getName()}"
                    }
                }
                
                // Execute the tool's apply method
                val result = apply(kwargs = kwargs)
                
                // Limit the result length if needed
                val maxChars = kwargs["max_answer_chars"] as? Int ?: TOOL_DEFAULT_MAX_ANSWER_LENGTH
                return limitLength(result, maxChars)
                
            } catch (e: SolidLSPException) {
                logger.error(e) { "SolidLSPException in tool ${getName()}" }
                return "LSP Error: ${e.message}"
            } catch (e: Exception) {
                if (!catchExceptions) throw e
                logger.error(e) { "Exception in tool ${getName()}" }
                return "Error: ${e.message ?: e::class.simpleName}"
            }
        }
        
        return task()
    }
}

// Annotation for tool descriptions
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ToolDescription(val value: String)

