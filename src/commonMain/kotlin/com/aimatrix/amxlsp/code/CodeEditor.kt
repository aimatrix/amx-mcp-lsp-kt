package com.aimatrix.amxlsp.code

import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.amxlsp.project.Project
import com.aimatrix.amxlsp.symbol.LanguageServerSymbolRetriever

// Code editor interface stubs
interface CodeEditor {
    fun openFile(path: String)
    fun saveFile(path: String, content: String)
}

class JetBrainsCodeEditor(
    private val project: Project,
    private val agent: AmxLspAgent
) : CodeEditor {
    override fun openFile(path: String) {
        // Stub implementation
    }
    
    override fun saveFile(path: String, content: String) {
        // Stub implementation
    }
}

class LanguageServerCodeEditor(
    private val symbolRetriever: LanguageServerSymbolRetriever,
    private val agent: AmxLspAgent
) : CodeEditor {
    override fun openFile(path: String) {
        // Stub implementation
    }
    
    override fun saveFile(path: String, content: String) {
        // Stub implementation
    }
}