# ConversationalAI4J with Speech Support - Optimized Layer Caching
FROM gradle:9.0-jdk21 AS builder
WORKDIR /app
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/
COPY conversational-ai4j-library/ conversational-ai4j-library/
COPY conversational-ai4j-demo/ conversational-ai4j-demo/
RUN gradle :demo:bootJar --no-daemon

FROM openjdk:21-jdk-slim

# Early layer: Install system dependencies (rarely changes)
RUN apt-get update && apt-get install -y \
    curl python3 python3-pip python3-venv wget \
    && rm -rf /var/lib/apt/lists/*

# Early layer: Create virtual environment and install Python packages (rarely changes)
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
RUN pip install --no-cache-dir sherpa-onnx==1.10.46 soundfile numpy

# Early layer: Download heavy speech models (changes only when models update)
RUN mkdir -p /app/models && cd /app/models && \
    wget -q https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-2023-06-26.tar.bz2 && \
    tar -xf sherpa-onnx-streaming-zipformer-en-2023-06-26.tar.bz2 && \
    mv sherpa-onnx-streaming-zipformer-en-2023-06-26 stt && \
    rm sherpa-onnx-streaming-zipformer-en-2023-06-26.tar.bz2 && \
    wget -q https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2 && \
    tar -xf vits-piper-en_US-amy-low.tar.bz2 && \
    mv vits-piper-en_US-amy-low tts && \
    rm vits-piper-en_US-amy-low.tar.bz2

# Later layer: Copy application (changes frequently during development)
WORKDIR /app
COPY --from=builder /app/conversational-ai4j-demo/build/libs/demo.jar app.jar

# Final layer: Set environment and expose port
ENV SPEECH_ENABLED=true STT_MODEL_PATH=/app/models/stt TTS_MODEL_PATH=/app/models/tts
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]