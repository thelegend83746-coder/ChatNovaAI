package com.chatnova.ai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chatnova.ai.domain.model.Attachment
import com.chatnova.ai.domain.model.ChatMessage
import com.chatnova.ai.domain.model.MessageRole
import com.chatnova.ai.domain.model.MessageStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val status: String,
    val errorMessage: String?,
    val attachmentsJson: String?
) {
    fun toDomain(gson: Gson): ChatMessage {
        val attachmentList: List<Attachment> = if (!attachmentsJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<Attachment>>() {}.type
                gson.fromJson(attachmentsJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = try { MessageRole.valueOf(role) } catch (e: Exception) { MessageRole.USER },
            content = content,
            timestamp = timestamp,
            status = try { MessageStatus.valueOf(status) } catch (e: Exception) { MessageStatus.SUCCESS },
            errorMessage = errorMessage,
            attachments = attachmentList
        )
    }

    companion object {
        fun fromDomain(domain: ChatMessage, gson: Gson): MessageEntity {
            val json = if (domain.attachments.isNotEmpty()) gson.toJson(domain.attachments) else null
            return MessageEntity(
                id = domain.id,
                conversationId = domain.conversationId,
                role = domain.role.name,
                content = domain.content,
                timestamp = domain.timestamp,
                status = domain.status.name,
                errorMessage = domain.errorMessage,
                attachmentsJson = json
            )
        }
    }
}
