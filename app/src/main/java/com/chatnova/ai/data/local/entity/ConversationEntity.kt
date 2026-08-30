package com.chatnova.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatnova.ai.domain.model.Conversation

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String,
    val systemPrompt: String?
) {
    fun toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        modelId = modelId,
        systemPrompt = systemPrompt
    )

    companion object {
        fun fromDomain(domain: Conversation) = ConversationEntity(
            id = domain.id,
            title = domain.title,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            modelId = domain.modelId,
            systemPrompt = domain.systemPrompt
        )
    }
}
