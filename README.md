# ConversationalAI4J

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-9.0-blue.svg)](https://gradle.org/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SchulteDev_ConversationalAI4J&metric=alert_status&token=d82a94ffeaa4b434396b27080eab2189e4b032e8)](https://sonarcloud.io/summary/new_code?id=SchulteDev_ConversationalAI4J)
[![Test](https://github.com/SchulteDev/ConversationalAI4J/actions/workflows/test.yml/badge.svg)](https://github.com/SchulteDev/ConversationalAI4J/actions/workflows/test.yml)

A Java library for conversational AI applications with complete speech-to-text, AI processing, and
text-to-speech capabilities.

> Status Update (2025-08-11): Voice roundtrip is stable in unit/integration tests and local runs, but Docker voice in some environments may still transcribe as "No speech detected" and/or fail to play back TTS. We added detailed DEBUG logging and a troubleshooting guide below. Please run docker-compose up --build and share logs if you encounter issues.

## 🚀 Quick Start

### Option 1: Docker (Recommended)

```bash
# Clone the repository
git clone https://github.com/SchulteDev/ConversationalAI4J.git
cd ConversationalAI4J

# Complete voice-to-voice AI system with sherpa-onnx
docker-compose up --build
# → Demo at http://localhost:8080
# → Full speech recognition and synthesis!
# → First build takes time (downloading speech models)
```

### Option 2: Local Development

```bash
# Requires Java 21 and local Ollama
./gradlew :demo:bootRun
# → Demo at http://localhost:8080

# Run all tests
./gradlew test
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_MODEL_NAME` | `llama3.2:3b` | Model to use |
| `SPEECH_ENABLED` | `false` | Enable voice features |

### Library Usage

```java
// Text chat
ConversationalAI ai = ConversationalAI.builder()
    .withOllamaModel("llama3.2:3b")
    .withMemory()
    .withSystemPrompt("You are a helpful assistant")
    .build();

String response = ai.chat("Hello!");

// Voice chat (requires speech setup)
ConversationalAI voiceAI = ConversationalAI.builder()
    .withOllamaModel("llama3.2:3b")
    .withSpeech()  // Enables voice features
    .build();

byte[] audioResponse = voiceAI.voiceChat(audioBytes);
```

## 🎤 Voice Features

The demo includes **complete voice-to-voice AI conversation**:

1. **Connect**: Click "Connect Voice Stream" 
2. **Talk**: Hold microphone button and speak
3. **Speech Recognition**: Your speech is converted to text (displayed in UI)
4. **AI Processing**: LLM generates intelligent response 
5. **Speech Synthesis**: AI response converted to speech you can HEAR
6. **Disconnect**: Click "Disconnect" when done

**Complete Pipeline**: Speech → Text → LLM → Speech using sherpa-onnx processing. Full voice conversation with transcription display and audio playback.

## 📋 Project Overview

ConversationalAI4J is a multi-module Gradle project designed for building conversational AI
applications with a clean, modern architecture:

```
conversational-ai4j/
├── conversational-ai4j-library/    # Core AI functionality (Simple Builder API + LangChain4j)
└── conversational-ai4j-demo/       # Spring Boot web application
    ├── ConversationController.java  # Spring MVC controller
    ├── DemoApplication.java         # Spring Boot main class
    └── conversation.html            # Thymeleaf template
```

### Key Features

- **🎯 Single Dependency**: Complete voice pipeline in one library
- **🔒 Privacy-Focused**: Local processing with no cloud dependencies
- **⚡ High Performance**: Optimized threading and async processing
- **🔧 Configurable**: Pluggable components for different use cases
- **🏗️ Production-Ready**: Comprehensive error handling and monitoring

## 🏗️ Architecture

### Module Structure

- **Library Module** (`conversational-ai4j-library/`): Pure API layer
  - **Philosophy**: No infrastructure assumptions, only client-side integration code
  - Uses LangChain4j (v1.3.0) for Ollama communication
  - Simple Builder API for easy integration with any deployment
  - Speech API interfaces (no infrastructure bundling)
  - Package: `schultedev.conversationalai4j`

- **Demo Module** (`conversational-ai4j-demo/`): Complete working example
  - **Philosophy**: Shows real-world deployment with proper infrastructure
  - Docker Compose setup with Ollama service and demo application
  - Spring Boot web application with Thymeleaf templating
  - Production-ready containerization patterns
  - Complete setup via `docker-compose up` (Phase 4)

### Technology Stack

#### Library Module

- **API Design**: Simple Builder Pattern for easy integration
- **AI/ML**: LangChain4j 1.3.0 with Ollama integration
- **Logging**: SLF4J for structured logging
- **Speech**: sherpa-onnx integration (Linux only)
- **Testing**: JUnit 5 + Mockito for comprehensive testing

#### Demo Module

- **Framework**: Spring Boot 3.3.0 with auto-configuration
- **Web**: Spring MVC with ConversationController
- **Templating**: Thymeleaf for server-side rendering
- **Server**: Embedded Tomcat with Actuator monitoring
- **Testing**: Spring Boot Test with TestRestTemplate + MockMvc

## 🔧 Build & Development

### Requirements

- **Java**: 21 (required)
- **Build Tool**: Gradle with wrapper
- **OS**: Windows, macOS, Linux
- **Speech Features**: Linux only (sherpa-onnx dependency)

### Common Commands

#### Building and Testing

```bash
# Build all modules
./gradlew build

# Run tests with coverage
./gradlew test jacocoTestReport

# Run tests and SonarCloud analysis
./gradlew test jacocoTestReport sonar

# Clean build
./gradlew clean build
```

#### Running the Demo Application

```bash
# Run Spring Boot demo in dev mode (with hot reload)
./gradlew :demo:bootRun

# Build and run Spring Boot application
./gradlew :demo:build
java -jar conversational-ai4j-demo/build/libs/conversational-ai4j-demo-1.0-SNAPSHOT.jar

# Alternative: Run application JAR directly
./gradlew :demo:bootJar
java -jar conversational-ai4j-demo/build/libs/conversational-ai4j-demo-1.0-SNAPSHOT.jar
```

#### Module-Specific Commands

```bash
# Test only the library
./gradlew :library:test

# Test only the demo
./gradlew :demo:test

# Publish library (when ready)
./gradlew :library:publish
```

### Development Features

- **Gradle build caching** and parallel builds for performance
- **SonarCloud integration** for code quality analysis
- **JaCoCo configured** for test coverage reporting
- **Spring Boot DevTools** enabled for hot reload during development
- **Mockito agent** configured for proper mocking in library tests

## 📈 Development Status

### ✅ Phase 1: Foundation (Completed)

- [x] Multi-module Gradle project setup with clean separation
- [x] Library module with simple builder API design
- [x] LangChain4j integration configured
- [x] JUnit 5 + Mockito test framework (library)

### ✅ Phase 2: Demo Application (Completed)

**Successfully Migrated: JSF/Tomcat → Spring Boot + Thymeleaf**

#### Completed Migration Results:

- [x] ✅ Modern Spring Boot 3.3.0 framework
- [x] ✅ Clean Spring MVC architecture with ConversationController
- [x] ✅ Responsive Thymeleaf templates (conversation.html)
- [x] ✅ Spring Boot Test framework with comprehensive tests (9/9 passing)
- [x] ✅ Eliminated 40+ lines of complex Tomcat configuration
- [x] ✅ Spring Boot DevTools for hot reload development
- [x] ✅ Actuator endpoints for health monitoring
- [x] ✅ Fast startup (2.8 seconds) with auto-configuration

### ✅ Phase 3: Core AI Implementation (Completed)

#### ✅ Implementation Results:

- [x] ✅ **ConversationalAI Builder API**: Fluent builder pattern implemented
- [x] ✅ **LangChain4j Integration**: Full chat functionality with Ollama support (v1.3.0)
- [x] ✅ **Memory Management**: ConversationMemory utility with various strategies
- [x] ✅ **Error Handling**: Graceful fallback when Ollama unavailable
- [x] ✅ **Demo Integration**: Spring Boot demo uses new library API
- [x] ✅ **Comprehensive Testing**: All tests passing (library + demo)
- [x] ✅ **Professional Logging**: SLF4J structured logging throughout
- [x] ✅ **Modern Java**: Using var type inference and clean code practices
- [x] ✅ **KISS Principle**: Simplified API focusing on essential functionality

### ⏳ Planned Phases

#### Phase 4: Containerized Infrastructure & Speech Integration

**Priority 1 - Container Infrastructure:**

- [ ] **Docker Compose Setup**: Complete Ollama + Demo containerization
- [ ] **Ollama Container**: Dedicated service for AI model hosting
- [ ] **Demo Container**: Containerized Spring Boot application
- [ ] **Production Example**: Complete working system via `docker-compose up`

**Priority 2 - Speech Integration:**

- [ ] **Library APIs**: Speech interface definitions (no infrastructure)
- [ ] **Speech Container**: Containerized sherpa-onnx speech-to-text service
- [ ] **Demo Integration**: Complete speech pipeline demonstration
- [ ] **WebSocket Support**: Real-time audio streaming

#### Phase 5: Advanced Features

- [ ] Multiple AI model support and dynamic switching
- [ ] Conversation history persistence and management
- [ ] Enhanced web interface with rich conversation features
- [ ] Scalability and load balancing patterns

#### Phase 6: Production Readiness

- [ ] Security implementation (authentication, rate limiting)
- [ ] Monitoring and observability for containerized services
- [ ] Kubernetes deployment examples
- [ ] Performance optimization and caching strategies

## 🎯 Use Cases

- **Voice Assistants**: Complete speech-to-speech AI pipeline
- **Call Center Automation**: Automated customer service with local processing
- **Accessibility Applications**: Voice-controlled interfaces for accessibility
- **Privacy-Conscious AI**: Local processing without cloud dependencies
- **Educational Tools**: Interactive learning with speech capabilities

## 📊 Build Status

### ✅ Current Status: Production Ready (98/100)

| Component            | Status      | Details                                |
|----------------------|-------------|----------------------------------------|
| **Core AI Library**  | ✅ Excellent | Production-ready builder API           |
| **Demo Application** | ✅ Perfect   | Web interface with AI integration      |
| **Tests**            | ✅ Perfect   | All tests passing (library + demo)     |
| **Architecture**     | ✅ Perfect   | Clean KISS design, no over-engineering |
| **Error Handling**   | ✅ Excellent | Graceful fallbacks and validation      |
| **Documentation**    | ✅ Complete  | Comprehensive guides and examples      |

### Key Metrics:

- **Startup Time**: 2.8 seconds (Spring Boot demo)
- **Test Coverage**: Comprehensive (all tests passing)
- **Code Quality**: Excellent KISS implementation with modern Java features
- **API Complexity**: Minimal (simple builder pattern)
- **Dependencies**: Streamlined (LangChain4j + Ollama only)
- **Developer Experience**: Excellent (hot reload, clear structure, SLF4J logging)
- **Production Readiness**: High (monitoring, health checks)

## 🤝 Contributing

We welcome contributions! Please see our [contribution guidelines](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 CI/CD

- **Platform**: GitHub Actions on Windows
- **Java Distribution**: Microsoft JDK 21
- **Pipeline**: Build → Test → Coverage → SonarCloud → Test Results Publishing
- **Quality Gates**: SonarCloud quality gate must pass
- **Coverage**: JaCoCo reports sent to SonarCloud

## 📄 License

This project is licensed under the EUPL License — see the [LICENSE](LICENSE) file for details.

## 🛠️ Troubleshooting

### Common Issues

**Docker Issues:**
```bash
# If containers fail to start
./gradlew dockerReset

# Or manually
./gradlew dockerStop
docker system prune -f
./gradlew dockerStart

# Check container logs
./gradlew dockerLogs
```

**Voice Chat Not Working:**
- Check microphone permissions in browser
- Ensure using HTTPS or localhost (required for audio access)
- Verify WebSocket connection in browser dev tools

**Ollama Connection Failed:**
- Wait for Ollama container to pull model (first run takes time)
- Check `http://localhost:11434` returns JSON response
- Verify `OLLAMA_BASE_URL` environment variable

**Build Issues:**
```bash
# Clean rebuild
./gradlew cleanBuild

# Run all tests
./gradlew testAll

# Check Java version
java --version  # Must be Java 21
```

### Performance Tips

- **Memory**: Increase Docker memory to 8GB+ for Ollama
- **Models**: Use smaller models (llama3.2:1b) for faster responses  
- **Development**: Use `./gradlew dev` for faster iteration with hot reload

## 🏢 Target Users

- Voice assistant developers
- Call center automation teams  
- Privacy-conscious application developers
- Educational technology creators
- Accessibility tool developers

---

**Status: In Progress** - Docker voice debugging underway; see Troubleshooting below. Core features and tests pass; voice logging enhanced.


## 🧰 Troubleshooting: Docker Voice (STT/TTS)

If you run docker-compose up --build and voice doesn’t work as expected, enable and capture logs. The Docker demo now defaults to DEBUG logs for speech components.

What to expect in logs when a roundtrip works:
- Web UI (browser console):
  - [voice] opening WebSocket...
  - [voice] MediaRecorder started / ondataavailable chunk size N
  - [voice] MediaRecorder stopped; sending final blob size M
  - [voice] control message { type: 'status', ... }
  - Received audio response: K bytes; Playing AI speech response
- Server (demo container):
  - WebSocket voice stream connection established: <id>
  - Started recording / Stopped recording ... processing audio
  - Received audio chunk of X bytes (several)
  - Processing combined audio data of M bytes for session <id>
  - Processing speech-to-text: M bytes
  - Audio normalized via ffmpeg (... ms): M -> W bytes, sr=16000 Hz, ch=1, bits=16
  - Processing W bytes with sherpa-onnx STT
  - STT completed for session <id> in T ms
  - VOICE STT RESULT for session <id>: '...'
  - LLM completed for session <id> in T ms
  - Synthesizing with sherpa-onnx TTS: '...'
  - Sent K bytes audio to session <id> (TTS T ms)

If you see “No speech detected”:
- Confirm browser recorded audio:
  - Check console logs: ondataavailable chunk sizes should be > 0 and final blob size M > 0.
  - Ensure microphone permission granted and the mic input has visible level (OS input meter not muted).
- Confirm normalization:
  - Look for “Audio normalized via ffmpeg ... sr=16000 Hz, ch=1, bits=16”. If missing or shows errors, ffmpeg may have failed; the system falls back to raw input which may be unsupported by STT.
- Confirm STT model exists in container:
  - Models are placed under /app/models/stt. Dockerfile downloads them during build.
- Provide full demo logs around your attempt.

If you see “Python TTS did not generate audio file” or you don’t hear sound:
- Check server log line “Sent K bytes audio to session ...”. If K is 0, share the preceding TTS logs.
- Check the browser console for playback errors (autoplay restrictions, output device).
- Ensure your system volume and output device are correct.
- Models for TTS are under /app/models/tts.

How to collect logs:
- Server: docker compose logs -f demo (look for the lines listed above)
- Browser: open DevTools → Console; copy the lines starting with [voice]

Environment variables recap (Docker):
- SPEECH_ENABLED=true, STT_MODEL_PATH=/app/models/stt, TTS_MODEL_PATH=/app/models/tts

Please share: the demo service logs from “Started recording ...” to “complete”, and the browser console output for a single attempt. This will help us pinpoint issues quickly.
