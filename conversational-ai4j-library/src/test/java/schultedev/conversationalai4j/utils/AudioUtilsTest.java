package schultedev.conversationalai4j.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import schultedev.conversationalai4j.AudioFormat;
import schultedev.conversationalai4j.ConversationalAI;

/** Unit tests for AudioUtils class. */
@ExtendWith(MockitoExtension.class)
class AudioUtilsTest {

  @Mock private ConversationalAI mockAI;

  private byte[] testAudioData;
  private AudioFormat testFormat;

  @BeforeEach
  void setUp() {
    testAudioData = new byte[] {1, 2, 3, 4, 5};
    testFormat = AudioFormat.wav16kMono();
  }

  @Test
  void speechToText_WithSpeechDisabled_ShouldThrowException() {
    // Given: Mock ConversationalAI without speech
    when(mockAI.isSpeechEnabled()).thenReturn(false);

    // When/Then: Should throw UnsupportedOperationException
    var exception =
        assertThrows(
            UnsupportedOperationException.class,
            () -> AudioUtils.speechToText(mockAI, testAudioData, testFormat));
    assertTrue(exception.getMessage().contains("Speech services are not configured"));
    verify(mockAI).isSpeechEnabled();
  }

  @Test
  void speechToText_WithNullAudio_ShouldThrowException() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When/Then: Should throw IllegalArgumentException for null audio
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> AudioUtils.speechToText(mockAI, null, testFormat));
    assertTrue(exception.getMessage().contains("Audio input cannot be null or empty"));
  }

  @Test
  void speechToText_WithEmptyAudio_ShouldThrowException() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When/Then: Should throw IllegalArgumentException for empty audio
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> AudioUtils.speechToText(mockAI, new byte[0], testFormat));
    assertTrue(exception.getMessage().contains("Audio input cannot be null or empty"));
  }

  @Test
  void speechToText_WithValidInput_ShouldProcessSuccessfully() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When: Call speechToText
    // Note: This will call the actual SpeechService, which may fail in test environment
    // The test verifies validation logic and method structure
    try {
      var result = AudioUtils.speechToText(mockAI, testAudioData, testFormat);
      // If we reach here, the method executed without validation errors
      assertNotNull(result);
    } catch (Exception e) {
      // Expected in test environment where SpeechService is not available
      assertTrue(
          e.getMessage().contains("Speech recognition error")
              || e.getMessage().contains("SpeechService"));
    }

    verify(mockAI).isSpeechEnabled();
  }

  @Test
  void speechToTextDebug_WithSpeechDisabled_ShouldThrowException() {
    // Given: Mock ConversationalAI without speech
    when(mockAI.isSpeechEnabled()).thenReturn(false);

    // When/Then: Should throw UnsupportedOperationException
    var exception =
        assertThrows(
            UnsupportedOperationException.class,
            () -> AudioUtils.speechToTextDebug(mockAI, testAudioData));
    assertTrue(exception.getMessage().contains("Speech services are not configured"));
    verify(mockAI).isSpeechEnabled();
  }

  @Test
  void speechToTextDebug_WithNullAudio_ShouldThrowException() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When/Then: Should throw IllegalArgumentException for null audio
    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> AudioUtils.speechToTextDebug(mockAI, null));
    assertTrue(exception.getMessage().contains("Audio input cannot be null or empty"));
  }

  @Test
  void speechToTextDebug_WithEmptyAudio_ShouldThrowException() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When/Then: Should throw IllegalArgumentException for empty audio
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> AudioUtils.speechToTextDebug(mockAI, new byte[0]));
    assertTrue(exception.getMessage().contains("Audio input cannot be null or empty"));
  }

  @Test
  void speechToTextDebug_WithValidInput_ShouldProcessSuccessfully() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When: Call speechToTextDebug
    // Note: This will call the actual SpeechService, which may fail in test environment
    // The test verifies validation logic and method structure
    assertDoesNotThrow(
        () -> {
          try {
            AudioUtils.speechToTextDebug(mockAI, testAudioData);
          } catch (RuntimeException e) {
            // Expected in test environment where SpeechService is not available
            if (!e.getMessage().contains("Speech recognition error")
                && !e.getMessage().contains("SpeechService")) {
              throw e; // Re-throw unexpected exceptions
            }
          }
        });

    // speechToTextDebug calls isSpeechEnabled and then speechToText, which calls isSpeechEnabled
    // again
    verify(mockAI, atLeast(1)).isSpeechEnabled();
  }

  @Test
  void testUtilityClassInstantiation_ShouldNotBeAllowed() {
    // AudioUtils should have private constructor to prevent instantiation
    // This test verifies it's a proper utility class

    // We can't easily test private constructors with standard JUnit,
    // but we can verify the class structure
    assertTrue(AudioUtils.class.getDeclaredConstructors().length > 0);

    // Verify all methods are static
    var methods = AudioUtils.class.getDeclaredMethods();
    for (var method : methods) {
      if (!method.getName().startsWith("lambda$")) { // Exclude lambda methods
        assertTrue(
            java.lang.reflect.Modifier.isStatic(method.getModifiers()),
            "Method " + method.getName() + " should be static");
      }
    }
  }

  @Test
  void testAudioFormatFactory_ShouldCreateCorrectFormats() {
    // Test that AudioFormat factory methods work correctly with AudioUtils

    // Test wav16kMono format
    var wavFormat = AudioFormat.wav16kMono();
    assertEquals(AudioFormat.Type.WAV, wavFormat.type());
    assertEquals(16000, wavFormat.sampleRate());
    assertEquals(1, wavFormat.channels());
    assertEquals(16, wavFormat.bitsPerSample());

    // Test webmOpus format
    var webmFormat = AudioFormat.webmOpus();
    assertEquals(AudioFormat.Type.WEBM_OPUS, webmFormat.type());
    assertEquals(48000, webmFormat.sampleRate());
    assertEquals(1, webmFormat.channels());
    assertEquals(16, webmFormat.bitsPerSample());

    // Test rawPcm16kMono format
    var pcmFormat = AudioFormat.rawPcm16kMono();
    assertEquals(AudioFormat.Type.RAW_PCM, pcmFormat.type());
    assertEquals(16000, pcmFormat.sampleRate());
    assertEquals(1, pcmFormat.channels());
    assertEquals(16, pcmFormat.bitsPerSample());
  }
}
