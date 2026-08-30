package com.chatnova.ai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatnova.ai.domain.model.Conversation
import com.chatnova.ai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val conversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val filteredConversations: List<Conversation> = emptyList(),
    val conversationToRename: Conversation? = null,
    val conversationToDelete: Conversation? = null,
    val showClearAllDialog: Boolean = false
)

class HistoryViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getConversations().collect { list ->
                _uiState.update { current ->
                    current.copy(
                        conversations = list,
                        filteredConversations = filterList(list, current.searchQuery)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredConversations = filterList(current.conversations, query)
            )
        }
    }

    private fun filterList(list: List<Conversation>, query: String): List<Conversation> {
        if (query.isBlank()) return list
        return list.filter { it.title.contains(query, ignoreCase = true) }
    }

    fun requestRename(conversation: Conversation) {
        _uiState.update { it.copy(conversationToRename = conversation) }
    }

    fun dismissRename() {
        _uiState.update { it.copy(conversationToRename = null) }
    }

    fun confirmRename(newTitle: String) {
        val conv = _uiState.value.conversationToRename ?: return
        viewModelScope.launch {
            chatRepository.updateConversationTitle(conv.id, newTitle)
            dismissRename()
        }
    }

    fun requestDelete(conversation: Conversation) {
        _uiState.update { it.copy(conversationToDelete = conversation) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(conversationToDelete = null) }
    }

    fun confirmDelete() {
        val conv = _uiState.value.conversationToDelete ?: return
        viewModelScope.launch {
            chatRepository.deleteConversation(conv.id)
            dismissDelete()
        }
    }

    fun requestClearAll() {
        _uiState.update { it.copy(showClearAllDialog = true) }
    }

    fun dismissClearAll() {
        _uiState.update { it.copy(showClearAllDialog = false) }
    }

    fun confirmClearAll() {
        viewModelScope.launch {
            chatRepository.clearAllConversations()
            dismissClearAll()
        }
    }

    companion object {
        fun provideFactory(chatRepository: ChatRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(chatRepository) as T
                }
            }
    }
}
