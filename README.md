# ConversationalAI4J

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-9.0-blue.svg)](https://gradle.org/)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-pink.svg)](https://conventionalcommits.org)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SchulteDev_ConversationalAI4J&metric=alert_status&token=d82a94ffeaa4b434396b27080eab2189e4b032e8)](https://sonarcloud.io/summary/new_code?id=SchulteDev_ConversationalAI4J)
[![Test](https://github.com/SchulteDev/ConversationalAI4J/actions/workflows/test.yml/badge.svg)](https://github.com/SchulteDev/ConversationalAI4J/actions/workflows/test.yml)

A Java library for voice-enabled conversational AI. Speak to an LLM through your browser and get
intelligent spoken responses.

## Status

[![Project Status](https://img.shields.io/badge/Project%20Status-Alpha-red.svg)](https://github.com/SchulteDev/ConversationalAI4J)

This project is in early alpha stage. Features are functional but may contain bugs and APIs may
change.

This project was vibe coded, serving as a PoC for
[PhoneBlock -> "Enhance PhoneBlock-AB with Local AI for Intelligent Scam Call Conversations"](https://github.com/haumacher/phoneblock/issues/187)

[![Project Status](https://img.shields.io/badge/Project%20Status-Alpha-red.svg)](https://github.com/SchulteDev/ConversationalAI4J)

## Quick Start

```bash
git clone https://github.com/SchulteDev/ConversationalAI4J.git
cd ConversationalAI4J

# Complete voice AI system
# First start NEEDS A FEW MINUTES to download the models
docker-compose up --build
# → http://localhost:8080
```

## Library Usage

```java
// Text chat
ConversationalAI ai = ConversationalAI.builder()
    .withOllamaModel("llama3.2:3b")
    .build();
String response = ai.chat("Hello!");

// Voice chat
ConversationalAI voiceAI = ConversationalAI.builder()
  .withOllamaModel("llama3.2:3b")
  .withSpeech()
  .build();
byte[] audioResponse = voiceAI.voiceChat(audioBytes);
```

## Voice Demo

1. Click microphone button to start recording
2. Speak your message
3. Click microphone again to stop and send
4. AI responds with both text and synthesized speech

Pipeline: Browser Audio → FFmpeg Decoding → Whisper.cpp → Ollama → Piper TTS

**Browser Compatibility**: Works with modern browsers (Chrome, Firefox, Safari) using MediaRecorder
API. Supports WebM/Opus and WAV formats with server-side FFmpeg decoding.

## Configuration

| Variable             | Default                                        | Purpose                |
|----------------------|------------------------------------------------|------------------------|
| `OLLAMA_BASE_URL`    | `http://localhost:11434`                       | Ollama server          |
| `SPEECH_ENABLED`     | `false`                                        | Enable voice features  |
| `WHISPER_MODEL_PATH` | `/app/models/whisper/ggml-base.en.bin`         | Whisper STT model path |
| `PIPER_MODEL_PATH`   | `/app/models/piper/en_US-amy-medium.onnx`      | Piper TTS model path   |
| `PIPER_CONFIG_PATH`  | `/app/models/piper/en_US-amy-medium.onnx.json` | Piper TTS config path  |

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup and [ARCHITECTURE.md](ARCHITECTURE.md)
for technical details.
