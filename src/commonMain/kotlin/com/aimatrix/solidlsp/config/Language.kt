package com.aimatrix.solidlsp.config

enum class Language(
    val displayName: String,
    val fileExtensions: List<String>,
    val languageId: String // LSP language identifier
) {
    PYTHON("Python", listOf(".py", ".pyi"), "python"),
    KOTLIN("Kotlin", listOf(".kt", ".kts"), "kotlin"),
    JAVA("Java", listOf(".java"), "java"),
    TYPESCRIPT("TypeScript", listOf(".ts", ".tsx"), "typescript"),
    JAVASCRIPT("JavaScript", listOf(".js", ".jsx", ".mjs"), "javascript"),
    GO("Go", listOf(".go"), "go"),
    RUST("Rust", listOf(".rs"), "rust"),
    CSHARP("C#", listOf(".cs"), "csharp"),
    PHP("PHP", listOf(".php"), "php"),
    RUBY("Ruby", listOf(".rb"), "ruby"),
    ELIXIR("Elixir", listOf(".ex", ".exs"), "elixir"),
    CLOJURE("Clojure", listOf(".clj", ".cljs", ".cljc"), "clojure"),
    DART("Dart", listOf(".dart"), "dart"),
    TERRAFORM("Terraform", listOf(".tf", ".tfvars"), "terraform");
    
    fun getSourceFnMatcher(): FilenameMatcher {
        return FilenameMatcher(fileExtensions)
    }
}

class FilenameMatcher(private val extensions: List<String>) {
    fun isRelevantFilename(filename: String): Boolean {
        return extensions.any { ext ->
            filename.endsWith(ext, ignoreCase = true)
        }
    }
}

data class LanguageServerConfig(
    val language: Language,
    val workspaceRoot: String,
    val settings: Map<String, Any> = emptyMap()
)