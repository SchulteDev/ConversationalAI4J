package schultedev.conversationalai4j.demo;

import java.io.Serializable;

/**
 * JSF backing bean for the conversation demo page.
 * Configured via faces-config.xml
 * Handles user input and responses for conversational AI.
 */
public class ConversationBean implements Serializable {

    private String message = "";
    private String response = "";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponse() {
        return response;
    }

    /**
     * Processes the user message and generates a response.
     * Currently just echoes the message - integrate with conversational AI library here.
     */
    public String sendMessage() {
        if (message != null && !message.trim().isEmpty()) {
            // TODO: Replace with actual conversational AI processing
            response = "Echo: " + message;
        } else {
            response = "Please enter a message";
        }
        return null; // Stay on same page
    }

    public String getWelcomeText() {
        return "ConversationalAI4J Demo";
    }
}