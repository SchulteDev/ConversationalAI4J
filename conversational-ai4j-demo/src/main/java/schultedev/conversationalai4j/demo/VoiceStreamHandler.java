package schultedev.conversationalai4j.demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

  private final ConversationalAI conversationalAI;
  private final ConcurrentHashMap<String, CopyOnWriteArrayList<ByteBuffer>> audioBuffers =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Boolean> recordingStates = new ConcurrentHashMap<>();

  public VoiceStreamHandler() {
    ConversationalAI tempAI;
    try {
      String modelName = System.getenv().getOrDefault("OLLAMA_MODEL_NAME", "llama3.2:3b");
      String baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");

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
    String sessionId = session.getId();

    if (message instanceof TextMessage textMessage) {
      handleTextMessage(session, textMessage);
    } else if (message instanceof BinaryMessage binaryMessage) {
      handleBinaryMessage(session, binaryMessage);
    }
  }

  private void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    String payload = message.getPayload();
    String sessionId = session.getId();

    log.debug("Received text message from {}: {}", sessionId, payload);

    if ("start_recording".equals(payload)) {
      recordingStates.put(sessionId, true);
      audioBuffers.get(sessionId).clear();
      sendStatus(session, "recording", "Recording started");
      log.info("Started recording for session {}", sessionId);

    } else if ("stop_recording".equals(payload)) {
      recordingStates.put(sessionId, false);
      sendStatus(session, "processing", "Processing voice...");
      log.info("Stopped recording for session {}, processing audio", sessionId);
      processAccumulatedAudio(session);

    } else if ("check_status".equals(payload)) {
      sendSpeechStatus(session);
    }
  }

  private void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    String sessionId = session.getId();

    if (recordingStates.get(sessionId)) {
      audioBuffers.get(sessionId).add(message.getPayload());
      log.debug(
          "Received audio chunk of {} bytes from session {}",
          message.getPayload().remaining(),
          sessionId);
    }
  }

  private void processAccumulatedAudio(WebSocketSession session) throws IOException {
    String sessionId = session.getId();
    var chunks = audioBuffers.get(sessionId);

    if (chunks.isEmpty()) {
      sendStatus(session, "error", "No audio data received");
      return;
    }

    // Log chunk diagnostics
    int chunkCount = chunks.size();
    int firstSize = chunks.get(0).remaining();
    int lastSize = chunks.get(chunkCount - 1).remaining();
    log.debug(
        "Session {} has {} audio chunks; first={} bytes, last={} bytes",
        sessionId,
        chunkCount,
        firstSize,
        lastSize);

    try {
      // Combine all audio chunks
      ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
      int totalBytes = 0;
      for (ByteBuffer chunk : chunks) {
        byte[] chunkBytes = new byte[chunk.remaining()];
        chunk.get(chunkBytes);
        audioStream.write(chunkBytes);
        totalBytes += chunkBytes.length;
        chunk.rewind(); // Reset for potential reuse
      }

      byte[] audioData = audioStream.toByteArray();
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
      long t0 = System.nanoTime();
      String transcribedText = conversationalAI.speechToText(audioData);
      long t1 = System.nanoTime();
      log.info("STT completed for session {} in {} ms", sessionId, ((t1 - t0) / 1_000_000));

      if (transcribedText == null || transcribedText.trim().isEmpty()) {
        log.warn("Speech-to-text returned empty result for session {}", sessionId);
        sendStatus(session, "error", "Could not understand speech");
        return;
      }

      log.info("VOICE STT RESULT for session {}: '{}'", sessionId, transcribedText);

      // Send transcribed text to user interface
      String transcriptJson =
          String.format(
              "{\"type\":\"transcription\",\"text\":\"%s\"}",
              transcribedText.replace("\"", "\\\""));
      session.sendMessage(new TextMessage(transcriptJson));

      // Step 2: LLM Processing
      sendStatus(session, "llm_processing", "AI is thinking...");
      log.info("VOICE USER INPUT for session {}: '{}'", sessionId, transcribedText);
      long t2 = System.nanoTime();
      String aiResponse = conversationalAI.chat(transcribedText);
      long t3 = System.nanoTime();
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
    String statusJson =
        String.format(
            "{\"type\":\"status\",\"status\":\"%s\",\"message\":\"%s\"}", status, message);
    session.sendMessage(new TextMessage(statusJson));
  }

  private void sendSpeechStatus(WebSocketSession session) throws IOException {
    if (conversationalAI == null) {
      String status =
          "{\"type\":\"speech_status\",\"available\":false,\"reason\":\"AI service not available\"}";
      session.sendMessage(new TextMessage(status));
      return;
    }

    boolean speechEnabled = conversationalAI.isSpeechEnabled();
    boolean sttEnabled = conversationalAI.isSpeechToTextEnabled();
    boolean ttsEnabled = conversationalAI.isTextToSpeechEnabled();

    String status =
        String.format(
            "{\"type\":\"speech_status\",\"available\":%s,\"speechToText\":%s,\"textToSpeech\":%s,\"fullSpeech\":%s}",
            speechEnabled, sttEnabled, ttsEnabled, speechEnabled);

    session.sendMessage(new TextMessage(status));
    log.debug("Sent speech status to session {}: {}", session.getId(), status);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    log.error(
        "WebSocket transport error for session {}: {}",
        session.getId(),
        exception.getMessage(),
        exception);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
      throws Exception {
    String sessionId = session.getId();
    log.info("WebSocket voice stream connection closed: {} ({})", sessionId, closeStatus);

    // Clean up session data
    audioBuffers.remove(sessionId);
    recordingStates.remove(sessionId);
  }

  @Override
  public boolean supportsPartialMessages() {
    return true;
  }

  private void sendAIResponse(WebSocketSession session, String aiResponse, String sessionId)
      throws IOException {
    if (conversationalAI.isTextToSpeechEnabled()) {
      try {
        long t0 = System.nanoTime();
        // Directly convert AI response to speech without reprocessing through LLM
        byte[] responseAudio = conversationalAI.textToSpeech(aiResponse);
        long t1 = System.nanoTime();

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
    String responseJson =
        String.format(
            "{\"type\":\"text_response\",\"message\":\"%s\"}", text.replace("\"", "\\\""));
    session.sendMessage(new TextMessage(responseJson));
    sendStatus(session, "complete", "Text ready");
  }
}
