// ConversationalAI4J - Unified Chat Interface
// Both text and voice inputs behave consistently:
// 1. Immediate text display
// 2. Async TTS generation and auto-play
// 3. Audio controls for all AI responses

class UnifiedChatInterface {
  constructor() {
    this.socket = null;
    this.audioStream = null;
    this.mediaRecorder = null;
    this.isVoiceMode = false;
    this.isConnected = false;
    this.isRecording = false;
    this.recordedChunks = [];
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

  // UNIFIED MESSAGE HANDLING - Both text and voice use this flow
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

  async handleVoiceToggle() {
    if (!this.isVoiceMode) {
      // Start voice recording
      try {
        await this.requestMicrophoneAccess();
        await this.connectWebSocket();
        this.isVoiceMode = true;
        this.voiceToggle.classList.add('active');
        this.messageInput.placeholder = "Recording... Click again to stop and send.";
        this.showNotification('Recording started! Click microphone again to stop and send.',
          'success');

        setTimeout(async () => {
          await this.startRecording();
        }, 200);
      } catch (error) {
        this.showNotification('Failed to enable voice mode: ' + error.message, 'error');
        this.isVoiceMode = false;
        this.voiceToggle.classList.remove('active');
      }
    } else {
      // Stop recording and process
      if (this.isRecording) {
        this.stopRecording();
        this.messageInput.placeholder = "Processing voice...";
        this.showNotification('Processing your voice message...', 'info');
      }
    }
  }

  async requestMicrophoneAccess() {
    try {
      this.audioStream = await navigator.mediaDevices.getUserMedia({
        audio: {sampleRate: 16000, channelCount: 1}
      });
      console.log('Microphone access granted');
    } catch (error) {
      throw new Error('Microphone access denied or not available');
    }
  }

  async connectWebSocket() {
    return new Promise((resolve, reject) => {
      this.socket = new WebSocket(`ws://${window.location.host}/voice-stream`);

      this.socket.onopen = () => {
        console.log('WebSocket connected');
        this.isConnected = true;

        setTimeout(() => {
          if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send('check_status');
            resolve();
          } else {
            reject(new Error('WebSocket connection lost during initialization'));
          }
        }, 100);
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

        if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
          this.mediaRecorder.stop();
          this.showNotification('Connection lost during recording', 'error');
        }
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

    this.isConnected = false;
    this.isRecording = false;
    this.updateVoiceButtonState();
  }

  handleWebSocketMessage(event) {
    if (event.data instanceof Blob) {
      // Voice mode: Handle audio response (deprecated - now using unified approach)
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
          this.finishVoiceMode();
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

  async startRecording() {
    try {
      if (!this.audioStream || !this.socket || this.socket.readyState !== WebSocket.OPEN) {
        throw new Error('Recording prerequisites not met');
      }

      this.mediaRecorder = new MediaRecorder(this.audioStream, {
        mimeType: 'audio/webm;codecs=opus'
      });

      this.recordedChunks = [];

      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          this.recordedChunks.push(event.data);
        }
      };

      this.mediaRecorder.onstop = () => {
        const blob = new Blob(this.recordedChunks, {
          type: 'audio/webm;codecs=opus'
        });

        if (blob.size > 0 && this.socket && this.socket.readyState === WebSocket.OPEN) {
          this.socket.send(blob);
          this.socket.send('stop_recording');
          this.showTypingIndicator();
        } else {
          this.showNotification('No audio recorded or connection lost', 'error');
        }

        this.recordedChunks = [];
      };

      this.mediaRecorder.onerror = (event) => {
        console.error('MediaRecorder error:', event.error);
        this.showNotification('Recording error: ' + event.error.message, 'error');
        this.isRecording = false;
        this.updateVoiceButtonState();
      };

      this.mediaRecorder.start(1000);
      this.isRecording = true;
      this.updateVoiceButtonState();

      setTimeout(() => {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
          this.socket.send('start_recording');
        }
      }, 50);

    } catch (error) {
      console.error('Failed to start recording:', error);
      this.showNotification('Recording failed: ' + error.message, 'error');
      this.isRecording = false;
      this.updateVoiceButtonState();
    }
  }

  stopRecording() {
    if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
      this.mediaRecorder.stop();
      this.isRecording = false;
      this.updateVoiceButtonState();
    }
  }

