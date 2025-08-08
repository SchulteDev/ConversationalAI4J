package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Test;

class ConversationMemoryTest {

  @Test
  void testSlidingMemory() {
    // When: Create sliding memory
    MessageWindowChatMemory memory = ConversationMemory.sliding(5);

    // Then: Should create successfully
    assertNotNull(memory);
  }

  @Test
  void testSlidingMemoryWithInvalidSize() {
    // When/Then: Should throw exception for invalid size
    IllegalArgumentException exception1 =
        assertThrows(IllegalArgumentException.class, () -> ConversationMemory.sliding(0));
    assertEquals("maxMessages must be positive", exception1.getMessage());

    IllegalArgumentException exception2 =
        assertThrows(IllegalArgumentException.class, () -> ConversationMemory.sliding(-1));
    assertEquals("maxMessages must be positive", exception2.getMessage());
  }


  @Test
  void testDefaultMemory() {
    // When: Create default memory
    MessageWindowChatMemory memory = ConversationMemory.defaultMemory();

    // Then: Should create successfully
    assertNotNull(memory);
  }

}
