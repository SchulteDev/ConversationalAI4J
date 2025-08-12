# Contributing to ConversationalAI4J

## Development Setup

```bash
git clone https://github.com/SchulteDev/ConversationalAI4J.git
cd ConversationalAI4J

# Requires Java 21
./gradlew build
./gradlew :demo:bootRun  # → http://localhost:8080
```

## Project-Specific Commands

```bash
# Build & test
./gradlew build
./gradlew :library:test    # Library only
./gradlew :demo:test       # Demo only

# Docker development
docker-compose up --build  # Full voice features
docker-compose logs -f demo

# Voice debugging  
export SPEECH_ENABLED=true
./gradlew :demo:bootRun
```

## Code Style

- **Builder Pattern**: Follow existing `ConversationalAI.builder()` pattern
- **KISS Principle**: Avoid over-engineering
- **Module Separation**: Library has no Spring dependencies
- **Error Handling**: Graceful fallback when services unavailable

## Testing This Project

```java
// Library: Mock external dependencies
@ExtendWith(MockitoExtension.class)
class ConversationalAITest {
    @Mock private OllamaLanguageModel mockModel;
    // Test builder pattern and core functionality
}

// Demo: Spring Boot integration tests  
@SpringBootTest(webEnvironment = RANDOM_PORT)
class DemoIntegrationTest {
    @Autowired private TestRestTemplate restTemplate;
    // Test REST endpoints and WebSocket
}
```

## Speech Development

Voice features require Linux containers:

- **Development**: Text-only mode on Windows/macOS
- **Testing**: Use Docker for full voice pipeline
- **Models**: sherpa-onnx STT/TTS downloaded during Docker build

## Pull Request Process

1. Create feature branch from `main`
2. Add tests for new functionality
3. Ensure `./gradlew build` passes
4. Update module-specific documentation if needed
5. Create PR with clear description

## Project-Specific Issues

**Voice not working in Docker**: Check `docker-compose logs -f demo` for STT/TTS errors
**Ollama connection fails**: Wait for model download on first Docker run
**Tests fail**: Verify Java 21 and clean build with `./gradlew clean build`
