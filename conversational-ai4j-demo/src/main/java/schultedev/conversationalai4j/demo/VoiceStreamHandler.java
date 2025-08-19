package schultedev.conversationalai4j.demo;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import schultedev.conversationalai4j.ConversationalAI;
import schultedev.conversationalai4j.AudioSessionManager;
import schultedev.conversationalai4j.AudioChunkProcessor;

/**
 * WebSocket handler for real-time voice streaming. Delegates audio processing to specialized
 * services and focuses on WebSocket connection management and message routing.
 */
@Component
public class VoiceStreamHandler implements WebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(VoiceStreamHandler.class);

  private final ConversationalAI conversationalAI;
  private final AudioSessionManager sessionManager;
  private final AudioChunkProcessor chunkProcessor;

  public VoiceStreamHandler() {
    this.sessionManager = new AudioSessionManager();
    this.chunkProcessor = new AudioChunkProcessor();

    ConversationalAI tempAI;
    try {
      var modelName = System.getenv().getOrDefault("OLLAMA_MODEL_NAME", "llama3.2:3b");
      var baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");

      log.info("Initializing VoiceStreamHandler with model '{}' at '{}'", modelName, baseUrl);

      tempAI =
          ConversationalAI.builder()
              .withOllamaModel(modelName, baseUrl)
              .withMemory()
              .withSystemPrompt("Keep responses brief since this is voice chat.")
              .withSpeech()
              .build();

      log.info("VoiceStreamHandler initialized");
    } catch (Exception e) {
      log.warn("Failed to initialize ConversationalAI: {}", e.getMessage());
      tempAI = null;
    }
    this.conversationalAI = tempAI;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    log.info("WebSocket voice stream connection established: {}", session.getId());
    sessionManager.initializeSession(session.getId());

    // Send initial status
    sendStatus(session, "connected", "Voice stream ready");
  }

  @Override
  public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message)
      throws Exception {
    if (message instanceof TextMessage textMessage) {
      handleTextMessage(session, textMessage);
    } else if (message instanceof BinaryMessage binaryMessage) {
      handleBinaryMessage(session, binaryMessage);
    }
  }

  private void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    var payload = message.getPayload();
    var sessionId = session.getId();

    log.debug("Received text message from {}: {}", sessionId, payload);

    switch (payload) {
      case "start_recording" -> {
        sessionManager.startRecording(sessionId);
        sendStatus(session, "recording", "Recording started");
      }
      case "stop_recording" -> {
        sessionManager.stopRecording(sessionId);
        sendStatus(session, "processing", "Processing voice...");
        log.info("Stopped recording for session {}, processing audio", sessionId);
        // Process audio asynchronously to avoid blocking WebSocket thread
        processAccumulatedAudio(session);
      }
      case "check_status" -> sendSpeechStatus(session);
    }
  }

  private void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    var sessionId = session.getId();

    if (!sessionManager.isRecording(sessionId)) {
      log.warn("Received audio data from session {} but recording is not active", sessionId);
      return;
    }

    // Convert ByteBuffer to byte array
    var payload = message.getPayload();
    var audioData = new byte[payload.remaining()];
    payload.get(audioData);

    // Add audio chunk through session manager (includes validation and limits)
    sessionManager.addAudioChunk(sessionId, audioData);
  }

  private void processAccumulatedAudio(WebSocketSession session) {
    var sessionId = session.getId();
    var chunks = sessionManager.getAudioChunks(sessionId);
    var format = sessionManager.getSessionFormat(sessionId);

    // Check if session is still open
    if (!session.isOpen()) {
      log.warn("Session {} is closed, cannot process audio", sessionId);
      return;
    }

    // Create callback for processing status updates
    AudioChunkProcessor.ProcessingCallback callback =
        new AudioChunkProcessor.ProcessingCallback() {
          @Override
          public void onStatusUpdate(String status, String message) {
            if (session.isOpen()) {
              try {
                sendStatus(session, status, message);
              } catch (IOException e) {
                log.error("Failed to send status update: {}", e.getMessage());
              }
            }
          }

          @Override
          public void onTranscriptionReady(String transcribedText) {
            if (session.isOpen()) {
              try {
                var transcriptJson =
                    String.format(
                        "{\"type\":\"transcription\",\"text\":\"%s\"}",
                        transcribedText.replace("\"", "\\\""));
                session.sendMessage(new TextMessage(transcriptJson));
              } catch (IOException e) {
                log.error("Failed to send transcription: {}", e.getMessage());
              }
            }
          }
        };

    // Process audio asynchronously
    chunkProcessor
        .processAudioChunks(chunks, format, conversationalAI, callback)
        .thenAccept(
            result -> {
              if (!session.isOpen()) {
                log.warn("Session {} closed before processing completed", sessionId);
                return;
              }

              if (result.isSuccess()) {
                try {
                  sendAIResponse(
                      session, result.getAiResponse(), result.getResponseAudio(), sessionId);
                } catch (IOException e) {
                  log.error("Failed to send AI response: {}", e.getMessage());
                  try {
                    sendStatus(session, "error", "Failed to send response: " + e.getMessage());
                  } catch (IOException ex) {
                    log.error("Failed to send error status", ex);
                  }
                }
              } else {
                try {
                  sendStatus(session, "error", result.getErrorMessage());
                } catch (IOException e) {
                  log.error("Failed to send error status: {}", e.getMessage());
                }
              }
            })
        .exceptionally(
            throwable -> {
              log.error(
                  "Audio processing failed for session {}: {}",
                  sessionId,
                  throwable.getMessage(),
                  throwable);
              if (session.isOpen()) {
                try {
                  sendStatus(session, "error", "Processing failed: " + throwable.getMessage());
                } catch (IOException e) {
                  log.error("Failed to send error status", e);
                }
              }
              return null;
            })
        .whenComplete(
            (result, throwable) -> {
              // Clear buffers after processing
              sessionManager.clearAudioChunks(sessionId);
            });
  }

  private void sendStatus(WebSocketSession session, String status, String message)
      throws IOException {
    var statusJson =
        String.format(
            "{\"type\":\"status\",\"status\":\"%s\",\"message\":\"%s\"}", status, message);
    session.sendMessage(new TextMessage(statusJson));
  }

  private void sendSpeechStatus(WebSocketSession session) throws IOException {
    if (conversationalAI == null) {
      var status =
          "{\"type\":\"speech_status\",\"available\":false,\"reason\":\"AI service not available\"}";
      session.sendMessage(new TextMessage(status));
      return;
    }

    var speechEnabled = conversationalAI.isSpeechEnabled();
    var sttEnabled = conversationalAI.isSpeechEnabled();
    var ttsEnabled = conversationalAI.isSpeechEnabled();

    var status =
        String.format(
            "{\"type\":\"speech_status\",\"available\":%s,\"speechToText\":%s,\"textToSpeech\":%s,\"fullSpeech\":%s}",
            speechEnabled, sttEnabled, ttsEnabled, speechEnabled);

    session.sendMessage(new TextMessage(status));
    log.debug("Sent speech status to session {}: {}", session.getId(), status);
  }

  @Override
  public void handleTransportError(
      @NonNull WebSocketSession session, @NonNull Throwable exception) {
    log.error(
        "WebSocket transport error for session {}: {}",
        session.getId(),
        exception.getMessage(),
        exception);
  }

  @Override
  public void afterConnectionClosed(
      @NonNull WebSocketSession session, @NonNull CloseStatus closeStatus) {
    var sessionId = session.getId();
    log.info("WebSocket voice stream connection closed: {} ({})", sessionId, closeStatus);

    // Clean up session data
    sessionManager.removeSession(sessionId);
  }

  @Override
  public boolean supportsPartialMessages() {
    return true;
  }

  private void sendAIResponse(
      WebSocketSession session, String aiResponse, byte[] responseAudio, String sessionId)
      throws IOException {
    // Send text first so it's always visible
    sendTextResponse(session, aiResponse);

    // Then send audio if available
    if (responseAudio != null && responseAudio.length > 0) {
      session.sendMessage(new BinaryMessage(responseAudio));
      sendStatus(session, "complete", "Speech ready");
      log.info("Sent {} bytes audio to session {}", responseAudio.length, sessionId);
    } else {
      sendStatus(session, "complete", "Text ready");
    }
  }

  private void sendTextResponse(WebSocketSession session, String text) throws IOException {
    var responseJson =
        String.format(
            "{\"type\":\"text_response\",\"message\":\"%s\"}", text.replace("\"", "\\\""));
    session.sendMessage(new TextMessage(responseJson));
    sendStatus(session, "complete", "Text ready");
  }
}
