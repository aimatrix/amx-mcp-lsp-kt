package com.aimatrix.amxlsp.util

import com.aimatrix.amxlsp.project.PathSpec
import kotlin.test.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.*

class FileSystemTest {
    
    @Test
    fun testMatchPath() {
        assertTrue(matchPath("/home/user/project/src/main.kt", "main"))
        assertTrue(matchPath("/project/test.java", "java"))
        assertFalse(matchPath("/project/main.kt", "python"))
        assertTrue(matchPath("/PROJECT/Main.KT", "main")) // Case insensitive
    }
    
    @Test
    fun testPathSpecMatching() {
        val spec = object : PathSpec {
            override fun matches(path: String): Boolean {
                return path.endsWith(".kt") || path.endsWith(".java")
            }
        }
        
        assertTrue(matchPath("/src/Main.kt", spec, "/project"))
        assertTrue(matchPath("/src/Test.java", spec, "/project"))
        assertFalse(matchPath("/src/script.py", spec, "/project"))
    }
    
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @Test
    fun testGitignoreParserCreation() {
        val tempDir = Files.createTempDirectory("amxlsp-test")
        val parser = GitignoreParser(tempDir.toString())
        
        assertNotNull(parser)
        assertTrue(parser.getIgnoreSpecs().isEmpty()) // No .gitignore files initially
        
        tempDir.deleteRecursively()
    }
    
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @Test
    fun testGitignoreParserWithFile() {
        val tempDir = Files.createTempDirectory("amxlsp-test")
        val gitignoreFile = tempDir.resolve(".gitignore")
        
        gitignoreFile.writeText("""
            *.class
            *.jar
            build/
            # Comment line
            .gradle/
        """.trimIndent())
        
        val parser = GitignoreParser(tempDir.toString())
        val specs = parser.getIgnoreSpecs()
        
        assertEquals(1, specs.size)
        val spec = specs.first()
        assertEquals(gitignoreFile.toString(), spec.filePath)
        assertEquals(4, spec.patterns.size) // 4 non-comment, non-empty lines
        assertTrue(spec.patterns.contains("*.class"))
        assertTrue(spec.patterns.contains("build/"))
        assertFalse(spec.patterns.any { it.startsWith("#") })
        
        tempDir.deleteRecursively()
    }
    
    @Test
    fun testIgnoreSpecCreation() {
        val spec = IgnoreSpec(
            filePath = "/project/.gitignore",
            patterns = listOf("*.class", "build/", "*.jar")
        )
        
        assertEquals("/project/.gitignore", spec.filePath)
        assertEquals(3, spec.patterns.size)
        assertTrue(spec.patterns.contains("*.class"))
    }
}

class FileSystemUtilsTest {
    
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @Test
    fun testEnsureDirectoryCreation() {
        val tempDir = Files.createTempDirectory("amxlsp-test")
        val newDir = tempDir.resolve("new-directory")
        
        assertFalse(newDir.exists())
        FileSystemUtils.ensureDirectory(newDir)
        assertTrue(newDir.exists())
        assertTrue(newDir.isDirectory())
        
        tempDir.deleteRecursively()
    }
    
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @Test
    fun testEnsureDirectoryExisting() {
        val tempDir = Files.createTempDirectory("amxlsp-test")
        
        assertTrue(tempDir.exists())
        // Should not throw when directory already exists
        FileSystemUtils.ensureDirectory(tempDir)
        assertTrue(tempDir.exists())
        
        tempDir.deleteRecursively()
    }
    
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @Test
    fun testEnsureDirectoryWithFile() {
        val tempDir = Files.createTempDirectory("amxlsp-test")
        val file = tempDir.resolve("regular-file.txt")
        file.writeText("test content")
        
        assertTrue(file.exists())
        assertTrue(file.isRegularFile())
        
        assertFailsWith<IllegalArgumentException> {
            FileSystemUtils.ensureDirectory(file)
        }
        
        tempDir.deleteRecursively()
    }
    
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @Test
    fun testCopyDirectory() {
        val sourceDir = Files.createTempDirectory("amxlsp-source")
        val targetDir = Files.createTempDirectory("amxlsp-target")
        
        // Create some test files in source
        sourceDir.resolve("file1.txt").writeText("content1")
        sourceDir.resolve("subdir").createDirectories()
        sourceDir.resolve("subdir").resolve("file2.txt").writeText("content2")
        
        FileSystemUtils.copyDirectory(sourceDir, targetDir)
        
        assertTrue(targetDir.resolve("file1.txt").exists())
        assertTrue(targetDir.resolve("subdir").exists())
        assertTrue(targetDir.resolve("subdir").resolve("file2.txt").exists())
        assertEquals("content1", targetDir.resolve("file1.txt").readText())
        assertEquals("content2", targetDir.resolve("subdir").resolve("file2.txt").readText())
        
        sourceDir.deleteRecursively()
        targetDir.deleteRecursively()
    }
    
