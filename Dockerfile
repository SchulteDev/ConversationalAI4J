# ConversationalAI4J with Whisper.cpp + Piper TTS
FROM gradle:9.2-jdk21 AS deps-cache

WORKDIR /app

# Copy only dependency files first for better caching
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/
COPY conversational-ai4j-library/build.gradle conversational-ai4j-library/
COPY conversational-ai4j-demo/build.gradle conversational-ai4j-demo/

# Download dependencies only - creates cached layer
RUN gradle :demo:dependencies --no-daemon || true

# Build stage - separate from deps to optimize rebuilds
FROM gradle:9.2-jdk21 AS builder

WORKDIR /app

# Copy cached dependencies
COPY --from=deps-cache /root/.gradle /root/.gradle

# Copy Gradle configuration
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/
COPY conversational-ai4j-library/build.gradle conversational-ai4j-library/
COPY conversational-ai4j-demo/build.gradle conversational-ai4j-demo/

# Add build argument to invalidate cache when source changes
ARG BUILD_TIME=unknown

# Copy source code last (most frequently changed)
COPY conversational-ai4j-library/src/ conversational-ai4j-library/src/
COPY conversational-ai4j-demo/src/ conversational-ai4j-demo/src/

# Build the application (dependencies already cached)
RUN echo "Building at ${BUILD_TIME}" && gradle :demo:bootJar --no-daemon

# Runtime stage with speech support
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS runtime-base

# Install system dependencies (cached layer)
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    libgomp1 \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Create models directory
RUN mkdir -p /app/models/whisper /app/models/piper

WORKDIR /app

# Download models in separate layers for better caching
FROM runtime-base AS models

# Download Whisper model (cached unless model changes)
RUN cd /app/models/whisper && \
    wget -q https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin

# Download Piper model (cached unless model changes)
RUN cd /app/models/piper && \
    wget -q https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/amy/medium/en_US-amy-medium.onnx && \
    wget -q https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/amy/medium/en_US-amy-medium.onnx.json

# Final runtime stage
FROM models AS runtime

# Copy the built JAR (only rebuilds when code changes)
COPY --from=builder /app/conversational-ai4j-demo/build/libs/demo.jar app.jar

# Set environment variables for Whisper and Piper
ENV SPEECH_ENABLED=true
ENV WHISPER_MODEL_PATH=/app/models/whisper/ggml-base.en.bin
ENV PIPER_MODEL_PATH=/app/models/piper/en_US-amy-medium.onnx
ENV PIPER_CONFIG_PATH=/app/models/piper/en_US-amy-medium.onnx.json

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
