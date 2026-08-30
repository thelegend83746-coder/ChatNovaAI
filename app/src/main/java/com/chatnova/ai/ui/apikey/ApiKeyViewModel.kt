package com.chatnova.ai.ui.apikey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatnova.ai.domain.model.ApiConfig
import com.chatnova.ai.domain.model.ApiStatus
import com.chatnova.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ApiKeyUiState(
    val apiKeyInput: String = "",
    val isKeyVisible: Boolean = false,
    val apiConfig: ApiConfig = ApiConfig(),
    val isTesting: Boolean = false,
    val testResultMessage: String? = null,
    val isSuccess: Boolean = false
)

class ApiKeyViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeyUiState())
    val uiState: StateFlow<ApiKeyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.apiConfig.collect { config ->
                _uiState.update {
                    it.copy(
                        apiConfig = config,
                        apiKeyInput = if (it.apiKeyInput.isBlank()) config.apiKey else it.apiKeyInput
                    )
                }
            }
        }
    }

    fun onApiKeyChanged(key: String) {
        _uiState.update { it.copy(apiKeyInput = key, testResultMessage = null) }
    }

    fun toggleKeyVisibility() {
        _uiState.update { it.copy(isKeyVisible = !it.isKeyVisible) }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        viewModelScope.launch {
            settingsRepository.saveApiKey(key)
            testApiKey()
        }
    }

    fun testApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) {
            _uiState.update {
                it.copy(
                    testResultMessage = "Please enter an OpenRouter API key.",
                    isSuccess = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResultMessage = null) }
            val result = settingsRepository.testApiKey(key)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResultMessage = "Connected successfully! Models are accessible.",
                        isSuccess = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResultMessage = error.localizedMessage ?: "Failed to connect.",
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun removeApiKey() {
        viewModelScope.launch {
            settingsRepository.removeApiKey()
            _uiState.update {
                it.copy(
                    apiKeyInput = "",
                    testResultMessage = "API key removed.",
                    isSuccess = false
                )
            }
        }
    }

    companion object {
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ApiKeyViewModel(settingsRepository) as T
                }
            }
    }
}
