package com.aimatrix.amxlsp.util

import com.aimatrix.amxlsp.project.PathSpec
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

// Simplified path matching function
fun matchPath(path: String, pattern: String): Boolean {
    return path.contains(pattern, ignoreCase = true)
}

/**
 * Parser for .gitignore files
 */
class GitignoreParser(private val rootPath: String) {
    private val ignoreSpecs = mutableListOf<IgnoreSpec>()
    
    init {
        parseGitignoreFiles()
    }
    
    private fun parseGitignoreFiles() {
        val root = Paths.get(rootPath)
        
        Files.walk(root).use { paths ->
            paths.filter { it.fileName.toString() == ".gitignore" }
                .forEach { gitignorePath ->
                    val patterns = gitignorePath.readLines()
                        .filter { line -> line.isNotBlank() && !line.startsWith("#") }
                        .map { it.trim() }
                    
                    if (patterns.isNotEmpty()) {
                        ignoreSpecs.add(IgnoreSpec(gitignorePath.toString(), patterns))
                    }
                }
        }
    }
    
    fun getIgnoreSpecs(): List<IgnoreSpec> = ignoreSpecs.toList()
}

data class IgnoreSpec(
    val filePath: String,
    val patterns: List<String>
)

/**
 * Match a path against a PathSpec
 */
fun matchPath(path: String, spec: PathSpec, rootPath: String): Boolean {
    val normalizedPath = path.replace('\\', '/')
    return spec.matches(normalizedPath)
}

/**
 * File system utilities
 */
object FileSystemUtils {
    fun ensureDirectory(path: Path) {
        if (!path.exists()) {
            path.createDirectories()
        } else if (!path.isDirectory()) {
            throw IllegalArgumentException("Path exists but is not a directory: $path")
        }
    }
    
    fun copyDirectory(source: Path, target: Path) {
        Files.walk(source).use { paths ->
            paths.forEach { sourcePath ->
                val targetPath = target.resolve(source.relativize(sourcePath))
                when {
                    sourcePath.isDirectory() -> targetPath.createDirectories()
                    sourcePath.isRegularFile() -> sourcePath.copyTo(targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
    
    fun deleteRecursively(path: Path) {
        if (path.exists()) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach { it.deleteIfExists() }
        }
    }
    
    fun getRelativePath(basePath: Path, targetPath: Path): Path {
        return basePath.relativize(targetPath)
    }
    
    fun findFiles(
        rootPath: Path,
        pattern: String,
        maxDepth: Int = Int.MAX_VALUE
    ): List<Path> {
        val matcher = rootPath.fileSystem.getPathMatcher("glob:$pattern")
        val results = mutableListOf<Path>()
        
        Files.walkFileTree(rootPath, setOf(FileVisitOption.FOLLOW_LINKS), maxDepth, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (matcher.matches(file.fileName)) {
                    results.add(file)
                }
                return FileVisitResult.CONTINUE
            }
        })
        
        return results
    }
}