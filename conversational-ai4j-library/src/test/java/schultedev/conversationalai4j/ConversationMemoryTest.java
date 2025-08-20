package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConversationMemoryTest {

  @Test
  void testSlidingMemory() {
    // When: Create AI with custom memory size
    var builder = ConversationalAI.builder().withOllamaModel("test-model").withMemory(5);

    // Then: Should create successfully without throwing
    assertDoesNotThrow(builder::build);
  }

  @Test
  void testSlidingMemoryWithInvalidSize() {
    // When/Then: Should throw exception for invalid size during withMemory() call
    var builder1 = ConversationalAI.builder().withOllamaModel("test-model");
    var exception1 = assertThrows(IllegalArgumentException.class, () -> builder1.withMemory(0));
    assertEquals("maxMessages must be positive", exception1.getMessage());

    var builder2 = ConversationalAI.builder().withOllamaModel("test-model");
    var exception2 = assertThrows(IllegalArgumentException.class, () -> builder2.withMemory(-1));
    assertEquals("maxMessages must be positive", exception2.getMessage());
  }

  @Test
  void testDefaultMemory() {
    // When: Create AI with default memory
    var builder = ConversationalAI.builder().withOllamaModel("test-model").withMemory();

    // Then: Should create successfully without throwing
    assertDoesNotThrow(builder::build);
  }
}
