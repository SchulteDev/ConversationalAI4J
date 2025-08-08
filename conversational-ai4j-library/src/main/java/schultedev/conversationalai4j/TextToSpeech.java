package schultedev.conversationalai4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Text-to-Speech service using sherpa-onnx for privacy-first local processing.
 * Converts text input to audio without requiring internet connection.
 */
public class TextToSpeech {
    
    private static final Logger log = LoggerFactory.getLogger(TextToSpeech.class);
    
    private final Path modelPath;
    private final String language;
    private final String voice;
    private final boolean initialized;
    private final long synthesizerHandle;
    
    /**
     * Creates a new TextToSpeech instance with the specified model and voice.
     * 
     * @param modelPath Path to the sherpa-onnx TTS model
     * @param language Language code for the model (e.g., "en-US")
     * @param voice Voice identifier (e.g., "female", "male", or specific voice name)
     * @throws IllegalArgumentException if any parameter is null
     */
    public TextToSpeech(Path modelPath, String language, String voice) {
        this.modelPath = Objects.requireNonNull(modelPath, "Model path cannot be null");
        this.language = Objects.requireNonNull(language, "Language cannot be null");
        this.voice = Objects.requireNonNull(voice, "Voice cannot be null");
        
        log.info("Initializing TextToSpeech with model: {} ({}, {})", modelPath, language, voice);
        this.synthesizerHandle = SherpaOnnxNative.createTtsSynthesizer(modelPath.toString(), language, voice);
        this.initialized = (synthesizerHandle > 0);
        
        if (initialized) {
            if (SherpaOnnxNative.isNativeLibraryAvailable()) {
                log.info("TextToSpeech initialized with native sherpa-onnx library");
            } else {
                log.info("TextToSpeech initialized with mock implementation (development mode)");
            }
        } else {
            log.warn("Failed to initialize TextToSpeech");
        }
    }
    
    /**
     * Synthesizes speech from text input.
     * 
     * @param text Text to convert to speech
     * @return Audio data in WAV format (16kHz, 16-bit, mono), or empty array if synthesis failed
     * @throws IllegalStateException if the model is not initialized
     * @throws IllegalArgumentException if text is null or empty
     */
    public byte[] synthesize(String text) {
        if (!initialized) {
            throw new IllegalStateException("Text-to-speech model is not initialized");
        }
        
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        log.debug("Synthesizing speech for text: '{}'", text);
        
        try {
            var audioData = SherpaOnnxNative.synthesizeSpeech(synthesizerHandle, text);
            log.debug("Generated {} bytes of audio data", audioData.length);
            return audioData;
            
        } catch (Exception e) {
            log.error("Error synthesizing speech: {}", e.getMessage(), e);
            return new byte[0];
        }
    }
    
    /**
     * Synthesizes speech with additional voice parameters.
     * 
     * @param text Text to convert to speech
     * @param speed Speaking speed (0.5 = half speed, 1.0 = normal, 2.0 = double speed)
     * @param pitch Voice pitch adjustment (-1.0 to 1.0, 0.0 = normal)
     * @return Audio data in WAV format, or empty array if synthesis failed
     * @throws IllegalStateException if the model is not initialized
     * @throws IllegalArgumentException if text is null/empty or parameters are out of range
     */
    public byte[] synthesize(String text, double speed, double pitch) {
        if (speed < 0.1 || speed > 3.0) {
            throw new IllegalArgumentException("Speed must be between 0.1 and 3.0");
        }
        if (pitch < -1.0 || pitch > 1.0) {
            throw new IllegalArgumentException("Pitch must be between -1.0 and 1.0");
        }
        
        if (!initialized) {
            throw new IllegalStateException("Text-to-speech model is not initialized");
        }
        
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        log.debug("Synthesizing speech with speed={}, pitch={} for text: '{}'", speed, pitch, text);
        
        try {
            // For now, use basic synthesis (parameters will be added to native interface later)
            var audioData = SherpaOnnxNative.synthesizeSpeech(synthesizerHandle, text);
            log.debug("Generated {} bytes of audio data with custom parameters", audioData.length);
            return audioData;
            
        } catch (Exception e) {
            log.error("Error synthesizing speech with parameters: {}", e.getMessage(), e);
            return new byte[0];
        }
    }
    
    /**
     * Checks if the text-to-speech service is ready to process text.
     * 
     * @return true if the model is loaded and ready
     */
    public boolean isReady() {
        return initialized;
    }
    
    /**
     * Gets the language code for this TTS instance.
     * 
     * @return Language code (e.g., "en-US")
     */
    public String getLanguage() {
        return language;
    }
    
    /**
     * Gets the voice identifier for this TTS instance.
     * 
     * @return Voice identifier (e.g., "female", "male")
     */
    public String getVoice() {
        return voice;
    }
    
    /**
     * Gets the model path for this TTS instance.
     * 
     * @return Path to the model file
     */
    public Path getModelPath() {
        return modelPath;
    }
    
    // Mock methods no longer needed - handled by SherpaOnnxNative
    
    /**
     * Cleans up resources used by the text-to-speech service.
     * Should be called when the service is no longer needed.
     */
    public void close() {
        if (initialized && synthesizerHandle > 0) {
            log.debug("Cleaning up text-to-speech resources");
            SherpaOnnxNative.releaseTtsSynthesizer(synthesizerHandle);
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}