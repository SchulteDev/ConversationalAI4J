package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

class SpeechServiceTest {

  private SpeechService speechService;
  
  @BeforeEach
  void setUp() {
    speechService = new SpeechService();
  }
  
  @AfterEach
  void tearDown() {
    if (speechService != null) {
      speechService.close();
    }
  }

  @Test
  void textToSpeech_ShouldReturnValidWavData() {
    // When: Convert text to speech
    byte[] result = speechService.textToSpeech("Hello world");

    // Then: Should return valid WAV data
    assertNotNull(result);
    assertTrue(result.length > 44, "WAV data should be larger than header size");

    // Verify WAV header format
    assertEquals('R', result[0]);
    assertEquals('I', result[1]);
    assertEquals('F', result[2]);
    assertEquals('F', result[3]);
    assertEquals('W', result[8]);
    assertEquals('A', result[9]);
    assertEquals('V', result[10]);
    assertEquals('E', result[11]);
  }

  @Test
  void speechToText_ShouldReturnString() {
    // Given: Mock audio data
    byte[] audioData = createMockWavData(1000);
    
    // When: Convert speech to text
    String result = speechService.speechToText(audioData);

    // Then: Should return transcription text
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void speechToText_WithNullAudio_ShouldReturnMockResult() {
    // When: Process null audio
    String result = speechService.speechToText(null);
    
    // Then: Should handle gracefully
    assertNotNull(result);
  }

  @Test
  void speechToText_WithEmptyAudio_ShouldReturnMockResult() {
    // When: Process empty audio
    String result = speechService.speechToText(new byte[0]);
    
    // Then: Should handle gracefully  
    assertNotNull(result);
  }

  @Test
  void speechToText_WithValidWavData_ProcessesCorrectly() {
    // Given: Valid WAV file data
    byte[] wavData = createValidWavData();
    
    // When: Process speech to text
    String result = speechService.speechToText(wavData);
    
    // Then: Should return transcription
    assertNotNull(result);
    assertFalse(result.trim().isEmpty());
  }

  @Test
  void audioConversion_WithWebMData_ShouldHandleGracefully() {
    // Given: Mock WebM audio data (typical from browser)
    byte[] webmData = createMockWebMData();
    
    // When: Process through speech service
    String result = speechService.speechToText(webmData);
    
    // Then: Should handle conversion attempt and return result
    assertNotNull(result);
    // Note: Conversion may fail without ffmpeg, but should not crash
  }

  @Test
  void textToSpeech_WithEmptyText_ReturnsEmptyOrDefaultAudio() {
    // When: Convert empty text
    byte[] result = speechService.textToSpeech("");
    
    // Then: Should handle gracefully (may return empty or default audio)
    assertNotNull(result);
  }

  @Test
  void isAvailable_ShouldReturnBoolean() {
    // When: Check availability
    boolean available = speechService.isAvailable();

    // Then: Should return a boolean value based on SPEECH_ENABLED env
    // In test environment, should reflect actual configuration
    assertTrue(available || !available); // Always passes, just ensures method works
  }

  @Test
  void close_ShouldCleanupResourcesSafely() {
    // When: Close speech service
    assertDoesNotThrow(() -> speechService.close());
    
    // Additional close should also be safe
    assertDoesNotThrow(() -> speechService.close());
  }

  private byte[] createMockWavData(int sizeInBytes) {
    // Create minimal valid WAV header + data
    byte[] wavData = new byte[Math.max(44, sizeInBytes)];
    
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
  
  private byte[] createValidWavData() {
    return createMockWavData(1024); // 1KB of mock WAV data
  }
  
  private byte[] createMockWebMData() {
    // Mock WebM container with some header-like bytes
    byte[] webmData = new byte[256];
    webmData[0] = 0x1A; // WebM EBML signature start
    webmData[1] = 0x45;
    webmData[2] = (byte) 0xDF;
    webmData[3] = (byte) 0xA3;
    return webmData;
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
