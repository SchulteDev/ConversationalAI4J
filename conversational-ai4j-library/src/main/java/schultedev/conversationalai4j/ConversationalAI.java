package schultedev.conversationalai4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

/**
 * Main entry point for conversational AI functionality.
 * Provides fluent builder API for easy configuration and usage.
 */
public class ConversationalAI {
    
    private final ChatModel model;
    private final ConversationService service;
    
    private ConversationalAI(Builder builder) {
        this.model = builder.model;
        var aiServiceBuilder = AiServices.builder(ConversationService.class)
            .chatModel(model)
            .chatMemory(builder.memory);
            
        if (builder.systemPrompt != null && !builder.systemPrompt.trim().isEmpty()) {
            aiServiceBuilder.systemMessageProvider(chatMemoryId -> builder.systemPrompt);
        }
            
        this.service = aiServiceBuilder.build();
    }
    
    /**
     * Send a message and get AI response
     */
    public String chat(String message) {
        return service.chat(message);
    }
    
    /**
     * Get the underlying chat model
     */
    public ChatModel getModel() {
        return model;
    }
    
    /**
     * Create a new builder for ConversationalAI
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for ConversationalAI configuration
     */
    public static class Builder {
        private ChatModel model;
        private MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);
        private String systemPrompt;
        private double temperature = 0.7;
        
        private Builder() {}
        
        /**
         * Configure Ollama model
         */
        public Builder withOllamaModel(String modelName) {
            return withOllamaModel(modelName, "http://localhost:11434");
        }
        
        /**
         * Configure Ollama model with custom base URL
         */
        public Builder withOllamaModel(String modelName, String baseUrl) {
            this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .build();
            return this;
        }
        
        /**
         * Configure conversation memory with default window size (10 messages)
         */
        public Builder withMemory() {
            this.memory = ConversationMemory.defaultMemory();
            return this;
        }
        
        /**
         * Configure conversation memory with custom window size
         */
        public Builder withMemory(int maxMessages) {
            this.memory = ConversationMemory.sliding(maxMessages);
            return this;
        }
        
        /**
         * Configure conversation memory
         */
        public Builder withMemory(MessageWindowChatMemory memory) {
            this.memory = memory;
            return this;
        }
        
        /**
         * Set system prompt
         */
        public Builder withSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }
        
        /**
         * Set temperature for model responses (0.0 to 1.0)
         */
        public Builder withTemperature(double temperature) {
            if (temperature < 0.0 || temperature > 1.0) {
                throw new IllegalArgumentException("Temperature must be between 0.0 and 1.0");
            }
            this.temperature = temperature;
            return this;
        }
        
        /**
         * Build the ConversationalAI instance
         */
        public ConversationalAI build() {
            if (model == null) {
                throw new IllegalStateException("Model must be configured. Use withOllamaModel() or withModel()");
            }
            return new ConversationalAI(this);
        }
    }
    
    /**
     * Internal service interface for AI interactions
     */
    private interface ConversationService {
        String chat(@UserMessage String message);
    }
}