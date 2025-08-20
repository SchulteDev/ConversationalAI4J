# Demo Module Architecture

## Purpose

Spring Boot web application demonstrating voice-enabled conversational AI.

## Key Components

### Controllers

- **ConversationController**: REST API for text chat
- **VoiceStreamHandler**: WebSocket handler for real-time voice streaming

### Configuration

- **WebSocketConfig**: WebSocket setup for voice streaming
- **DemoApplication**: Spring Boot main class
- **AppConfig**: Application configuration and beans

### Web Interface

- **conversation.html**: Thymeleaf template with voice controls
- JavaScript WebSocket client for audio streaming

## Voice Pipeline

1. Browser captures microphone → WebSocket binary frames
2. **VoiceStreamHandler** accumulates audio data using **AudioSessionManager**
3. **AudioChunkProcessor** handles real-time audio processing
4. **SpeechToTextService** and **TextToSpeechService** (from library) process STT → LLM → TTS
5. Audio response sent back via WebSocket

## Spring Boot Features

- **WebSocket**: Real-time binary audio streaming
- **Thymeleaf**: Server-side HTML templating
- **Actuator**: Health monitoring endpoints
- **DevTools**: Hot reload for development

## Docker Integration

- **Multi-stage build**: Optimized for dependency caching and fast rebuilds
- **Whisper model**: ggml-base.en.bin downloaded during build
- **Piper model**: en_US-amy-medium.onnx (upgraded from low to medium quality)
- **Native JNI libraries**: Included in JAR dependencies
- **FFmpeg**: Integrated for WebM/Opus audio decoding
- **Health checks**: Built-in monitoring for both Ollama and demo services
