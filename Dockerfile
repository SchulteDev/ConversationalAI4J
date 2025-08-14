# ConversationalAI4J with Whisper.cpp + Piper TTS
FROM gradle:9.0-jdk21 AS builder

WORKDIR /app

# Copy Gradle configuration
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/

# Copy source code
COPY conversational-ai4j-library/ conversational-ai4j-library/
COPY conversational-ai4j-demo/ conversational-ai4j-demo/

# Build the application
RUN gradle :demo:bootJar --no-daemon

# Runtime stage with speech support
FROM openjdk:21-jdk-slim

# Install basic dependencies and native library requirements
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Create models directory and download Whisper and Piper models
RUN mkdir -p /app/models/whisper /app/models/piper

# Download Whisper base.en model (smaller, English-only)
RUN cd /app/models/whisper && \
    wget -q https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin

# Download Piper TTS model (Amy voice)
RUN cd /app/models/piper && \
    wget -q https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/amy/low/en_US-amy-low.onnx && \
    wget -q https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/amy/low/en_US-amy-low.onnx.json

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/conversational-ai4j-demo/build/libs/demo.jar app.jar

# Set environment variables for Whisper and Piper
ENV SPEECH_ENABLED=true
ENV WHISPER_MODEL_PATH=/app/models/whisper/ggml-base.en.bin
ENV PIPER_MODEL_PATH=/app/models/piper/en_US-amy-low.onnx
ENV PIPER_CONFIG_PATH=/app/models/piper/en_US-amy-low.onnx.json

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]