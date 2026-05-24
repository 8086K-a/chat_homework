package com.example.chat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.data.local.AppDatabase
import com.example.chat.data.local.entity.ConversationEntity
import com.example.chat.data.remote.OpenAiService
import com.example.chat.data.remote.SimpleInputItem
import com.example.chat.data.repository.ChatRepository
import com.example.chat.model.ChatMessage
import com.example.chat.model.Role
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val apiService = OpenAiService()
    private val repository = ChatRepository(db, apiService)

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _selectedImageDataUrl = MutableStateFlow<String?>(null)
    val selectedImageDataUrl: StateFlow<String?> = _selectedImageDataUrl.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var modelName: String = OpenAiService.DEFAULT_MODEL
    private val _selectedModel = MutableStateFlow(OpenAiService.DEFAULT_MODEL)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _baseUrl = MutableStateFlow(apiService.currentBaseUrl())
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _apiKey = MutableStateFlow(apiService.currentApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getConversations().collect { list ->
                _conversations.value = list
            }
        }
    }

    fun selectConversation(conversationId: String) {
        if (conversationId.isBlank()) return
        if (_currentConversationId.value == conversationId) return
        _currentConversationId.value = conversationId
        observeConversationMessages(conversationId)
    }

    fun newConversation() {
        _streamingContent.value = ""
        _messages.value = emptyList()
        _currentConversationId.value = null
    }

    fun deleteConversation(conversationId: String) {
        if (_currentConversationId.value == conversationId) {
            messagesJob?.cancel()
            messagesJob = null
        }
        viewModelScope.launch {
            try {
                repository.deleteConversation(conversationId)
            } catch (_: Exception) { }
            if (_currentConversationId.value == conversationId) {
                newConversation()
            }
        }
    }

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val imageDataUrl = _selectedImageDataUrl.value
        if ((text.isBlank() && imageDataUrl == null) || _isLoading.value) return

        _selectedImageDataUrl.value = null
        sendMessageInternal(text = text, imageDataUrl = imageDataUrl, saveUserMessage = true)
    }

    fun setSelectedImage(dataUrl: String) {
        _selectedImageDataUrl.value = dataUrl
    }

    fun clearSelectedImage() {
        _selectedImageDataUrl.value = null
    }

    fun regenerateLastResponse() {
        if (_isLoading.value) return

        viewModelScope.launch {
            val convId = _currentConversationId.value ?: return@launch
            val currentMessages = repository.getMessages(convId).first()
            val lastUserIndex = currentMessages.indexOfLast { it.role == Role.User }
            if (lastUserIndex < 0) return@launch

            val input = buildInput(currentMessages.take(lastUserIndex + 1))

            _error.value = null
            _isLoading.value = true
            _streamingContent.value = ""

            try {
                streamAssistantReply(convId, input)
            } catch (e: Exception) {
                _error.value = e.message ?: "发送失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildInput(messages: List<ChatMessage>): Any {
        val items = mutableListOf<Any>()

        for (msg in messages) {
            if (msg.imageDataUrl != null) {
                val parts = mutableListOf<Map<String, Any>>()
                if (msg.content.isNotBlank()) {
                    parts.add(mapOf("type" to "input_text", "text" to msg.content))
                }
                parts.add(mapOf("type" to "input_image", "image_url" to msg.imageDataUrl))
                items.add(mapOf(
                    "role" to if (msg.role == Role.User) "user" else "assistant",
                    "content" to parts,
                ))
            } else {
                items.add(SimpleInputItem(
                    role = if (msg.role == Role.User) "user" else "assistant",
                    content = msg.content,
                ))
            }
        }

        return if (items.size == 1) {
            val first = items[0]
            if (first is SimpleInputItem) first.content else items
        } else {
            items
        }
    }

    private fun sendMessageInternal(text: String, imageDataUrl: String?, saveUserMessage: Boolean) {
        _inputText.value = ""
        _error.value = null
        _isLoading.value = true
        _streamingContent.value = ""

        viewModelScope.launch {
            try {
                var convId = _currentConversationId.value
                if (convId == null) {
                    val conv = repository.createConversation(
                        title = if (text.length > 30) text.take(30) + "..." else text
                    )
                    convId = conv.id
                    _currentConversationId.value = convId
                    observeConversationMessages(convId)
                }

                if (saveUserMessage) {
                    try {
                        val displayText = when {
                            text.isNotBlank() && imageDataUrl != null -> text
                            text.isBlank() && imageDataUrl != null -> "[图片]"
                            else -> text
                        }
                        repository.saveMessage(
                            convId, Role.User, displayText,
                            imageDataUrl = imageDataUrl,
                        )
                    } catch (_: Exception) { }
                }

                val allMessages = try {
                    repository.getMessages(convId).first()
                } catch (_: Exception) { emptyList() }

                if (allMessages.isEmpty()) {
                    _isLoading.value = false
                    return@launch
                }

                val input = buildInput(allMessages)

                streamAssistantReply(convId, input)

                try {
                    val title = if (text.length > 30) text.take(30) + "..." else text
                    repository.updateConversationTitle(convId, title)
                } catch (_: Exception) { }

            } catch (e: Exception) {
                _error.value = e.message ?: "发送失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun streamAssistantReply(convId: String, input: Any) {
        val fullContent = StringBuilder()
        var lastUpdate = 0L
        var lastEmittedLength = 0

        try {
            repository.sendMessageStream(input, modelName).collect { token ->
                fullContent.append(token)
                val now = System.currentTimeMillis()
                val currentLength = fullContent.length
                val deltaLength = currentLength - lastEmittedLength
                val shouldEmitByLength = deltaLength >= 36
                val shouldEmitByTime = now - lastUpdate >= 220
                val shouldEmitByBoundary = token.contains('\n') || token.endsWith("|")

                if (shouldEmitByLength || shouldEmitByTime || shouldEmitByBoundary) {
                    _streamingContent.value = fullContent.toString()
                    lastUpdate = now
                    lastEmittedLength = currentLength
                }
            }
        } catch (_: Exception) { }

        val finalContent = fullContent.toString()
        _streamingContent.value = finalContent

        if (finalContent.isNotBlank()) {
            try {
                repository.saveMessage(convId, Role.Assistant, finalContent)
            } catch (_: Exception) { }
        }
        _streamingContent.value = ""
    }

    fun updateApiConfig(baseUrl: String, apiKey: String, model: String) {
        apiService.updateApiConfig(baseUrl, apiKey)
        _baseUrl.value = apiService.currentBaseUrl()
        _apiKey.value = apiService.currentApiKey()
        this@ChatViewModel.modelName = model
        _selectedModel.value = model
    }

    fun clearError() {
        _error.value = null
    }

    private fun observeConversationMessages(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            try {
                repository.getMessages(conversationId).collect { list ->
                    if (_currentConversationId.value == conversationId) {
                        _messages.value = list
                    }
                }
            } catch (_: Exception) { }
        }
    }
}
