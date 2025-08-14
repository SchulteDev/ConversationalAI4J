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

### Web Interface

- **conversation.html**: Thymeleaf template with voice controls
- JavaScript WebSocket client for audio streaming

## Voice Pipeline

1. Browser captures microphone → WebSocket binary frames
2. **VoiceStreamHandler** accumulates audio data
3. **SpeechService** (from library) processes STT → LLM → TTS
4. Audio response sent back via WebSocket

## Spring Boot Features

- **WebSocket**: Real-time binary audio streaming
- **Thymeleaf**: Server-side HTML templating
- **Actuator**: Health monitoring endpoints
- **DevTools**: Hot reload for development

## Docker Integration

- **Whisper and Piper models**: Downloaded during Docker build
- **Native JNI libraries**: Included in JAR dependencies
- **Multi-stage build**: Optimized container size
