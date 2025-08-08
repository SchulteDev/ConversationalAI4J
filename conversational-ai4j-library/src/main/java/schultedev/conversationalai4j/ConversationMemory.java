package schultedev.conversationalai4j;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;

/**
 * Utility class for creating conversation memory configurations.
 * Provides convenient factory methods for different memory strategies.
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
     * Create token-based memory that keeps messages within a token limit
     * 
     * @param maxTokens Maximum number of tokens to keep
     * @param tokenizer Tokenizer to use for counting tokens
     * @return TokenWindowChatMemory configured with the specified token limit
     */
    public static TokenWindowChatMemory tokenBased(int maxTokens, TokenCountEstimator tokenizer) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        if (tokenizer == null) {
            throw new IllegalArgumentException("tokenizer cannot be null");
        }
        return TokenWindowChatMemory.withMaxTokens(maxTokens, tokenizer);
    }
    
    /**
     * Create default memory with 10 message sliding window
     * 
     * @return MessageWindowChatMemory with default configuration
     */
    public static MessageWindowChatMemory defaultMemory() {
        return sliding(10);
    }
    
    /**
     * Create short-term memory with 5 message sliding window
     * 
     * @return MessageWindowChatMemory for short conversations
     */
    public static MessageWindowChatMemory shortTerm() {
        return sliding(5);
    }
    
    /**
     * Create long-term memory with 50 message sliding window
     * 
     * @return MessageWindowChatMemory for long conversations
     */
    public static MessageWindowChatMemory longTerm() {
        return sliding(50);
    }
}