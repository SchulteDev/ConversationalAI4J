// ConversationalAI4J - Simplified Voice Chat Interface
// Simple client-side audio capture using MediaRecorder API
// All complex audio processing moved to server-side Java library

class SimplifiedVoiceInterface {
  constructor() {
    this.socket = null;
    this.mediaRecorder = null;
    this.audioStream = null;
    this.isVoiceMode = false;
    this.isConnected = false;
    this.isRecording = false;
    this.recordedBlobs = [];
    this.currentAudioUrl = null;

    this.initializeElements();
    this.setupEventListeners();
    this.setupAudioControls();
    this.scrollToBottom();
  }

  initializeElements() {
    this.chatArea = document.getElementById('chat-area');
    this.messageInput = document.getElementById('message-input');
    this.voiceToggle = document.getElementById('voice-toggle');
    this.sendButton = document.getElementById('send-button');
    this.messageForm = document.getElementById('message-form');
    this.audioPlayer = document.getElementById('ai-audio-player');
    this.notificationOverlay = document.getElementById('notification-overlay');
    this.notificationText = document.getElementById('notification-text');
    this.notification = document.getElementById('notification');
    this.typingIndicator = document.getElementById('typing-indicator');
  }

  setupEventListeners() {
    // Send button click
    this.sendButton.addEventListener('click', (e) => {
      e.preventDefault();
      this.handleTextMessage();
    });

    // Enter key in input
    this.messageInput.addEventListener('keypress', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        this.handleTextMessage();
      }
    });

    // Voice toggle button
    this.voiceToggle.addEventListener('click', (e) => {
      e.preventDefault();
      this.handleVoiceToggle();
    });

    // Prevent context menu on voice button
    this.voiceToggle.addEventListener('contextmenu', (e) => e.preventDefault());
  }

  setupAudioControls() {
    // Add click handlers for audio play buttons in conversation history
    this.chatArea.addEventListener('click', (e) => {
      if (e.target.closest('.audio-play-btn')) {
        const button = e.target.closest('.audio-play-btn');
        this.handleAudioButtonClick(button);
      }
    });
  }

  // TEXT MESSAGE HANDLING - Same as before
  async handleTextMessage() {
    const message = this.messageInput.value.trim();
    if (!message) {
      return;
    }

    // Clear input immediately
    this.messageInput.value = '';

    // Show user message immediately
    this.displayUserMessage(message);
    this.showTypingIndicator();

    try {
      // Send text to AI and get JSON response
      const response = await fetch('/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `message=${encodeURIComponent(message)}`
      });

      if (!response.ok) {
        throw new Error(`Server error: ${response.status}`);
      }

      // Parse JSON response
      const data = await response.json();

      if (data.error) {
        throw new Error(data.error);
      }

      const aiResponse = data.response;

      // Display AI response immediately
      this.displayAIMessage(aiResponse);

      // Start async TTS generation and auto-play
      this.generateAndPlayTTS(aiResponse);

    } catch (error) {
      console.error('Text message error:', error);
      this.displayAIMessage('Sorry, there was an error processing your message.');
      this.showNotification('Error: ' + error.message, 'error');
    }
  }

  // SIMPLIFIED VOICE HANDLING - Using MediaRecorder API
  async handleVoiceToggle() {
    if (!this.isVoiceMode) {
      // Start voice recording
      try {
        await this.startVoiceRecording();
      } catch (error) {
        this.showNotification('Failed to enable voice mode: ' + error.message, 'error');
        this.isVoiceMode = false;
        this.voiceToggle.classList.remove('active');
      }
    } else {
      // Stop recording and process
      if (this.isRecording) {
        this.stopVoiceRecording();
      }
    }
  }

  async startVoiceRecording() {
    // Request microphone access
    this.audioStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        sampleRate: 16000,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    });

    // Connect WebSocket
    await this.connectWebSocket();

    // Create MediaRecorder - force compatible format
    let options = {};
    
    // Debug: Log all supported formats
    const testFormats = [
      'audio/wav',
      'audio/webm;codecs=pcm', 
      'audio/webm;codecs=opus',
      'audio/webm',
      'audio/mp4',
      'audio/ogg'
    ];
    
    console.log('Browser MediaRecorder format support:');
    testFormats.forEach(format => {
      console.log(`  ${format}: ${MediaRecorder.isTypeSupported(format)}`);
    });
    
    // Try to use the best available format, server will handle decoding
    if (MediaRecorder.isTypeSupported('audio/wav')) {
      options.mimeType = 'audio/wav';
      console.log('Selected: WAV format');
      this.useWebAudioFallback = false;
    } else if (MediaRecorder.isTypeSupported('audio/webm;codecs=pcm')) {
      options.mimeType = 'audio/webm;codecs=pcm';
      console.log('Selected: WebM/PCM format');
      this.useWebAudioFallback = false;
    } else if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
      options.mimeType = 'audio/webm;codecs=opus';
      console.log('Selected: WebM/Opus format (server will decode)');
      this.useWebAudioFallback = false;
    } else {
      console.warn('No supported audio formats found, using defaults');
      this.useWebAudioFallback = false;
    }
    
    // Set audio quality
    if (options.mimeType) {
      options.audioBitsPerSecond = 128000;
    }

    if (this.useWebAudioFallback) {
      await this.startWebAudioRecording();
    } else {
      this.mediaRecorder = new MediaRecorder(this.audioStream, options);
      this.recordedBlobs = [];

      // Handle recorded data
      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          this.recordedBlobs.push(event.data);
        }
      };

      // Handle recording stop
      this.mediaRecorder.onstop = () => {
        this.processRecordedAudio();
      };

      // Start recording
      this.mediaRecorder.start(1000); // Collect data every second
      this.isRecording = true;
      this.isVoiceMode = true;
      this.voiceToggle.classList.add('active', 'recording');
      this.messageInput.placeholder = "Recording... Click again to stop and send.";
      this.showNotification('Recording started! Click microphone again to stop and send.', 'success');

      console.log('Voice recording started with MediaRecorder');
    }
  }

  async startWebAudioRecording() {
    try {
      // Create AudioContext with 16kHz sample rate for compatibility
      this.audioContext = new (window.AudioContext || window.webkitAudioContext)({
        sampleRate: 16000
      });
      
      this.source = this.audioContext.createMediaStreamSource(this.audioStream);
      this.audioChunks = [];
      
      // Use ScriptProcessorNode (deprecated but widely supported)
      // AudioWorkletNode would be better but requires HTTPS
      this.processor = this.audioContext.createScriptProcessor(4096, 1, 1);
      
      this.processor.onaudioprocess = (event) => {
        if (this.isRecording) {
          const inputBuffer = event.inputBuffer;
          const samples = inputBuffer.getChannelData(0); // Float32Array
          
          // Convert Float32 samples to Int16 PCM
          const pcmData = this.convertFloat32ToInt16PCM(samples);
          this.audioChunks.push(pcmData);
          
          // Send chunk immediately for real-time processing
          if (this.socket && this.socket.readyState === WebSocket.OPEN && this.audioChunks.length === 1) {
            this.socket.send('start_recording');
          }
          if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(pcmData);
          }
        }
      };
      
      this.source.connect(this.processor);
      this.processor.connect(this.audioContext.destination);
      
      // Start recording
      this.isRecording = true;
      this.isVoiceMode = true;
      this.voiceToggle.classList.add('active', 'recording');
      this.messageInput.placeholder = "Recording with Web Audio API... Click again to stop and send.";
      this.showNotification('Recording started with Web Audio API! Click microphone again to stop and send.', 'success');
      
      console.log('Voice recording started with Web Audio API');
      
    } catch (error) {
      console.error('Web Audio API setup failed:', error);
      this.showNotification('Failed to start Web Audio recording: ' + error.message, 'error');
      throw error;
    }
  }

  convertFloat32ToInt16PCM(float32Array) {
    const buffer = new ArrayBuffer(float32Array.length * 2);
    const view = new DataView(buffer);
    
    for (let i = 0; i < float32Array.length; i++) {
      // Clamp to [-1, 1] and convert to 16-bit signed integer
      const sample = Math.max(-1, Math.min(1, float32Array[i]));
      const int16 = sample * 0x7FFF;
      view.setInt16(i * 2, int16, true); // little-endian
    }
    
    return buffer;
  }

  stopVoiceRecording() {
    if (this.useWebAudioFallback) {
      this.stopWebAudioRecording();
    } else if (this.mediaRecorder && this.isRecording) {
      this.mediaRecorder.stop();
      this.isRecording = false;
      this.messageInput.placeholder = "Processing voice...";
      this.showNotification('Processing your voice message...', 'info');
      console.log('Voice recording stopped');
    }
  }

  stopWebAudioRecording() {
    if (this.isRecording) {
      this.isRecording = false;
      
      // Send stop signal
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send('stop_recording');
      }
      
      // Cleanup Web Audio API components
      if (this.processor) {
        this.processor.disconnect();
        this.processor = null;
      }
      if (this.source) {
        this.source.disconnect();
        this.source = null;
      }
      if (this.audioContext && this.audioContext.state !== 'closed') {
        this.audioContext.close();
        this.audioContext = null;
      }
      
      this.messageInput.placeholder = "Processing voice...";
      this.showNotification('Processing your voice message...', 'info');
      console.log('Web Audio recording stopped, sent', this.audioChunks.length, 'chunks');
    }
  }

  async processRecordedAudio() {
    if (this.recordedBlobs.length === 0) {
      this.showNotification('No audio recorded', 'error');
      this.finishVoiceMode();
      return;
    }

    try {
      // Combine all recorded blobs into single blob
      const audioBlob = new Blob(this.recordedBlobs, { 
        type: this.recordedBlobs[0].type 
      });

      console.log(`Sending ${audioBlob.size} bytes of ${audioBlob.type} audio to server`);

      // Send audio blob to server via WebSocket
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send('start_recording');
        this.socket.send(audioBlob);
        this.socket.send('stop_recording');
        this.showTypingIndicator();
      } else {
        throw new Error('WebSocket connection not available');
      }

    } catch (error) {
      console.error('Error processing recorded audio:', error);
      this.showNotification('Error processing audio: ' + error.message, 'error');
      this.finishVoiceMode();
    }
  }

  async connectWebSocket() {
    return new Promise((resolve, reject) => {
      this.socket = new WebSocket(`ws://${window.location.host}/voice-stream`);

      this.socket.onopen = () => {
        console.log('WebSocket connected');
        this.isConnected = true;
        resolve();
      };

      this.socket.onmessage = (event) => this.handleWebSocketMessage(event);

      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error);
        reject(new Error('Connection failed'));
      };

      this.socket.onclose = (event) => {
        console.log('WebSocket closed:', event.code, event.reason);
        this.isConnected = false;
        this.isRecording = false;
        this.updateVoiceButtonState();
      };
    });
  }

  disconnectWebSocket() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }

    if (this.audioStream) {
      this.audioStream.getTracks().forEach(track => track.stop());
      this.audioStream = null;
    }

    if (this.mediaRecorder) {
      this.mediaRecorder = null;
    }

    this.isConnected = false;
    this.isRecording = false;
    this.recordedBlobs = [];
    this.updateVoiceButtonState();
  }

  handleWebSocketMessage(event) {
    if (event.data instanceof Blob) {
      // Handle audio response
      this.handleAudioResponse(event.data);
    } else {
      try {
        const message = JSON.parse(event.data);
        this.handleControlMessage(message);
      } catch (e) {
        console.log('WebSocket text message:', event.data);
      }
    }
  }

  handleControlMessage(message) {
    console.log('Control message:', message);

    switch (message.type) {
      case 'status':
        const statusType = message.status === 'error' ? 'error' :
          message.status === 'complete' ? 'success' : null;
        if (statusType) {
          this.showNotification(message.message, statusType);
        }

        if (message.status === 'processing') {
          this.showTypingIndicator();
        } else if (message.status === 'complete') {
          this.hideTypingIndicator();
          this.finishVoiceMode();
        } else if (message.status === 'error') {
          this.hideTypingIndicator();
          this.finishVoiceMode();
          // Show a user-friendly error message
          this.displayAIMessage('Sorry, I had trouble processing your voice input. Please try again or check your microphone settings.');
        }
        break;

      case 'transcription':
        // Voice input transcribed - show as user message
        this.displayUserMessage(message.text, true);
        break;

      case 'text_response':
        // AI response from voice input - show immediately and generate TTS
        this.displayAIMessage(message.message);
        this.generateAndPlayTTS(message.message);
        break;
    }
  }

  finishVoiceMode() {
    setTimeout(() => {
      this.disconnectWebSocket();
      this.isVoiceMode = false;
      this.voiceToggle.classList.remove('active', 'recording');
      this.messageInput.placeholder = "Type a message...";
    }, 1000);
  }

  async handleAudioResponse(audioBlob) {
    console.log('Received audio response:', audioBlob.size, 'bytes');

    try {
      const audioUrl = URL.createObjectURL(audioBlob);
      this.audioPlayer.src = audioUrl;

      await this.audioPlayer.play();
      this.showNotification('Playing AI response', 'success');

      this.audioPlayer.onended = () => {
        URL.revokeObjectURL(audioUrl);
        this.hideTypingIndicator();
      };
    } catch (error) {
      console.error('Audio playback failed:', error);
      this.showNotification('Audio playback failed', 'error');
    }
  }

  updateVoiceButtonState() {
    this.voiceToggle.classList.toggle('recording', this.isRecording);
    this.voiceToggle.classList.toggle('active', this.isVoiceMode && this.isConnected);
  }

  // MESSAGE DISPLAY METHODS - Same as before
  displayUserMessage(text, isVoiceTranscription = false) {
    this.addMessageToChat(text, 'user');
    this.scrollToBottom();
  }

  displayAIMessage(text) {
    const messageElement = this.addMessageToChat(text, 'ai', true);
    this.hideTypingIndicator();
    this.scrollToBottom();
    return messageElement;
  }

  addMessageToChat(text, sender, withAudioButton = false) {
    const messageGroup = document.createElement('div');
    messageGroup.className = `message-group ${sender}-message`;

    const messageBubble = document.createElement('div');
    messageBubble.className = `message-bubble ${sender}`;
    if (withAudioButton) {
      messageBubble.setAttribute('data-has-audio', 'true');
    }

    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    messageContent.textContent = text;

    messageBubble.appendChild(messageContent);

    // Add audio controls for AI messages
    if (sender === 'ai' && withAudioButton) {
      const audioControls = document.createElement('div');
      audioControls.className = 'message-audio-controls';

      const playButton = document.createElement('button');
      playButton.className = 'audio-play-btn';
      playButton.setAttribute('data-text', text);
      playButton.setAttribute('title', 'Play audio');
      playButton.innerHTML = '<svg viewBox="0 0 24 24" width="16" height="16"><path d="M8 5v14l11-7z"/></svg>';

      const audioPlayer = document.createElement('audio');
      audioPlayer.className = 'message-audio-player';
      audioPlayer.style.display = 'none';

      audioControls.appendChild(playButton);
      audioControls.appendChild(audioPlayer);
      messageBubble.appendChild(audioControls);
    }

    messageGroup.appendChild(messageBubble);

    // Insert before typing indicator
    this.chatArea.insertBefore(messageGroup, this.typingIndicator);

    return messageGroup;
  }

  // TTS GENERATION AND PLAYBACK - Same as before
  async generateAndPlayTTS(text) {
    try {
      console.log('Generating TTS for:', text);

      const response = await fetch('/direct-tts', {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain'
        },
        body: text
      });

      if (!response.ok) {
        throw new Error(`TTS failed: ${response.status}`);
      }

      const audioData = await response.blob();
      const audioUrl = URL.createObjectURL(audioData);

      // Auto-play the generated audio
      this.audioPlayer.src = audioUrl;

      try {
        this.stopAllAudio();
        
        this.audioPlayer.oncanplaythrough = async () => {
          try {
            await this.audioPlayer.play();
            console.log('Auto-playing TTS audio');
          } catch (playError) {
            console.log('Auto-play blocked, audio available via button');
          }
        };
        this.audioPlayer.load();
      } catch (error) {
        console.log('Audio setup failed:', error);
      }

      // Clean up URL when audio ends
      this.audioPlayer.onended = () => {
        URL.revokeObjectURL(audioUrl);
      };

      this.audioPlayer.onerror = () => {
        URL.revokeObjectURL(audioUrl);
      };

    } catch (error) {
      console.error('TTS generation failed:', error);
    }
  }

  async handleAudioButtonClick(button) {
    const text = button.getAttribute('data-text');
    const audioPlayer = button.parentElement.querySelector('.message-audio-player');

    if (button.classList.contains('playing')) {
      audioPlayer.pause();
      button.classList.remove('playing');
      return;
    }

    this.stopAllAudioExcept(button);

    try {
      button.classList.add('loading');

      const response = await fetch('/direct-tts', {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain'
        },
        body: text
      });

      if (!response.ok) {
        throw new Error(`TTS failed: ${response.status}`);
      }

      const audioData = await response.blob();
      const audioUrl = URL.createObjectURL(audioData);
      audioPlayer.src = audioUrl;

      button.classList.remove('loading');
      button.classList.add('playing');

      audioPlayer.oncanplaythrough = async () => {
        try {
          await audioPlayer.play();
        } catch (err) {
          console.error('Audio playback failed:', err);
          button.classList.remove('playing');
        }
      };
      audioPlayer.load();

      audioPlayer.onended = () => {
        button.classList.remove('playing');
        URL.revokeObjectURL(audioUrl);
      };

      audioPlayer.onerror = () => {
        button.classList.remove('playing', 'loading');
        URL.revokeObjectURL(audioUrl);
      };

    } catch (error) {
      button.classList.remove('loading');
      console.error('Audio generation failed:', error);
    }
  }

  stopAllAudio() {
    if (this.audioPlayer) {
      this.audioPlayer.pause();
      this.audioPlayer.currentTime = 0;
    }

    const audioButtons = this.chatArea.querySelectorAll('.audio-play-btn.playing');
    audioButtons.forEach(button => {
      const audioPlayer = button.parentElement.querySelector('.message-audio-player');
      if (audioPlayer) {
        audioPlayer.pause();
        audioPlayer.currentTime = 0;
      }
      button.classList.remove('playing');
    });
  }

  stopAllAudioExcept(exceptButton) {
    if (this.audioPlayer) {
      this.audioPlayer.pause();
      this.audioPlayer.currentTime = 0;
    }

    const audioButtons = this.chatArea.querySelectorAll('.audio-play-btn.playing');
    audioButtons.forEach(button => {
      if (button !== exceptButton) {
        const audioPlayer = button.parentElement.querySelector('.message-audio-player');
        if (audioPlayer) {
          audioPlayer.pause();
          audioPlayer.currentTime = 0;
        }
        button.classList.remove('playing');
      }
    });
  }

  showTypingIndicator() {
    this.typingIndicator.classList.remove('hidden');
    this.scrollToBottom();
  }

  hideTypingIndicator() {
    this.typingIndicator.classList.add('hidden');
  }

  showNotification(message, type = 'info') {
    this.notificationText.textContent = message;
    this.notification.className = `notification ${type}`;
    this.notificationOverlay.classList.remove('hidden');

    setTimeout(() => {
      this.notificationOverlay.classList.add('hidden');
    }, type === 'error' ? 5000 : 3000);
  }

  scrollToBottom() {
    setTimeout(() => {
      this.chatArea.scrollTop = this.chatArea.scrollHeight;
    }, 100);
  }
}

// Initialize the simplified voice interface when DOM is loaded
document.addEventListener('DOMContentLoaded', function () {
  const voiceInterface = new SimplifiedVoiceInterface();

  // Make it available globally for debugging
  window.voiceInterface = voiceInterface;

  console.log('Simplified voice interface initialized - audio processing moved to server');
});