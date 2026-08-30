package com.chatnova.ai.data.repository

import android.content.Context
import com.chatnova.ai.data.local.preference.DataStoreManager
import com.chatnova.ai.data.local.preference.EncryptedPreferenceManager
import com.chatnova.ai.data.remote.OpenRouterApi
import com.chatnova.ai.domain.model.ApiConfig
import com.chatnova.ai.domain.model.ApiStatus
import com.chatnova.ai.domain.model.ChatSettings
import com.chatnova.ai.domain.model.CustomInstructions
import com.chatnova.ai.domain.repository.SettingsRepository
import com.chatnova.ai.util.FileAttachmentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val encryptedPreferenceManager: EncryptedPreferenceManager,
    private val openRouterApi: OpenRouterApi
) : SettingsRepository {

    override val apiConfig: Flow<ApiConfig> = encryptedPreferenceManager.apiConfig
    override val chatSettings: Flow<ChatSettings> = dataStoreManager.chatSettings
    override val customInstructions: Flow<CustomInstructions> = dataStoreManager.customInstructions

    override suspend fun saveApiKey(apiKey: String) {
        encryptedPreferenceManager.saveApiKey(apiKey)
    }

    override suspend fun testApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            encryptedPreferenceManager.updateStatus(ApiStatus.UNCONFIGURED, "API Key cannot be empty")
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty"))
        }

        encryptedPreferenceManager.updateStatus(ApiStatus.TESTING)

        try {
            val response = openRouterApi.getModels("Bearer $trimmed")
            if (response.isSuccessful) {
                encryptedPreferenceManager.updateStatus(ApiStatus.CONNECTED)
                Result.success(true)
            } else {
                val status = when (response.code()) {
                    401 -> ApiStatus.INVALID_KEY
                    else -> ApiStatus.NETWORK_ERROR
                }
                val msg = "Connection failed (HTTP ${response.code()}): ${response.message()}"
                encryptedPreferenceManager.updateStatus(status, msg)
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            encryptedPreferenceManager.updateStatus(ApiStatus.NETWORK_ERROR, e.localizedMessage)
            Result.failure(e)
        }
    }

    override suspend fun removeApiKey() {
        encryptedPreferenceManager.removeApiKey()
    }

    override suspend fun updateChatSettings(settings: ChatSettings) {
        dataStoreManager.updateChatSettings(settings)
    }

    override suspend fun updateCustomInstructions(instructions: CustomInstructions) {
        dataStoreManager.updateCustomInstructions(instructions)
    }

    override suspend fun isFirstLaunch(): Boolean {
        return dataStoreManager.isFirstLaunch()
    }

    override suspend fun setFirstLaunchCompleted() {
        dataStoreManager.setFirstLaunchCompleted()
    }

    override suspend fun clearCache(): Long {
        return FileAttachmentHelper.cleanCache(context)
    }
}
