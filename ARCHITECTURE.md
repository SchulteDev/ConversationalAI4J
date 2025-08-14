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
Browser WebSocket → Demo Module → Library API → Ollama LLM
                 ↓
              Speech Processing (Whisper.cpp + Piper)
```

## Docker Environment

- **ollama**: AI model server (llama3.2:3b)
- **demo**: Spring Boot app with speech processing

## Key Integration Points

- **LangChain4j**: Library → Ollama communication
- **WebSocket**: Browser → Demo real-time voice streaming
- **Whisper.cpp + Piper**: Speech processing with native Java bindings

See module-specific ARCHITECTURE.md files for implementation details.
