package com.chatnova.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ModelListResponseDto(
    @SerializedName("data") val data: List<ModelDto>?
)

data class ModelDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("context_length") val contextLength: Int?,
    @SerializedName("pricing") val pricing: ModelPricingDto?,
    @SerializedName("architecture") val architecture: ModelArchitectureDto?,
    @SerializedName("top_provider") val topProvider: ModelTopProviderDto?
)

data class ModelPricingDto(
    @SerializedName("prompt") val prompt: String?,
    @SerializedName("completion") val completion: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("request") val request: String?
)

data class ModelArchitectureDto(
    @SerializedName("modality") val modality: String?,
    @SerializedName("tokenizer") val tokenizer: String?,
    @SerializedName("instruct_type") val instructType: String?
)

data class ModelTopProviderDto(
    @SerializedName("context_length") val contextLength: Int?,
    @SerializedName("max_completion_tokens") val maxCompletionTokens: Int?,
    @SerializedName("is_moderated") val isModerated: Boolean?
)
