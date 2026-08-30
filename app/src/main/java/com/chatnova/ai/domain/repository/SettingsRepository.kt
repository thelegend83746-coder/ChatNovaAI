package com.chatnova.ai.domain.repository

import com.chatnova.ai.domain.model.ApiConfig
import com.chatnova.ai.domain.model.ChatSettings
import com.chatnova.ai.domain.model.CustomInstructions
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val apiConfig: Flow<ApiConfig>
    val chatSettings: Flow<ChatSettings>
    val customInstructions: Flow<CustomInstructions>

    suspend fun saveApiKey(apiKey: String)
    suspend fun testApiKey(apiKey: String): Result<Boolean>
    suspend fun removeApiKey()
    suspend fun updateChatSettings(settings: ChatSettings)
    suspend fun updateCustomInstructions(instructions: CustomInstructions)
    suspend fun isFirstLaunch(): Boolean
    suspend fun setFirstLaunchCompleted()
    suspend fun clearCache(): Long
}
