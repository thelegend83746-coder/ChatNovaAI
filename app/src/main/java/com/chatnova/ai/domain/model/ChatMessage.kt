package com.chatnova.ai.domain.model

enum class MessageStatus {
    IDLE,
    SENDING,
    STREAMING,
    SUCCESS,
    ERROR
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SUCCESS,
    val errorMessage: String? = null,
    val attachments: List<Attachment> = emptyList()
)
