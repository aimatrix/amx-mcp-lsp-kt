# Serena Agent - Kotlin Implementation

This is a Kotlin port of the Serena Agent MCP server, originally written in Python.

## Overview

Serena is a dual-layer coding agent toolkit that provides:
- Language Server Protocol (LSP) integration for 13+ programming languages
- Model Context Protocol (MCP) server for AI agent interactions
- Symbol-aware code editing and navigation
- Project memory and knowledge persistence
- Flexible context and mode configurations

## Project Structure

```
amx-serena-kotlin/
├── build.gradle.kts          # Gradle build configuration
├── settings.gradle.kts       # Gradle settings
├── gradle.properties         # Gradle properties
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/aimatrix/
│   │   │       ├── serena/       # Core Serena implementation
│   │   │       ├── interprompt/  # Prompt generation system
│   │   │       └── solidlsp/     # LSP wrapper implementation
│   │   └── resources/            # Configuration files and templates
│   └── test/
│       └── kotlin/               # Test files
└── docs/                         # Documentation
```

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

# Create executable JAR
./gradlew shadowJar

# Run the CLI
./gradlew run --args="--help"
```

## Running Serena

### Start MCP Server
```bash
# Start with stdio transport (default)
java -jar build/libs/serena-agent-all.jar mcp-server

# Start with websocket transport
java -jar build/libs/serena-agent-all.jar mcp-server --transport websocket --port 3000
```

### Project Management
```bash
# List projects
java -jar build/libs/serena-agent-all.jar project list

# Create a new project
java -jar build/libs/serena-agent-all.jar project create myproject /path/to/project -l KOTLIN

# Activate a project
java -jar build/libs/serena-agent-all.jar project activate myproject
```

## Architecture

### Core Components

1. **SerenaAgent** - Central orchestrator managing projects, tools, and interactions
2. **SolidLanguageServer** - Unified LSP wrapper for multiple language servers
3. **Tool System** - Modular tools for file operations, symbol navigation, and editing
4. **Configuration System** - Contexts and modes for different workflows
5. **MCP Server** - Exposes tools to AI agents via Model Context Protocol

### Language Support

The Kotlin implementation supports the same languages as the Python version:
- Python, Go, Java, Rust, TypeScript, JavaScript
- C#, PHP, Ruby, Kotlin, Dart
- Elixir, Clojure, Terraform

## Key Differences from Python Version

1. **Type Safety**: Full static typing with Kotlin's type system
2. **Coroutines**: Async operations use Kotlin coroutines instead of Python asyncio
3. **GUI Framework**: JavaFX/Swing instead of Tkinter for GUI components
4. **Dependencies**: Uses Kotlin libraries (Ktor, kotlinx.serialization, etc.)
5. **Build System**: Gradle instead of Python packaging tools

## Development Status

This is a work-in-progress port. The following components have been converted:
- ✅ Basic project structure and build configuration
- ✅ Constants and configuration classes
- ✅ CLI command structure
- ✅ Tool base classes
- ✅ Exception handling utilities
- ⏳ Language server integration
- ⏳ MCP server implementation
- ⏳ File and symbol tools
- ⏳ Memory management system

## Contributing

When adding new components:
1. Follow Kotlin coding conventions
2. Maintain the same public API as the Python version where possible
3. Use coroutines for async operations
4. Add comprehensive tests for new functionality
5. Update this README with implementation progress

## License

MIT License (same as the original Python implementation)