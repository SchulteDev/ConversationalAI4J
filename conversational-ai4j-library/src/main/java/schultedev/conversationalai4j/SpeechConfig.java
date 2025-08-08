package schultedev.conversationalai4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Configuration for speech capabilities including speech-to-text and text-to-speech settings.
 * Provides convenient factory methods for common configurations.
 */
public class SpeechConfig {

  private final String language;
  private final String voice;
  private final Path sttModelPath;
  private final Path ttsModelPath;
  private final boolean enabled;

  private SpeechConfig(Builder builder) {
    this.language = builder.language;
    this.voice = builder.voice;
    this.sttModelPath = builder.sttModelPath;
    this.ttsModelPath = builder.ttsModelPath;
    this.enabled = builder.enabled;
  }

  /**
   * Creates default English speech configuration with automatic model download. Uses compressed
   * models for optimal size/quality balance.
   *
   * @return SpeechConfig with English STT and TTS
   */
  public static SpeechConfig defaults() {
    return new Builder().withLanguage("en-US").withVoice("female").withAutoDownload(true).build();
  }

  /**
   * Creates speech configuration for specific language and voice.
   *
   * @param language Language code (e.g., "en-US", "de-DE", "fr-FR")
   * @param voice Voice type ("male", "female", or specific voice name)
   * @return SpeechConfig for the specified language and voice
   */
  public static SpeechConfig of(String language, String voice) {
    return new Builder().withLanguage(language).withVoice(voice).withAutoDownload(true).build();
  }

  /**
   * Creates custom speech configuration with specific model paths.
   *
   * @param sttModelPath Path to speech-to-text model
   * @param ttsModelPath Path to text-to-speech model
   * @return SpeechConfig with custom model paths
   */
  public static SpeechConfig custom(Path sttModelPath, Path ttsModelPath) {
    return new Builder().withSttModel(sttModelPath).withTtsModel(ttsModelPath).build();
  }

  /**
   * Creates disabled speech configuration (text-only mode).
   *
   * @return Disabled SpeechConfig
   */
  public static SpeechConfig disabled() {
    return new Builder().withEnabled(false).build();
  }

  // Getters
  public String getLanguage() {
    return language;
  }

  public String getVoice() {
    return voice;
  }

  public Path getSttModelPath() {
    return sttModelPath;
  }

  public Path getTtsModelPath() {
    return ttsModelPath;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public String toString() {
    return String.format(
        "SpeechConfig{language='%s', voice='%s', enabled=%s}", language, voice, enabled);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    var that = (SpeechConfig) o;
    return enabled == that.enabled
        && Objects.equals(language, that.language)
        && Objects.equals(voice, that.voice)
        && Objects.equals(sttModelPath, that.sttModelPath)
        && Objects.equals(ttsModelPath, that.ttsModelPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(language, voice, sttModelPath, ttsModelPath, enabled);
  }

  public static class Builder {
    private String language = "en-US";
    private String voice = "female";
    private Path sttModelPath;
    private Path ttsModelPath;
    private boolean enabled = true;
    private boolean autoDownload = false;

    public Builder withLanguage(String language) {
      this.language = Objects.requireNonNull(language, "Language cannot be null");
      return this;
    }

    public Builder withVoice(String voice) {
      this.voice = Objects.requireNonNull(voice, "Voice cannot be null");
      return this;
    }

    public Builder withSttModel(Path modelPath) {
      this.sttModelPath = Objects.requireNonNull(modelPath, "STT model path cannot be null");
      return this;
    }

    public Builder withTtsModel(Path modelPath) {
      this.ttsModelPath = Objects.requireNonNull(modelPath, "TTS model path cannot be null");
      return this;
    }

    public Builder withAutoDownload(boolean autoDownload) {
      this.autoDownload = autoDownload;
      return this;
    }

    public Builder withEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public SpeechConfig build() {
      if (enabled && autoDownload && (sttModelPath == null || ttsModelPath == null)) {
        resolveDefaultModelPaths();
      }
      return new SpeechConfig(this);
    }

    private void resolveDefaultModelPaths() {
      // Check for Docker environment with pre-configured models
      var dockerSttPath = System.getenv("STT_MODEL_PATH");
      var dockerTtsPath = System.getenv("TTS_MODEL_PATH");

      if (dockerSttPath != null && dockerTtsPath != null) {
        // Use Docker environment paths
        if (sttModelPath == null) {
          sttModelPath = Paths.get(dockerSttPath);
        }
        if (ttsModelPath == null) {
          ttsModelPath = Paths.get(dockerTtsPath);
        }
        return;
      }

      // Use standard model paths (Linux systems)
      var modelsDir = Paths.get(System.getProperty("user.home"), ".sherpa-onnx", "models");

      if (sttModelPath == null) {
        // Default STT model path
        sttModelPath = modelsDir.resolve("stt").resolve(language).resolve("model.onnx");
      }

      if (ttsModelPath == null) {
        // Default TTS model path based on language and voice
        var voiceName = String.format("%s-%s", language.toLowerCase().replace("-", "_"), voice);
        ttsModelPath = modelsDir.resolve("tts").resolve(voiceName).resolve("model.onnx");
      }
    }
  }
}
