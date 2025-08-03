package com.aimatrix.amxlsp.symbol

import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.solidlsp.SolidLanguageServer

// Symbol retriever stub
class LanguageServerSymbolRetriever(
    private val languageServer: SolidLanguageServer,
    private val agent: AmxLspAgent
) {
    fun findSymbol(name: String): List<String> {
        // Stub implementation
        return emptyList()
    }
    
    fun getSymbols(): List<String> {
        // Stub implementation
        return emptyList()
    }
}