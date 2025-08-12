// Voice Chat WebSocket functionality for ConversationalAI4J Demo
// Handles real-time voice recording, WebSocket communication, and audio playback

// Global variables for voice chat functionality
let socket, audioStream, mediaRecorder, isConnected = false, isRecording = false, recordedChunks = [];

// Initialize voice chat functionality when DOM is loaded
document.addEventListener('DOMContentLoaded', function () {
  const voiceBtn = document.getElementById('voice-btn');
  if (voiceBtn) {
    voiceBtn.addEventListener('mousedown', startRecording);
    voiceBtn.addEventListener('mouseup', stopRecording);
    voiceBtn.addEventListener('contextmenu', e => e.preventDefault());
    updateUI();
  }
});

/**
 * Connect to voice stream by requesting microphone access and setting up WebSocket
 */
function connectVoiceStream() {
  showStatus('info', 'Connecting...');

  navigator.mediaDevices.getUserMedia({audio: {sampleRate: 16000, channelCount: 1}})
    .then(stream => {
      audioStream = stream;
      setupWebSocket();
    })
    .catch(error => showStatus('error', 'Microphone access failed: ' + error.message));
}

/**
 * Set up WebSocket connection for real-time voice communication
 */
function setupWebSocket() {
  socket = new WebSocket(`ws://${window.location.host}/voice-stream`);
  console.log('[voice] opening WebSocket to', `ws://${window.location.host}/voice-stream`);

  socket.onopen = function () {
    console.log('[voice] WebSocket opened');
    isConnected = true;
    updateUI();
    showStatus('success', 'Connected! Hold microphone to talk.');
    try {
      socket.send('check_status');
    } catch (e) {
      console.warn('[voice] send check_status failed', e);
    }
  };

  socket.onmessage = function (event) {
    if (event.data instanceof Blob) {
      handleAudioResponse(event.data);
    } else {
      try {
        handleControlMessage(JSON.parse(event.data));
      } catch (e) {
        console.log('Message:', event.data);
      }
    }
  };

  socket.onerror = (e) => {
    console.error('[voice] WebSocket error', e);
    showStatus('error', 'Connection error');
  };

  socket.onclose = function (evt) {
    console.log('[voice] WebSocket closed', {
      code: evt.code,
      reason: evt.reason,
      wasClean: evt.wasClean
    });
    isConnected = false;
    updateUI();
    showStatus('info', 'Disconnected');
    cleanup();
  };
}

/**
 * Handle control messages from the WebSocket (JSON format)
 * @param {Object} message - The parsed JSON message
 */
function handleControlMessage(message) {
  console.log('[voice] control message', message);
  if (message.type === 'status') {
    const statusType = message.status === 'error' ? 'error' :
      message.status === 'complete' ? 'success' : 'info';
    showStatus(statusType, message.message);
  } else if (message.type === 'transcription') {
    // Show what the user said (speech-to-text result)
    displayTranscription(message.text);
    showStatus('info', 'Speech recognized: ' + message.text);
  } else if (message.type === 'text_response') {
    // Handle text response when audio processing is not available
    displayTextResponse(message.message);
    showStatus('success', 'AI responded (text mode)');
  }
}

/**
 * Handle audio response from the server (binary audio data)
 * @param {Blob} audioBlob - The audio response as a blob
 */
function handleAudioResponse(audioBlob) {
  console.log('Received audio response:', audioBlob.size, 'bytes');

  const audioUrl = URL.createObjectURL(audioBlob);
  const audioEl = document.getElementById('ai-response');
  audioEl.src = audioUrl;
  audioEl.style.display = 'block';

  // Hide text response when showing audio
  document.getElementById('text-response').style.display = 'none';

  audioEl.play()
    .then(() => showStatus('success', 'Playing AI speech response'))
    .catch(err => showStatus('error',
      'Audio playback failed' + (err && err.message ? ': ' + err.message : '')));

  audioEl.onended = () => URL.revokeObjectURL(audioUrl);
}

/**
 * Disconnect from voice stream and clean up resources
 */
function disconnectVoiceStream() {
  cleanup();
}

/**
 * Start recording audio when microphone button is pressed
 * @param {Event} event - The mouse down event
 */
