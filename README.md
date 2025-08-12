# ConversationalAI4J

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SchulteDev_ConversationalAI4J&metric=alert_status&token=d82a94ffeaa4b434396b27080eab2189e4b032e8)](https://sonarcloud.io/summary/new_code?id=SchulteDev_ConversationalAI4J)

A Java library for voice-enabled conversational AI. Speak to an LLM through your browser and get
intelligent spoken responses.

## Quick Start

```bash
git clone https://github.com/SchulteDev/ConversationalAI4J.git
cd ConversationalAI4J

# Complete voice AI system
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

1. Click "Connect Voice Stream"
2. Hold microphone button and speak
3. AI responds with synthesized speech

Pipeline: Speech → Text → LLM → Speech (sherpa-onnx + Ollama)

## Configuration

| Variable          | Default                  | Purpose               |
|-------------------|--------------------------|-----------------------|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server         |
| `SPEECH_ENABLED`  | `false`                  | Enable voice features |

## Troubleshooting

**Docker voice issues**: Check logs with `docker-compose logs -f demo`
**Ollama not responding**: Wait for model download on first run
**No microphone**: Grant browser permissions for localhost

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup.
