package com.chatnova.ai.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatnova.ai.domain.model.*
import com.chatnova.ai.domain.repository.ChatRepository
import com.chatnova.ai.domain.repository.ModelRepository
import com.chatnova.ai.domain.repository.SettingsRepository
import com.chatnova.ai.util.FileAttachmentHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val conversationId: String? = null,
    val conversationTitle: String = "New Chat",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val pendingAttachments: List<Attachment> = emptyList(),
    val selectedModelId: String = "stealth/ox-alpha",
    val availableModels: List<AiModel> = emptyList(),
    val isGenerating: Boolean = false,
    val sendOnEnter: Boolean = false,
    val enableMarkdown: Boolean = true,
    val showTimestamps: Boolean = true,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentGenerationJob: Job? = null
    private var messagesJob: Job? = null
    private var activeConversationId: String? = null

    init {
        // Observe Settings
        viewModelScope.launch {
            settingsRepository.chatSettings.collect { settings ->
                _uiState.update {
                    it.copy(
                        sendOnEnter = settings.sendOnEnter,
                        enableMarkdown = settings.enableMarkdown,
                        showTimestamps = settings.showTimestamps,
                        selectedModelId = if (activeConversationId == null) settings.defaultModelId else it.selectedModelId
                    )
                }
            }
        }

        // Observe Models
        viewModelScope.launch {
            modelRepository.getCachedModels().collect { models ->
                _uiState.update { it.copy(availableModels = models) }
            }
        }
    }

    fun loadConversation(conversationId: String?) {
        activeConversationId = conversationId
        messagesJob?.cancel()

        if (conversationId == null) {
            _uiState.update {
                it.copy(
                    conversationId = null,
                    conversationTitle = "New Chat",
                    messages = emptyList(),
                    pendingAttachments = emptyList(),
                    errorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            val conv = chatRepository.getConversation(conversationId)
            if (conv != null) {
                _uiState.update {
                    it.copy(
                        conversationId = conv.id,
                        conversationTitle = conv.title,
                        selectedModelId = conv.modelId
                    )
                }
            }
        }

        messagesJob = viewModelScope.launch {
            chatRepository.getMessages(conversationId).collect { msgList ->
                _uiState.update { it.copy(messages = msgList) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSelectModel(modelId: String) {
        _uiState.update { it.copy(selectedModelId = modelId) }
        activeConversationId?.let { convId ->
            viewModelScope.launch {
                chatRepository.updateConversationModel(convId, modelId)
            }
        }
    }

    fun addAttachmentFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = FileAttachmentHelper.processUri(context, uri)
            result.onSuccess { attachment ->
                _uiState.update {
                    it.copy(pendingAttachments = it.pendingAttachments + attachment)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.localizedMessage) }
            }
        }
    }

    fun removeAttachment(attachment: Attachment) {
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments.filter { it.id != attachment.id })
        }
    }

    fun startNewChat() {
        stopGeneration()
        messagesJob?.cancel()
        activeConversationId = null
        _uiState.update {
            it.copy(
                conversationId = null,
                conversationTitle = "New Chat",
                messages = emptyList(),
                inputText = "",
                pendingAttachments = emptyList(),
                errorMessage = null
            )
        }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val text = currentState.inputText.trim()
        val attachments = currentState.pendingAttachments

        if (text.isBlank() && attachments.isEmpty()) return
        if (currentState.isGenerating) return

        viewModelScope.launch {
            val isNewConv = (activeConversationId == null)
            val convId = activeConversationId ?: run {
                val newId = UUID.randomUUID().toString()
                activeConversationId = newId
                val newConv = Conversation(
                    id = newId,
                    title = "New Chat",
                    modelId = currentState.selectedModelId
                )
                chatRepository.createConversation(newConv)
                _uiState.update { it.copy(conversationId = newId) }
                newId
            }

            // Create User Message
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = MessageRole.USER,
                content = text,
                status = MessageStatus.SUCCESS,
                attachments = attachments
            )

            // Save to Room DB
            chatRepository.saveMessage(userMessage)

            // Start observing messages if needed
            if (isNewConv || messagesJob == null || messagesJob?.isActive == false) {
                messagesJob?.cancel()
                messagesJob = launch {
                    chatRepository.getMessages(convId).collect { msgList ->
                        _uiState.update { it.copy(messages = msgList) }
                    }
                }
            }

            // Immediately reflect in UI State
            _uiState.update {
                it.copy(
                    inputText = "",
                    pendingAttachments = emptyList(),
                    errorMessage = null,
                    messages = (it.messages + userMessage).distinctBy { m -> m.id }
                )
            }

            // Trigger AI Stream Response with explicit message history
            triggerAiResponse(convId, _uiState.value.messages)
        }
    }

    fun retryLastMessage() {
        val convId = activeConversationId ?: return
        val validHistory = _uiState.value.messages.filter { it.status != MessageStatus.ERROR }
        triggerAiResponse(convId, validHistory)
    }

    private fun triggerAiResponse(conversationId: String, currentHistory: List<ChatMessage>) {
        stopGeneration()

        currentGenerationJob = viewModelScope.launch {
            val assistantMessageId = UUID.randomUUID().toString()
            val initialAssistantMsg = ChatMessage(
                id = assistantMessageId,
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                status = MessageStatus.STREAMING
            )
            chatRepository.saveMessage(initialAssistantMsg)

            _uiState.update {
                it.copy(
                    isGenerating = true,
                    errorMessage = null,
                    messages = (it.messages + initialAssistantMsg).distinctBy { m -> m.id }
                )
            }

            val settings = settingsRepository.chatSettings.first()
            val instructions = settingsRepository.customInstructions.first()
            val fullInstruction = if (instructions.isEnabled && instructions.globalInstructions.isNotBlank()) {
                instructions.globalInstructions + "\nResponse Style: ${instructions.responseStyle}"
            } else null

            var accumulatedText = ""

            chatRepository.streamChatCompletion(
                conversationId = conversationId,
                messages = currentHistory,
                modelId = _uiState.value.selectedModelId,
                temperature = settings.temperature,
                topP = settings.topP,
                maxTokens = settings.maxTokens,
                systemInstruction = fullInstruction,
                onChunk = { chunk ->
                    accumulatedText += chunk
                    chatRepository.updateMessage(
                        initialAssistantMsg.copy(
                            content = accumulatedText,
                            status = MessageStatus.STREAMING
                        )
                    )
                },
                onError = { error ->
                    _uiState.update { it.copy(isGenerating = false, errorMessage = error) }
                    chatRepository.updateMessage(
                        initialAssistantMsg.copy(
                            content = accumulatedText,
                            status = MessageStatus.ERROR,
                            errorMessage = error
                        )
                    )
                },
                onComplete = { fullText ->
                    _uiState.update { it.copy(isGenerating = false) }
                    chatRepository.updateMessage(
                        initialAssistantMsg.copy(
                            content = fullText,
                            status = MessageStatus.SUCCESS
                        )
                    )
                }
            )
        }
    }

    fun stopGeneration() {
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            chatRepository: ChatRepository,
            modelRepository: ModelRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(chatRepository, modelRepository, settingsRepository) as T
            }
        }
    }
}
