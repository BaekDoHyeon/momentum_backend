# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**mo_backend** is a Kotlin/JVM backend application built with Gradle.

- **Language**: Kotlin 2.1.0
- **JVM Version**: Java 17
- **Build Tool**: Gradle with Kotlin DSL

## Common Commands

### Build
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Run a Single Test Class
```bash
./gradlew test --tests "ClassName"
```

### Run a Single Test Method
```bash
./gradlew test --tests "ClassName.methodName"
```

### Clean Build
```bash
./gradlew clean build
```

## Project Structure

```
mo_backend/
├── src/
│   ├── main/
│   │   ├── kotlin/      # Main application code
│   │   └── resources/   # Application resources
│   └── test/
│       ├── kotlin/      # Test code
│       └── resources/   # Test resources
└── build.gradle.kts     # Build configuration
```

## Code Architecture

The project uses standard Kotlin/Gradle structure with JUnit Platform for testing.