    @Test
    fun testDeleteRecursively() {
        val tempDir = Files.createTempDirectory("amxlsp-test")
        tempDir.resolve("file.txt").writeText("test")
        tempDir.resolve("subdir").createDirectories()
        tempDir.resolve("subdir").resolve("nested.txt").writeText("nested")
        
        assertTrue(tempDir.exists())
        assertTrue(tempDir.resolve("file.txt").exists())
        assertTrue(tempDir.resolve("subdir").resolve("nested.txt").exists())
        
        FileSystemUtils.deleteRecursively(tempDir)
        
        assertFalse(tempDir.exists())
    }
    
    @Test
    fun testGetRelativePath() {
        val basePath = Paths.get("/home/user/projects")
        val targetPath = Paths.get("/home/user/projects/my-app/src/main.kt")
        
        val relativePath = FileSystemUtils.getRelativePath(basePath, targetPath)
        assertEquals("my-app/src/main.kt", relativePath.toString())
    }
    
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @Test
    fun testFindFiles() {
        val tempDir = Files.createTempDirectory("amxlsp-test")
        
        // Create test files
        tempDir.resolve("Main.kt").writeText("kotlin code")
        tempDir.resolve("Test.java").writeText("java code")
        tempDir.resolve("script.py").writeText("python code")
        tempDir.resolve("subdir").createDirectories()
        tempDir.resolve("subdir").resolve("Nested.kt").writeText("nested kotlin")
        
        val kotlinFiles = FileSystemUtils.findFiles(tempDir, "*.kt")
        assertEquals(2, kotlinFiles.size)
        assertTrue(kotlinFiles.any { it.fileName.toString() == "Main.kt" })
        assertTrue(kotlinFiles.any { it.fileName.toString() == "Nested.kt" })
        
        val javaFiles = FileSystemUtils.findFiles(tempDir, "*.java")
        assertEquals(1, javaFiles.size)
        assertTrue(javaFiles.any { it.fileName.toString() == "Test.java" })
        
        tempDir.deleteRecursively()
    }
    
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @Test
    fun testFindFilesWithMaxDepth() {
        val tempDir = Files.createTempDirectory("amxlsp-test")
        
        tempDir.resolve("root.kt").writeText("root")
        tempDir.resolve("level1").createDirectories()
        tempDir.resolve("level1").resolve("level1.kt").writeText("level1")
        tempDir.resolve("level1").resolve("level2").createDirectories()
        tempDir.resolve("level1").resolve("level2").resolve("level2.kt").writeText("level2")
        
        val allFiles = FileSystemUtils.findFiles(tempDir, "*.kt")
        assertEquals(3, allFiles.size)
        
        val limitedFiles = FileSystemUtils.findFiles(tempDir, "*.kt", maxDepth = 2)
        assertEquals(2, limitedFiles.size) // Should exclude level2.kt
        
        tempDir.deleteRecursively()
    }
}