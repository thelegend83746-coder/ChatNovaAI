package com.chatnova.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatnova.ai.domain.model.ChatSettings
import com.chatnova.ai.domain.model.ThemeMode
import com.chatnova.ai.domain.repository.ChatRepository
import com.chatnova.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val chatSettings: ChatSettings = ChatSettings(),
    val cacheFreedBytes: Long? = null,
    val showClearHistoryDialog: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.chatSettings.collect { settings ->
                _uiState.update { it.copy(chatSettings = settings) }
            }
        }
    }

    fun onThemeChanged(themeMode: ThemeMode) {
        updateSettings { it.copy(themeMode = themeMode) }
    }

    fun onSendOnEnterChanged(enabled: Boolean) {
        updateSettings { it.copy(sendOnEnter = enabled) }
    }

    fun onEnableMarkdownChanged(enabled: Boolean) {
        updateSettings { it.copy(enableMarkdown = enabled) }
    }

    fun onShowTimestampsChanged(enabled: Boolean) {
        updateSettings { it.copy(showTimestamps = enabled) }
    }

    fun onStreamResponseChanged(enabled: Boolean) {
        updateSettings { it.copy(streamResponse = enabled) }
    }

    fun onTemperatureChanged(temp: Float) {
        updateSettings { it.copy(temperature = temp) }
    }

    fun onTopPChanged(topP: Float) {
        updateSettings { it.copy(topP = topP) }
    }

    fun onMaxTokensChanged(maxTokens: Int) {
        updateSettings { it.copy(maxTokens = maxTokens) }
    }

    private fun updateSettings(update: (ChatSettings) -> ChatSettings) {
        val newSettings = update(_uiState.value.chatSettings)
        viewModelScope.launch {
            settingsRepository.updateChatSettings(newSettings)
        }
    }

    fun cleanCache() {
        viewModelScope.launch {
            val freed = settingsRepository.clearCache()
            _uiState.update { it.copy(cacheFreedBytes = freed) }
        }
    }

    fun requestClearHistory() {
        _uiState.update { it.copy(showClearHistoryDialog = true) }
    }

    fun dismissClearHistory() {
        _uiState.update { it.copy(showClearHistoryDialog = false) }
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            chatRepository.clearAllConversations()
            dismissClearHistory()
        }
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            chatRepository: ChatRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, chatRepository) as T
            }
        }
    }
}
