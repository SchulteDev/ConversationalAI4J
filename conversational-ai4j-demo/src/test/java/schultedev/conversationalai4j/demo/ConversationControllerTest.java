package schultedev.conversationalai4j.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ConversationController functionality.
 */
@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testIndex_ShouldReturnConversationPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attribute("welcomeText", "ConversationalAI4J Demo"));
    }

    @Test
    void testSendMessage_WithValidMessage() throws Exception {
        String testMessage = "Hello, AI!";
        
        mockMvc.perform(post("/send")
                .param("message", testMessage))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attribute("welcomeText", "ConversationalAI4J Demo"))
                .andExpect(model().attribute("message", testMessage))
                // Response should either be AI response or fallback echo - we check for fallback since no Ollama in tests
                .andExpect(model().attributeExists("response"));
    }

    @Test
    void testSendMessage_WithEmptyMessage() throws Exception {
        mockMvc.perform(post("/send")
                .param("message", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attribute("welcomeText", "ConversationalAI4J Demo"))
                .andExpect(model().attribute("message", ""))
                .andExpect(model().attribute("response", "Please enter a message."));
    }

    @Test
    void testSendMessage_WithWhitespaceOnlyMessage() throws Exception {
        mockMvc.perform(post("/send")
                .param("message", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attribute("welcomeText", "ConversationalAI4J Demo"))
                .andExpect(model().attribute("message", "   "))
                .andExpect(model().attribute("response", "Please enter a message."));
    }
}