function startRecording(event) {
  event.preventDefault();

  if (!isConnected || !audioStream || isRecording) {
    return;
  }

  try {
    console.log('[voice] starting recording');
    mediaRecorder = new MediaRecorder(audioStream, {mimeType: 'audio/webm;codecs=opus'});
    recordedChunks = [];

    mediaRecorder.ondataavailable = event => {
      if (event.data && event.data.size > 0) {
        recordedChunks.push(event.data);
        console.log('[voice] ondataavailable chunk size', event.data.size);
      }
    };

    mediaRecorder.onstart = () => {
      console.log('[voice] MediaRecorder started, state=', mediaRecorder.state);
    };

    mediaRecorder.onstop = () => {
      try {
        const blob = new Blob(recordedChunks, {type: 'audio/webm;codecs=opus'});
        console.log('[voice] MediaRecorder stopped; sending final blob size', blob.size);
        if (blob.size > 0) {
          socket.send(blob);
        }
        socket.send('stop_recording');
        recordedChunks = [];
      } catch (e) {
        showStatus('error', 'Failed to send audio: ' + e.message);
      }
    };

    mediaRecorder.start();
    isRecording = true;
    updateUI();
    socket.send('start_recording');
    showStatus('info', 'Recording...');

  } catch (error) {
    showStatus('error', 'Recording failed: ' + error.message);
  }
}

/**
 * Stop recording audio when microphone button is released
 * @param {Event} event - The mouse up event
 */
function stopRecording(event) {
  event.preventDefault();

  if (!isRecording) {
    return;
  }

  console.log('[voice] stopRecording invoked; state before stop=',
    mediaRecorder && mediaRecorder.state);
  mediaRecorder.stop();
  isRecording = false;
  updateUI();
  showStatus('info', 'Processing...');
}

/**
 * Update the UI elements based on current connection and recording state
 */
function updateUI() {
  const voiceBtn = document.getElementById('voice-btn');
  const connectBtn = document.getElementById('connect-btn');
  const disconnectBtn = document.getElementById('disconnect-btn');

  if (connectBtn) connectBtn.style.display = isConnected ? 'none' : 'inline-block';
  if (disconnectBtn) disconnectBtn.style.display = isConnected ? 'inline-block' : 'none';
  if (voiceBtn) {
    voiceBtn.disabled = !isConnected;
    voiceBtn.classList.toggle('recording', isRecording);
    voiceBtn.textContent = isRecording ? '🔴' : '🎤';
  }
}

/**
 * Show status message to the user
 * @param {string} type - Status type ('info', 'success', 'error')
 * @param {string} message - The message to display
 */
function showStatus(type, message) {
  const statusDiv = document.getElementById('status');
  if (statusDiv) {
    statusDiv.className = 'status ' + type;
    statusDiv.textContent = message;
    statusDiv.classList.remove('hidden');

    if (type === 'success') {
      setTimeout(() => statusDiv.classList.add('hidden'), 3000);
    }
  }
}

/**
 * Display transcription result (speech-to-text)
 * @param {string} text - The transcribed text
 */
function displayTranscription(text) {
  const transcriptionDiv = document.getElementById('transcription');
  const transcriptionContent = document.getElementById('transcription-content');

  if (transcriptionContent) {
    transcriptionContent.textContent = text;
  }
  if (transcriptionDiv) {
    transcriptionDiv.style.display = 'block';
  }
}

/**
 * Display text response from AI
 * @param {string} text - The AI response text
 */
function displayTextResponse(text) {
  const textResponseDiv = document.getElementById('text-response');
  const textContent = document.getElementById('text-response-content');

  if (textContent) {
    textContent.textContent = text;
  }
  if (textResponseDiv) {
    textResponseDiv.style.display = 'block';
  }

  // Hide audio player when showing text
  const audioEl = document.getElementById('ai-response');
  if (audioEl) {
    audioEl.style.display = 'none';
  }
}

/**
 * Clean up all resources (WebSocket, MediaRecorder, audio streams)
 */
function cleanup() {
  console.log('[voice] cleanup called');
  
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    console.log('[voice] stopping active MediaRecorder; state=', mediaRecorder.state);
    mediaRecorder.stop();
  }
  
  if (audioStream) {
    console.log('[voice] stopping audio tracks');
    audioStream.getTracks().forEach(track => track.stop());
    audioStream = null;
  }
  
  if (socket) {
    console.log('[voice] closing WebSocket');
    socket.close();
    socket = null;
  }

  mediaRecorder = null;
  isRecording = false;
  isConnected = false;
  updateUI();
}