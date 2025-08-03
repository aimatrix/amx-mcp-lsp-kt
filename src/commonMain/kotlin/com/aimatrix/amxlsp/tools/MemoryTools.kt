package com.aimatrix.amxlsp.tools

import com.aimatrix.amxlsp.agent.AmxLspAgent
import com.aimatrix.amxlsp.memory.FileBasedMemoryManager

@ToolDescription("Store information in project memory")
class StoreMemoryTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val key = kwargs["key"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: key")
        val content = kwargs["content"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: content")
        val tags = kwargs["tags"] as? List<String> ?: emptyList()
        val metadata = kwargs["metadata"] as? Map<String, String> ?: emptyMap()
        
        return try {
            val memoryManager = memoriesManager
            if (memoryManager is FileBasedMemoryManager) {
                memoryManager.storeWithTags(key, content, tags, metadata)
            } else {
                memoryManager.store(key, content)
            }
            "Successfully stored memory with key: $key"
        } catch (e: Exception) {
            "Error storing memory: ${e.message}"
        }
    }
}

@ToolDescription("Retrieve information from project memory")
class RetrieveMemoryTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val key = kwargs["key"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: key")
        
        return try {
            val content = memoriesManager.retrieve(key)
            if (content != null) {
                "Memory content for key '$key':\n\n$content"
            } else {
                "No memory found with key: $key"
            }
        } catch (e: Exception) {
            "Error retrieving memory: ${e.message}"
        }
    }
}

@ToolDescription("Search through project memory")
class SearchMemoryTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val query = kwargs["query"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: query")
        val maxResults = kwargs["max_results"] as? Int ?: 10
        
        return try {
            val results = memoriesManager.search(query).take(maxResults)
            
            if (results.isEmpty()) {
                "No memories found matching query: $query"
            } else {
                buildString {
                    appendLine("Found ${results.size} memories matching '$query':")
                    appendLine()
                    
                    results.forEachIndexed { index, result ->
                        appendLine("${index + 1}. $result")
                        appendLine()
                    }
                }
            }
        } catch (e: Exception) {
            "Error searching memory: ${e.message}"
        }
    }
}

@ToolDescription("Find memories by tag")
class FindMemoriesByTagTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val tag = kwargs["tag"] as? String
        val tags = kwargs["tags"] as? List<String>
        
        if (tag == null && tags == null) {
            throw IllegalArgumentException("Either 'tag' or 'tags' parameter is required")
        }
        
        return try {
            val memoryManager = memoriesManager
            if (memoryManager !is FileBasedMemoryManager) {
                return "Tag-based search not supported with current memory manager"
            }
            
            val memories = if (tag != null) {
                memoryManager.findByTag(tag)
            } else {
                memoryManager.findByTags(tags!!)
            }
            
            if (memories.isEmpty()) {
                val tagDisplay = tag ?: tags!!.joinToString(", ")
                "No memories found with tag(s): $tagDisplay"
            } else {
                buildString {
                    val tagDisplay = tag ?: tags!!.joinToString(", ")
                    appendLine("Found ${memories.size} memories with tag(s) '$tagDisplay':")
                    appendLine()
                    
                    memories.forEach { memory ->
                        appendLine("Key: ${memory.key}")
                        appendLine("Content: ${memory.content.take(100)}${if (memory.content.length > 100) "..." else ""}")
                        appendLine("Tags: ${memory.tags.joinToString(", ")}")
                        appendLine("Created: ${java.time.Instant.ofEpochMilli(memory.createdAt)}")
                        appendLine()
                    }
                }
            }
        } catch (e: Exception) {
            "Error finding memories by tag: ${e.message}"
        }
    }
}

@ToolDescription("List all available memory tags")
class ListMemoryTagsTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        return try {
            val memoryManager = memoriesManager
            if (memoryManager !is FileBasedMemoryManager) {
                return "Tag listing not supported with current memory manager"
            }
            
            val tags = memoryManager.getAllTags()
            
            if (tags.isEmpty()) {
                "No tags found in memory"
            } else {
                buildString {
                    appendLine("Available memory tags (${tags.size}):")
                    appendLine()
                    
                    tags.forEach { tag ->
                        val memoriesWithTag = memoryManager.findByTag(tag)
                        appendLine("- $tag (${memoriesWithTag.size} memories)")
                    }
                }
            }
        } catch (e: Exception) {
            "Error listing memory tags: ${e.message}"
        }
    }
}

@ToolDescription("Get memory statistics")
class GetMemoryStatsTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        return try {
            val memoryManager = memoriesManager
            if (memoryManager !is FileBasedMemoryManager) {
                return "Memory statistics not supported with current memory manager"
            }
            
            val stats = memoryManager.getStatistics()
            
            buildString {
                appendLine("Memory Statistics:")
                appendLine("- Total memories: ${stats.totalMemories}")
                appendLine("- Total tags: ${stats.totalTags}")
                appendLine("- Memory size: ${formatBytes(stats.memorySize)}")
                
                stats.oldestMemory?.let { oldest ->
                    appendLine("- Oldest memory: ${java.time.Instant.ofEpochMilli(oldest)}")
                }
                
                stats.newestMemory?.let { newest ->
                    appendLine("- Newest memory: ${java.time.Instant.ofEpochMilli(newest)}")
                }
            }
        } catch (e: Exception) {
            "Error getting memory statistics: ${e.message}"
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.1f %s".format(size, units[unitIndex])
    }
}

@ToolDescription("Delete a memory by key")
class DeleteMemoryTool(agent: AmxLspAgent) : Tool(agent), ToolMarkerDoesNotRequireActiveProject {
    override fun apply(vararg args: Any, kwargs: Map<String, Any>): String {
        val key = kwargs["key"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: key")
        
        return try {
            val memoryManager = memoriesManager
            if (memoryManager !is FileBasedMemoryManager) {
                return "Memory deletion not supported with current memory manager"
            }
            
            val deleted = memoryManager.deleteMemory(key)
            
            if (deleted) {
                "Successfully deleted memory with key: $key"
            } else {
                "No memory found with key: $key"
            }
        } catch (e: Exception) {
            "Error deleting memory: ${e.message}"
        }
    }
}

// Register memory tools
fun registerMemoryTools() {
    ToolRegistry.register(StoreMemoryTool::class)
    ToolRegistry.register(RetrieveMemoryTool::class)
    ToolRegistry.register(SearchMemoryTool::class)
    ToolRegistry.register(FindMemoriesByTagTool::class)
    ToolRegistry.register(ListMemoryTagsTool::class)
    ToolRegistry.register(GetMemoryStatsTool::class)
    ToolRegistry.register(DeleteMemoryTool::class)
}