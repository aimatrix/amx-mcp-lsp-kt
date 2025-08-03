package com.aimatrix.amxlsp.project

import com.aimatrix.amxlsp.config.ProjectConfig
import kotlin.test.*
import java.nio.file.Files
import kotlin.io.path.*

@OptIn(kotlin.io.path.ExperimentalPathApi::class)
class ProjectTest {
    
    private fun createTempProjectDir(): String {
        val tempDir = Files.createTempDirectory("amxlsp-test-project")
        return tempDir.toString()
    }
    
    @AfterTest
    fun cleanup() {
        // Clean up any temp directories created during tests
        Files.walk(Files.createTempDirectory("").parent)
            .filter { it.fileName.toString().startsWith("amxlsp-test-project") }
            .sorted(Comparator.reverseOrder())
            .forEach { 
                try { 
                    Files.deleteIfExists(it) 
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
    }
    
    @Test
    fun testProjectCreation() {
        val tempDir = createTempProjectDir()
        val projectConfig = ProjectConfig(
            projectName = "test-project",
            rootPath = tempDir
        )
        val project = Project(
            projectRoot = tempDir,
            projectConfig = projectConfig
        )
        
        assertEquals("test-project", project.projectConfig.projectName)
        assertEquals(tempDir, project.projectRoot)
        assertFalse(project.isNewlyCreated)
        
        Path(tempDir).deleteRecursively()
    }
    
    @Test
    fun testProjectWithNewlyCreatedFlag() {
        val tempDir = createTempProjectDir()
        val projectConfig = ProjectConfig(
            projectName = "new-project",
            rootPath = tempDir
        )
        val project = Project(
            projectRoot = tempDir,
            projectConfig = projectConfig,
            isNewlyCreated = true
        )
        
        assertEquals("new-project", project.projectConfig.projectName)
        assertEquals(tempDir, project.projectRoot)
        assertTrue(project.isNewlyCreated)
        
        Path(tempDir).deleteRecursively()
    }
    
    @Test
    fun testProjectWithComplexPath() {
        val tempDir = createTempProjectDir()
        // Create a subdirectory with spaces in the name
        val complexPath = Path(tempDir).resolve("my complex project").resolve("v2.0")
        complexPath.createDirectories()
        
        val projectConfig = ProjectConfig(
            projectName = "complex-project",
            rootPath = complexPath.toString()
        )
        val project = Project(
            projectRoot = complexPath.toString(),
            projectConfig = projectConfig
        )
        
        assertEquals("complex-project", project.projectConfig.projectName)
        assertEquals(complexPath.toString(), project.projectRoot)
        
        Path(tempDir).deleteRecursively()
    }
    
    @Test
    fun testProjectConfigDetails() {
        val tempDir1 = createTempProjectDir()
        val tempDir2 = createTempProjectDir()
        
        val projectConfig = ProjectConfig(
            projectName = "detailed-project",
            rootPath = tempDir1
        )
        val project = Project(
            projectRoot = tempDir2,
            projectConfig = projectConfig
        )
        
        assertEquals("detailed-project", project.projectConfig.projectName)
        assertEquals(tempDir1, project.projectConfig.rootPath)
        assertEquals(tempDir2, project.projectRoot)
        
        Path(tempDir1).deleteRecursively()
        Path(tempDir2).deleteRecursively()
    }
    
    @Test
    fun testProjectEquality() {
        val tempDir1 = createTempProjectDir()
        val tempDir2 = createTempProjectDir()
        
        val config1 = ProjectConfig(
            projectName = "test",
            rootPath = tempDir1
        )
        val config2 = ProjectConfig(
            projectName = "test",
            rootPath = tempDir1
        )
        val config3 = ProjectConfig(
            projectName = "test",
            rootPath = tempDir2
        )
        
        val project1 = Project(tempDir1, config1)
        val project2 = Project(tempDir1, config2)
        val project3 = Project(tempDir2, config3)
        
        // Projects are equal if they have the same root and config
        assertEquals(project1.projectRoot, project2.projectRoot)
        assertEquals(project1.projectConfig.projectName, project2.projectConfig.projectName)
        assertNotEquals(project1.projectRoot, project3.projectRoot)
        
        Path(tempDir1).deleteRecursively()
        Path(tempDir2).deleteRecursively()
    }
    
    @Test
    fun testProjectWithEmptyProjectName() {
        val tempDir = createTempProjectDir()
        val projectConfig = ProjectConfig(
            projectName = "",
            rootPath = tempDir
        )
        val project = Project(
            projectRoot = tempDir,
            projectConfig = projectConfig
        )
        
        assertEquals("", project.projectConfig.projectName)
        assertEquals(tempDir, project.projectRoot)
        
        Path(tempDir).deleteRecursively()
    }
    
    @Test
    fun testProjectWithGitignore() {
        val tempDir = createTempProjectDir()
        val gitignoreFile = Path(tempDir).resolve(".gitignore")
        gitignoreFile.writeText("""
            *.class
            build/
            .gradle/
        """.trimIndent())
        
        val projectConfig = ProjectConfig(
            projectName = "gitignore-test",
            rootPath = tempDir,
            ignoreAllFilesInGitignore = true
        )
        val project = Project(
            projectRoot = tempDir,
            projectConfig = projectConfig
        )
        
        // Test that the project was created successfully with gitignore
        assertEquals("gitignore-test", project.projectConfig.projectName)
        assertTrue(project.projectConfig.ignoreAllFilesInGitignore)
        
        // Test that gitignore spec was created
        val ignoreSpec = project.getIgnoreSpec()
        assertNotNull(ignoreSpec)
        
        // Note: Actual gitignore pattern matching is complex and depends on PathSpec implementation
        // For now, we just verify the project can be created with gitignore enabled
        // and that it processes the .gitignore file without errors
        
        Path(tempDir).deleteRecursively()
    }
}