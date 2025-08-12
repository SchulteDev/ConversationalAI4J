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

## Communication Flow

```
Browser WebSocket → Demo Module → Library API → Ollama LLM
                 ↓
              Speech Processing (sherpa-onnx)
```

## Docker Environment

- **ollama**: AI model server (llama3.2:3b)
- **demo**: Spring Boot app with speech processing

## Key Integration Points

- **LangChain4j**: Library → Ollama communication
- **WebSocket**: Browser → Demo real-time voice streaming  
- **sherpa-onnx**: Demo speech processing (Linux containers only)

See module-specific ARCHITECTURE.md files for implementation details.