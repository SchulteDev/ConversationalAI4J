# ConversationalAI4J with Working Speech - Single Solution
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

# Install speech dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Create Python virtual environment
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Install sherpa-onnx and dependencies
RUN pip install --no-cache-dir \
    sherpa-onnx==1.10.46 \
    soundfile \
    numpy

# Create models directory and download working models
RUN mkdir -p /app/models

# Download small working English STT model
RUN cd /app/models && \
    wget -q https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-2023-06-26.tar.bz2 && \
    tar -xf sherpa-onnx-streaming-zipformer-en-2023-06-26.tar.bz2 && \
    mv sherpa-onnx-streaming-zipformer-en-2023-06-26 stt && \
    rm sherpa-onnx-streaming-zipformer-en-2023-06-26.tar.bz2

# Download small working English TTS model  
RUN cd /app/models && \
    wget -q https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2 && \
    tar -xf vits-piper-en_US-amy-low.tar.bz2 && \
    mv vits-piper-en_US-amy-low tts && \
    rm vits-piper-en_US-amy-low.tar.bz2

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/conversational-ai4j-demo/build/libs/demo.jar app.jar

# Set environment variables for working speech
ENV SPEECH_ENABLED=true
ENV STT_MODEL_PATH=/app/models/stt
ENV TTS_MODEL_PATH=/app/models/tts

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]