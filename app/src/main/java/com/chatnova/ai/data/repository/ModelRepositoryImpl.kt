package com.chatnova.ai.data.repository

import com.chatnova.ai.data.local.dao.ModelCacheDao
import com.chatnova.ai.data.local.entity.ModelCacheEntity
import com.chatnova.ai.data.local.preference.EncryptedPreferenceManager
import com.chatnova.ai.data.remote.OpenRouterApi
import com.chatnova.ai.domain.model.AiModel
import com.chatnova.ai.domain.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ModelRepositoryImpl(
    private val modelCacheDao: ModelCacheDao,
    private val openRouterApi: OpenRouterApi,
    private val encryptedPreferenceManager: EncryptedPreferenceManager
) : ModelRepository {

    override fun getCachedModels(): Flow<List<AiModel>> {
        return modelCacheDao.getAllModels().map { list ->
            if (list.isEmpty()) {
                getDefaultFallbackModels()
            } else {
                list.map { it.toDomain() }
            }
        }
    }

    override suspend fun fetchModels(): Result<List<AiModel>> = withContext(Dispatchers.IO) {
        val apiKey = encryptedPreferenceManager.getApiKey()
        val authHeader = if (apiKey.isNotBlank()) "Bearer $apiKey" else ""

        try {
            val response = openRouterApi.getModels(authHeader)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    val models = data.map { dto ->
                        val promptPrice = dto.pricing?.prompt?.toDoubleOrNull() ?: 0.0
                        val completionPrice = dto.pricing?.completion?.toDoubleOrNull() ?: 0.0
                        val isFree = (promptPrice == 0.0 && completionPrice == 0.0) || dto.id.endsWith(":free")
                        val hasVision = dto.architecture?.modality?.contains("image") == true ||
                                dto.id.contains("vision") ||
                                dto.id.contains("vl") ||
                                dto.id.contains("flash")

                        AiModel(
                            id = dto.id,
                            name = dto.name ?: dto.id.substringAfterLast('/'),
                            description = dto.description ?: "",
                            contextLength = dto.contextLength ?: dto.topProvider?.contextLength ?: 128000,
                            promptPrice = promptPrice * 1_000_000,
                            completionPrice = completionPrice * 1_000_000,
                            isFree = isFree,
                            hasVision = hasVision,
                            provider = dto.id.substringBefore('/', "OpenRouter")
                        )
                    }

                    // Save to Room Cache
                    modelCacheDao.clearModels()
                    modelCacheDao.insertModels(models.map { ModelCacheEntity.fromDomain(it) })
                    return@withContext Result.success(models)
                }
            }
            Result.failure(Exception("Failed to fetch models: HTTP ${response.code()} ${response.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModelById(modelId: String): AiModel? {
        val cached = modelCacheDao.getModelById(modelId)
        if (cached != null) return cached.toDomain()
        return getDefaultFallbackModels().find { it.id == modelId }
    }

    private fun getDefaultFallbackModels(): List<AiModel> {
        return listOf(
            AiModel(
                id = "stealth/ox-alpha",
                name = "Ox Alpha (GLM-5.3-Flash)",
                description = "Frontier 1M context reasoning & coding model by Zhipu AI",
                contextLength = 1048576,
                promptPrice = 0.0,
                completionPrice = 0.0,
                isFree = true,
                hasVision = true,
                provider = "Zhipu AI"
            ),
            AiModel(
                id = "z-ai/glm-5.2:free",
                name = "GLM 5.2 (Free)",
                description = "High efficiency reasoning model by Z-AI with 128k context",
                contextLength = 131072,
                promptPrice = 0.0,
                completionPrice = 0.0,
                isFree = true,
                hasVision = false,
                provider = "Z-AI"
            ),
            AiModel(
                id = "google/gemma-4-31b-it:free",
                name = "Google Gemma 4 31B (Free)",
                description = "Google's latest open instruction-tuned language model",
                contextLength = 131072,
                promptPrice = 0.0,
                completionPrice = 0.0,
                isFree = true,
                hasVision = false,
                provider = "Google"
            ),
            AiModel(
                id = "google/gemini-2.5-flash",
                name = "Google Gemini 2.5 Flash",
                description = "Ultra-fast multimodal model with 1M context",
                contextLength = 1048576,
                promptPrice = 0.075,
                completionPrice = 0.30,
                isFree = false,
                hasVision = true,
                provider = "Google"
            ),
            AiModel(
                id = "google/gemini-2.5-pro",
                name = "Google Gemini 2.5 Pro",
                description = "Google's most capable reasoning and architecture model",
                contextLength = 1048576,
                promptPrice = 1.25,
                completionPrice = 5.0,
                isFree = false,
                hasVision = true,
                provider = "Google"
            ),
            AiModel(
                id = "deepseek/deepseek-v4-flash-vision-exp",
                name = "DeepSeek V4 Flash Vision",
                description = "Fast multimodal reasoning model with visual analysis",
                contextLength = 131072,
                promptPrice = 0.14,
                completionPrice = 0.28,
                isFree = false,
                hasVision = true,
                provider = "DeepSeek"
            ),
            AiModel(
                id = "minimax/minimax-m3:free",
                name = "MiniMax M3 (Free)",
                description = "Free conversational & instruction-following AI model",
                contextLength = 1000000,
                promptPrice = 0.0,
                completionPrice = 0.0,
                isFree = true,
                hasVision = false,
                provider = "MiniMax"
            ),
            AiModel(
                id = "nvidia/nemotron-3.5-lightning:free",
                name = "Nvidia Nemotron 3.5 (Free)",
                description = "High throughput accelerated model by NVIDIA",
                contextLength = 131072,
                promptPrice = 0.0,
                completionPrice = 0.0,
                isFree = true,
                hasVision = false,
                provider = "NVIDIA"
            ),
            AiModel(
                id = "anthropic/claude-3.5-sonnet",
                name = "Claude 3.5 Sonnet",
                description = "Anthropic's flagship coding & reasoning model",
                contextLength = 200000,
                promptPrice = 3.0,
                completionPrice = 15.0,
                isFree = false,
                hasVision = true,
                provider = "Anthropic"
            ),
            AiModel(
                id = "openai/gpt-4o",
                name = "GPT-4o",
                description = "OpenAI's flagship multimodal intelligence model",
                contextLength = 128000,
                promptPrice = 2.50,
                completionPrice = 10.0,
                isFree = false,
                hasVision = true,
                provider = "OpenAI"
            )
        )
    }
}
