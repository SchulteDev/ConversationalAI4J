package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ConversationBean CDI component.
 * These tests do not require server startup and can run independently.
 */
class ConversationBeanTest {

    @Test
    void testConversationBeanUnit() {
        // Unit test for the CDI bean
        ConversationBean bean = new ConversationBean();
        
        // Test welcome text
        assertEquals("ConversationalAI4J Demo", bean.getWelcomeText());
        
        // Test message processing
        bean.setMessage("Hello");
        bean.sendMessage();
        assertEquals("Echo: Hello", bean.getResponse());
        
        // Test empty message
        bean.setMessage("");
        bean.sendMessage();
        assertEquals("Please enter a message", bean.getResponse());
    }

    @Test
    void testConversationBeanNullMessage() {
        ConversationBean bean = new ConversationBean();
        
        // Test null message handling
        bean.setMessage(null);
        bean.sendMessage();
        assertEquals("Please enter a message", bean.getResponse());
    }

    @Test
    void testConversationBeanInitialState() {
        ConversationBean bean = new ConversationBean();
        
        // Test initial state - fields are initialized to empty strings
        assertEquals("", bean.getMessage());
        assertEquals("", bean.getResponse());
        assertEquals("ConversationalAI4J Demo", bean.getWelcomeText());
    }
}
