package com.chatnova.ai.domain.repository

import com.chatnova.ai.domain.model.AiModel
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    fun getCachedModels(): Flow<List<AiModel>>
    suspend fun fetchModels(): Result<List<AiModel>>
    suspend fun getModelById(modelId: String): AiModel?
}
