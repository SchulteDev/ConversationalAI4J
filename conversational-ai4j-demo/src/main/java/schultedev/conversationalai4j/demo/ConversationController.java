package schultedev.conversationalai4j.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import schultedev.conversationalai4j.ConversationalAI;

/**
 * Spring MVC Controller for handling conversation interactions in the demo application.
 * Provides a simple interface for users to send messages and receive responses.
 */
@Controller
public class ConversationController {
    
    private final ConversationalAI conversationalAI;
    
    /**
     * Constructor that initializes the ConversationalAI instance with default configuration.
     * In a production environment, this could be configured via Spring properties.
     */
    public ConversationController() {
        ConversationalAI tempAI;
        try {
            // Initialize with Ollama model - fallback to echo mode if model not available
            tempAI = ConversationalAI.builder()
                .withOllamaModel("llama2")  // Assumes Ollama is running with llama2
                .withMemory()  // Use default memory
                .withSystemPrompt("You are a helpful AI assistant in a demo application. " +
                                "Keep responses concise and friendly.")
                .withTemperature(0.7)
                .build();
        } catch (Exception e) {
            // If Ollama is not available, we'll use null and fall back to echo mode
            tempAI = null;
        }
        this.conversationalAI = tempAI;
    }

    /**
     * Displays the main conversation page.
     *
     * @param model the model to add attributes to
     * @return the template name
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("welcomeText", "ConversationalAI4J Demo");
        return "conversation";
    }

    /**
     * Processes the user's message and generates a response.
     * Uses the ConversationalAI library if available, otherwise falls back to echo mode.
     *
     * @param message the user's input message
     * @param model the model to add attributes to
     * @return the template name
     */
    @PostMapping("/send")
    public String sendMessage(@RequestParam("message") String message, Model model) {
        model.addAttribute("welcomeText", "ConversationalAI4J Demo");
        model.addAttribute("message", message);
        
        String response;
        if (message == null || message.trim().isEmpty()) {
            response = "Please enter a message.";
        } else {
            try {
                if (conversationalAI != null) {
                    // Use the ConversationalAI library
                    response = conversationalAI.chat(message);
                } else {
                    // Fallback to echo mode if AI is not available
                    response = "Echo (AI unavailable): " + message;
                }
            } catch (Exception e) {
                // Handle any errors gracefully
                response = "Sorry, I'm having trouble processing your request. " +
                          "Error: " + e.getMessage() + 
                          "\nFallback echo: " + message;
            }
        }
        
        model.addAttribute("response", response);
        return "conversation";
    }
}