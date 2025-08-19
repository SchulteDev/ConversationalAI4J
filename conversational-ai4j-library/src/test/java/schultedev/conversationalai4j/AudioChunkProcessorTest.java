package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests for AudioChunkProcessor audio processing pipeline. */
class AudioChunkProcessorTest {

  @Mock private ConversationalAI mockConversationalAI;
  private AudioChunkProcessor processor;
  private AutoCloseable mocks;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    processor = new AudioChunkProcessor();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mocks != null) {
      mocks.close();
    }
    processor.shutdown();
  }

  @Test
  void processAudioChunks_WithEmptyChunks_ShouldReturnError() throws Exception {
    // Given
    var callback = createMockCallback();

    // When
    var result =
        processor
            .processAudioChunks(List.of(), AudioFormat.wav16kMono(), mockConversationalAI, callback)
            .get();

    // Then
    assertFalse(result.success());
    assertEquals("No audio data received", result.errorMessage());
  }

  @Test
  void processAudioChunks_WithNullAI_ShouldReturnError() throws Exception {
    // Given
    var chunks = List.of(new byte[] {1, 2, 3});
    var callback = createMockCallback();

    // When
    var result =
        processor.processAudioChunks(chunks, AudioFormat.wav16kMono(), null, callback).get();

    // Then
    assertFalse(result.success());
    assertEquals("AI service not available", result.errorMessage());
  }

  @Test
  void processAudioChunks_WithSpeechDisabled_ShouldReturnError() throws Exception {
    // Given
    var chunks = List.of(new byte[] {1, 2, 3});
    var callback = createMockCallback();
    when(mockConversationalAI.isSpeechEnabled()).thenReturn(false);

    // When
    var result =
        processor
            .processAudioChunks(chunks, AudioFormat.wav16kMono(), mockConversationalAI, callback)
            .get();

    // Then
    assertFalse(result.success());
    assertEquals("Speech services not available in this environment", result.errorMessage());
  }

  @Test
  void processAudioChunks_WithValidInput_ShouldInvokeCallback() throws Exception {
    // Given
    var chunks = List.of(createMockWavData(256));
    var statusUpdates = new AtomicReference<String>();
    var transcriptionReady = new AtomicReference<String>();

    var callback =
        new AudioChunkProcessor.ProcessingCallback() {
          @Override
          public void onStatusUpdate(String status, String message) {
            statusUpdates.set(status);
          }

          @Override
          public void onTranscriptionReady(String transcribedText) {
            transcriptionReady.set(transcribedText);
          }
        };

    when(mockConversationalAI.isSpeechEnabled()).thenReturn(true);

    // When
    var futureResult =
        processor.processAudioChunks(
            chunks, AudioFormat.wav16kMono(), mockConversationalAI, callback);

    // Then - Should be processing asynchronously
    assertFalse(futureResult.isDone());

    // Wait a moment for processing to start
    Thread.sleep(50);

    // Should have received status updates (even though processing may fail due to mock)
    assertNotNull(statusUpdates.get());
  }

  @Test
  void processingResult_Success_ShouldContainAllData() {
    // When
    var result =
        AudioChunkProcessor.ProcessingResult.success("Hello", "Hi there", new byte[] {1, 2, 3});

    // Then
    assertTrue(result.success());
    assertEquals("Hello", result.transcribedText());
    assertEquals("Hi there", result.aiResponse());
    assertArrayEquals(new byte[] {1, 2, 3}, result.responseAudio());
    assertNull(result.errorMessage());
  }

  @Test
  void processingResult_Error_ShouldContainErrorMessage() {
    // When
    var result = AudioChunkProcessor.ProcessingResult.error("Test error");

    // Then
    assertFalse(result.success());
    assertNull(result.transcribedText());
    assertNull(result.aiResponse());
    assertNull(result.responseAudio());
    assertEquals("Test error", result.errorMessage());
  }

  @Test
  void processAudioChunks_ExceptionDuringProcessing_ShouldReturnError() throws Exception {
    // Given
    var chunks = List.of(createMockWavData(256));
    var callback = createMockCallback();
    when(mockConversationalAI.isSpeechEnabled()).thenReturn(true);
    when(mockConversationalAI.chat(anyString())).thenThrow(new RuntimeException("Mock chat error"));

    // When
    var result =
        processor
            .processAudioChunks(chunks, AudioFormat.wav16kMono(), mockConversationalAI, callback)
            .get();

    // Then
    assertFalse(result.success());
    assertTrue(result.errorMessage().contains("Processing error"));
  }

  @Test
  void multipleProcessingRequests_ShouldBeHandledConcurrently() {
    // Given
    var chunks1 = List.of(createMockWavData(128));
    var chunks2 = List.of(createMockWavData(256));
    var callback = createMockCallback();
    when(mockConversationalAI.isSpeechEnabled()).thenReturn(true);

    // When - Submit multiple processing requests
    var future1 =
        processor.processAudioChunks(
            chunks1, AudioFormat.wav16kMono(), mockConversationalAI, callback);
    var future2 =
        processor.processAudioChunks(
            chunks2, AudioFormat.wav16kMono(), mockConversationalAI, callback);

    // Then - Both should be processing
    assertFalse(future1.isDone());
    assertFalse(future2.isDone());

    // Both futures should exist and be independent
    assertNotSame(future1, future2);
  }

  private AudioChunkProcessor.ProcessingCallback createMockCallback() {
    return new AudioChunkProcessor.ProcessingCallback() {
      @Override
      public void onStatusUpdate(String status, String message) {
        // Mock implementation
      }

      @Override
      public void onTranscriptionReady(String transcribedText) {
        // Mock implementation
      }
    };
  }

  private byte[] createMockWavData(int sizeInBytes) {
    // Create minimal valid WAV header + data
    var wavData = new byte[Math.max(44, sizeInBytes)];

    // WAV header
    System.arraycopy("RIFF".getBytes(), 0, wavData, 0, 4);
    writeInt32LE(wavData, 4, wavData.length - 8);
    System.arraycopy("WAVE".getBytes(), 0, wavData, 8, 4);
    System.arraycopy("fmt ".getBytes(), 0, wavData, 12, 4);
    writeInt32LE(wavData, 16, 16);
    writeInt16LE(wavData, 20, (short) 1);
    writeInt16LE(wavData, 22, (short) 1);
    writeInt32LE(wavData, 24, 16000);
    writeInt32LE(wavData, 28, 32000);
    writeInt16LE(wavData, 32, (short) 2);
    writeInt16LE(wavData, 34, (short) 16);
    System.arraycopy("data".getBytes(), 0, wavData, 36, 4);
    writeInt32LE(wavData, 40, wavData.length - 44);

    return wavData;
  }

  private void writeInt32LE(byte[] data, int offset, int value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    data[offset + 2] = (byte) ((value >> 16) & 0xFF);
    data[offset + 3] = (byte) ((value >> 24) & 0xFF);
  }

  private void writeInt16LE(byte[] data, int offset, short value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
  }
}
