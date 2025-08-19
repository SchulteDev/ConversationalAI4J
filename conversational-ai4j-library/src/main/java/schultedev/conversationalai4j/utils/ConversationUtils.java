package schultedev.conversationalai4j.utils;

import schultedev.conversationalai4j.ConversationalAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for advanced conversation processing operations.
 * 
 * This class provides specialized conversation functionality that combines
 * different input/output modalities beyond the core ConversationalAI API.
 */
public class ConversationUtils {
    
    private static final Logger log = LoggerFactory.getLogger(ConversationUtils.class);
    
    private ConversationUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Mixed modality: Process audio input and return text response.
     * 
     * This method provides voice input with text output, useful for scenarios
     * where you want to capture spoken input but display text responses.
     *
     * @param ai ConversationalAI instance with speech configured
     * @param audioInput Raw audio data in WAV format (16kHz, 16-bit, mono)
     * @return Text response from AI, or empty string if processing failed
     * @throws UnsupportedOperationException if speech is not configured
     * @throws IllegalArgumentException if audio input is null or empty
     */
    public static String chatWithTextResponse(ConversationalAI ai, byte[] audioInput) {
        if (!ai.isSpeechEnabled()) {
            throw new UnsupportedOperationException(
                "Speech services are not configured. Use withSpeech() in builder.");
        }
        
        if (audioInput == null || audioInput.length == 0) {
            throw new IllegalArgumentException("Audio input cannot be null or empty");
        }
        
        log.debug("Processing audio input with text response: {} bytes", audioInput.length);
        
        try {
            // Convert speech to text first - we need to access internal methods
            // For now, use voiceChat and extract text part
            String result = AudioUtils.speechToText(ai, audioInput, 
                schultedev.conversationalai4j.AudioFormat.wav16kMono());
            
            if (result.trim().isEmpty() || result.startsWith("Speech recognition error:")) {
                log.warn("No text transcribed from audio input: {}", result);
                return "";
            }
            
            // Get AI response
            String aiResponse = ai.chat(result);
            log.debug("Generated text response for audio input: '{}'", aiResponse);
            
            return aiResponse;
            
        } catch (Exception e) {
            log.error("Error generating text response for audio: {}", e.getMessage(), e);
            return "";
        }
    }
    
    /**
     * Mixed modality: Process text input and return audio response.
     *
     * This method provides text input with voice output, useful for scenarios
     * where you want to type input but receive spoken responses.
     *
     * @param ai ConversationalAI instance with speech configured
     * @param textInput Text message to process
     * @return Audio response in WAV format, or empty array if processing failed
     * @throws UnsupportedOperationException if speech is not configured
     * @throws IllegalArgumentException if text input is null or empty
     */
    public static byte[] chatWithVoiceResponse(ConversationalAI ai, String textInput) {
        if (!ai.isSpeechEnabled()) {
            throw new UnsupportedOperationException(
                "Speech services are not configured. Use withSpeech() in builder.");
        }
        
        if (textInput == null || textInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Text input cannot be null or empty");
        }
        
        log.debug("Processing text input with voice response: '{}'", textInput);
        
        try {
            // Get AI response as text
            String aiResponse = ai.chat(textInput);
            
            // Convert to speech
            byte[] audioResponse = ai.textToSpeech(aiResponse);
            log.debug("Generated {} bytes of audio response for text input", audioResponse.length);
            
            return audioResponse;
            
        } catch (Exception e) {
            log.error("Error generating voice response for text: {}", e.getMessage(), e);
            return new byte[0];
        }
    }
}