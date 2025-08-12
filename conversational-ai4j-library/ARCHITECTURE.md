# Library Module Architecture

## Purpose
Pure API layer for conversational AI with optional speech capabilities.

## Core Classes

### ConversationalAI
Main API entry point with builder pattern:
```java
ConversationalAI ai = ConversationalAI.builder()
    .withOllamaModel("llama3.2:3b")
    .withSpeech()
    .build();
```

### Speech Components
- **SpeechService**: Interface for STT/TTS operations
- **SpeechConfig**: Configuration for speech processing
- **SherpaOnnxNative**: Native sherpa-onnx integration

### Memory Management  
- **ConversationMemory**: Interface for conversation persistence
- Pluggable strategies (in-memory, database, etc.)

## Dependencies
- **LangChain4j**: Ollama LLM integration (v1.3.0)
- **SLF4J**: Logging interface only

## Design Principles
- No infrastructure dependencies (no Spring, no HTTP clients)
- Interface-based design for testability
- Builder pattern for easy configuration
- Optional speech capabilities (graceful fallback)