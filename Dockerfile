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

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

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