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

  @Mock
  private OllamaLanguageModel mockModel;
  // Test builder pattern and core functionality
}

// Demo: Spring Boot integration tests  
@SpringBootTest(webEnvironment = RANDOM_PORT)
class DemoIntegrationTest {

  @Autowired
  private TestRestTemplate restTemplate;
  // Test REST endpoints and WebSocket
}
```

## Speech Development

Voice features work cross-platform with proper setup:

- **Development**: Full voice features available in Docker on all platforms
- **Browser Testing**: Modern browsers (Chrome, Firefox, Safari) with MediaRecorder API
- **Audio Formats**: WebM/Opus and WAV automatically handled by FFmpeg server-side
- **Models**: Whisper.cpp and Piper models downloaded during Docker build
- **FFmpeg**: Included in Docker container for WebM/Opus decoding

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
**WebM/Opus audio issues**: Use latest Docker build — FFmpeg decoding is now integrated
**Spring Boot restarts twice**: Fixed - DevTools removed from production build
**"you you you" transcriptions**: Fixed - eliminated double audio preprocessing

## Docker Build Tips

```bash
# Force fresh build with source changes
docker build --build-arg BUILD_TIME=$(date +%s) -t conversational-ai4j .

# Or use docker-compose
docker-compose up --build
```
