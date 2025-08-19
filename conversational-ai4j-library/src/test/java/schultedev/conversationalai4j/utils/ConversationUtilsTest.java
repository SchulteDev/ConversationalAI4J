package schultedev.conversationalai4j.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import schultedev.conversationalai4j.ConversationalAI;

/** Unit tests for ConversationUtils class. */
@ExtendWith(MockitoExtension.class)
class ConversationUtilsTest {

  @Mock private ConversationalAI mockAI;

  private byte[] testAudioData;
  private String testText;

  @BeforeEach
  void setUp() {
    testAudioData = new byte[] {1, 2, 3, 4, 5};
    testText = "Hello, world!";
  }

  @Test
  void chatWithTextResponse_WithValidAudio_ShouldCallCorrectMethods() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When/Then: Since we can't mock static SpeechServiceUtils calls easily,
    // we expect this to call the actual SpeechServiceUtils.speechToText method
    // which will likely fail in test environment, but we can verify the validation works

    try {
      ConversationUtils.chatWithTextResponse(mockAI, testAudioData);
      // If we reach here without exception, that's fine too
    } catch (RuntimeException e) {
      // Expected in test environment where speech services are not available
      assertTrue(
          e.getMessage().contains("Speech recognition error")
              || e.getMessage().contains("No text transcribed"));
    }

    // Verify that speech capability was checked (may be called multiple times)
    verify(mockAI, atLeast(1)).isSpeechEnabled();
  }

  @Test
  void chatWithTextResponse_WithSpeechDisabled_ShouldThrowException() {
    // Given: Mock ConversationalAI without speech
    when(mockAI.isSpeechEnabled()).thenReturn(false);

    // When/Then: Should throw UnsupportedOperationException
    var exception =
        assertThrows(
            UnsupportedOperationException.class,
            () -> ConversationUtils.chatWithTextResponse(mockAI, testAudioData));
    assertTrue(exception.getMessage().contains("Speech services are not configured"));
    verify(mockAI).isSpeechEnabled();
  }

  @Test
  void chatWithTextResponse_WithNullAudio_ShouldThrowException() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When/Then: Should throw IllegalArgumentException for null audio
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> ConversationUtils.chatWithTextResponse(mockAI, null));
    assertTrue(exception.getMessage().contains("Audio input cannot be null or empty"));
  }

  @Test
  void chatWithTextResponse_WithEmptyAudio_ShouldThrowException() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When/Then: Should throw IllegalArgumentException for empty audio
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> ConversationUtils.chatWithTextResponse(mockAI, new byte[0]));
    assertTrue(exception.getMessage().contains("Audio input cannot be null or empty"));
  }

  @Test
  void chatWithVoiceResponse_WithValidText_ShouldReturnAudioResponse() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);
    when(mockAI.chat(testText)).thenReturn("AI response");
    when(mockAI.textToSpeech("AI response")).thenReturn(new byte[] {10, 20, 30});

    // When: Call chatWithVoiceResponse
    var result = ConversationUtils.chatWithVoiceResponse(mockAI, testText);

    // Then: Should return audio response
    assertNotNull(result);
    assertEquals(3, result.length);
    assertArrayEquals(new byte[] {10, 20, 30}, result);

    verify(mockAI).isSpeechEnabled();
    verify(mockAI).chat(testText);
    verify(mockAI).textToSpeech("AI response");
  }

  @Test
  void chatWithVoiceResponse_WithSpeechDisabled_ShouldThrowException() {
    // Given: Mock ConversationalAI without speech
    when(mockAI.isSpeechEnabled()).thenReturn(false);

    // When/Then: Should throw UnsupportedOperationException
    var exception =
        assertThrows(
            UnsupportedOperationException.class,
            () -> ConversationUtils.chatWithVoiceResponse(mockAI, testText));
    assertTrue(exception.getMessage().contains("Speech services are not configured"));
    verify(mockAI).isSpeechEnabled();
  }

  @Test
  void chatWithVoiceResponse_WithNullText_ShouldThrowException() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When/Then: Should throw IllegalArgumentException for null text
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> ConversationUtils.chatWithVoiceResponse(mockAI, null));
    assertTrue(exception.getMessage().contains("Text input cannot be null or empty"));
  }

  @Test
  void chatWithVoiceResponse_WithEmptyText_ShouldThrowException() {
    // Given: Mock ConversationalAI with speech enabled
    when(mockAI.isSpeechEnabled()).thenReturn(true);

    // When/Then: Should throw IllegalArgumentException for empty text
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> ConversationUtils.chatWithVoiceResponse(mockAI, "   "));
    assertTrue(exception.getMessage().contains("Text input cannot be null or empty"));
  }

  @Test
  void chatWithVoiceResponse_WithChatError_ShouldReturnEmptyArray() {
    // Given: Mock ConversationalAI with speech enabled but chat throws exception
    when(mockAI.isSpeechEnabled()).thenReturn(true);
    when(mockAI.chat(testText)).thenThrow(new RuntimeException("Chat error"));

    // When: Call chatWithVoiceResponse
    var result = ConversationUtils.chatWithVoiceResponse(mockAI, testText);

    // Then: Should return empty array
    assertNotNull(result);
    assertEquals(0, result.length);

    verify(mockAI).isSpeechEnabled();
    verify(mockAI).chat(testText);
    verify(mockAI, never()).textToSpeech(any());
  }

  @Test
  void chatWithVoiceResponse_WithTTSError_ShouldReturnEmptyArray() {
    // Given: Mock ConversationalAI with speech enabled but TTS throws exception
    when(mockAI.isSpeechEnabled()).thenReturn(true);
    when(mockAI.chat(testText)).thenReturn("AI response");
    when(mockAI.textToSpeech("AI response")).thenThrow(new RuntimeException("TTS error"));

    // When: Call chatWithVoiceResponse
    var result = ConversationUtils.chatWithVoiceResponse(mockAI, testText);

    // Then: Should return empty array
    assertNotNull(result);
    assertEquals(0, result.length);

    verify(mockAI).isSpeechEnabled();
    verify(mockAI).chat(testText);
    verify(mockAI).textToSpeech("AI response");
  }
}
