package com.aimatrix.amxlsp.text

object TextProcessor {
    fun splitIntoChunks(text: String, chunkSize: Int): List<String> {
        if (text.length <= chunkSize) return listOf(text)
        
        val chunks = mutableListOf<String>()
        var currentIndex = 0
        
        while (currentIndex < text.length) {
            val endIndex = minOf(currentIndex + chunkSize, text.length)
            chunks.add(text.substring(currentIndex, endIndex))
            currentIndex = endIndex
        }
        
        return chunks
    }
    
    fun cleanText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[\\r\\n]+"), "\n")
    }
}

// Missing data class and function stubs for Project.kt
data class MatchedConsecutiveLines(
    val lines: List<String>,
    val startLine: Int,
    val file: String
)

fun searchFiles(
    rootPath: String,
    query: String,
    maxResults: Int = 100,
    ignoreCase: Boolean = true,
    useRegex: Boolean = false,
    ignoreSpec: Any? = null
): List<MatchedConsecutiveLines> {
    // Simplified implementation - in a real project this would use proper file search
    return emptyList()
}