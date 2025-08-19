package schultedev.conversationalai4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes accumulated audio chunks through the complete AI pipeline: Audio chunks → Combined
 * audio → Speech-to-Text → LLM → Text-to-Speech → Response
 *
 * <p>This is a pure Java class with no framework dependencies, making it reusable across different
 * application types (Spring Boot, plain Java, etc.).
 */
public class AudioChunkProcessor {

  private static final Logger log = LoggerFactory.getLogger(AudioChunkProcessor.class);

  // Thread pool for async voice processing to avoid blocking caller threads
  private final ExecutorService voiceProcessingExecutor = Executors.newFixedThreadPool(4);

  /** Processes audio chunks asynchronously through the complete AI pipeline. */
  public CompletableFuture<ProcessingResult> processAudioChunks(
      List<byte[]> chunks,
      AudioFormat format,
      ConversationalAI conversationalAI,
      ProcessingCallback callback) {

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return processAudioChunksSync(chunks, format, conversationalAI, callback);
          } catch (Exception e) {
            log.error("Error in audio processing pipeline: {}", e.getMessage(), e);
            return ProcessingResult.error("Processing error: " + e.getMessage());
          }
        },
        voiceProcessingExecutor);
  }

  private ProcessingResult processAudioChunksSync(
      List<byte[]> chunks,
      AudioFormat format,
      ConversationalAI conversationalAI,
      ProcessingCallback callback) {

    if (chunks == null || chunks.isEmpty()) {
      log.warn("No audio chunks available for processing");
      return ProcessingResult.error("No audio data received");
    }

    if (conversationalAI == null) {
      return ProcessingResult.error("AI service not available");
    }

    if (!conversationalAI.isSpeechEnabled()) {
      return ProcessingResult.error("Speech services not available in this environment");
    }

    // Log chunk diagnostics
    var chunkCount = chunks.size();
    var firstSize = chunks.isEmpty() ? 0 : chunks.getFirst().length;
    var lastSize = chunks.isEmpty() ? 0 : chunks.get(chunkCount - 1).length;
    log.debug(
        "Processing {} audio chunks; first={} bytes, last={} bytes",
        chunkCount,
        firstSize,
        lastSize);

    try {
      // Step 1: Combine audio chunks
      var combinedAudio = AudioProcessor.combineAudioChunks(chunks);

      if (combinedAudio.length == 0) {
        log.warn("No audio data after combining chunks");
        return ProcessingResult.error("No valid audio data");
      }

      log.info("Processing {} bytes of combined audio (format: {})", combinedAudio.length, format);

      // Step 2: Speech-to-Text
      callback.onStatusUpdate("stt_processing", "Converting speech to text...");
      log.info("Starting speech-to-text conversion with {} bytes of audio", combinedAudio.length);

      var t0 = System.nanoTime();
      var transcribedText =
          SpeechServiceUtils.speechToText(conversationalAI, combinedAudio, format);
      var t1 = System.nanoTime();
      log.info("STT completed in {} ms", ((t1 - t0) / 1_000_000));

      if (transcribedText == null || transcribedText.trim().isEmpty()) {
        log.warn("Speech-to-text returned empty result");
        return ProcessingResult.error("Could not understand speech");
      }

      log.info("VOICE STT RESULT: '{}'", transcribedText);

      // Notify callback with transcription
      callback.onTranscriptionReady(transcribedText);

      // Step 3: LLM Processing
      callback.onStatusUpdate("llm_processing", "AI is thinking...");
      log.info("VOICE USER INPUT: '{}'", transcribedText);

      var t2 = System.nanoTime();
      var aiResponse = conversationalAI.chat(transcribedText);
      var t3 = System.nanoTime();
      log.info("LLM completed in {} ms", ((t3 - t2) / 1_000_000));

      if (aiResponse == null || aiResponse.trim().isEmpty()) {
        log.warn("LLM returned empty response");
        return ProcessingResult.error("AI failed to generate response");
      }

      log.info("VOICE AI RESPONSE: '{}'", aiResponse);

      // Step 4: Text-to-Speech
      callback.onStatusUpdate("tts_processing", "Converting to speech...");

      byte[] responseAudio = null;
      if (conversationalAI.isSpeechEnabled()) {
        try {
          var t4 = System.nanoTime();
          responseAudio = conversationalAI.textToSpeech(aiResponse);
          var t5 = System.nanoTime();

          if (responseAudio != null && responseAudio.length > 0) {
            log.info(
                "TTS completed in {} ms, generated {} bytes",
                ((t5 - t4) / 1_000_000),
                responseAudio.length);
          } else {
            log.warn("TTS did not generate audio file");
          }
        } catch (Exception e) {
          log.warn("TTS failed: {}", e.getMessage());
          responseAudio = null;
        }
      }

      return ProcessingResult.success(transcribedText, aiResponse, responseAudio);

    } catch (Exception e) {
      log.error("Error in audio processing pipeline: {}", e.getMessage(), e);
      return ProcessingResult.error("Processing error: " + e.getMessage());
    }
  }

  /** Shutdown the processing executor. */
  public void shutdown() {
    voiceProcessingExecutor.shutdown();
  }

  /** Callback interface for processing status updates. */
  public interface ProcessingCallback {
    void onStatusUpdate(String status, String message);

    void onTranscriptionReady(String transcribedText);
  }

  /** Result of audio processing containing both text and audio responses. */
  public record ProcessingResult(
      String transcribedText,
      String aiResponse,
      byte[] responseAudio,
      boolean success,
      String errorMessage) {

    public static ProcessingResult success(
        String transcribedText, String aiResponse, byte[] responseAudio) {
      return new ProcessingResult(transcribedText, aiResponse, responseAudio, true, null);
    }

    public static ProcessingResult error(String errorMessage) {
      return new ProcessingResult(null, null, null, false, errorMessage);
    }
  }
}
