// ── Send message on form submit ──────────────────────────
function sendMessage(event) {
    event.preventDefault();

    const userInput = document.getElementById('userInput');
    const sendBtn   = document.getElementById('sendBtn');
    const message   = userInput.value.trim();

    if (message === '') return;

    addMessage(message, 'user-message');
    userInput.value = '';
    userInput.disabled = true;
    sendBtn.disabled   = true;

    showTypingIndicator();

    const payload = {
        model: "llama-3.1-8b-instant",
        messages: [{ role: "user", content: message }],
        temperature: 0.7,
        max_tokens: 1024
    };

    fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(response => {
        if (!response.ok) throw new Error('HTTP error, status: ' + response.status);
        return response.json();
    })
    .then(data => {
        removeTypingIndicator();
        if (data.response) {
            addMessage(data.response, 'bot-message');
        } else {
            addMessage('⚠️ No response received.', 'bot-message');
        }
    })
    .catch(error => {
        removeTypingIndicator();
        addMessage('❌ Error: ' + error.message, 'bot-message');
    })
    .finally(() => {
        userInput.disabled = false;
        sendBtn.disabled   = false;
        userInput.focus();
    });
}

// ── Upload PDF to RAG pipeline ───────────────────────────
function uploadPdf(input) {
    const file = input.files[0];
    if (!file) return;

    const uploadStatus = document.getElementById('uploadStatus');
    const uploadBtn    = document.querySelector('.upload-btn');

    // Show uploading state
    uploadStatus.textContent = '⏳ Processing...';
    uploadBtn.style.opacity  = '0.6';

    addMessage('📄 Uploading PDF: ' + file.name, 'user-message');
    showTypingIndicator();

    const formData = new FormData();
    formData.append('file', file);

    fetch('/api/upload', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) throw new Error('Upload failed: ' + response.status);
        return response.json();
    })
    .then(data => {
        removeTypingIndicator();
        uploadBtn.style.opacity = '1';

        if (data.status === 'SUCCESS') {
            uploadStatus.textContent = '✅ ' + file.name;
            addMessage(
                '✅ PDF processed successfully!\n' +
                '📊 ' + data.totalChunks + ' sections indexed.\n' +
                '💬 You can now ask me anything about: ' + data.fileName,
                'bot-message pdf-message'
            );
        } else {
            uploadStatus.textContent = '❌ Failed';
            addMessage('❌ Upload failed: ' + data.message, 'bot-message');
        }
    })
    .catch(error => {
        removeTypingIndicator();
        uploadBtn.style.opacity = '1';
        uploadStatus.textContent = '❌ Error';
        addMessage('❌ Upload error: ' + error.message, 'bot-message');
    })
    .finally(() => {
        // Reset file input so same file can be re-uploaded
        input.value = '';
    });
}

// ── Add message bubble ────────────────────────────────────
function addMessage(text, className) {
    const chatMessages = document.getElementById('chatMessages');
    const messageDiv   = document.createElement('div');
    messageDiv.className = 'message ' + className;

    // Handle newlines in message
    const formattedText = escapeHtml(text).replace(/\n/g, '<br>');
    messageDiv.innerHTML = '<div class="message-content"><p>' + formattedText + '</p></div>';
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// ── Typing indicator ──────────────────────────────────────
function showTypingIndicator() {
    const chatMessages = document.getElementById('chatMessages');
    const typingDiv    = document.createElement('div');
    typingDiv.className = 'message bot-message typing-indicator';
    typingDiv.id        = 'typingIndicator';
    typingDiv.innerHTML =
        '<div class="message-content">' +
        '  <div class="typing-dots">' +
        '    <span></span><span></span><span></span>' +
        '  </div>' +
        '</div>';
    chatMessages.appendChild(typingDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function removeTypingIndicator() {
    const indicator = document.getElementById('typingIndicator');
    if (indicator) indicator.remove();
}

// ── Escape HTML ───────────────────────────────────────────
function escapeHtml(text) {
    const map = { '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;' };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

// ── Enter key to send ─────────────────────────────────────
document.getElementById('userInput').addEventListener('keypress', function(e) {
    if (e.key === 'Enter' && !e.shiftKey) sendMessage(e);
});

document.getElementById('userInput').focus();
