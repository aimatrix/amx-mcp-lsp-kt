import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.aimatrix"
version = "1.0.3"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                
                // Kotlin standard library
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                
                // HTTP client for API calls
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                implementation("io.ktor:ktor-client-logging:2.3.7")
                
                // Server for MCP
                implementation("io.ktor:ktor-server-core:2.3.7")
                implementation("io.ktor:ktor-server-netty:2.3.7")
                implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-server-websockets:2.3.7")
                
                // Configuration
                implementation("com.sksamuel.hoplite:hoplite-core:2.7.5")
                implementation("com.sksamuel.hoplite:hoplite-yaml:2.7.5")
                
                // Logging
                implementation("io.github.microutils:kotlin-logging:3.0.5")
                
                // Command line parsing
                implementation("com.github.ajalt.clikt:clikt:4.2.2")
                
                // Markdown processing
                implementation("org.jetbrains:markdown:0.5.2")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                
                // Platform-specific dependencies
                implementation("io.ktor:ktor-client-cio:2.3.7")
                implementation("ch.qos.logback:logback-classic:1.4.14")
                implementation("com.github.ajalt.mordant:mordant:2.2.0")
                implementation("com.github.pgreze:kotlin-process:1.4.1")
                implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")
                implementation("com.beust:klaxon:5.6")
                implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
                
                // File dialogs for desktop
                implementation("cafe.adriel.voyager:voyager-navigator:1.0.0")
                implementation("cafe.adriel.voyager:voyager-tab-navigator:1.0.0")
            }
        }
        
        val desktopTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.10.1")
                implementation("io.mockk:mockk:1.13.9")
                implementation("io.kotest:kotest-runner-junit5:5.8.0")
                implementation("io.kotest:kotest-assertions-core:5.8.0")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.aimatrix.amxlsp.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "AmxLSP"
            packageVersion = "1.0.3"
            description = "AmxLSP - Advanced Language Server Protocol client"
            copyright = "© 2024 AiMatrix. All rights reserved."
            vendor = "AiMatrix"
            
            windows {
                menuGroup = "AiMatrix"
                upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
            }
            
            macOS {
                bundleID = "com.aimatrix.amxlsp"
                dockName = "AmxLSP"
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
            }
            
            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }
            
            modules("java.base", "java.desktop", "java.logging", "java.sql", "jdk.unsupported")
        }
    }
}

tasks.named<Test>("desktopTest") {
    useJUnitPlatform()
}

// Configure shadow jar for CLI distribution (when applied)
afterEvaluate {
    tasks.findByName("shadowJar")?.let { shadowTask ->
        shadowTask as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
        shadowTask.archiveBaseName.set("amxlsp-agent")
        shadowTask.mergeServiceFiles()
        shadowTask.manifest {
            attributes["Main-Class"] = "com.aimatrix.amxlsp.cli.MainKt"
        }
    }
}