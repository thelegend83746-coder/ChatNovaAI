package com.chatnova.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequestDto(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<MessageRequestDto>,
    @SerializedName("stream") val stream: Boolean = true,
    @SerializedName("temperature") val temperature: Float? = null,
    @SerializedName("top_p") val topP: Float? = null,
    @SerializedName("max_tokens") val maxTokens: Int? = null
)

data class MessageRequestDto(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: Any
)

data class ContentPartDto(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrlDto? = null
)

data class ImageUrlDto(
    @SerializedName("url") val url: String
)

data class ChatCompletionResponseDto(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<ChoiceDto>?,
    @SerializedName("error") val error: ApiErrorDto?
)

data class ChoiceDto(
    @SerializedName("index") val index: Int?,
    @SerializedName("message") val message: ChoiceMessageDto?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ChoiceMessageDto(
    @SerializedName("role") val role: String?,
    @SerializedName("content") val content: String?
)

data class ChatCompletionChunkDto(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<ChunkChoiceDto>?,
    @SerializedName("error") val error: ApiErrorDto?
)

data class ChunkChoiceDto(
    @SerializedName("index") val index: Int?,
    @SerializedName("delta") val delta: ChunkDeltaDto?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ChunkDeltaDto(
    @SerializedName("role") val role: String?,
    @SerializedName("content") val content: String?
)

data class ApiErrorDto(
    @SerializedName("message") val message: String?,
    @SerializedName("code") val code: Any?,
    @SerializedName("metadata") val metadata: Map<String, Any>?
)
