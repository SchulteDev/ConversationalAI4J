package schultedev.conversationalai4j.demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import schultedev.conversationalai4j.ConversationalAI;

/**
 * WebSocket handler for real-time voice streaming. Handles bidirectional audio communication with
 * the ConversationalAI library.
 */
@Component
public class VoiceStreamHandler implements WebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(VoiceStreamHandler.class);
  
  // Performance and memory optimization constants
  private static final int MAX_AUDIO_CHUNKS_PER_SESSION = 1000; // Prevent memory issues
  private static final int MAX_AUDIO_BYTES_PER_SESSION = 10 * 1024 * 1024; // 10MB limit

  private final ConversationalAI conversationalAI;
  private final ConcurrentHashMap<String, CopyOnWriteArrayList<ByteBuffer>> audioBuffers =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Boolean> recordingStates = new ConcurrentHashMap<>();
  // Thread pool for async voice processing to avoid blocking WebSocket threads
  private final ExecutorService voiceProcessingExecutor = Executors.newFixedThreadPool(4);

  public VoiceStreamHandler() {
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
    audioBuffers.put(session.getId(), new CopyOnWriteArrayList<>());
    recordingStates.put(session.getId(), false);

    // Send initial status
    sendStatus(session, "connected", "Voice stream ready");
  }

  @Override
  public void handleMessage(WebSocketSession session, WebSocketMessage<?> message)
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
        recordingStates.put(sessionId, true);
        audioBuffers.get(sessionId).clear();
        sendStatus(session, "recording", "Recording started");
        log.info("Started recording for session {}", sessionId);
      }
      case "stop_recording" -> {
        recordingStates.put(sessionId, false);
        sendStatus(session, "processing", "Processing voice...");
        log.info("Stopped recording for session {}, processing audio", sessionId);
        // Process audio asynchronously to avoid blocking WebSocket thread
        CompletableFuture.runAsync(() -> {
          try {
            processAccumulatedAudio(session);
          } catch (IOException e) {
            log.error("Failed to process audio for session {}: {}", sessionId, e.getMessage(), e);
            try {
              sendStatus(session, "error", "Processing failed: " + e.getMessage());
            } catch (IOException ex) {
              log.error("Failed to send error status", ex);
            }
          }
        }, voiceProcessingExecutor);
      }
      case "check_status" -> sendSpeechStatus(session);
    }
  }

  private void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    var sessionId = session.getId();

    if (recordingStates.get(sessionId)) {
      var chunks = audioBuffers.get(sessionId);
      
      // Memory protection - prevent excessive memory usage
      if (chunks.size() >= MAX_AUDIO_CHUNKS_PER_SESSION) {
        log.warn("Session {} exceeded maximum audio chunks limit ({}), dropping chunk", 
                 sessionId, MAX_AUDIO_CHUNKS_PER_SESSION);
        return;
      }
      
      var currentSize = chunks.stream().mapToInt(ByteBuffer::remaining).sum();
      var incomingSize = message.getPayload().remaining();
      
      if (currentSize + incomingSize > MAX_AUDIO_BYTES_PER_SESSION) {
        log.warn("Session {} exceeded maximum audio size limit ({}MB), dropping chunk", 
                 sessionId, MAX_AUDIO_BYTES_PER_SESSION / (1024 * 1024));
        return;
      }
      
      chunks.add(message.getPayload());
      log.debug(
          "Received audio chunk of {} bytes from session {} (total: {} chunks, {} bytes)",
          incomingSize, sessionId, chunks.size(), currentSize + incomingSize);
    }
  }

  private void processAccumulatedAudio(WebSocketSession session) throws IOException {
    var sessionId = session.getId();
    var chunks = audioBuffers.get(sessionId);

    if (chunks.isEmpty()) {
      sendStatus(session, "error", "No audio data received");
      return;
    }

    // Log chunk diagnostics
    var chunkCount = chunks.size();
    var firstSize = chunks.getFirst().remaining();
    var lastSize = chunks.get(chunkCount - 1).remaining();
    log.debug(
        "Session {} has {} audio chunks; first={} bytes, last={} bytes",
        sessionId,
        chunkCount,
        firstSize,
        lastSize);

    try {
      // Efficiently combine all audio chunks - calculate total size first
      var totalBytes = chunks.stream().mapToInt(ByteBuffer::remaining).sum();
      var audioData = new byte[totalBytes];
      var position = 0;
      
      // Direct copy into pre-allocated array - much more efficient
      for (var chunk : chunks) {
        var chunkSize = chunk.remaining();
        chunk.get(audioData, position, chunkSize);
        position += chunkSize;
        chunk.rewind(); // Reset for potential reuse
      }
      log.info(
          "Processing combined audio data of {} bytes for session {}", audioData.length, sessionId);
      log.debug(
          "Session {} accumulated bytes sum={}, combined array size={}",
          sessionId,
          totalBytes,
          audioData.length);

      if (conversationalAI == null) {
        sendStatus(session, "error", "AI service not available");
        return;
      }

      if (!conversationalAI.isSpeechEnabled()) {
        sendStatus(session, "error", "Speech services not available in this environment");
        return;
      }

      // Step 1: Speech-to-Text
      sendStatus(session, "stt_processing", "Converting speech to text...");
      log.info(
          "Starting speech-to-text conversion for session {} with {} bytes of audio",
          sessionId,
          audioData.length);
      var t0 = System.nanoTime();
      var transcribedText = conversationalAI.speechToText(audioData);
      var t1 = System.nanoTime();
      log.info("STT completed for session {} in {} ms", sessionId, ((t1 - t0) / 1_000_000));

      if (transcribedText == null || transcribedText.trim().isEmpty()) {
        log.warn("Speech-to-text returned empty result for session {}", sessionId);
        sendStatus(session, "error", "Could not understand speech");
        return;
      }

      log.info("VOICE STT RESULT for session {}: '{}'", sessionId, transcribedText);

      // Send transcribed text to user interface
      var transcriptJson =
          String.format(
              "{\"type\":\"transcription\",\"text\":\"%s\"}",
              transcribedText.replace("\"", "\\\""));
      session.sendMessage(new TextMessage(transcriptJson));

      // Step 2: LLM Processing
      sendStatus(session, "llm_processing", "AI is thinking...");
      log.info("VOICE USER INPUT for session {}: '{}'", sessionId, transcribedText);
      var t2 = System.nanoTime();
      var aiResponse = conversationalAI.chat(transcribedText);
      var t3 = System.nanoTime();
      log.info("LLM completed for session {} in {} ms", sessionId, ((t3 - t2) / 1_000_000));

      if (aiResponse == null || aiResponse.trim().isEmpty()) {
        log.warn("LLM returned empty response for session {}", sessionId);
        sendStatus(session, "error", "AI failed to generate response");
        return;
      }

      log.info("VOICE AI RESPONSE for session {}: '{}'", sessionId, aiResponse);

      // Step 3: Text-to-Speech
      sendStatus(session, "tts_processing", "Converting to speech...");
      sendAIResponse(session, aiResponse, sessionId);

    } catch (Exception e) {
      log.error("Error processing voice stream for session {}: {}", sessionId, e.getMessage(), e);
      sendStatus(session, "error", "Processing error: " + e.getMessage());
    } finally {
      // Clear buffers
      chunks.clear();
    }
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
    var sttEnabled = conversationalAI.isSpeechToTextEnabled();
    var ttsEnabled = conversationalAI.isTextToSpeechEnabled();

    var status =
        String.format(
            "{\"type\":\"speech_status\",\"available\":%s,\"speechToText\":%s,\"textToSpeech\":%s,\"fullSpeech\":%s}",
            speechEnabled, sttEnabled, ttsEnabled, speechEnabled);

    session.sendMessage(new TextMessage(status));
    log.debug("Sent speech status to session {}: {}", session.getId(), status);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.error(
        "WebSocket transport error for session {}: {}",
        session.getId(),
        exception.getMessage(),
        exception);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
    var sessionId = session.getId();
    log.info("WebSocket voice stream connection closed: {} ({})", sessionId, closeStatus);

    // Clean up session data
    audioBuffers.remove(sessionId);
    recordingStates.remove(sessionId);
  }

  // Cleanup method for when handler is destroyed
  public void cleanup() {
    log.info("Shutting down voice processing executor");
    voiceProcessingExecutor.shutdown();
    try {
      if (!voiceProcessingExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
        voiceProcessingExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      voiceProcessingExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public boolean supportsPartialMessages() {
    return true;
  }

  private void sendAIResponse(WebSocketSession session, String aiResponse, String sessionId)
      throws IOException {
    if (conversationalAI.isTextToSpeechEnabled()) {
      try {
        var t0 = System.nanoTime();
        // Directly convert AI response to speech without reprocessing through LLM
        var responseAudio = conversationalAI.textToSpeech(aiResponse);
        var t1 = System.nanoTime();

        if (responseAudio == null || responseAudio.length == 0) {
          log.warn("TTS did not generate audio file for session {}", sessionId);
          sendTextResponse(session, aiResponse);
          return;
        }

        session.sendMessage(new BinaryMessage(responseAudio));
        sendStatus(session, "complete", "Speech ready");
        log.info(
            "Sent {} bytes audio to session {} (TTS {} ms)",
            responseAudio.length,
            sessionId,
            ((t1 - t0) / 1_000_000));
      } catch (Exception e) {
        log.warn("TTS failed for session {}: {}", sessionId, e.getMessage());
        sendTextResponse(session, aiResponse);
      }
    } else {
      sendTextResponse(session, aiResponse);
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
