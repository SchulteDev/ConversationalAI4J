# Full Docker build with sherpa-onnx speech processing
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

# Runtime stage with sherpa-onnx
FROM openjdk:21-jdk-slim

# Install system dependencies for sherpa-onnx and health checks
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    python3 \
    python3-pip \
    python3-venv \
    libasound2-dev \
    ffmpeg \
    tar \
    bzip2 \
    && rm -rf /var/lib/apt/lists/*

# Create and activate Python virtual environment
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Install latest compatible sherpa-onnx Python package
RUN pip install sherpa-onnx==1.12.8 soundfile numpy

# Create models directory
RUN mkdir -p /app/models/speech

# Download smaller/faster models for KISS approach
ARG STT_MODEL_URL=https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-20.tar.bz2
ARG STT_MODEL_NAME=sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-20

RUN cd /app/models/speech && \
    wget -q ${STT_MODEL_URL} && \
    tar -xjf ${STT_MODEL_NAME}.tar.bz2 && \
    mv ${STT_MODEL_NAME} stt && \
    rm ${STT_MODEL_NAME}.tar.bz2

# Use faster TTS model
ARG TTS_MODEL_URL=https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-lessac-low.tar.bz2

RUN cd /app/models/speech && \
    wget -q ${TTS_MODEL_URL} && \
    tar -xjf vits-piper-en_US-lessac-low.tar.bz2 && \
    mv vits-piper-en_US-lessac-low tts && \
    rm vits-piper-en_US-lessac-low.tar.bz2

# Set environment variables for speech model paths
ENV STT_MODEL_PATH=/app/models/speech/stt
ENV TTS_MODEL_PATH=/app/models/speech/tts
ENV SPEECH_ENABLED=true
ENV PYTHON_PATH="/opt/venv/bin/python"

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/conversational-ai4j-demo/build/libs/demo.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]