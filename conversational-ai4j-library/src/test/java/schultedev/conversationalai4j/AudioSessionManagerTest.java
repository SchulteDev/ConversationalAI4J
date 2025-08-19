package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for AudioSessionManager session state management. */
class AudioSessionManagerTest {

  private AudioSessionManager sessionManager;
  private static final String TEST_SESSION_ID = "test-session-123";

  @BeforeEach
  void setUp() {
    sessionManager = new AudioSessionManager();
  }

  @Test
  void initializeSession_ShouldSetupDefaultState() {
    // When
    sessionManager.initializeSession(TEST_SESSION_ID);

    // Then
    assertFalse(sessionManager.isRecording(TEST_SESSION_ID));
    assertNotNull(sessionManager.getSessionFormat(TEST_SESSION_ID));
    assertNotNull(sessionManager.getAudioChunks(TEST_SESSION_ID));
    assertTrue(sessionManager.getAudioChunks(TEST_SESSION_ID).isEmpty());
  }

  @Test
  void startRecording_ShouldEnableRecordingAndClearChunks() {
    // Given
    sessionManager.initializeSession(TEST_SESSION_ID);
    sessionManager.addAudioChunk(TEST_SESSION_ID, new byte[] {1, 2, 3});

    // When
    sessionManager.startRecording(TEST_SESSION_ID);

    // Then
    assertTrue(sessionManager.isRecording(TEST_SESSION_ID));
    assertTrue(sessionManager.getAudioChunks(TEST_SESSION_ID).isEmpty());
  }

  @Test
  void stopRecording_ShouldDisableRecording() {
    // Given
    sessionManager.initializeSession(TEST_SESSION_ID);
    sessionManager.startRecording(TEST_SESSION_ID);

    // When
    sessionManager.stopRecording(TEST_SESSION_ID);

    // Then
    assertFalse(sessionManager.isRecording(TEST_SESSION_ID));
  }

  @Test
  void addAudioChunk_WithValidSession_ShouldAddChunk() {
    // Given
    sessionManager.initializeSession(TEST_SESSION_ID);
    sessionManager.startRecording(TEST_SESSION_ID);
    var audioData = createMockWavData(256);

    // When
    var result = sessionManager.addAudioChunk(TEST_SESSION_ID, audioData);

    // Then
    assertTrue(result);
    assertEquals(1, sessionManager.getAudioChunks(TEST_SESSION_ID).size());
    assertArrayEquals(audioData, sessionManager.getAudioChunks(TEST_SESSION_ID).get(0));
  }

  @Test
  void addAudioChunk_WithInvalidSession_ShouldReturnFalse() {
    // When
    var result = sessionManager.addAudioChunk("invalid-session", new byte[] {1, 2, 3});

    // Then
    assertFalse(result);
  }

  @Test
  void addAudioChunk_DetectsFormatOnFirstChunk() {
    // Given
    sessionManager.initializeSession(TEST_SESSION_ID);
    sessionManager.startRecording(TEST_SESSION_ID);
    var wavData = createMockWavData(256);

    // When
    sessionManager.addAudioChunk(TEST_SESSION_ID, wavData);

    // Then
    var format = sessionManager.getSessionFormat(TEST_SESSION_ID);
    assertNotNull(format);
  }

  @Test
  void setSessionFormat_ShouldUpdateFormat() {
    // Given
    sessionManager.initializeSession(TEST_SESSION_ID);
    var newFormat = AudioFormat.wav16kMono();

    // When
    sessionManager.setSessionFormat(TEST_SESSION_ID, newFormat);

    // Then
    assertEquals(newFormat, sessionManager.getSessionFormat(TEST_SESSION_ID));
  }

  @Test
  void clearAudioChunks_ShouldRemoveAllChunks() {
    // Given
    sessionManager.initializeSession(TEST_SESSION_ID);
    sessionManager.addAudioChunk(TEST_SESSION_ID, new byte[] {1, 2, 3});
    sessionManager.addAudioChunk(TEST_SESSION_ID, new byte[] {4, 5, 6});

    // When
    sessionManager.clearAudioChunks(TEST_SESSION_ID);

    // Then
    assertTrue(sessionManager.getAudioChunks(TEST_SESSION_ID).isEmpty());
  }

