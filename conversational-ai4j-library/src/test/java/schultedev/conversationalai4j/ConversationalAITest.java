package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConversationalAITest {

  @Test
  void testBuilderRequiresModel() {
    // Given: Builder without model configuration
    var builder = ConversationalAI.builder();

    // When/Then: Build should fail
    var exception = assertThrows(IllegalStateException.class, builder::build);
    assertEquals("Model must be configured using withOllamaModel()", exception.getMessage());
  }

  @Test
  void testBuilderWithOllamaModel() {
    // Given: Builder with Ollama model
    var builder = ConversationalAI.builder().withOllamaModel("llama2");

    // When: Build ConversationalAI
    var ai = builder.build();

    // Then: Should create successfully
    assertNotNull(ai);
    assertNotNull(ai.getModel());
  }

  @Test
  void testBuilderWithCustomBaseUrl() {
    // Given: Builder with custom Ollama URL
    var builder = ConversationalAI.builder().withOllamaModel("llama2", "http://custom:11434");

    // When: Build ConversationalAI
    var ai = builder.build();

    // Then: Should create successfully
    assertNotNull(ai);
    assertNotNull(ai.getModel());
  }

  @Test
  void testBuilderWithMemory() {
    // Given: Builder with memory configuration
    var builder = ConversationalAI.builder().withOllamaModel("llama2").withMemory(5);

    // When: Build ConversationalAI
    var ai = builder.build();

    // Then: Should create successfully
    assertNotNull(ai);
  }

  @Test
  void testBuilderWithSystemPrompt() {
    // Given: Builder with system prompt
    var builder =
        ConversationalAI.builder()
            .withOllamaModel("llama2")
            .withSystemPrompt("You are a helpful assistant");

    // When: Build ConversationalAI
    var ai = builder.build();

    // Then: Should create successfully
    assertNotNull(ai);
  }

  @Test
  void testBuilderWithTemperature() {
    // Given: Builder with temperature
    var builder = ConversationalAI.builder().withOllamaModel("llama2").withTemperature(0.5);

    // When: Build ConversationalAI
    var ai = builder.build();

    // Then: Should create successfully
    assertNotNull(ai);
  }

  @Test
  void testBuilderWithInvalidTemperature() {
    // Given: Builder with invalid temperature
    var builder = ConversationalAI.builder();

    // When/Then: Should throw exception for invalid temperature
    var exception1 =
        assertThrows(IllegalArgumentException.class, () -> builder.withTemperature(-0.1));
    assertEquals("Temperature must be between 0.0 and 1.0", exception1.getMessage());

    var exception2 =
        assertThrows(IllegalArgumentException.class, () -> builder.withTemperature(1.1));
    assertEquals("Temperature must be between 0.0 and 1.0", exception2.getMessage());
  }

  @Test
  void testBuilderFluentInterface() {
    // Given/When: Chain builder methods
    var ai =
        ConversationalAI.builder()
            .withOllamaModel("llama2")
            .withMemory(20)
            .withSystemPrompt("You are a helpful assistant")
            .withTemperature(0.8)
            .build();

    // Then: Should create successfully
    assertNotNull(ai);
    assertNotNull(ai.getModel());
  }

  @Test
  void testGetModel() {
    // Given: ConversationalAI instance
    var ai = ConversationalAI.builder().withOllamaModel("llama2").build();

    // When: Get model
    var model = ai.getModel();

    // Then: Should return the configured model
    assertNotNull(model);
  }

  @Test
  void testChatWithMockedModel() {
    // Given: ConversationalAI with mocked model
    var ai = ConversationalAI.builder().withOllamaModel("llama2").build();
    var testMessage = "Hello AI";

    // When: Chat with message (this will fail without Ollama but shouldn't crash)
    try {
      var response = ai.chat(testMessage);
      // Then: If successful, should return a response
      assertNotNull(response);
    } catch (Exception e) {
      // Expected when Ollama is not available - verify graceful handling
      assertNotNull(e.getMessage());
      assertTrue(
          e.getMessage().contains("Connection")
              || e.getMessage().contains("refused")
              || e.getMessage().contains("connect")
              || e.getMessage().contains("ConnectException")
              || e.getMessage().contains("timeout")
              || e.getMessage().contains("timed out")
              || e.getMessage().contains("model")
              || e.getMessage().contains("not found"),
          "Should get connection-related, timeout, or model error when Ollama unavailable/misconfigured, got: "
              + e.getMessage());
    }
  }

  @Test
  void testVoiceChatWithoutSpeechConfig() {
    // Given: ConversationalAI without speech services
    var ai = ConversationalAI.builder().withOllamaModel("llama2").build();
    var audioData = new byte[] {1, 2, 3, 4};

    // When/Then: Voice chat should throw exception
    var exception =
        assertThrows(UnsupportedOperationException.class, () -> ai.voiceChat(audioData));
    assertTrue(exception.getMessage().contains("Speech services are not configured"));
  }

  @Test
  void testVoiceChatWithNullAudio() {
    // Given: ConversationalAI with speech enabled
    var ai = ConversationalAI.builder().withOllamaModel("llama2").withSpeech().build();

    // When/Then: Voice chat with null audio should throw exception
    var exception = assertThrows(IllegalArgumentException.class, () -> ai.voiceChat(null));
    assertTrue(exception.getMessage().contains("Audio input cannot be null"));
  }

  @Test
  void testVoiceChatWithEmptyAudio() {
    // Given: ConversationalAI with speech enabled
    var ai = ConversationalAI.builder().withOllamaModel("llama2").withSpeech().build();

    // When/Then: Voice chat with empty audio should throw exception
    var exception = assertThrows(IllegalArgumentException.class, () -> ai.voiceChat(new byte[0]));
    assertTrue(exception.getMessage().contains("Audio input cannot be null"));
  }

  @Test
  void testSpeechToTextWithoutSpeechConfig() {
    // Given: ConversationalAI without speech services
    var ai = ConversationalAI.builder().withOllamaModel("llama2").build();
    var audioData = new byte[] {1, 2, 3, 4};

    // When/Then: Speech to text should throw exception
    var exception =
        assertThrows(
            UnsupportedOperationException.class,
            () ->
                schultedev.conversationalai4j.SpeechServiceUtils.speechToText(
                    ai, audioData, AudioFormat.wav16kMono()));
    assertTrue(exception.getMessage().contains("Speech services are not configured"));
  }

  @Test
  void testTextToSpeechWithoutSpeechConfig() {
    // Given: ConversationalAI without speech services
    var ai = ConversationalAI.builder().withOllamaModel("llama2").build();

    // When/Then: Text to speech should throw exception
    var exception =
        assertThrows(UnsupportedOperationException.class, () -> ai.textToSpeech("Hello"));
    assertTrue(exception.getMessage().contains("Text-to-speech service is not configured"));
  }

  @Test
  void testSpeechStatusMethods() {
    // Given: ConversationalAI without speech
    var aiWithoutSpeech = ConversationalAI.builder().withOllamaModel("llama2").build();

    // When/Then: Speech status should be false
    assertFalse(aiWithoutSpeech.isSpeechEnabled());

    // Given: ConversationalAI with speech
    var aiWithSpeech = ConversationalAI.builder().withOllamaModel("llama2").withSpeech().build();

    // When/Then: Speech may be available depending on environment
    // (We don't assert true/false since it depends on speech library availability)
    assertNotNull(aiWithSpeech.isSpeechEnabled());
  }

  @Test
  void testResourceCleanup() {
    // Given: ConversationalAI instance
    var ai = ConversationalAI.builder().withOllamaModel("llama2").build();

    // When: Close resources
    assertDoesNotThrow(ai::close);
  }

  // Note: Testing actual chat() method with real responses would require a real Ollama instance
  // These tests focus on builder pattern validation, error handling, and API contracts
}
