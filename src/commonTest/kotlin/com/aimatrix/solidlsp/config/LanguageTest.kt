package com.aimatrix.solidlsp.config

import kotlin.test.*

class LanguageTest {
    
    @Test
    fun testAllLanguagesHaveValidProperties() {
        val languages = Language.entries
        
        assertTrue(languages.isNotEmpty())
        
        languages.forEach { language ->
            assertNotNull(language.languageId)
            assertTrue(language.languageId.isNotBlank())
            assertNotNull(language.fileExtensions)
            assertTrue(language.fileExtensions.isNotEmpty())
            
            // Each extension should start with a dot
            language.fileExtensions.forEach { ext ->
                assertTrue(ext.startsWith("."), "Extension $ext should start with a dot")
            }
        }
    }
    
    @Test
    fun testKotlinLanguage() {
        assertEquals("kotlin", Language.KOTLIN.languageId)
        assertTrue(Language.KOTLIN.fileExtensions.contains(".kt"))
        assertTrue(Language.KOTLIN.fileExtensions.contains(".kts"))
    }
    
    @Test
    fun testJavaLanguage() {
        assertEquals("java", Language.JAVA.languageId)
        assertTrue(Language.JAVA.fileExtensions.contains(".java"))
    }
    
    @Test
    fun testPythonLanguage() {
        assertEquals("python", Language.PYTHON.languageId)
        assertTrue(Language.PYTHON.fileExtensions.contains(".py"))
        assertTrue(Language.PYTHON.fileExtensions.contains(".pyi"))
    }
    
    @Test
    fun testTypeScriptLanguage() {
        assertEquals("typescript", Language.TYPESCRIPT.languageId)
        assertTrue(Language.TYPESCRIPT.fileExtensions.contains(".ts"))
        assertTrue(Language.TYPESCRIPT.fileExtensions.contains(".tsx"))
    }
    
    @Test
    fun testJavaScriptLanguage() {
        assertEquals("javascript", Language.JAVASCRIPT.languageId)
        assertTrue(Language.JAVASCRIPT.fileExtensions.contains(".js"))
        assertTrue(Language.JAVASCRIPT.fileExtensions.contains(".jsx"))
    }
    
    @Test
    fun testRustLanguage() {
        assertEquals("rust", Language.RUST.languageId)
        assertTrue(Language.RUST.fileExtensions.contains(".rs"))
    }
    
    @Test
    fun testGoLanguage() {
        assertEquals("go", Language.GO.languageId)
        assertTrue(Language.GO.fileExtensions.contains(".go"))
    }
    
    @Test
    fun testCSharpLanguage() {
        assertEquals("csharp", Language.CSHARP.languageId)
        assertTrue(Language.CSHARP.fileExtensions.contains(".cs"))
    }
    
    @Test
    fun testPHPLanguage() {
        assertEquals("php", Language.PHP.languageId)
        assertTrue(Language.PHP.fileExtensions.contains(".php"))
    }
    
    @Test
    fun testRubyLanguage() {
        assertEquals("ruby", Language.RUBY.languageId)
        assertTrue(Language.RUBY.fileExtensions.contains(".rb"))
    }
    
    @Test
    fun testDartLanguage() {
        assertEquals("dart", Language.DART.languageId)
        assertTrue(Language.DART.fileExtensions.contains(".dart"))
    }
    
    @Test
    fun testElixirLanguage() {
        assertEquals("elixir", Language.ELIXIR.languageId)
        assertTrue(Language.ELIXIR.fileExtensions.contains(".ex"))
        assertTrue(Language.ELIXIR.fileExtensions.contains(".exs"))
    }
    
    @Test
    fun testClojureLanguage() {
        assertEquals("clojure", Language.CLOJURE.languageId)
        assertTrue(Language.CLOJURE.fileExtensions.contains(".clj"))
        assertTrue(Language.CLOJURE.fileExtensions.contains(".cljs"))
    }
    
    @Test
    fun testTerraformLanguage() {
        assertEquals("terraform", Language.TERRAFORM.languageId)
        assertTrue(Language.TERRAFORM.fileExtensions.contains(".tf"))
    }
    
    @Test
    fun testLanguageFromExtension() {
        // Test that we can identify languages by file extensions
        val kotlinFiles = listOf("Main.kt", "script.kts", "build.gradle.kts")
        val javaFiles = listOf("Main.java", "Test.java")
        val pythonFiles = listOf("script.py", "module.pyi")
        
        kotlinFiles.forEach { filename ->
            val ext = filename.substringAfterLast(".")
            assertTrue(Language.KOTLIN.fileExtensions.any { it == ".$ext" })
        }
        
        javaFiles.forEach { filename ->
            val ext = filename.substringAfterLast(".")
            assertTrue(Language.JAVA.fileExtensions.any { it == ".$ext" })
        }
        
        pythonFiles.forEach { filename ->
            val ext = filename.substringAfterLast(".")
            assertTrue(Language.PYTHON.fileExtensions.any { it == ".$ext" })
        }
    }
    
    @Test
    fun testUniqueLanguageIds() {
        val languages = Language.entries
        val languageIds = languages.map { it.languageId }
        val uniqueIds = languageIds.toSet()
        
        assertEquals(languageIds.size, uniqueIds.size, "All language IDs should be unique")
    }
    
    @Test
    fun testExtensionOverlaps() {
        val languages = Language.entries
        val allExtensions = mutableListOf<String>()
        
        languages.forEach { language ->
            allExtensions.addAll(language.fileExtensions)
        }
        
        val uniqueExtensions = allExtensions.toSet()
        
        // Some extensions might legitimately overlap (e.g., .js for both JS modules and Node.js)
        // But we should ensure the main extensions are properly distributed
        assertTrue(uniqueExtensions.contains(".kt"))
        assertTrue(uniqueExtensions.contains(".java"))
        assertTrue(uniqueExtensions.contains(".py"))
        assertTrue(uniqueExtensions.contains(".ts"))
        assertTrue(uniqueExtensions.contains(".rs"))
        assertTrue(uniqueExtensions.contains(".go"))
    }
    
    @Test
    fun testLanguageEnumValues() {
        val expectedLanguages = setOf(
            "KOTLIN", "JAVA", "PYTHON", "TYPESCRIPT", "JAVASCRIPT",
            "RUST", "GO", "CSHARP", "PHP", "RUBY", "DART",
            "ELIXIR", "CLOJURE", "TERRAFORM"
        )
        
        val actualLanguages = Language.entries.map { it.name }.toSet()
        
        assertEquals(expectedLanguages, actualLanguages)
    }
}