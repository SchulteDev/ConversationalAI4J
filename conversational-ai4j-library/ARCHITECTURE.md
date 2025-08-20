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

- **SpeechToTextService**: Speech-to-text processing with Whisper.cpp
- **TextToSpeechService**: Text-to-speech processing with Piper
- **SpeechConfig**: Configuration for speech processing
- **WhisperNative**: Native Whisper.cpp integration for STT
- **PiperNative**: Native Piper integration for TTS
- **SpeechServiceUtils**: Utility class for speech operations

#### Speech Architecture Design

The speech processing system uses a **native Java bindings approach** that eliminates the complexity
and performance overhead of inter-process communication:

**Whisper.cpp Integration:**

- **High-performance C++ engine** with JNI bindings
  via [whisper-jni](https://github.com/GiviMAD/whisper-jni)
- **Direct JNI calls** - no subprocess spawning or Python dependencies
- **Self-contained deployment** - native libraries included in JAR
- **Cross-platform support** - Windows, Linux, macOS (x86_64/arm64)
- **Real-time transcription** with model lifecycle management in Java

**Piper Integration:**

- **Dedicated TTS engine** optimized for speed and local processing
- **JNI bindings** via [piper-jni](https://github.com/GiviMAD/piper-jni)
- **Lightweight and fast** - designed specifically for real-time speech synthesis
- **Native audio generation** - direct WAV output without external tools

**Architecture Benefits:**

- **Eliminated inter-process overhead** - no temporary files, process communication, or stderr
  parsing
- **Simplified maintenance** - entire speech pipeline within Java ecosystem
- **Better performance** - native bindings offer lower latency than subprocess calls
- **Improved reliability** - no external Python environment or script dependencies
- **Enhanced testability** - direct Java interfaces for mocking and unit testing

This approach transforms what was previously a fragile Python subprocess system into a robust,
high-performance native Java solution suitable for production voice applications.

### Audio Processing Components

- **AudioChunkProcessor**: Processes audio data chunks for streaming
- **AudioSessionManager**: Manages audio session lifecycle
- **AudioProcessor**: Core audio processing logic
- **AudioFormat**: Audio format detection and handling
- **ConversationUtils**: Utilities for conversation management

## Dependencies

- **LangChain4j**: Ollama LLM integration (v1.3.0)
- **SLF4J**: Logging interface
- **whisper-jni**: Native Whisper.cpp bindings (v1.7.1)
- **piper-jni**: Native Piper TTS bindings (v1.2.0-c0670df)

## Design Principles

- No infrastructure dependencies (no Spring, no HTTP clients)
- Interface-based design for testability
- Builder pattern for easy configuration
- Optional speech capabilities (graceful fallback)
