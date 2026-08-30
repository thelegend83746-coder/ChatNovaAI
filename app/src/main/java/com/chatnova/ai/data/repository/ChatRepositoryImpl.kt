package com.chatnova.ai.data.repository

import com.chatnova.ai.data.local.dao.ConversationDao
import com.chatnova.ai.data.local.dao.MessageDao
import com.chatnova.ai.data.local.entity.ConversationEntity
import com.chatnova.ai.data.local.entity.MessageEntity
import com.chatnova.ai.data.local.preference.EncryptedPreferenceManager
import com.chatnova.ai.data.remote.OpenRouterApi
import com.chatnova.ai.data.remote.dto.*
import com.chatnova.ai.data.remote.error.ApiException
import com.chatnova.ai.data.remote.sse.SseStreamParser
import com.chatnova.ai.domain.model.*
import com.chatnova.ai.domain.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class ChatRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val openRouterApi: OpenRouterApi,
    private val encryptedPreferenceManager: EncryptedPreferenceManager,
    private val sseStreamParser: SseStreamParser,
    private val gson: Gson
) : ChatRepository {

    override fun getConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getConversation(id: String): Conversation? {
        return conversationDao.getConversationById(id)?.toDomain()
    }

    override suspend fun createConversation(conversation: Conversation) {
        conversationDao.insertOrUpdate(ConversationEntity.fromDomain(conversation))
    }

    override suspend fun updateConversationTitle(id: String, title: String) {
        conversationDao.updateTitle(id, title)
    }

    override suspend fun updateConversationModel(id: String, modelId: String) {
        conversationDao.updateModel(id, modelId)
    }

    override suspend fun deleteConversation(id: String) {
        conversationDao.deleteById(id)
    }

    override suspend fun clearAllConversations() {
        conversationDao.clearAll()
    }

    override fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain(gson) }
        }
    }

    override suspend fun saveMessage(message: ChatMessage) {
        messageDao.insertOrUpdate(MessageEntity.fromDomain(message, gson))

        // Update conversation's updatedAt timestamp
        val conv = conversationDao.getConversationById(message.conversationId)
        if (conv != null) {
            var updatedTitle = conv.title
            if ((conv.title == "New Chat" || conv.title.isBlank()) && message.role == MessageRole.USER) {
                updatedTitle = generateTitle(message.content)
            }
            conversationDao.insertOrUpdate(
                conv.copy(
                    title = updatedTitle,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun deleteMessage(id: String) {
        messageDao.deleteById(id)
    }

    override suspend fun updateMessage(message: ChatMessage) {
        messageDao.insertOrUpdate(MessageEntity.fromDomain(message, gson))
    }

    override suspend fun streamChatCompletion(
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
    ) = withContext(Dispatchers.IO) {
        val apiKey = encryptedPreferenceManager.getApiKey()
        if (apiKey.isBlank()) {
            val error = "OpenRouter API key is missing. Please add your API key in Settings or the API Key screen."
            onError(error)
            return@withContext
        }

        val requestMessages = mutableListOf<MessageRequestDto>()

        // 1. Add System prompt if present
        if (!systemInstruction.isNullOrBlank()) {
            requestMessages.add(
                MessageRequestDto(
                    role = "system",
                    content = systemInstruction
                )
            )
        }

        // 2. Format conversation messages (support text, images, and file attachments)
        messages.filter { it.status != MessageStatus.ERROR }.forEach { msg ->
            val roleStr = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }

            val imageAttachments = msg.attachments.filter { it.type == AttachmentType.IMAGE && it.base64Data != null }
            val textAttachments = msg.attachments.filter { it.extractedText != null }

            if (imageAttachments.isNotEmpty()) {
                val parts = mutableListOf<ContentPartDto>()
                var textContent = msg.content
                textAttachments.forEach { att ->
                    textContent += "\n\n[Attached File: ${att.name}]\n${att.extractedText}"
                }
                if (textContent.isNotBlank()) {
                    parts.add(ContentPartDto(type = "text", text = textContent))
                }
                imageAttachments.forEach { img ->
                    parts.add(
                        ContentPartDto(
                            type = "image_url",
                            imageUrl = ImageUrlDto(url = img.base64Data!!)
                        )
                    )
                }
                requestMessages.add(MessageRequestDto(role = roleStr, content = parts))
            } else if (textAttachments.isNotEmpty()) {
                var textContent = msg.content
                textAttachments.forEach { att ->
                    textContent += "\n\n[Attached File: ${att.name}]\n${att.extractedText}"
                }
                requestMessages.add(MessageRequestDto(role = roleStr, content = textContent))
            } else {
                requestMessages.add(MessageRequestDto(role = roleStr, content = msg.content))
            }
        }

        val request = ChatCompletionRequestDto(
            model = modelId,
            messages = requestMessages,
            stream = true,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens
        )

        try {
            val response = openRouterApi.createStreamingChatCompletion(
                authHeader = "Bearer $apiKey",
                request = request
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                val parsedError = parseHttpError(response.code(), errorBody)
                onError(parsedError)
                return@withContext
            }

            val body = response.body()
            if (body == null) {
                onError("Received empty response body from server.")
                return@withContext
            }

            sseStreamParser.parseStream(
                responseBody = body,
                onChunk = onChunk,
                onComplete = onComplete
            )
        } catch (e: CancellationException) {
            // Cancelled by user clicking Stop button
            throw e
        } catch (e: IOException) {
            onError("Network error: Please check your internet connection and try again.")
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "An unexpected error occurred while communicating with OpenRouter.")
        }
    }

    private fun parseHttpError(code: Int, errorBody: String?): String {
        var errorMsg = ""
        if (!errorBody.isNullOrEmpty()) {
            try {
                val errorDto = gson.fromJson(errorBody, ChatCompletionResponseDto::class.java)
                errorMsg = errorDto?.error?.message ?: ""
            } catch (ignored: Exception) {}
        }

        return when (code) {
            401 -> "Authentication failed. Invalid OpenRouter API Key. (${errorMsg.ifEmpty { "Check Settings" }})"
            402 -> "Insufficient credits. Your OpenRouter balance is insufficient for this model."
            403 -> "Access forbidden: ${errorMsg.ifEmpty { "You do not have permission to access this model." }}"
            404 -> "Model not found on OpenRouter: ${errorMsg.ifEmpty { "Please choose another model." }}"
            429 -> "Rate limit reached. Please wait a few seconds before trying again. (${errorMsg.ifEmpty { "Too many requests" }})"
            500, 502, 503 -> "OpenRouter server error (HTTP $code). Please try again shortly."
            else -> "Request failed (HTTP $code): ${errorMsg.ifEmpty { "Unknown error" }}"
        }
    }

    private fun generateTitle(firstMessage: String): String {
        val clean = firstMessage.trim().replace("\n", " ")
        return if (clean.length <= 35) clean else clean.take(35) + "..."
    }
}
