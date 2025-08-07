package schultedev.conversationalai4j.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Spring MVC Controller for handling conversation interactions in the demo application.
 * Provides a simple interface for users to send messages and receive responses.
 */
@Controller
public class ConversationController {

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
     * Currently returns a simple echo response as placeholder functionality.
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
        if (message != null && !message.trim().isEmpty()) {
            // TODO: Integrate with the conversational AI library
            response = "Echo: " + message;
        } else {
            response = "Please enter a message.";
        }
        
        model.addAttribute("response", response);
        return "conversation";
    }
}