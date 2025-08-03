package com.aimatrix.amxlsp.memory

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.*

class FileBasedMemoryManagerTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var memoryManager: FileBasedMemoryManager
    
    @BeforeEach
    fun setUp() {
        memoryManager = FileBasedMemoryManager(tempDir / "memories")
    }
    
    @Test
    fun `store and retrieve simple memory`() {
        val key = "test-key"
        val content = "test content"
        
        memoryManager.store(key, content)
        val retrieved = memoryManager.retrieve(key)
        
        assertEquals(content, retrieved)
    }
    
    @Test
    fun `store and retrieve memory with tags`() {
        val key = "tagged-memory"
        val content = "memory with tags"
        val tags = listOf("tag1", "tag2", "important")
        val metadata = mapOf("author" to "test", "version" to "1.0")
        
        memoryManager.storeWithTags(key, content, tags, metadata)
        val retrieved = memoryManager.retrieve(key)
        
        assertEquals(content, retrieved)
        
        val tagResults = memoryManager.findByTag("tag1")
        assertEquals(1, tagResults.size)
        assertEquals(key, tagResults.first().key)
        assertEquals(tags, tagResults.first().tags)
        assertEquals(metadata, tagResults.first().metadata)
    }
    
    @Test
    fun `search memories by content`() {
        memoryManager.store("key1", "This is about Kotlin programming")
        memoryManager.store("key2", "This is about Python programming")
        memoryManager.store("key3", "This is about project management")
        
        val results = memoryManager.search("Kotlin")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.contains("Kotlin") })
        
        val programmingResults = memoryManager.search("programming")
        assertTrue(programmingResults.size >= 2)
    }
    
    @Test
    fun `find memories by multiple tags`() {
        memoryManager.storeWithTags("mem1", "content1", listOf("tag1", "tag2"))
        memoryManager.storeWithTags("mem2", "content2", listOf("tag1", "tag3"))
        memoryManager.storeWithTags("mem3", "content3", listOf("tag2", "tag3"))
        
        val tag1Results = memoryManager.findByTag("tag1")
        assertEquals(2, tag1Results.size)
        
        val multiTagResults = memoryManager.findByTags(listOf("tag1", "tag2"))
        assertEquals(1, multiTagResults.size)
        assertEquals("mem1", multiTagResults.first().key)
    }
    
    @Test
    fun `delete memory`() {
        val key = "to-delete"
        val content = "this will be deleted"
        val tags = listOf("temporary")
        
        memoryManager.storeWithTags(key, content, tags)
        assertNotNull(memoryManager.retrieve(key))
        
        val deleted = memoryManager.deleteMemory(key)
        assertTrue(deleted)
        assertNull(memoryManager.retrieve(key))
        
        val tagResults = memoryManager.findByTag("temporary")
        assertTrue(tagResults.isEmpty())
    }
    
    @Test
    fun `update existing memory`() {
        val key = "update-test"
        val originalContent = "original content"
        val updatedContent = "updated content"
        val originalTags = listOf("original")
        val updatedTags = listOf("updated", "modified")
        
        memoryManager.storeWithTags(key, originalContent, originalTags)
        val originalMemory = memoryManager.findByTag("original").first()
        
        memoryManager.storeWithTags(key, updatedContent, updatedTags)
        val retrieved = memoryManager.retrieve(key)
        
        assertEquals(updatedContent, retrieved)
        
        val updatedMemory = memoryManager.findByTag("updated").first()
        assertEquals(key, updatedMemory.key)
        assertEquals(updatedTags, updatedMemory.tags)
        assertEquals(originalMemory.id, updatedMemory.id) // Same ID, updated content
        assertTrue(updatedMemory.updatedAt > originalMemory.createdAt)
        
        // Original tag should no longer exist
        assertTrue(memoryManager.findByTag("original").isEmpty())
    }
    
    @Test
    fun `get all tags`() {
        memoryManager.storeWithTags("mem1", "content1", listOf("tag1", "tag2"))
        memoryManager.storeWithTags("mem2", "content2", listOf("tag2", "tag3"))
        memoryManager.storeWithTags("mem3", "content3", listOf("tag4"))
        
        val allTags = memoryManager.getAllTags()
        assertEquals(4, allTags.size)
        assertTrue(allTags.containsAll(listOf("tag1", "tag2", "tag3", "tag4")))
    }
    
    @Test
    fun `get memory statistics`() {
        memoryManager.storeWithTags("mem1", "content1", listOf("tag1"))
        memoryManager.storeWithTags("mem2", "content2", listOf("tag2"))
        memoryManager.storeWithTags("mem3", "content3", listOf("tag1", "tag3"))
        
        val stats = memoryManager.getStatistics()
        assertEquals(3, stats.totalMemories)
        assertEquals(3, stats.totalTags)
        assertTrue(stats.memorySize > 0)
        assertNotNull(stats.oldestMemory)
        assertNotNull(stats.newestMemory)
    }
    
    @Test
    fun `handle missing memory gracefully`() {
        val retrieved = memoryManager.retrieve("non-existent")
        assertNull(retrieved)
        
        val deleted = memoryManager.deleteMemory("non-existent")
        assertFalse(deleted)
        
        val searchResults = memoryManager.search("non-existent")
        assertTrue(searchResults.isEmpty())
    }
    
    @Test
    fun `persist across manager instances`() {
        val key = "persistent-test"
        val content = "this should persist"
        val tags = listOf("persistent", "test")
        
        memoryManager.storeWithTags(key, content, tags)
        
        // Create new manager instance with same directory
        val newManager = FileBasedMemoryManager(tempDir / "memories")
        val retrieved = newManager.retrieve(key)
        
        assertEquals(content, retrieved)
        
        val tagResults = newManager.findByTag("persistent")
        assertEquals(1, tagResults.size)
        assertEquals(key, tagResults.first().key)
    }
}