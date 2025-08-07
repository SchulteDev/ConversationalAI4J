package schultedev.conversationalai4j;

import schultedev.conversationalai4j.examples.ConversationalAIExamples;

/**
 * Main class for running ConversationalAI4J examples.
 * This demonstrates the basic usage of the library.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("ConversationalAI4J Library Examples");
        System.out.println("===================================");
        
        ConversationalAIExamples examples = new ConversationalAIExamples();
        
        try {
            System.out.println("\n1. Basic Usage Example:");
            examples.basicUsage();
            
            System.out.println("\n2. Error Handling Example:");
            examples.errorHandlingExample();
            
            System.out.println("\nFor more examples, see ConversationalAIExamples class");
            
        } catch (Exception e) {
            System.err.println("Note: Examples require Ollama to be running with llama2 model");
            System.err.println("Install Ollama and run: ollama pull llama2");
            System.err.println("Error: " + e.getMessage());
        }
    }
}
