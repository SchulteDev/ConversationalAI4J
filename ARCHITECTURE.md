# ConversationalAI4J Architecture

## Project Structure

```
conversational-ai4j/
├── conversational-ai4j-library/    # Core API (see library/ARCHITECTURE.md)
└── conversational-ai4j-demo/       # Spring Boot demo (see demo/ARCHITECTURE.md)
```

## Design Philosophy

**Library**: Pure API, no infrastructure dependencies
**Demo**: Complete working example with Docker deployment
**Speech**: Native Java bindings eliminate inter-process overhead and Python dependencies

## Communication Flow

```
Browser (MediaRecorder) → WebSocket → Spring Boot Demo → Library API → Ollama LLM
                                   ↓
                               Audio Processing:
                               1. Format Detection (WebM/Opus/WAV)
                               2. FFmpeg Decoding (WebM/Opus → PCM)
                               3. Whisper.cpp (PCM → Text)
                               4. Piper TTS (Text → Speech)
```

## Audio Processing Pipeline

**Client-side**:

- MediaRecorder API captures audio in browser's preferred format
- Supports WebM/Opus (Chrome/Firefox) and WAV (where available)
- Simple JavaScript without complex fallbacks

**Server-side**:

- FFmpeg decodes WebM/Opus to raw PCM for Whisper compatibility
- Single preprocessing path prevents audio corruption
- Industry-standard approach used by Google Meet, Zoom, Discord

## Docker Environment

- **ollama**: AI model server (llama3.2:3b)
- **demo**: Spring Boot app with FFmpeg + speech processing

## Key Integration Points

- **LangChain4j**: Library → Ollama communication
- **WebSocket**: Browser → Demo real-time voice streaming
- **FFmpeg**: WebM/Opus decoding for browser compatibility
- **Whisper.cpp**: Speech-to-text with native Java bindings
- **Piper**: Text-to-speech with native Java bindings

See module-specific ARCHITECTURE.md files for implementation details.
