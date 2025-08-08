package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

class ConversationalAITest {

  @Test
  void testBuilderRequiresModel() {
    // Given: Builder without model configuration
    var builder = ConversationalAI.builder();

    // When/Then: Build should fail
    var exception = assertThrows(IllegalStateException.class, builder::build);
    assertEquals(
        "Model must be configured. Use withOllamaModel() or withModel()", exception.getMessage());
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
    var builder =
        ConversationalAI.builder().withOllamaModel("llama2", "http://custom:11434");

    // When: Build ConversationalAI
    var ai = builder.build();

    // Then: Should create successfully
    assertNotNull(ai);
    assertNotNull(ai.getModel());
  }

  @Test
  void testBuilderWithMemory() {
    // Given: Builder with memory configuration
    var builder =
        ConversationalAI.builder().withOllamaModel("llama2").withMemory(5);

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
    var builder =
        ConversationalAI.builder().withOllamaModel("llama2").withTemperature(0.5);

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

  // Note: Testing actual chat() method would require a real Ollama instance
  // In integration tests, we would test with a real model
  // Here we focus on builder pattern validation and configuration
}
