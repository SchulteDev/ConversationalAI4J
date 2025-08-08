package schultedev.conversationalai4j;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * Utility class for creating conversation memory configurations. Provides convenient factory
 * methods for different memory strategies.
 */
public class ConversationMemory {

  private ConversationMemory() {
    // Utility class
  }

  /**
   * Create sliding window memory that keeps the last N messages
   *
   * @param maxMessages Maximum number of messages to keep
   * @return MessageWindowChatMemory configured with the specified window size
   */
  public static MessageWindowChatMemory sliding(int maxMessages) {
    if (maxMessages <= 0) {
      throw new IllegalArgumentException("maxMessages must be positive");
    }
    return MessageWindowChatMemory.withMaxMessages(maxMessages);
  }


  /**
   * Create default memory with 10 message sliding window
   *
   * @return MessageWindowChatMemory with default configuration
   */
  public static MessageWindowChatMemory defaultMemory() {
    return sliding(10);
  }

}
