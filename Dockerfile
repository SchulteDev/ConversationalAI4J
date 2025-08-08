# Multi-stage build for ConversationalAI4J Demo Application
FROM gradle:9.0-jdk21 AS builder

# Set working directory
WORKDIR /app

# Copy Gradle configuration files
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/

# Copy source code
COPY conversational-ai4j-library/ conversational-ai4j-library/
COPY conversational-ai4j-demo/ conversational-ai4j-demo/

# Build the application
RUN gradle :demo:bootJar --no-daemon

# Runtime stage
FROM openjdk:21-jdk-slim

# Install system dependencies for sherpa-onnx and health checks
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    libasound2-dev \
    python3 \
    python3-pip \
    ffmpeg \
    tar \
    bzip2 \
    && rm -rf /var/lib/apt/lists/*

# Install sherpa-onnx Python package and dependencies
RUN pip3 install sherpa-onnx==1.22.1 soundfile

# Create models directory for speech processing
RUN mkdir -p /app/models/speech && chmod -R 777 /app/models/speech

# Download a lightweight English STT model (streaming zipformer)
ARG STT_MODEL_URL=https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20.tar.bz2
ARG STT_MODEL_NAME=sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20

RUN cd /app/models/speech && \
    wget -q ${STT_MODEL_URL} && \
    tar -xjf sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20.tar.bz2 && \
    mv ${STT_MODEL_NAME} stt && \
    rm sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20.tar.bz2

# Download a lightweight English TTS model (VITS)
ARG TTS_MODEL_URL=https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2

RUN cd /app/models/speech && \
    wget -q ${TTS_MODEL_URL} && \
    tar -xjf vits-piper-en_US-amy-low.tar.bz2 && \
    mv vits-piper-en_US-amy-low tts && \
    rm vits-piper-en_US-amy-low.tar.bz2

# Set environment variables for speech model paths
ENV STT_MODEL_PATH=/app/models/speech/stt
ENV TTS_MODEL_PATH=/app/models/speech/tts
ENV SPEECH_ENABLED=true

# Create application directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/conversational-ai4j-demo/build/libs/demo.jar app.jar

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]