  @Test
  void removeSession_ShouldCleanupAllData() {
    // Given
    sessionManager.initializeSession(TEST_SESSION_ID);
    sessionManager.startRecording(TEST_SESSION_ID);
    sessionManager.addAudioChunk(TEST_SESSION_ID, new byte[] {1, 2, 3});

    // When
    sessionManager.removeSession(TEST_SESSION_ID);

    // Then
    assertFalse(sessionManager.isRecording(TEST_SESSION_ID));
    assertNull(sessionManager.getSessionFormat(TEST_SESSION_ID));
    assertNull(sessionManager.getAudioChunks(TEST_SESSION_ID));
  }

  @Test
  void addAudioChunk_ExceedsChunkLimit_ShouldRejectChunk() {
    // Given
    sessionManager.initializeSession(TEST_SESSION_ID);

    // When - Add chunks until limit is reached
    for (var i = 0; i < 1000; i++) {
      sessionManager.addAudioChunk(TEST_SESSION_ID, new byte[] {(byte) i});
    }

    // Then - Next chunk should be rejected
    var result = sessionManager.addAudioChunk(TEST_SESSION_ID, new byte[] {101});
    assertFalse(result);
    assertEquals(1000, sessionManager.getAudioChunks(TEST_SESSION_ID).size());
  }

  @Test
  void addAudioChunk_ExceedsSizeLimit_ShouldRejectChunk() {
    // Given
    sessionManager.initializeSession(TEST_SESSION_ID);
    var largeChunk = new byte[6 * 1024 * 1024]; // 6MB (so two chunks = 12MB > 10MB limit)

    // When - Add two large chunks (should exceed 10MB limit)
    var firstResult = sessionManager.addAudioChunk(TEST_SESSION_ID, largeChunk);
    var secondResult = sessionManager.addAudioChunk(TEST_SESSION_ID, largeChunk);

    // Then - First chunk should be accepted, second should be rejected
    assertTrue(firstResult);
    assertFalse(secondResult);
    assertEquals(1, sessionManager.getAudioChunks(TEST_SESSION_ID).size());
  }

  @Test
  void multipleSessionsOperateIndependently() {
    // Given
    var session1 = "session-1";
    var session2 = "session-2";
    sessionManager.initializeSession(session1);
    sessionManager.initializeSession(session2);

    // When
    sessionManager.startRecording(session1);
    sessionManager.addAudioChunk(session1, new byte[] {1, 2, 3});

    // Then
    assertTrue(sessionManager.isRecording(session1));
    assertFalse(sessionManager.isRecording(session2));
    assertEquals(1, sessionManager.getAudioChunks(session1).size());
    assertTrue(sessionManager.getAudioChunks(session2).isEmpty());
  }

  private byte[] createMockWavData(int sizeInBytes) {
    // Create minimal valid WAV header + data
    var wavData = new byte[Math.max(44, sizeInBytes)];

    // WAV header
    System.arraycopy("RIFF".getBytes(), 0, wavData, 0, 4);
    writeInt32LE(wavData, 4, wavData.length - 8);
    System.arraycopy("WAVE".getBytes(), 0, wavData, 8, 4);
    System.arraycopy("fmt ".getBytes(), 0, wavData, 12, 4);
    writeInt32LE(wavData, 16, 16);
    writeInt16LE(wavData, 20, (short) 1);
    writeInt16LE(wavData, 22, (short) 1);
    writeInt32LE(wavData, 24, 16000);
    writeInt32LE(wavData, 28, 32000);
    writeInt16LE(wavData, 32, (short) 2);
    writeInt16LE(wavData, 34, (short) 16);
    System.arraycopy("data".getBytes(), 0, wavData, 36, 4);
    writeInt32LE(wavData, 40, wavData.length - 44);

    return wavData;
  }

  private void writeInt32LE(byte[] data, int offset, int value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    data[offset + 2] = (byte) ((value >> 16) & 0xFF);
    data[offset + 3] = (byte) ((value >> 24) & 0xFF);
  }

  private void writeInt16LE(byte[] data, int offset, short value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
  }
}
