package com.chatnova.ai.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatnova.ai.domain.model.AiModel
import com.chatnova.ai.domain.repository.ModelRepository
import com.chatnova.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ModelFilter {
    ALL,
    FREE,
    VISION,
    CODING
}

data class ModelsUiState(
    val models: List<AiModel> = emptyList(),
    val filteredModels: List<AiModel> = emptyList(),
    val currentFilter: ModelFilter = ModelFilter.ALL,
    val searchQuery: String = "",
    val defaultModelId: String = "stealth/ox-alpha",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ModelsViewModel(
    private val modelRepository: ModelRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    init {
        // Automatically fetch latest models from OpenRouter API
        refreshModels()

        // Observe Cached Models
        viewModelScope.launch {
            modelRepository.getCachedModels().collect { list ->
                _uiState.update { current ->
                    current.copy(
                        models = list,
                        filteredModels = applyFilterAndSearch(list, current.currentFilter, current.searchQuery)
                    )
                }
            }
        }

        // Observe Default Model Setting
        viewModelScope.launch {
            settingsRepository.chatSettings.collect { settings ->
                _uiState.update { it.copy(defaultModelId = settings.defaultModelId) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredModels = applyFilterAndSearch(current.models, current.currentFilter, query)
            )
        }
    }

    fun onFilterSelected(filter: ModelFilter) {
        _uiState.update { current ->
            current.copy(
                currentFilter = filter,
                filteredModels = applyFilterAndSearch(current.models, filter, current.searchQuery)
            )
        }
    }

    fun refreshModels() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = modelRepository.fetchModels()
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not sync live models: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setDefaultModel(modelId: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultModel(modelId)
            _uiState.update { it.copy(defaultModelId = modelId) }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applyFilterAndSearch(
        models: List<AiModel>,
        filter: ModelFilter,
        query: String
    ): List<AiModel> {
        return models
            .filter { model ->
                when (filter) {
                    ModelFilter.ALL -> true
                    ModelFilter.FREE -> model.isFree
                    ModelFilter.VISION -> model.hasVision
                    ModelFilter.CODING -> model.id.contains("code") ||
                            model.id.contains("coder") ||
                            model.id.contains("qwen") ||
                            model.id.contains("deepseek")
                }
            }
            .filter { model ->
                if (query.isBlank()) true
                else model.name.contains(query, ignoreCase = true) ||
                        model.id.contains(query, ignoreCase = true) ||
                        model.provider.contains(query, ignoreCase = true) ||
                        model.description.contains(query, ignoreCase = true)
            }
    }

    companion object {
        fun provideFactory(
            modelRepository: ModelRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ModelsViewModel(modelRepository, settingsRepository) as T
            }
        }
    }
}
