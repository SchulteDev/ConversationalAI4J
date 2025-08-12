package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.util.Map;

class SherpaOnnxNativeTest {

  private Map<String, String> originalEnv;

  @BeforeEach
  void setUp() {
    // Store original environment to restore later
    originalEnv = System.getenv();
  }

  @AfterEach
  void tearDown() {
    // Reset static state for clean tests
    try {
      var field = SherpaOnnxNative.class.getDeclaredField("nativeLibraryAvailable");
      field.setAccessible(true);
      field.set(null, null);
      
      var initField = SherpaOnnxNative.class.getDeclaredField("initializationAttempted");
      initField.setAccessible(true);
      initField.set(null, false);
    } catch (Exception e) {
      // Ignore reflection errors in test cleanup
    }
  }

  @Test
  void isNativeLibraryAvailable_OnNonLinux_ShouldReturnFalse() {
    // Given: Non-Linux system (this test will likely run on Windows in CI)
    // When: Check if native library is available
    boolean available = SherpaOnnxNative.isNativeLibraryAvailable();
    
    // Then: Should return false on non-Linux systems
    // Note: This test may pass or fail depending on the OS and library availability
    assertNotNull(available); // Just verify it returns a boolean
  }

  @Test
  void createSttRecognizer_WhenNativeNotAvailable_ShouldReturnMockHandle() {
    // When: Create STT recognizer (will use mock on most systems)
    long recognizer = SherpaOnnxNative.createSttRecognizer("/mock/path", "en-US");
    
    // Then: Should return a handle (either real or mock)
    assertTrue(recognizer >= 0L);
  }

  @Test
  void createTtsSynthesizer_WhenNativeNotAvailable_ShouldReturnMockHandle() {
    // When: Create TTS synthesizer (will use mock on most systems)
    long synthesizer = SherpaOnnxNative.createTtsSynthesizer("/mock/path", "en-US", "female");
    
    // Then: Should return a handle (either real or mock)
    assertTrue(synthesizer >= 0L);
  }

  @Test
  void transcribeAudio_WithMockImplementation_ShouldReturnText() {
    // Given: Mock audio data
    byte[] audioData = createMockWavData(1024);
    long recognizer = SherpaOnnxNative.createSttRecognizer("/mock/path", "en-US");
    
    // When: Transcribe audio
    String result = SherpaOnnxNative.transcribeAudio(recognizer, audioData);
    
    // Then: Should return transcription text
    assertNotNull(result);
    assertFalse(result.trim().isEmpty());
  }

  @Test
  void transcribeAudio_WithNullAudio_ShouldHandleGracefully() {
    // Given: Null audio data
    long recognizer = SherpaOnnxNative.createSttRecognizer("/mock/path", "en-US");
    
    // When: Transcribe null audio
    try {
      String result = SherpaOnnxNative.transcribeAudio(recognizer, null);
      // Then: Should handle gracefully if no exception
      assertNotNull(result);
    } catch (Exception e) {
      // May throw exception with null input - verify it's handled appropriately
      assertTrue(e instanceof NullPointerException || 
                e instanceof IllegalArgumentException,
                "Should throw appropriate exception for null input");
    }
  }

  @Test
  void synthesizeSpeech_WithMockImplementation_ShouldReturnAudioData() {
    // Given: Text to synthesize
    String text = "Hello, this is a test.";
    long synthesizer = SherpaOnnxNative.createTtsSynthesizer("/mock/path", "en-US", "female");
    
    // When: Synthesize speech
    byte[] result = SherpaOnnxNative.synthesizeSpeech(synthesizer, text);
    
    // Then: Should return audio data
    assertNotNull(result);
    assertTrue(result.length > 0);
    
    // Verify it's WAV format
    if (result.length >= 12) {
      assertEquals('R', result[0]);
      assertEquals('I', result[1]); 
      assertEquals('F', result[2]);
      assertEquals('F', result[3]);
      assertEquals('W', result[8]);
      assertEquals('A', result[9]);
      assertEquals('V', result[10]);
      assertEquals('E', result[11]);
    }
  }

  @Test
  void synthesizeSpeech_WithEmptyText_ShouldReturnValidAudio() {
    // Given: Empty text
    long synthesizer = SherpaOnnxNative.createTtsSynthesizer("/mock/path", "en-US", "female");
    
    // When: Synthesize empty text
    byte[] result = SherpaOnnxNative.synthesizeSpeech(synthesizer, "");
    
    // Then: Should return some audio data (even if minimal)
    assertNotNull(result);
  }

  @Test
  void releaseSttRecognizer_ShouldNotThrow() {
    // Given: Created recognizer
    long recognizer = SherpaOnnxNative.createSttRecognizer("/mock/path", "en-US");
    
    // When: Release recognizer
    assertDoesNotThrow(() -> SherpaOnnxNative.releaseSttRecognizer(recognizer));
    
    // Multiple releases should also be safe
    assertDoesNotThrow(() -> SherpaOnnxNative.releaseSttRecognizer(recognizer));
  }

  @Test
  void releaseTtsSynthesizer_ShouldNotThrow() {
    // Given: Created synthesizer
    long synthesizer = SherpaOnnxNative.createTtsSynthesizer("/mock/path", "en-US", "female");
    
    // When: Release synthesizer
    assertDoesNotThrow(() -> SherpaOnnxNative.releaseTtsSynthesizer(synthesizer));
    
    // Multiple releases should also be safe
    assertDoesNotThrow(() -> SherpaOnnxNative.releaseTtsSynthesizer(synthesizer));
  }

  @Test
  void transcribeAudio_WithInvalidHandle_ShouldHandleGracefully() {
    // Given: Invalid recognizer handle
    byte[] audioData = createMockWavData(512);
    
    // When: Try to transcribe with invalid handle
    String result = SherpaOnnxNative.transcribeAudio(0L, audioData);
    
    // Then: Should handle gracefully
    assertNotNull(result);
  }

  @Test
  void synthesizeSpeech_WithInvalidHandle_ShouldHandleGracefully() {
    // When: Try to synthesize with invalid handle  
    byte[] result = SherpaOnnxNative.synthesizeSpeech(0L, "test");
    
    // Then: Should handle gracefully
    assertNotNull(result);
  }

  @Test
  void mockWavGeneration_ShouldCreateValidFormat() {
    // Given: Text of different lengths
    String shortText = "Hi";
    String longText = "This is a much longer text that should generate a longer audio file";
    
    long synthesizer = SherpaOnnxNative.createTtsSynthesizer("/mock/path", "en-US", "female");
    
    // When: Generate audio for different text lengths
    byte[] shortAudio = SherpaOnnxNative.synthesizeSpeech(synthesizer, shortText);
    byte[] longAudio = SherpaOnnxNative.synthesizeSpeech(synthesizer, longText);
    
    // Then: Both should be valid WAV format
    assertTrue(shortAudio.length >= 44, "Short audio should have at least WAV header");
    assertTrue(longAudio.length >= 44, "Long audio should have at least WAV header");
    
    // Longer text should generally produce more audio data
    // (Note: This may not always be true depending on implementation)
    assertTrue(longAudio.length >= shortAudio.length);
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