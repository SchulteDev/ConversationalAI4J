package schultedev.conversationalai4j;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConversationMemoryTest {

    @Mock
    private TokenCountEstimator mockTokenizer;

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
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, 
            () -> ConversationMemory.sliding(0));
        assertEquals("maxMessages must be positive", exception1.getMessage());
        
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, 
            () -> ConversationMemory.sliding(-1));
        assertEquals("maxMessages must be positive", exception2.getMessage());
    }

    @Test
    void testTokenBasedMemory() {
        // Given: Mock tokenizer
        MockitoAnnotations.openMocks(this);
        
        // When: Create token-based memory
        TokenWindowChatMemory memory = ConversationMemory.tokenBased(1000, mockTokenizer);
        
        // Then: Should create successfully
        assertNotNull(memory);
    }

    @Test
    void testTokenBasedMemoryWithInvalidTokens() {
        // Given: Mock tokenizer
        MockitoAnnotations.openMocks(this);
        
        // When/Then: Should throw exception for invalid token count
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, 
            () -> ConversationMemory.tokenBased(0, mockTokenizer));
        assertEquals("maxTokens must be positive", exception1.getMessage());
        
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, 
            () -> ConversationMemory.tokenBased(-1, mockTokenizer));
        assertEquals("maxTokens must be positive", exception2.getMessage());
    }

    @Test
    void testTokenBasedMemoryWithNullTokenizer() {
        // When/Then: Should throw exception for null tokenizer
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> ConversationMemory.tokenBased(1000, null));
        assertEquals("tokenizer cannot be null", exception.getMessage());
    }

    @Test
    void testDefaultMemory() {
        // When: Create default memory
        MessageWindowChatMemory memory = ConversationMemory.defaultMemory();
        
        // Then: Should create successfully
        assertNotNull(memory);
    }

    @Test
    void testShortTermMemory() {
        // When: Create short-term memory
        MessageWindowChatMemory memory = ConversationMemory.shortTerm();
        
        // Then: Should create successfully
        assertNotNull(memory);
    }

    @Test
    void testLongTermMemory() {
        // When: Create long-term memory
        MessageWindowChatMemory memory = ConversationMemory.longTerm();
        
        // Then: Should create successfully
        assertNotNull(memory);
    }
}