package schultedev.conversationalai4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Speech-to-Text service using sherpa-onnx for privacy-first local processing.
 * Converts audio input to text without requiring internet connection.
 */
public class SpeechToText {
    
    private static final Logger log = LoggerFactory.getLogger(SpeechToText.class);
    
    private final Path modelPath;
    private final String language;
    private final boolean initialized;
    private final long recognizerHandle;
    
    /**
     * Creates a new SpeechToText instance with the specified model.
     * 
     * @param modelPath Path to the sherpa-onnx STT model
     * @param language Language code for the model (e.g., "en-US")
     * @throws IllegalArgumentException if model path is null or doesn't exist
     */
    public SpeechToText(Path modelPath, String language) {
        this.modelPath = Objects.requireNonNull(modelPath, "Model path cannot be null");
        this.language = Objects.requireNonNull(language, "Language cannot be null");
        
        log.info("Initializing SpeechToText with model: {} ({})", modelPath, language);
        this.recognizerHandle = SherpaOnnxNative.createSttRecognizer(modelPath.toString(), language);
        this.initialized = (recognizerHandle > 0);
        
        if (initialized) {
            if (SherpaOnnxNative.isNativeLibraryAvailable()) {
                log.info("SpeechToText initialized with native sherpa-onnx library");
            } else {
                log.info("SpeechToText initialized with mock implementation (development mode)");
            }
        } else {
            log.warn("Failed to initialize SpeechToText");
        }
    }
    
    /**
     * Transcribes audio data to text.
     * 
     * @param audioData Raw audio data in WAV format (16kHz, 16-bit, mono)
     * @return Transcribed text, or empty string if transcription failed
     * @throws IllegalStateException if the model is not initialized
     * @throws IllegalArgumentException if audio data is null or empty
     */
    public String transcribe(byte[] audioData) {
        if (!initialized) {
            throw new IllegalStateException("Speech-to-text model is not initialized");
        }
        
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio data cannot be null or empty");
        }
        
        log.debug("Transcribing {} bytes of audio data", audioData.length);
        
        try {
            var result = SherpaOnnxNative.transcribeAudio(recognizerHandle, audioData);
            log.debug("Transcription result: '{}'", result);
            return result;
            
        } catch (Exception e) {
            log.error("Error transcribing audio: {}", e.getMessage(), e);
            return "";
        }
    }
    
    /**
     * Checks if the speech-to-text service is ready to process audio.
     * 
     * @return true if the model is loaded and ready
     */
    public boolean isReady() {
        return initialized;
    }
    
    /**
     * Gets the language code for this STT instance.
     * 
     * @return Language code (e.g., "en-US")
     */
    public String getLanguage() {
        return language;
    }
    
    /**
     * Gets the model path for this STT instance.
     * 
     * @return Path to the model file
     */
    public Path getModelPath() {
        return modelPath;
    }
    
    // No longer needed - initialization is handled in constructor via SherpaOnnxNative
    
    /**
     * Cleans up resources used by the speech-to-text service.
     * Should be called when the service is no longer needed.
     */
    public void close() {
        if (initialized && recognizerHandle > 0) {
            log.debug("Cleaning up speech-to-text resources");
            SherpaOnnxNative.releaseSttRecognizer(recognizerHandle);
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}