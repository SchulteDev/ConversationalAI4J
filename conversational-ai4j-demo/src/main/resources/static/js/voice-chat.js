// ConversationalAI4J - Modern Chat Interface
// Unified text and voice chat with notification system

class ChatInterface {
  constructor() {
    this.socket = null;
    this.audioStream = null;
    this.mediaRecorder = null;
    this.isVoiceMode = false;
    this.isConnected = false;
    this.isRecording = false;
    this.recordedChunks = [];
    
    this.initializeElements();
    this.setupEventListeners();
    this.setupAudioControls();
    this.scrollToBottom();
    
    // No automatic TTS - text is shown first, audio on demand
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
    
    // Chat message templates
    this.typingIndicator = document.getElementById('typing-indicator');
  }

  setupEventListeners() {
    // Voice toggle button - click to toggle recording
    this.voiceToggle.addEventListener('click', (e) => {
      e.preventDefault();
      this.handleVoiceToggle();
    });
    
    // Form submission
    this.messageForm.addEventListener('submit', (e) => this.handleFormSubmit(e));
    
    // Enter key in input
    this.messageInput.addEventListener('keypress', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        const message = this.messageInput.value.trim();
        if (message) {
          this.showTypingIndicator();
          // Don't clear here - let form submit with the value
          this.messageForm.submit();
          // Clear after a short delay to ensure form submission gets the value
          setTimeout(() => {
            this.messageInput.value = '';
          }, 10);
        }
      }
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
      
      button.classList.remove('loading');
      button.classList.add('playing');
      
      audioPlayer.play();
      
      audioPlayer.onended = () => {
        button.classList.remove('playing');
        URL.revokeObjectURL(audioUrl);
      };
      
      audioPlayer.onerror = () => {
        button.classList.remove('playing', 'loading');
        console.error('Audio playback failed');
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

  async handleVoiceToggle() {
    if (!this.isVoiceMode) {
      // Enable voice mode and start recording immediately
      try {
        await this.requestMicrophoneAccess();
        await this.connectWebSocket();
        this.isVoiceMode = true;
        this.voiceToggle.classList.add('active');
        this.messageInput.placeholder = "Recording... Click again to stop and send.";
        this.showNotification('Recording started! Click microphone again to stop and send.', 'success');
        
        // Start recording with a small delay to ensure WebSocket is fully ready
        setTimeout(async () => {
          await this.startRecording();
        }, 200);
      } catch (error) {
        this.showNotification('Failed to enable voice mode: ' + error.message, 'error');
        this.isVoiceMode = false;
        this.voiceToggle.classList.remove('active');
      }
    } else {
      // Stop recording but keep voice mode active until processing completes
      if (this.isRecording) {
        this.stopRecording();
        this.messageInput.placeholder = "Processing voice...";
        this.showNotification('Processing your voice message...', 'info');
      }
      // Don't close WebSocket immediately - let the server processing complete
    }
  }

  async requestMicrophoneAccess() {
    try {
      this.audioStream = await navigator.mediaDevices.getUserMedia({
        audio: { sampleRate: 16000, channelCount: 1 }
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
        
        // Wait a moment before sending status check to ensure connection is stable
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
        
        // If recording was interrupted, show notification
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
          // Close voice mode after successful completion
          this.finishVoiceMode();
        } else if (message.status === 'error') {
          // Close voice mode on error
          this.finishVoiceMode();
        }
        break;
        
      case 'transcription':
        this.displayUserMessage(message.text, true);
        break;
        
      case 'text_response':
        this.displayAIMessage(message.message, false);
        break;
    }
  }
  
  finishVoiceMode() {
    // Clean up voice mode after processing is complete
    setTimeout(() => {
      this.disconnectWebSocket();
      this.isVoiceMode = false;
      this.voiceToggle.classList.remove('active', 'recording');
      this.messageInput.placeholder = "Type a message...";
    }, 1000); // Give time for any final messages
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

  handleFormSubmit(e) {
    const message = this.messageInput.value.trim();
    if (!message) {
      e.preventDefault();
      return;
    }
    
    // Show typing indicator before form submission
    this.showTypingIndicator();
    
    // Input is already cleared by the Enter key handler
    // Let form submit normally - Spring Boot will handle the redirect
    // and Thymeleaf will render the response
    // No preventDefault() - we want the normal form submission behavior
  }

  // Voice toggle now handles start/stop directly

  async startRecording() {
    try {
      // Check if we have a valid stream and socket
      if (!this.audioStream) {
        throw new Error('No audio stream available');
      }
      
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        throw new Error('WebSocket not connected');
      }

      this.mediaRecorder = new MediaRecorder(this.audioStream, {
        mimeType: 'audio/webm;codecs=opus'
      });
      
      this.recordedChunks = [];
      
      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          this.recordedChunks.push(event.data);
          console.log('Audio chunk received:', event.data.size, 'bytes');
        }
      };

      this.mediaRecorder.onstop = () => {
        console.log('MediaRecorder stopped, processing', this.recordedChunks.length, 'chunks');
        
        const blob = new Blob(this.recordedChunks, {
          type: 'audio/webm;codecs=opus'
        });
        
        console.log('Created blob of size:', blob.size, 'bytes');
        
        if (blob.size > 0 && this.socket && this.socket.readyState === WebSocket.OPEN) {
          console.log('Sending audio data to server');
          this.socket.send(blob);
          this.socket.send('stop_recording');
          this.showTypingIndicator();
        } else {
          console.warn('Cannot send audio: blob size =', blob.size, ', socket state =', this.socket?.readyState);
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

      // Start recording with small time slice to ensure data events
      this.mediaRecorder.start(1000); // Request data every 1 second
      this.isRecording = true;
      this.updateVoiceButtonState();
      
      // Send start recording message with delay to ensure socket is stable
      setTimeout(() => {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
          this.socket.send('start_recording');
          console.log('Recording started with MediaRecorder state:', this.mediaRecorder.state);
        } else {
          console.error('Cannot send start_recording - WebSocket not ready');
          this.showNotification('Connection lost - cannot start recording', 'error');
          this.isRecording = false;
          this.updateVoiceButtonState();
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
      console.log('Recording stopped');
    }
  }

  updateVoiceButtonState() {
    this.voiceToggle.classList.toggle('recording', this.isRecording);
    this.voiceToggle.classList.toggle('active', this.isVoiceMode && this.isConnected);
    this.voiceToggle.classList.toggle('processing', false); // Reset processing state
  }

  displayUserMessage(text, isVoiceTranscription = false) {
    // Always show user message as a chat bubble for better conversation flow
    this.addMessageToChat(text, 'user');
    
    this.scrollToBottom();
  }

  displayAIMessage(text, hasAudio = false) {
    // Always show AI message as a chat bubble for better conversation flow
    this.addMessageToChat(text, 'ai');
    
    this.hideTypingIndicator();
    this.scrollToBottom();
  }

  addMessageToChat(text, sender) {
    const messageGroup = document.createElement('div');
    messageGroup.className = `message-group ${sender}-message`;
    
    const messageBubble = document.createElement('div');
    messageBubble.className = `message-bubble ${sender}`;
    
    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    messageContent.textContent = text;
    
    messageBubble.appendChild(messageContent);
    messageGroup.appendChild(messageBubble);
    
    // Insert before typing indicator if it exists
    const typingIndicator = document.getElementById('typing-indicator');
    this.chatArea.insertBefore(messageGroup, typingIndicator);
    
    this.scrollToBottom();
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
    
    // Auto-hide after delay
    setTimeout(() => {
      this.notificationOverlay.classList.add('hidden');
    }, type === 'error' ? 5000 : 3000);
  }

  scrollToBottom() {
    setTimeout(() => {
      this.chatArea.scrollTop = this.chatArea.scrollHeight;
    }, 100);
  }

  // Text is displayed first, audio is available via play buttons
}

// Initialize the chat interface when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
  const chatInterface = new ChatInterface();
  
  // Make it available globally for debugging
  window.chatInterface = chatInterface;
  
  console.log('Chat interface initialized');
});

// Legacy function names for backward compatibility (in case they're called elsewhere)
function connectVoiceStream() {
  console.warn('connectVoiceStream is deprecated. Use the voice toggle button instead.');
}

function disconnectVoiceStream() {
  console.warn('disconnectVoiceStream is deprecated. Use the voice toggle button instead.');
}
