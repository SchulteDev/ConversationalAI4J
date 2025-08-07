package schultedev.conversationalai4j.examples;

import schultedev.conversationalai4j.ConversationalAI;
import schultedev.conversationalai4j.ConversationMemory;

/**
 * Example usage patterns for the ConversationalAI library.
 * These examples demonstrate different ways to configure and use the library.
 */
public class ConversationalAIExamples {

    /**
     * Basic usage example with default configuration
     */
    public void basicUsage() {
        // Simple configuration with Ollama
        ConversationalAI ai = ConversationalAI.builder()
            .withOllamaModel("llama2")
            .withMemory()
            .build();

        // Start chatting
        String response = ai.chat("Hello, how are you?");
        System.out.println("AI: " + response);
    }

    /**
     * Advanced configuration example
     */
    public void advancedUsage() {
        ConversationalAI ai = ConversationalAI.builder()
            .withOllamaModel("llama2", "http://localhost:11434")
            .withMemory(ConversationMemory.longTerm())  // 50 message memory
            .withSystemPrompt("You are a helpful programming assistant. " +
                            "Provide concise, accurate answers about Java development.")
            .withTemperature(0.3)  // Lower temperature for more focused responses
            .build();

        String response = ai.chat("How do I implement the builder pattern in Java?");
        System.out.println("AI: " + response);
    }

    /**
     * Different memory configurations
     */
    public void memoryExamples() {
        // Short-term memory (5 messages)
        ConversationalAI shortTerm = ConversationalAI.builder()
            .withOllamaModel("llama2")
            .withMemory(ConversationMemory.shortTerm())
            .build();

        // Custom memory size
        ConversationalAI custom = ConversationalAI.builder()
            .withOllamaModel("llama2")
            .withMemory(20)  // 20 messages
            .build();

        // Default memory (10 messages)
        ConversationalAI defaultAI = ConversationalAI.builder()
            .withOllamaModel("llama2")
            .withMemory()  // Uses default
            .build();
    }

    /**
     * Error handling example
     */
    public void errorHandlingExample() {
        try {
            ConversationalAI ai = ConversationalAI.builder()
                .withOllamaModel("llama2")
                .withTemperature(0.7)
                .withMemory()
                .build();

            String response = ai.chat("Tell me a joke");
            System.out.println("AI: " + response);

        } catch (IllegalStateException e) {
            System.err.println("Configuration error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid parameter: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            // Fallback to simple echo or offline mode
        }
    }

    /**
     * Conversation with context example
     */
    public void conversationWithContext() {
        ConversationalAI ai = ConversationalAI.builder()
            .withOllamaModel("llama2")
            .withMemory(ConversationMemory.defaultMemory())
            .withSystemPrompt("You are a helpful tutor. Remember previous questions " +
                            "and build upon them in your explanations.")
            .build();

        // Multi-turn conversation
        System.out.println("AI: " + ai.chat("What is polymorphism in Java?"));
        System.out.println("AI: " + ai.chat("Can you give me a practical example?"));
        System.out.println("AI: " + ai.chat("How does it differ from inheritance?"));
    }

    /**
     * Multiple AI instances with different configurations
     */
    public void multipleInstances() {
        // Creative AI for brainstorming
        ConversationalAI creative = ConversationalAI.builder()
            .withOllamaModel("llama2")
            .withTemperature(0.9)  // Higher temperature for creativity
            .withSystemPrompt("You are a creative brainstorming assistant.")
            .build();

        // Technical AI for precise answers
        ConversationalAI technical = ConversationalAI.builder()
            .withOllamaModel("llama2")
            .withTemperature(0.2)  // Lower temperature for precision
            .withSystemPrompt("You are a technical expert. Provide precise, accurate answers.")
            .build();

        System.out.println("Creative: " + creative.chat("Give me 5 creative app ideas"));
        System.out.println("Technical: " + technical.chat("Explain Java garbage collection"));
    }
}