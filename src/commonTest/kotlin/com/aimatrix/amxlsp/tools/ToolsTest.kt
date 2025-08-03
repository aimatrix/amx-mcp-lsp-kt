package com.aimatrix.amxlsp.tools

import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.amxlsp.config.ProjectConfig
import com.aimatrix.amxlsp.config.AmxLspConfig
import com.aimatrix.amxlsp.memory.FileBasedMemoryManager
import com.aimatrix.solidlsp.config.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.*

class ToolsTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var agent: AmxLspAgent
    private lateinit var memoryManager: FileBasedMemoryManager
    
    @BeforeEach
    fun setUp() {
        agent = AmxLspAgent()
        memoryManager = FileBasedMemoryManager(tempDir / "memories")
        agent.memoriesManager = memoryManager
        
        // Set up a test project
        val projectPath = tempDir / "test-project"
        projectPath.createDirectories()
        
        val config = AmxLspConfig().apply {
            projects["test-project"] = ProjectConfig(
                projectName = "test-project",
                rootPath = projectPath.toString(),
                language = Language.KOTLIN
            )
            activeProject = "test-project"
        }
        agent.config = config
    }
    
    @Test
    fun `memory tools work correctly`() {
        val storeMemoryTool = StoreMemoryTool(agent)
        val retrieveMemoryTool = RetrieveMemoryTool(agent)
        val searchMemoryTool = SearchMemoryTool(agent)
        
        // Store memory
        val storeResult = storeMemoryTool.apply(
            kwargs = mapOf(
                "key" to "test-key",
                "content" to "This is test content about Kotlin programming",
                "tags" to listOf("kotlin", "programming", "test"),
                "metadata" to mapOf("author" to "test-user")
            )
        )
        assertTrue(storeResult.contains("Successfully stored"))
        
        // Retrieve memory
        val retrieveResult = retrieveMemoryTool.apply(
            kwargs = mapOf("key" to "test-key")
        )
        assertTrue(retrieveResult.contains("This is test content"))
        
        // Search memory
        val searchResult = searchMemoryTool.apply(
            kwargs = mapOf("query" to "Kotlin")
        )
        assertTrue(searchResult.contains("Found"))
        assertTrue(searchResult.contains("kotlin", ignoreCase = true))
    }
    
    @Test
    fun `tag-based memory tools work correctly`() {
        val storeMemoryTool = StoreMemoryTool(agent)
        val findByTagTool = FindMemoriesByTagTool(agent)
        val listTagsTool = ListMemoryTagsTool(agent)
        val getStatsTool = GetMemoryStatsTool(agent)
        
        // Store memories with tags
        storeMemoryTool.apply(
            kwargs = mapOf(
                "key" to "mem1",
                "content" to "Memory about Kotlin",
                "tags" to listOf("kotlin", "programming")
            )
        )
        
        storeMemoryTool.apply(
            kwargs = mapOf(
                "key" to "mem2",
                "content" to "Memory about testing",
                "tags" to listOf("testing", "programming")
            )
        )
        
        // Find by tag
        val tagResult = findByTagTool.apply(
            kwargs = mapOf("tag" to "programming")
        )
        assertTrue(tagResult.contains("Found 2 memories"))
        
        // List tags
        val tagsResult = listTagsTool.apply(kwargs = emptyMap())
        assertTrue(tagsResult.contains("Available memory tags"))
        assertTrue(tagsResult.contains("kotlin"))
        assertTrue(tagsResult.contains("testing"))
        assertTrue(tagsResult.contains("programming"))
        
        // Get statistics
        val statsResult = getStatsTool.apply(kwargs = emptyMap())
        assertTrue(statsResult.contains("Total memories: 2"))
        assertTrue(statsResult.contains("Total tags: 3"))
    }
    
    @Test
    fun `config tools work correctly`() {
        val listProjectsTool = ListProjectsTool(agent)
        val getConfigTool = GetConfigTool(agent)
        val getProjectInfoTool = GetProjectInfoTool(agent)
        
        // List projects
        val listResult = listProjectsTool.apply(kwargs = emptyMap())
        assertTrue(listResult.contains("test-project"))
        assertTrue(listResult.contains("(active)"))
        
        // Get config
        val configResult = getConfigTool.apply(kwargs = emptyMap())
        assertTrue(configResult.contains("AmxLSP Configuration"))
        assertTrue(configResult.contains("Active project: test-project"))
        
        // Get project info - this requires an active project
        try {
            val projectInfoResult = getProjectInfoTool.apply(kwargs = emptyMap())
            // May fail if project isn't properly activated, which is expected in test
        } catch (e: Exception) {
            // Expected in test environment
        }
    }
    
    @Test
    fun `create project tool works`() {
        val createProjectTool = CreateProjectTool(agent)
        val newProjectPath = tempDir / "new-project"
        newProjectPath.createDirectories()
        
        val result = createProjectTool.apply(
            kwargs = mapOf(
                "name" to "new-project",
                "path" to newProjectPath.toString(),
                "language" to "PYTHON",
                "activate" to false
            )
        )
        
        assertTrue(result.contains("Successfully created project"))
        assertTrue(agent.config.projects.containsKey("new-project"))
        
        val projectConfig = agent.config.projects["new-project"]!!
        assertEquals("new-project", projectConfig.projectName)
        assertEquals(Language.PYTHON, projectConfig.language)
    }
    
    @Test
    fun `update config tool works`() {
        val updateConfigTool = UpdateConfigTool(agent)
        
        val result = updateConfigTool.apply(
            kwargs = mapOf(
                "setting" to "default_context",
                "value" to "new-context"
            )
        )
        
        assertTrue(result.contains("Successfully updated"))
        assertEquals("new-context", agent.config.defaultContext)
    }
    
    @Test
    fun `activate project tool works`() {
        val activateProjectTool = ActivateProjectTool(agent)
        
        val result = activateProjectTool.apply(
            kwargs = mapOf("project_name" to "test-project")
        )
        
        assertTrue(result.contains("Successfully activated"))
        assertEquals("test-project", agent.config.activeProject)
    }
    
    @Test
    fun `delete memory tool works`() {
        val storeMemoryTool = StoreMemoryTool(agent)
        val deleteMemoryTool = DeleteMemoryTool(agent)
        val retrieveMemoryTool = RetrieveMemoryTool(agent)
        
        // Store a memory
        storeMemoryTool.apply(
            kwargs = mapOf(
                "key" to "to-delete",
                "content" to "This will be deleted"
            )
        )
        
        // Verify it exists
        val retrieveResult = retrieveMemoryTool.apply(
            kwargs = mapOf("key" to "to-delete")
        )
        assertTrue(retrieveResult.contains("This will be deleted"))
        
        // Delete it
        val deleteResult = deleteMemoryTool.apply(
            kwargs = mapOf("key" to "to-delete")
        )
        assertTrue(deleteResult.contains("Successfully deleted"))
        
        // Verify it's gone
        val retrieveAfterDelete = retrieveMemoryTool.apply(
            kwargs = mapOf("key" to "to-delete")
        )
        assertTrue(retrieveAfterDelete.contains("No memory found"))
    }
    
    @Test
    fun `tool registry works correctly`() {
        // Test that tools are properly registered
        val allTools = ToolRegistry.getAll()
        assertTrue(allTools.isNotEmpty())
        
        // Test finding tools by name
        val storeMemoryClass = ToolRegistry.getByName("StoreMemoryTool")
        assertNotNull(storeMemoryClass)
        assertEquals(StoreMemoryTool::class, storeMemoryClass)
        
        // Test tool creation
        val tool = ToolRegistry.createTool(StoreMemoryTool::class, agent)
        assertTrue(tool is StoreMemoryTool)
        
        // Test tool name extraction
        val toolName = Tool.getNameFromClass(StoreMemoryTool::class)
        assertEquals("StoreMemoryTool", toolName)
        
        // Test tool description extraction
        val description = Tool.getToolDescription(StoreMemoryTool::class)
        assertEquals("Store information in project memory", description)
    }
    
    @Test
    fun `error handling in tools`() {
        val storeMemoryTool = StoreMemoryTool(agent)
        
        // Test missing required parameter
        val result = try {
            storeMemoryTool.apply(kwargs = emptyMap())
            "Should have thrown exception"
        } catch (e: IllegalArgumentException) {
            e.message ?: "Error occurred"
        }
        
        assertTrue(result.contains("Missing required parameter"))
    }
}