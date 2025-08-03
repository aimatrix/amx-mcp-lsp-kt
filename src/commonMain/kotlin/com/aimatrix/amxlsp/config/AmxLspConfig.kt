package com.aimatrix.amxlsp.config

import com.aimatrix.amxlsp.Constants
import kotlin.io.path.ExperimentalPathApi
import com.aimatrix.solidlsp.config.Language
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.yaml.YamlPropertySource
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

const val DEFAULT_TOOL_TIMEOUT = 300 // seconds

@Serializable
data class ProjectConfig(
    val projectName: String = "",
    val rootPath: String,
    val language: Language? = null,
    val encoding: String = Constants.DEFAULT_ENCODING,
    val ignoredPaths: List<String> = emptyList(),
    val ignoreAllFilesInGitignore: Boolean = true
) {
    fun relPathToProjectYml(): String = ".amxlsp/project.yml"
    
    companion object {
        fun load(projectRoot: Path, autogenerate: Boolean = true): ProjectConfig {
            val projectYmlPath = projectRoot / ".amxlsp" / "project.yml"
            
            return if (projectYmlPath.exists()) {
                // Load existing project config
                ConfigLoaderBuilder.default()
                    .addPropertySource(YamlPropertySource(projectYmlPath.toString()))
                    .build()
                    .loadConfigOrThrow<ProjectConfig>()
            } else if (autogenerate) {
                // Auto-generate project config
                val projectName = projectRoot.fileName?.toString() ?: "unnamed"
                val detectedLanguage = detectProjectLanguage(projectRoot)
                
                ProjectConfig(
                    projectName = projectName,
                    rootPath = projectRoot.toString(),
                    language = detectedLanguage
                ).also { config ->
                    // Save the auto-generated config
                    projectYmlPath.parent?.createDirectories()
                    projectYmlPath.writeText(serializeToYaml(config))
                }
            } else {
                throw IllegalStateException("No project configuration found at $projectYmlPath")
            }
        }
        
        @OptIn(ExperimentalPathApi::class)
        private fun detectProjectLanguage(projectRoot: Path): Language? {
            // Simple language detection based on file extensions
            val languageFiles = mapOf(
                Language.PYTHON to listOf("*.py", "pyproject.toml", "setup.py"),
                Language.KOTLIN to listOf("*.kt", "*.kts", "build.gradle.kts"),
                Language.JAVA to listOf("*.java", "pom.xml", "build.gradle"),
                Language.TYPESCRIPT to listOf("*.ts", "*.tsx", "tsconfig.json"),
                Language.JAVASCRIPT to listOf("*.js", "*.jsx", "package.json"),
                Language.GO to listOf("*.go", "go.mod"),
                Language.RUST to listOf("*.rs", "Cargo.toml"),
                Language.CSHARP to listOf("*.cs", "*.csproj"),
                Language.PHP to listOf("*.php", "composer.json"),
                Language.RUBY to listOf("*.rb", "Gemfile"),
                Language.ELIXIR to listOf("*.ex", "*.exs", "mix.exs"),
                Language.CLOJURE to listOf("*.clj", "*.cljs", "project.clj"),
                Language.DART to listOf("*.dart", "pubspec.yaml")
            )
            
            for ((language, patterns) in languageFiles) {
                for (pattern in patterns) {
                    if (projectRoot.walk().any { file ->
                        file.name.matches(Regex(pattern.replace("*", ".*")))
                    }) {
                        return language
                    }
                }
            }
            
            return null
        }
        
        private fun serializeToYaml(config: ProjectConfig): String {
            // Simple YAML serialization (in production, use proper YAML library)
            return """
                project_name: ${config.projectName}
                root_path: ${config.rootPath}
                language: ${config.language?.name ?: "null"}
                encoding: ${config.encoding}
                ignored_paths: ${config.ignoredPaths.joinToString(", ", "[", "]")}
                ignore_all_files_in_gitignore: ${config.ignoreAllFilesInGitignore}
            """.trimIndent()
        }
    }
}

@Serializable
data class AmxLspConfig(
    var activeProject: String? = null,
    var defaultContext: String = Constants.DEFAULT_CONTEXT,
    var defaultModes: List<String> = listOf(Constants.DEFAULT_MODES),
    val projects: MutableMap<String, ProjectConfig> = mutableMapOf(),
    val analytics: AnalyticsConfig = AnalyticsConfig()
) {
    fun save() {
        val configFile = AmxLspPaths.amxlspConfigFile
        configFile.parent.createDirectories()
        configFile.writeText(serializeToYaml(this))
    }
    
    companion object {
        fun load(): AmxLspConfig {
            val configFile = AmxLspPaths.amxlspConfigFile
            
            return if (configFile.exists()) {
                ConfigLoaderBuilder.default()
                    .addPropertySource(YamlPropertySource(configFile.toString()))
                    .build()
                    .loadConfigOrThrow<AmxLspConfig>()
            } else {
                // Return default config
                AmxLspConfig()
            }
        }
        
        private fun serializeToYaml(config: AmxLspConfig): String {
            // Simple YAML serialization
            val projectsYaml = config.projects.entries.joinToString("\n") { (name, proj) ->
                """
                |  $name:
                |    root_path: ${proj.rootPath}
                |    language: ${proj.language?.name ?: "null"}
                """.trimMargin()
            }
            
            return """
                active_project: ${config.activeProject ?: "null"}
                default_context: ${config.defaultContext}
                default_modes: ${config.defaultModes.joinToString(", ", "[", "]")}
                projects:
                $projectsYaml
                analytics:
                  opt_out: ${config.analytics.optOut}
            """.trimIndent()
        }
    }
}

@Serializable
data class AnalyticsConfig(
    val optOut: Boolean = false
)

object AmxLspPaths {
    val userHome: Path = Paths.get(System.getProperty("user.home"))
    val amxlspHome: Path = userHome / ".amxlsp"
    val amxlspConfigFile: Path = amxlspHome / "amxlsp_config.yml"
    val contextsDir: Path = amxlspHome / "contexts"
    val modesDir: Path = amxlspHome / "modes"
    val promptTemplatesDir: Path = amxlspHome / "prompt_templates"
    val memoriesDir: Path = amxlspHome / "memories"
    
    init {
        // Ensure directories exist
        listOf(amxlspHome, contextsDir, modesDir, promptTemplatesDir, memoriesDir).forEach { dir ->
            dir.createDirectories()
        }
    }
}