# Speech Integration Guide

ConversationalAI4J supports speech-to-text and text-to-speech capabilities through sherpa-onnx integration.

## Platform Requirements

**Speech functionality is available on Linux only.**

- ✅ **Linux**: Full speech support (with sherpa-onnx installation)
- ⚠️ **Windows/macOS**: Mock implementation (for development/testing)

## Installation

### Linux Setup

1. **Install sherpa-onnx system-wide:**
   ```bash
   # Ubuntu/Debian
   sudo apt-get install sherpa-onnx
   
   # Or build from source
   git clone https://github.com/k2-fsa/sherpa-onnx
   cd sherpa-onnx
   mkdir build && cd build
   cmake -DCMAKE_BUILD_TYPE=Release ..
   make -j4
   sudo make install
   ```

2. **Download speech models:**
   ```bash
   mkdir -p ~/.sherpa-onnx/models
   # Download your preferred STT and TTS models to this directory
   ```

### Docker Setup (Recommended)

Use the provided Docker Compose setup which includes sherpa-onnx:

```bash
docker-compose up -d
```

## Usage

### Basic Speech Integration

```java
// Enable speech with default settings
ConversationalAI ai = ConversationalAI.builder()
    .withOllamaModel("llama3.2:3b")
    .withSpeech() // Enables both STT and TTS
    .build();

// Voice-to-voice conversation
byte[] audioResponse = ai.voiceChat(audioInput);

// Text-to-voice
byte[] audioOutput = ai.chatWithVoiceResponse("Hello!");

// Voice-to-text
String textResponse = ai.chatWithTextResponse(audioInput);
```

### Custom Speech Configuration

```java
// Custom language and voice
ConversationalAI ai = ConversationalAI.builder()
    .withOllamaModel("llama3.2:3b")
    .withSpeech("en-US", "female")
    .build();

// Advanced configuration
SpeechConfig speechConfig = SpeechConfig.custom(
    Paths.get("/path/to/stt-model.onnx"),
    Paths.get("/path/to/tts-model.onnx")
);

ConversationalAI ai = ConversationalAI.builder()
    .withOllamaModel("llama3.2:3b")
    .withSpeech(speechConfig)
    .build();
```

## REST API Endpoints

The demo application provides speech endpoints:

- `POST /voice-chat` - Audio input → Audio output
- `POST /text-to-voice` - Text input → Audio output  
- `POST /voice-to-text` - Audio input → Text output
- `GET /speech-status` - Check speech service availability

## Platform Behavior

### On Linux (with sherpa-onnx installed)
- ✅ Real speech processing using native libraries
- ✅ High-quality STT and TTS
- ✅ Offline processing (privacy-first)

### On Windows/macOS (development)
- ⚠️ Mock implementation returns placeholder audio
- ✅ API compatibility maintained
- ✅ Development and testing possible
- ℹ️ Log messages indicate mock mode usage

## Troubleshooting

### Speech Not Working on Linux

1. **Check library installation:**
   ```bash
   ldconfig -p | grep sherpa
   ```

2. **Check logs:**
   ```
   Speech functionality requires Linux platform. Current OS: xxx. Using mock implementation.
   ```

3. **Verify model paths:**
   ```bash
   ls -la ~/.sherpa-onnx/models/
   ```

### Docker Issues

1. **Ensure sufficient memory:**
   - Ollama container needs 8GB for llama3.2:3b
   - Increase Docker Desktop memory allocation

2. **Check container logs:**
   ```bash
   docker-compose logs demo
   ```

## Model Management

### Default Model Paths

- **STT Models**: `~/.sherpa-onnx/models/stt/{language}/model.onnx`
- **TTS Models**: `~/.sherpa-onnx/models/tts/{language}_{voice}/model.onnx`

### Recommended Models

For English speech processing:
- **STT**: sherpa-onnx streaming models (smaller, faster)
- **TTS**: VITS or Piper models (good quality/size balance)

See [sherpa-onnx model gallery](https://github.com/k2-fsa/sherpa-onnx/releases) for available models.

## Integration Examples

See `conversational-ai4j-demo` for complete integration examples:
- Web interface with speech controls
- REST API usage
- Error handling and fallbacks
- Docker deployment