package com.aimatrix.amxlsp.memory

import com.aimatrix.amxlsp.agent.MemoriesManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

@Serializable
data class Memory(
    val id: String,
    val key: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class MemoryIndex(
    val memories: Map<String, Memory> = emptyMap(),
    val tagIndex: Map<String, Set<String>> = emptyMap(), // tag -> memory IDs
    val keyIndex: Map<String, String> = emptyMap() // key -> memory ID
)

/**
 * File-based memory manager for storing project knowledge
 */
class FileBasedMemoryManager(
    private val memoriesDir: Path
) : MemoriesManager {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val indexFile = memoriesDir / "index.json"
    private var memoryIndex: MemoryIndex
    
    init {
        memoriesDir.createDirectories()
        memoryIndex = loadIndex()
    }
    
    private fun loadIndex(): MemoryIndex {
        return if (indexFile.exists()) {
            try {
                val content = indexFile.readText()
                json.decodeFromString<MemoryIndex>(content)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to load memory index, creating new one" }
                MemoryIndex()
            }
        } else {
            MemoryIndex()
        }
    }
    
    private fun saveIndex() {
        try {
            val content = json.encodeToString(MemoryIndex.serializer(), memoryIndex)
            indexFile.writeText(content)
        } catch (e: Exception) {
            logger.error(e) { "Failed to save memory index" }
        }
    }
    
    override fun store(key: String, value: String) {
        val now = Instant.now().toEpochMilli()
        val existingMemoryId = memoryIndex.keyIndex[key]
        
        val memory = if (existingMemoryId != null) {
            // Update existing memory
            val existingMemory = memoryIndex.memories[existingMemoryId]!!
            existingMemory.copy(
                content = value,
                updatedAt = now
            )
        } else {
            // Create new memory
            Memory(
                id = UUID.randomUUID().toString(),
                key = key,
                content = value,
                createdAt = now,
                updatedAt = now
            )
        }
        
        // Update index
        val updatedMemories = memoryIndex.memories.toMutableMap()
        updatedMemories[memory.id] = memory
        
        val updatedKeyIndex = memoryIndex.keyIndex.toMutableMap()
        updatedKeyIndex[key] = memory.id
        
        memoryIndex = memoryIndex.copy(
            memories = updatedMemories,
            keyIndex = updatedKeyIndex
        )
        
        // Save memory to file
        val memoryFile = memoriesDir / "${memory.id}.json"
        val memoryContent = json.encodeToString(Memory.serializer(), memory)
        memoryFile.writeText(memoryContent)
        
        // Save index
        saveIndex()
        
        logger.debug { "Stored memory with key: $key" }
    }
    
    override fun retrieve(key: String): String? {
        val memoryId = memoryIndex.keyIndex[key] ?: return null
        val memory = memoryIndex.memories[memoryId] ?: return null
        return memory.content
    }
    
    override fun search(query: String): List<String> {
        val queryLower = query.lowercase()
        val results = mutableListOf<String>()
        
        memoryIndex.memories.values.forEach { memory ->
            val score = calculateRelevanceScore(memory, queryLower)
            if (score > 0) {
                results.add("${memory.key}: ${memory.content}")
            }
        }
        
        return results.sortedByDescending { it.length } // Simple ranking
    }
    
    fun storeWithTags(key: String, content: String, tags: List<String>, metadata: Map<String, String> = emptyMap()) {
        val now = Instant.now().toEpochMilli()
        val existingMemoryId = memoryIndex.keyIndex[key]
        
        val memory = if (existingMemoryId != null) {
            // Update existing memory
            val existingMemory = memoryIndex.memories[existingMemoryId]!!
            existingMemory.copy(
                content = content,
                tags = tags,
                metadata = metadata,
                updatedAt = now
            )
        } else {
            // Create new memory
            Memory(
                id = UUID.randomUUID().toString(),
                key = key,
                content = content,
                tags = tags,
                createdAt = now,
                updatedAt = now,
                metadata = metadata
            )
        }
        
        // Update indexes
        val updatedMemories = memoryIndex.memories.toMutableMap()
        updatedMemories[memory.id] = memory
        
        val updatedKeyIndex = memoryIndex.keyIndex.toMutableMap()
        updatedKeyIndex[key] = memory.id
        
        val updatedTagIndex = memoryIndex.tagIndex.toMutableMap()
        
        // Remove old tag associations if updating existing memory
        if (existingMemoryId != null) {
            val oldMemory = memoryIndex.memories[existingMemoryId]!!
            oldMemory.tags.forEach { tag ->
                val memoryIds = updatedTagIndex[tag]?.toMutableSet() ?: mutableSetOf()
                memoryIds.remove(memory.id)
                if (memoryIds.isEmpty()) {
                    updatedTagIndex.remove(tag)
                } else {
                    updatedTagIndex[tag] = memoryIds
                }
            }
        }
        
        // Add new tag associations
        tags.forEach { tag ->
            val memoryIds = updatedTagIndex[tag]?.toMutableSet() ?: mutableSetOf()
            memoryIds.add(memory.id)
            updatedTagIndex[tag] = memoryIds
        }
        
        memoryIndex = memoryIndex.copy(
            memories = updatedMemories,
            keyIndex = updatedKeyIndex,
            tagIndex = updatedTagIndex
        )
        
        // Save memory to file
        val memoryFile = memoriesDir / "${memory.id}.json"
        val memoryContent = json.encodeToString(Memory.serializer(), memory)
        memoryFile.writeText(memoryContent)
        
        // Save index
        saveIndex()
        
        logger.debug { "Stored memory with key: $key, tags: $tags" }
    }
    
    fun findByTag(tag: String): List<Memory> {
        val memoryIds = memoryIndex.tagIndex[tag] ?: return emptyList()
        return memoryIds.mapNotNull { memoryIndex.memories[it] }
    }
    
    fun findByTags(tags: List<String>): List<Memory> {
        if (tags.isEmpty()) return emptyList()
        
        val memoryIdSets = tags.mapNotNull { memoryIndex.tagIndex[it] }
        if (memoryIdSets.isEmpty()) return emptyList()
        
        // Find intersection of all tag sets
        val commonMemoryIds = memoryIdSets.reduce { acc, set -> acc.intersect(set) }
        return commonMemoryIds.mapNotNull { memoryIndex.memories[it] }
    }
    
    fun getAllMemories(): List<Memory> {
        return memoryIndex.memories.values.toList()
    }
    
    fun getAllTags(): List<String> {
        return memoryIndex.tagIndex.keys.toList().sorted()
    }
    
    fun deleteMemory(key: String): Boolean {
        val memoryId = memoryIndex.keyIndex[key] ?: return false
        val memory = memoryIndex.memories[memoryId] ?: return false
        
        // Update indexes
        val updatedMemories = memoryIndex.memories.toMutableMap()
        updatedMemories.remove(memoryId)
        
        val updatedKeyIndex = memoryIndex.keyIndex.toMutableMap()
        updatedKeyIndex.remove(key)
        
        val updatedTagIndex = memoryIndex.tagIndex.toMutableMap()
        memory.tags.forEach { tag ->
            val memoryIds = updatedTagIndex[tag]?.toMutableSet() ?: mutableSetOf()
            memoryIds.remove(memoryId)
            if (memoryIds.isEmpty()) {
                updatedTagIndex.remove(tag)
            } else {
                updatedTagIndex[tag] = memoryIds
            }
        }
        
        memoryIndex = memoryIndex.copy(
            memories = updatedMemories,
            keyIndex = updatedKeyIndex,
            tagIndex = updatedTagIndex
        )
        
        // Delete memory file
        val memoryFile = memoriesDir / "$memoryId.json"
        if (memoryFile.exists()) {
            memoryFile.deleteExisting()
        }
        
        // Save index
        saveIndex()
        
        logger.debug { "Deleted memory with key: $key" }
        return true
    }
    
    private fun calculateRelevanceScore(memory: Memory, queryLower: String): Int {
        var score = 0
        
        // Check key
        if (memory.key.lowercase().contains(queryLower)) {
            score += 10
        }
        
        // Check content
        if (memory.content.lowercase().contains(queryLower)) {
            score += 5
        }
        
        // Check tags
        memory.tags.forEach { tag ->
            if (tag.lowercase().contains(queryLower)) {
                score += 3
            }
        }
        
        // Check metadata
        memory.metadata.values.forEach { value ->
            if (value.lowercase().contains(queryLower)) {
                score += 1
            }
        }
        
        return score
    }
    
    fun getStatistics(): MemoryStatistics {
        return MemoryStatistics(
            totalMemories = memoryIndex.memories.size,
            totalTags = memoryIndex.tagIndex.size,
            memorySize = calculateTotalSize(),
            oldestMemory = memoryIndex.memories.values.minByOrNull { it.createdAt }?.createdAt,
            newestMemory = memoryIndex.memories.values.maxByOrNull { it.createdAt }?.createdAt
        )
    }
    
    private fun calculateTotalSize(): Long {
        return memoriesDir.toFile().walkTopDown()
            .filter { it.isFile && it.name.endsWith(".json") }
            .sumOf { it.length() }
    }
}

data class MemoryStatistics(
    val totalMemories: Int,
    val totalTags: Int,
    val memorySize: Long,
    val oldestMemory: Long?,
    val newestMemory: Long?
)