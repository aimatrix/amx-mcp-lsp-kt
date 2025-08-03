package com.aimatrix.amxlsp.project

import com.aimatrix.amxlsp.config.ProjectConfig
import kotlin.test.*

class ProjectTest {
    
    @Test
    fun testProjectCreation() {
        val projectConfig = ProjectConfig(
            projectName = "test-project",
            rootPath = "/home/user/projects/test"
        )
        val project = Project(
            projectRoot = "/home/user/projects/test",
            projectConfig = projectConfig
        )
        
        assertEquals("test-project", project.projectConfig.projectName)
        assertEquals("/home/user/projects/test", project.projectRoot)
        assertFalse(project.isNewlyCreated)
    }
    
    @Test
    fun testProjectWithNewlyCreatedFlag() {
        val projectConfig = ProjectConfig(
            projectName = "new-project",
            rootPath = "/tmp/new-project"
        )
        val project = Project(
            projectRoot = "/tmp/new-project",
            projectConfig = projectConfig,
            isNewlyCreated = true
        )
        
        assertEquals("new-project", project.projectConfig.projectName)
        assertEquals("/tmp/new-project", project.projectRoot)
        assertTrue(project.isNewlyCreated)
    }
    
    @Test
    fun testProjectWithComplexPath() {
        val projectConfig = ProjectConfig(
            projectName = "complex-project",
            rootPath = "/home/user/projects/my complex project/v2.0"
        )
        val project = Project(
            projectRoot = "/home/user/projects/my complex project/v2.0",
            projectConfig = projectConfig
        )
        
        assertEquals("complex-project", project.projectConfig.projectName)
        assertEquals("/home/user/projects/my complex project/v2.0", project.projectRoot)
    }
    
    @Test
    fun testProjectConfigDetails() {
        val projectConfig = ProjectConfig(
            projectName = "detailed-project",
            rootPath = "/detailed/path"
        )
        val project = Project(
            projectRoot = "/actual/root",
            projectConfig = projectConfig
        )
        
        assertEquals("detailed-project", project.projectConfig.projectName)
        assertEquals("/detailed/path", project.projectConfig.rootPath)
        assertEquals("/actual/root", project.projectRoot)
    }
    
    @Test
    fun testProjectEquality() {
        val config1 = ProjectConfig(
            projectName = "test",
            rootPath = "/tmp/test"
        )
        val config2 = ProjectConfig(
            projectName = "test",
            rootPath = "/tmp/test"
        )
        val config3 = ProjectConfig(
            projectName = "test",
            rootPath = "/tmp/different"
        )
        
        val project1 = Project("/tmp/test", config1)
        val project2 = Project("/tmp/test", config2)
        val project3 = Project("/tmp/different", config3)
        
        // Projects are equal if they have the same root and config
        assertEquals(project1.projectRoot, project2.projectRoot)
        assertEquals(project1.projectConfig.projectName, project2.projectConfig.projectName)
        assertNotEquals(project1.projectRoot, project3.projectRoot)
    }
    
    @Test
    fun testProjectWithEmptyProjectName() {
        val projectConfig = ProjectConfig(
            projectName = "",
            rootPath = "/tmp/unnamed"
        )
        val project = Project(
            projectRoot = "/tmp/unnamed",
            projectConfig = projectConfig
        )
        
        assertEquals("", project.projectConfig.projectName)
        assertEquals("/tmp/unnamed", project.projectRoot)
    }
}