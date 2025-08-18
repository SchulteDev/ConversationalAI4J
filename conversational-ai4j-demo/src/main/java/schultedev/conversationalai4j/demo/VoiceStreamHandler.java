package schultedev.conversationalai4j.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import schultedev.conversationalai4j.AudioFormat;
import schultedev.conversationalai4j.AudioProcessor;
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
  private final ConcurrentHashMap<String, List<byte[]>> audioChunks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Boolean> recordingStates = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AudioFormat> sessionFormats = new ConcurrentHashMap<>();
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
    audioChunks.put(session.getId(), new ArrayList<>());
    recordingStates.put(session.getId(), false);
    sessionFormats.put(session.getId(), AudioFormat.wav16kMono()); // Default format

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
        audioChunks.get(sessionId).clear();
        sendStatus(session, "recording", "Recording started");
        log.info("Started recording for session {}", sessionId);
      }
      case "stop_recording" -> {
        recordingStates.put(sessionId, false);
        sendStatus(session, "processing", "Processing voice...");
        log.info("Stopped recording for session {}, processing audio", sessionId);
        // Process audio asynchronously to avoid blocking WebSocket thread
        CompletableFuture.runAsync(
            () -> {
              try {
                processAccumulatedAudio(session);
              } catch (IOException e) {
                log.error(
                    "Failed to process audio for session {}: {}", sessionId, e.getMessage(), e);
                try {
                  sendStatus(session, "error", "Processing failed: " + e.getMessage());
                } catch (IOException ex) {
                  log.error("Failed to send error status", ex);
                }
              }
            },
            voiceProcessingExecutor);
      }
      case "check_status" -> sendSpeechStatus(session);
    }
  }

  private void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    var sessionId = session.getId();

    if (!recordingStates.getOrDefault(sessionId, false)) {
      log.warn("Received audio data from session {} but recording is not active", sessionId);
      return;
    }

    var chunks = audioChunks.get(sessionId);
    if (chunks == null) {
      log.warn("No audio chunk list found for session {}", sessionId);
      return;
    }

    // Convert ByteBuffer to byte array
    var payload = message.getPayload();
    var audioData = new byte[payload.remaining()];
    payload.get(audioData);

    // Memory protection - prevent excessive memory usage
    if (chunks.size() >= MAX_AUDIO_CHUNKS_PER_SESSION) {
      log.warn(
          "Session {} exceeded maximum audio chunks limit ({}), dropping chunk",
          sessionId,
          MAX_AUDIO_CHUNKS_PER_SESSION);
      return;
    }

    var currentSize = chunks.stream().mapToInt(chunk -> chunk.length).sum();
    var incomingSize = audioData.length;

    if (currentSize + incomingSize > MAX_AUDIO_BYTES_PER_SESSION) {
      log.warn(
          "Session {} exceeded maximum audio size limit ({}MB), dropping chunk",
          sessionId,
          MAX_AUDIO_BYTES_PER_SESSION / (1024 * 1024));
      return;
    }

    // Detect audio format on first chunk
    if (chunks.isEmpty()) {
      var detectedFormat = AudioFormat.detect(audioData);
      sessionFormats.put(sessionId, detectedFormat);
      log.info("Detected audio format for session {}: {}", sessionId, detectedFormat);
    }

    chunks.add(audioData);
    log.trace(
        "Received audio chunk of {} bytes from session {} (total: {} chunks, {} bytes)",
        incomingSize,
        sessionId,
        chunks.size(),
        currentSize + incomingSize);
  }

  private void processAccumulatedAudio(WebSocketSession session) throws IOException {
    var sessionId = session.getId();
    var chunks = audioChunks.get(sessionId);
    var format = sessionFormats.get(sessionId);

    if (chunks == null || chunks.isEmpty()) {
      log.warn("No audio chunks available for session {}", sessionId);
      sendStatus(session, "error", "No audio data received");
      return;
    }

    // Check if session is still open
    if (!session.isOpen()) {
      log.warn("Session {} is closed, cannot process audio", sessionId);
      return;
    }

    // Log chunk diagnostics
    var chunkCount = chunks.size();
    var firstSize = chunks.isEmpty() ? 0 : chunks.getFirst().length;
    var lastSize = chunks.isEmpty() ? 0 : chunks.get(chunkCount - 1).length;
    log.debug(
        "Session {} has {} audio chunks; first={} bytes, last={} bytes",
        sessionId,
        chunkCount,
        firstSize,
        lastSize);

    try {
      // Use AudioProcessor to combine and preprocess audio chunks
      var combinedAudio = AudioProcessor.combineAudioChunks(chunks);

      if (combinedAudio.length == 0) {
        log.warn("No audio data after combining chunks for session {}", sessionId);
        sendStatus(session, "error", "No valid audio data");
        return;
      }

      log.info(
          "Processing {} bytes of combined audio for session {} (format: {})",
          combinedAudio.length,
          sessionId,
          format);

      if (conversationalAI == null) {
        sendStatus(session, "error", "AI service not available");
        return;
      }

      if (!conversationalAI.isSpeechEnabled()) {
        sendStatus(session, "error", "Speech services not available in this environment");
        return;
      }

      // Step 1: Speech-to-Text with format awareness
      sendStatus(session, "stt_processing", "Converting speech to text...");
      log.info(
          "Starting speech-to-text conversion for session {} with {} bytes of audio",
          sessionId,
          combinedAudio.length);
      var t0 = System.nanoTime();
      var transcribedText = conversationalAI.speechToText(combinedAudio, format);
      var t1 = System.nanoTime();
      log.info("STT completed for session {} in {} ms", sessionId, ((t1 - t0) / 1_000_000));

      if (transcribedText == null || transcribedText.trim().isEmpty()) {
        log.warn("Speech-to-text returned empty result for session {}", sessionId);
        sendStatus(session, "error", "Could not understand speech");
        return;
      }

      log.info("VOICE STT RESULT for session {}: '{}'", sessionId, transcribedText);

      // Send transcribed text to user interface
      if (session.isOpen()) {
        var transcriptJson =
            String.format(
                "{\"type\":\"transcription\",\"text\":\"%s\"}",
                transcribedText.replace("\"", "\\\""));
        session.sendMessage(new TextMessage(transcriptJson));
      } else {
        log.warn("Session {} closed before sending transcription", sessionId);
        return; // Exit early if session is closed
      }

      // Step 2: LLM Processing
      if (session.isOpen()) {
        sendStatus(session, "llm_processing", "AI is thinking...");
        log.info("VOICE USER INPUT for session {}: '{}'", sessionId, transcribedText);
        var t2 = System.nanoTime();
        var aiResponse = conversationalAI.chat(transcribedText);
        var t3 = System.nanoTime();
        log.info("LLM completed for session {} in {} ms", sessionId, ((t3 - t2) / 1_000_000));

        if (aiResponse == null || aiResponse.trim().isEmpty()) {
          log.warn("LLM returned empty response for session {}", sessionId);
          if (session.isOpen()) {
            sendStatus(session, "error", "AI failed to generate response");
          }
          return;
        }

        log.info("VOICE AI RESPONSE for session {}: '{}'", sessionId, aiResponse);

        // Step 3: Text-to-Speech
        if (session.isOpen()) {
          sendStatus(session, "tts_processing", "Converting to speech...");
          sendAIResponse(session, aiResponse, sessionId);
        } else {
          log.warn("Session {} closed before sending AI response", sessionId);
        }
      } else {
        log.warn("Session {} closed before LLM processing", sessionId);
      }

    } catch (Exception e) {
      log.error("Error processing voice stream for session {}: {}", sessionId, e.getMessage(), e);
      if (session.isOpen()) {
        try {
          sendStatus(session, "error", "Processing error: " + e.getMessage());
        } catch (Exception ex) {
          log.error("Failed to send error status to session {}: {}", sessionId, ex.getMessage());
        }
      }
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
    audioChunks.remove(sessionId);
    recordingStates.remove(sessionId);
    sessionFormats.remove(sessionId);
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

        // Send text first so it's always visible
        sendTextResponse(session, aiResponse);

        // Then send audio
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
