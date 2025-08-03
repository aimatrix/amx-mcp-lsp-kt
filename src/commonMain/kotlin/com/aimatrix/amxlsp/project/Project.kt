package com.aimatrix.amxlsp.project

import com.aimatrix.amxlsp.Constants
import com.aimatrix.amxlsp.config.ProjectConfig
import com.aimatrix.amxlsp.text.MatchedConsecutiveLines
import com.aimatrix.amxlsp.text.searchFiles
import com.aimatrix.amxlsp.util.GitignoreParser
import com.aimatrix.amxlsp.util.matchPath
import com.aimatrix.solidlsp.SolidLanguageServer
import com.aimatrix.solidlsp.config.Language
import com.aimatrix.solidlsp.config.LanguageServerConfig
import com.aimatrix.solidlsp.logger.LanguageServerLogger
import com.aimatrix.solidlsp.settings.SolidLSPSettings
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

class Project(
    val projectRoot: String,
    val projectConfig: ProjectConfig,
    val isNewlyCreated: Boolean = false
) {
    private val ignoredPatterns: List<String>
    private val ignoreSpec: PathSpec
    
    init {
        // Gather ignored paths from the project configuration and gitignore files
        val patterns = mutableListOf<String>()
        patterns.addAll(projectConfig.ignoredPaths)
        
        if (patterns.isNotEmpty()) {
            logger.info { "Using ${patterns.size} ignored paths from the explicit project configuration." }
            logger.debug { "Ignored paths: $patterns" }
        }
        
        if (projectConfig.ignoreAllFilesInGitignore) {
            logger.info { "Parsing all gitignore files in $projectRoot" }
            val gitignoreParser = GitignoreParser(projectRoot)
            logger.info { "Found ${gitignoreParser.getIgnoreSpecs().size} gitignore files." }
            
            gitignoreParser.getIgnoreSpecs().forEach { spec ->
                logger.debug { "Adding ${spec.patterns.size} patterns from ${spec.filePath} to the ignored paths." }
                patterns.addAll(spec.patterns)
            }
        }
        
        ignoredPatterns = patterns
        
        // Set up the pathspec matcher for the ignored paths
        val processedPatterns = patterns.toSet().map { pattern ->
            // Normalize separators (pathspec expects forward slashes)
            pattern.replace('\\', '/')
        }
        
        logger.debug { "Processing ${processedPatterns.size} ignored paths" }
        ignoreSpec = PathSpec.fromLines(GitWildMatchPattern, processedPatterns)
    }
    
    val projectName: String
        get() = projectConfig.projectName
    
    val language: Language
        get() = projectConfig.language ?: Language.KOTLIN
    
    companion object {
        fun load(projectRoot: Path, autogenerate: Boolean = true): Project {
            val resolvedRoot = projectRoot.toAbsolutePath()
            if (!resolvedRoot.exists()) {
                throw IllegalArgumentException("Project root not found: $resolvedRoot")
            }
            val projectConfig = ProjectConfig.load(resolvedRoot, autogenerate)
            return Project(
                projectRoot = resolvedRoot.toString(),
                projectConfig = projectConfig
            )
        }
        
        fun load(projectRoot: String, autogenerate: Boolean = true): Project {
            return load(Paths.get(projectRoot), autogenerate)
        }
    }
    
    fun pathToProjectYml(): String {
        return Paths.get(projectRoot, projectConfig.relPathToProjectYml()).toString()
    }
    
    /**
     * Reads a file relative to the project root.
     *
     * @param relativePath the path to the file relative to the project root
     * @return the content of the file
     */
    fun readFile(relativePath: String): String {
        val absPath = Paths.get(projectRoot) / relativePath
        if (!absPath.exists()) {
            throw IllegalArgumentException("File not found: $absPath")
        }
        return absPath.readText(charset = Charsets.UTF_8)
    }
    
    /**
     * @return the pathspec matcher for the paths that were configured to be ignored,
     *         either explicitly or implicitly through .gitignore files.
     */
    fun getIgnoreSpec(): PathSpec = ignoreSpec
    
    private fun isIgnoredDirname(dirname: String): Boolean {
        return dirname.startsWith(".")
    }
    
    /**
     * Determine whether a path should be ignored based on file type and ignore patterns.
     *
     * @param relativePath Relative path to check
     * @param ignoreNonSourceFiles whether files that are not source files (according to the file masks
     *        determined by the project's programming language) shall be ignored
     * @return whether the path should be ignored
     */
    private fun isIgnoredRelativePath(relativePath: String, ignoreNonSourceFiles: Boolean = true): Boolean {
        val absPath = Paths.get(projectRoot, relativePath)
        if (!absPath.exists()) {
            throw IllegalArgumentException("File $absPath not found, the ignore check cannot be performed")
        }
        
        // Check file extension if it's a file
        val isFile = absPath.isRegularFile()
        if (isFile && ignoreNonSourceFiles) {
            val fnMatcher = language.getSourceFnMatcher()
            if (!fnMatcher.isRelevantFilename(absPath.toString())) {
                return true
            }
        }
        
        // Create normalized path for consistent handling
        val relPath = Paths.get(relativePath)
        
        // Check each part of the path against always fulfilled ignore conditions
        val dirParts = if (isFile) {
            relPath.parent?.let { it.toList() } ?: emptyList()
        } else {
            relPath.toList()
        }
        
        for (part in dirParts) {
            val partStr = part.toString()
            if (partStr.isEmpty()) continue // Skip empty parts
            if (isIgnoredDirname(partStr)) {
                return true
            }
        }
        
        return matchPath(relativePath, getIgnoreSpec(), projectRoot)
    }
    
    /**
     * Checks whether the given path is ignored
     *
     * @param path the path to check, can be absolute or relative
     */
    fun isIgnoredPath(path: Path): Boolean {
        val relativePath = if (path.isAbsolute()) {
            Paths.get(projectRoot).relativize(path)
        } else {
            path
        }
        
        // Always ignore paths inside .git
        if (relativePath.nameCount > 0 && relativePath.getName(0).toString() == ".git") {
            return true
        }
        
        return matchPath(relativePath.toString(), getIgnoreSpec(), projectRoot)
    }
    
    fun isIgnoredPath(path: String): Boolean {
        return isIgnoredPath(Paths.get(path))
    }
    
    /**
     * Checks if the given (absolute or relative) path is inside the project directory.
     * Note that even relative paths may be outside if they contain ".." or point to symlinks.
     */
    fun isPathInProject(path: Path): Boolean {
        val normalizedPath = if (path.isAbsolute()) {
            path.normalize()
        } else {
            Paths.get(projectRoot).resolve(path).normalize()
        }
        
        val projectRootPath = Paths.get(projectRoot).normalize()
        return normalizedPath.startsWith(projectRootPath)
    }
    
    fun isPathInProject(path: String): Boolean {
        return isPathInProject(Paths.get(path))
    }
    
    /**
     * Create and initialize a language server for this project
     */
    fun createLanguageServer(
        serverSettings: SolidLSPSettings = SolidLSPSettings(),
        timeout: Long = Constants.DEFAULT_TOOL_TIMEOUT
    ): SolidLanguageServer {
        val config = LanguageServerConfig(
            language = language,
            workspaceRoot = projectRoot
        )
        
        return SolidLanguageServer(
            config = config,
            timeout = timeout
        )
    }
    
    /**
     * Search for files matching a pattern in the project
     */
    fun searchFiles(
        query: String,
        maxResults: Int = 100,
        ignoreCase: Boolean = true,
        useRegex: Boolean = false
    ): List<MatchedConsecutiveLines> {
        return searchFiles(
            rootPath = projectRoot,
            query = query,
            maxResults = maxResults,
            ignoreCase = ignoreCase,
            useRegex = useRegex,
            ignoreSpec = getIgnoreSpec()
        )
    }
}

// PathSpec interface for pattern matching (simplified version)
interface PathSpec {
    fun matches(path: String): Boolean
    
    companion object {
        fun fromLines(pattern: GitWildMatchPattern, lines: List<String>): PathSpec {
            return PathSpecImpl(lines)
        }
    }
}

object GitWildMatchPattern

// Simple implementation of PathSpec
private class PathSpecImpl(private val patterns: List<String>) : PathSpec {
    override fun matches(path: String): Boolean {
        // Simplified implementation - in real version would use proper gitignore pattern matching
        return patterns.any { pattern ->
            when {
                pattern.startsWith("*") && pattern.endsWith("*") -> {
                    path.contains(pattern.trim('*'))
                }
                pattern.startsWith("*") -> {
                    path.endsWith(pattern.substring(1))
                }
                pattern.endsWith("*") -> {
                    path.startsWith(pattern.substring(0, pattern.length - 1))
                }
                else -> {
                    path == pattern || path.startsWith("$pattern/")
                }
            }
        }
    }
}