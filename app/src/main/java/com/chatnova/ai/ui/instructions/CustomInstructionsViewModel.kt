package com.chatnova.ai.ui.instructions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatnova.ai.domain.model.CustomInstructions
import com.chatnova.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InstructionsUiState(
    val isEnabled: Boolean = true,
    val globalInstructions: String = "",
    val responseStyle: String = "Balanced",
    val isSaved: Boolean = false
)

class CustomInstructionsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstructionsUiState())
    val uiState: StateFlow<InstructionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.customInstructions.collect { instructions ->
                _uiState.update {
                    it.copy(
                        isEnabled = instructions.isEnabled,
                        globalInstructions = instructions.globalInstructions,
                        responseStyle = instructions.responseStyle
                    )
                }
            }
        }
    }

    fun onEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled, isSaved = false) }
    }

    fun onGlobalInstructionsChanged(text: String) {
        _uiState.update { it.copy(globalInstructions = text, isSaved = false) }
    }

    fun onResponseStyleChanged(style: String) {
        _uiState.update { it.copy(responseStyle = style, isSaved = false) }
    }

    fun applyPreset(preset: String) {
        val (style, text) = when (preset) {
            "concise" -> Pair("Concise", "Be clear, concise, and direct. Avoid unnecessary greetings and filler words. Provide direct answers and actionable code.")
            "coder" -> Pair("Coding Expert", "You are an expert senior software architect. Write clean, production-ready, compiling code. Follow best design patterns and explain key decisions.")
            "teacher" -> Pair("In-depth", "Explain concepts step-by-step with clear real-world examples, analogies, and detailed breakdowns suitable for thorough understanding.")
            else -> Pair("Balanced", "")
        }
        _uiState.update { it.copy(responseStyle = style, globalInstructions = text, isSaved = false) }
    }

    fun saveInstructions() {
        viewModelScope.launch {
            settingsRepository.updateCustomInstructions(
                CustomInstructions(
                    isEnabled = _uiState.value.isEnabled,
                    globalInstructions = _uiState.value.globalInstructions.trim(),
                    responseStyle = _uiState.value.responseStyle
                )
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            val defaultInstructions = CustomInstructions(isEnabled = true, globalInstructions = "", responseStyle = "Balanced")
            settingsRepository.updateCustomInstructions(defaultInstructions)
            _uiState.update {
                it.copy(
                    isEnabled = true,
                    globalInstructions = "",
                    responseStyle = "Balanced",
                    isSaved = false
                )
            }
        }
    }

    companion object {
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CustomInstructionsViewModel(settingsRepository) as T
                }
            }
    }
}