  updateVoiceButtonState() {
    this.voiceToggle.classList.toggle('recording', this.isRecording);
    this.voiceToggle.classList.toggle('active', this.isVoiceMode && this.isConnected);
    this.voiceToggle.classList.toggle('processing', false);
  }

  // UNIFIED MESSAGE DISPLAY
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

  displayVoiceProcessingMessage() {
    const messageGroup = document.createElement('div');
    messageGroup.className = 'message-group user-message';
    messageGroup.id = 'voice-processing-indicator';
    // Force right alignment with inline styles
    messageGroup.style.alignItems = 'flex-end';
    messageGroup.style.justifyContent = 'flex-end';

    const messageBubble = document.createElement('div');
    messageBubble.className = 'message-bubble user processing';
    // Force right positioning
    messageBubble.style.marginLeft = 'auto';
    messageBubble.style.marginRight = '0';

    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    messageContent.innerHTML = '🎤 Converting speech to text...';

    const progressBar = document.createElement('div');
    progressBar.className = 'voice-progress-bar';
    progressBar.innerHTML = '<div class="voice-progress-fill"></div>';

    messageBubble.appendChild(messageContent);
    messageBubble.appendChild(progressBar);
    messageGroup.appendChild(messageBubble);

    // Insert before typing indicator
    this.chatArea.insertBefore(messageGroup, this.typingIndicator);
    this.scrollToBottom();

    return messageGroup;
  }

  hideVoiceProcessingMessage() {
    const indicator = document.getElementById('voice-processing-indicator');
    if (indicator) {
      indicator.remove();
    }
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

  // UNIFIED TTS GENERATION AND PLAYBACK
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

      // Auto-play the generated audio - simple approach
      this.audioPlayer.src = audioUrl;

      try {
        // Simple approach - just play when ready
        this.audioPlayer.oncanplay = async () => {
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
      // Don't show error notification for TTS failures to avoid cluttering UX
    }
  }

  async handleAudioButtonClick(button) {
    const text = button.getAttribute('data-text');
    const audioPlayer = button.parentElement.querySelector('.message-audio-player');

    if (button.classList.contains('playing')) {
      // Pause current audio
      audioPlayer.pause();
      button.classList.remove('playing');
      return;
    }

    // Stop any other playing audio
    this.stopAllAudio();

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

      const audioLength = button.parentElement.querySelector('.audio-length');

      button.classList.remove('loading');
      button.classList.add('playing');

      // Simple playback approach
      audioPlayer.oncanplay = async () => {
        try {
          await audioPlayer.play();
        } catch (err) {
          console.error('Audio playback failed:', err);
        }
      };
      audioPlayer.load();

      // Update audio length when metadata loads
      audioPlayer.onloadedmetadata = () => {
        if (audioLength && !audioLength.textContent) {
          const duration = this.formatAudioDuration(audioPlayer.duration);
          audioLength.textContent = duration;
          audioLength.style.display = 'inline';
        }
      };

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
    // Stop main audio player
    if (this.audioPlayer) {
      this.audioPlayer.pause();
      this.audioPlayer.currentTime = 0;
    }

    // Stop all message audio players
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

// Initialize the unified chat interface when DOM is loaded
document.addEventListener('DOMContentLoaded', function () {
  const chatInterface = new UnifiedChatInterface();

  // Make it available globally for debugging
  window.chatInterface = chatInterface;

  console.log('Unified chat interface initialized');
});
