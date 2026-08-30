package com.chatnova.ai.domain.repository

import com.chatnova.ai.domain.model.ChatMessage
import com.chatnova.ai.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversations(): Flow<List<Conversation>>
    suspend fun getConversation(id: String): Conversation?
    suspend fun createConversation(conversation: Conversation)
    suspend fun updateConversationTitle(id: String, title: String)
    suspend fun updateConversationModel(id: String, modelId: String)
    suspend fun deleteConversation(id: String)
    suspend fun clearAllConversations()

    fun getMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun deleteMessage(id: String)
    suspend fun updateMessage(message: ChatMessage)

    suspend fun streamChatCompletion(
        conversationId: String,
        messages: List<ChatMessage>,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        systemInstruction: String?,
        onChunk: suspend (String) -> Unit,
        onError: suspend (String) -> Unit,
        onComplete: suspend (String) -> Unit
    )
}
