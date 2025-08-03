# AmxLSP Agent - Kotlin Implementation

A comprehensive Kotlin-based dual-layer coding agent toolkit that provides intelligent code assistance through Language Server Protocol integration and AI model interactions via Model Context Protocol.

## Overview

AmxLSP is a powerful coding agent toolkit that combines:
- Language Server Protocol (LSP) integration for 13+ programming languages
- Model Context Protocol (MCP) server for seamless AI agent interactions
- Symbol-aware code editing and intelligent navigation
- Project memory and knowledge persistence system
- Flexible context and mode configurations for different workflows
- Modern desktop UI built with Compose Multiplatform

## Project Structure

```
amx-mcp-lsp-kt/
├── build.gradle.kts              # Gradle build configuration
├── settings.gradle.kts           # Gradle settings
├── gradle.properties             # Gradle properties
├── src/
│   ├── commonMain/kotlin/        # Shared Kotlin code
│   │   └── com/aimatrix/
│   │       ├── amxlsp/           # Core AmxLSP implementation
│   │       └── solidlsp/         # LSP wrapper implementation
│   ├── commonTest/kotlin/        # Shared tests
│   ├── desktopMain/kotlin/       # Desktop-specific code
│   │   └── com/aimatrix/amxlsp/
│   │       ├── cli/              # Command-line interface
│   │       └── desktop/          # Desktop GUI application
│   └── desktopTest/kotlin/       # Desktop tests
└── docs/                         # Documentation
```

## Features

### 🚀 Core Capabilities
- **Multi-Language Support**: Native LSP integration for Python, Java, Kotlin, TypeScript, Rust, Go, C#, and more
- **AI Integration**: MCP server enables seamless interaction with AI models
- **Smart Code Navigation**: Symbol-aware editing with intelligent code completion
- **Project Management**: Comprehensive project configuration and memory systems
- **Desktop GUI**: Modern Material3 interface for visual project management

### 🛠️ Developer Tools
- **File Operations**: Advanced file manipulation and editing tools
- **Symbol Tools**: Code symbol extraction and navigation
- **Memory System**: Persistent knowledge and context management
- **Configuration Management**: Flexible context and mode configurations

## Building the Project

### Prerequisites
- JDK 17 or higher
- Gradle 8.0 or higher (wrapper included)

### Build Commands
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the desktop application
./gradlew run

# Create executable JAR
./gradlew desktopJar

# Run CLI commands
./gradlew run --args="--help"
```

## Running AmxLSP

### Desktop Application
```bash
# Launch the desktop GUI
./gradlew run
```

### MCP Server
```bash
# Start with stdio transport (default)
java -jar build/libs/amxlsp-desktop.jar mcp-server

# Start with websocket transport
java -jar build/libs/amxlsp-desktop.jar mcp-server --transport websocket --port 3000
```

### Command Line Interface
```bash
# List projects
java -jar build/libs/amxlsp-desktop.jar project list

# Create a new project
java -jar build/libs/amxlsp-desktop.jar project create myproject /path/to/project -l KOTLIN

# Activate a project
java -jar build/libs/amxlsp-desktop.jar project activate myproject

# Show configuration
java -jar build/libs/amxlsp-desktop.jar config show
```

## Architecture

### Core Components

1. **AmxLspAgent** - Central orchestrator managing projects, tools, and AI interactions
2. **SolidLanguageServer** - Unified LSP wrapper providing multi-language support
3. **Tool System** - Modular architecture with specialized tools for different operations
4. **Configuration System** - Flexible contexts and modes for different development workflows
5. **MCP Server** - Exposes capabilities to AI agents via Model Context Protocol
6. **Desktop UI** - Compose Multiplatform application for visual project management

### Language Support

AmxLSP provides intelligent support for:
- **JVM Languages**: Java, Kotlin, Scala
- **Web Technologies**: TypeScript, JavaScript, HTML, CSS
- **Systems Languages**: Rust, Go, C, C++
- **Dynamic Languages**: Python, Ruby, PHP
- **Functional Languages**: Elixir, Clojure
- **Other**: C#, Dart, Terraform

### Technology Stack

- **Kotlin Multiplatform**: Shared code across desktop and potential mobile platforms
- **Compose Multiplatform**: Modern declarative UI framework
- **Coroutines**: Efficient async/await operations
- **Kotlinx Serialization**: Type-safe JSON and configuration handling
- **Material3**: Modern design system for consistent UI/UX
- **Gradle**: Build automation and dependency management

## Key Advantages

1. **Type Safety**: Full static typing with Kotlin's advanced type system
2. **Modern Async**: Coroutines provide efficient non-blocking operations
3. **Cross-Platform**: Kotlin Multiplatform enables code sharing
4. **Modern UI**: Compose Multiplatform delivers native performance with modern design
5. **Robust Testing**: Comprehensive test suite ensuring code quality
6. **AI-Ready**: Built-in MCP support for seamless AI model integration

## Development Status

✅ **Completed Features**
- Project structure and build configuration
- Core agent architecture and configuration system
- Complete tool system (file, memory, symbol, config tools)
- Language server integration and protocol handling
- MCP server implementation with full AI agent support
- Desktop GUI application with Material3 design
- CLI interface for project and configuration management
- Comprehensive test suite

🚀 **Production Ready**
- Fully functional desktop application
- Complete CLI toolset
- MCP server for AI integration
- Multi-language LSP support

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/aimatrix/amx-mcp-lsp-kt.git
   cd amx-mcp-lsp-kt
   ```

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Run the desktop application**
   ```bash
   ./gradlew run
   ```

4. **Create your first project**
   ```bash
   ./gradlew run --args="project create myproject /path/to/your/code -l KOTLIN"
   ```

## Contributing

We welcome contributions! When adding new features:

1. Follow Kotlin coding conventions and best practices
2. Use coroutines for all async operations
3. Add comprehensive tests for new functionality
4. Update documentation for API changes
5. Ensure Material3 design consistency in UI components

## License

MIT License - see LICENSE file for details

---

**AmxLSP Agent** - Intelligent coding assistance powered by Kotlin and AI