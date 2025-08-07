package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ConversationBean JSF backing bean.
 */
class ConversationBeanTest {

    @Test
    void testWelcomeText() {
        ConversationBean bean = new ConversationBean();
        assertEquals("ConversationalAI4J Demo", bean.getWelcomeText());
    }

    @Test
    void testMessageProcessing() {
        ConversationBean bean = new ConversationBean();
        
        // Test valid message
        bean.setMessage("Hello");
        bean.sendMessage();
        assertEquals("Echo: Hello", bean.getResponse());
        
        // Test empty message
        bean.setMessage("");
        bean.sendMessage();
        assertEquals("Please enter a message", bean.getResponse());
        
        // Test null message
        bean.setMessage(null);
        bean.sendMessage();
        assertEquals("Please enter a message", bean.getResponse());
    }

    @Test
    void testGettersAndSetters() {
        ConversationBean bean = new ConversationBean();
        
        bean.setMessage("test message");
        assertEquals("test message", bean.getMessage());
        
        // Response is initially empty
        assertEquals("", bean.getResponse());
    }
}
