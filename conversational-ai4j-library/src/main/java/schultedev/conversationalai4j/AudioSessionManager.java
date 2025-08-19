package schultedev.conversationalai4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages audio recording sessions and their state. Tracks recording state, audio chunks, and
 * format information per session.
 *
 * <p>This is a pure Java class with no framework dependencies, making it reusable across different
 * application types (Spring Boot, plain Java, etc.).
 */
public class AudioSessionManager {

  private static final Logger log = LoggerFactory.getLogger(AudioSessionManager.class);

  // Performance and memory optimization constants
  private static final int MAX_AUDIO_CHUNKS_PER_SESSION = 1000;
  private static final int MAX_AUDIO_BYTES_PER_SESSION = 10 * 1024 * 1024; // 10MB limit

  private final ConcurrentHashMap<String, List<byte[]>> audioChunks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Boolean> recordingStates = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AudioFormat> sessionFormats = new ConcurrentHashMap<>();

  /** Initializes a new session with default values. */
  public void initializeSession(String sessionId) {
    log.info("Initializing audio session: {}", sessionId);
    audioChunks.put(sessionId, new ArrayList<>());
    recordingStates.put(sessionId, false);
    sessionFormats.put(sessionId, AudioFormat.wav16kMono()); // Default format
  }

  /** Starts recording for the specified session. */
  public void startRecording(String sessionId) {
    recordingStates.put(sessionId, true);
    var chunks = audioChunks.get(sessionId);
    if (chunks != null) {
      chunks.clear();
    }
    log.info("Started recording for session {}", sessionId);
  }

  /** Stops recording for the specified session. */
  public void stopRecording(String sessionId) {
    recordingStates.put(sessionId, false);
    log.info("Stopped recording for session {}", sessionId);
  }

  /** Checks if a session is currently recording. */
  public boolean isRecording(String sessionId) {
    return recordingStates.getOrDefault(sessionId, false);
  }

  /** Gets the audio format for a session. */
  public AudioFormat getSessionFormat(String sessionId) {
    return sessionFormats.get(sessionId);
  }

  /** Sets the audio format for a session. */
  public void setSessionFormat(String sessionId, AudioFormat format) {
    sessionFormats.put(sessionId, format);
    log.info("Set audio format for session {}: {}", sessionId, format);
  }

  /**
   * Adds an audio chunk to the session if recording and within limits. Returns true if the chunk
   * was added, false if it was rejected.
   */
  public boolean addAudioChunk(String sessionId, byte[] audioData) {
    var chunks = audioChunks.get(sessionId);
    if (chunks == null) {
      log.warn("No audio chunk list found for session {}", sessionId);
      return false;
    }

    // Memory protection - prevent excessive memory usage
    if (chunks.size() >= MAX_AUDIO_CHUNKS_PER_SESSION) {
      log.warn(
          "Session {} exceeded maximum audio chunks limit ({}), dropping chunk",
          sessionId,
          MAX_AUDIO_CHUNKS_PER_SESSION);
      return false;
    }

    var currentSize = chunks.stream().mapToInt(chunk -> chunk.length).sum();
    var incomingSize = audioData.length;

    if (currentSize + incomingSize > MAX_AUDIO_BYTES_PER_SESSION) {
      log.warn(
          "Session {} exceeded maximum audio size limit ({}MB), dropping chunk",
          sessionId,
          MAX_AUDIO_BYTES_PER_SESSION / (1024 * 1024));
      return false;
    }

    // Detect audio format on first chunk
    if (chunks.isEmpty()) {
      var detectedFormat = AudioFormat.detect(audioData);
      setSessionFormat(sessionId, detectedFormat);
    }

    chunks.add(audioData);
    log.trace(
        "Added audio chunk of {} bytes to session {} (total: {} chunks, {} bytes)",
        incomingSize,
        sessionId,
        chunks.size(),
        currentSize + incomingSize);
    return true;
  }

  /** Gets all audio chunks for a session. */
  public List<byte[]> getAudioChunks(String sessionId) {
    return audioChunks.get(sessionId);
  }

  /** Clears audio chunks for a session. */
  public void clearAudioChunks(String sessionId) {
    var chunks = audioChunks.get(sessionId);
    if (chunks != null) {
      chunks.clear();
    }
  }

  /** Removes all session data for cleanup. */
  public void removeSession(String sessionId) {
    log.info("Removing audio session: {}", sessionId);
    audioChunks.remove(sessionId);
    recordingStates.remove(sessionId);
    sessionFormats.remove(sessionId);
  }
}
