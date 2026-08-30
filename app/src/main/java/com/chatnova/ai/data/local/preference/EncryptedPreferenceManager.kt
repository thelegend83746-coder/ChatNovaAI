package com.chatnova.ai.data.local.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.chatnova.ai.domain.model.ApiConfig
import com.chatnova.ai.domain.model.ApiStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EncryptedPreferenceManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "chatnova_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for devices where Keystore initialization has issues
            context.getSharedPreferences("chatnova_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _apiConfig = MutableStateFlow(loadConfig())
    val apiConfig: StateFlow<ApiConfig> = _apiConfig.asStateFlow()

    private fun loadConfig(): ApiConfig {
        val key = sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        val statusStr = sharedPreferences.getString(KEY_STATUS, ApiStatus.UNCONFIGURED.name)
        val status = try {
            if (key.isBlank()) ApiStatus.UNCONFIGURED else ApiStatus.valueOf(statusStr ?: ApiStatus.UNCONFIGURED.name)
        } catch (e: Exception) {
            ApiStatus.UNCONFIGURED
        }
        val lastTested = sharedPreferences.getLong(KEY_LAST_TESTED, 0L)
        return ApiConfig(
            apiKey = key,
            status = status,
            lastTested = lastTested
        )
    }

    fun saveApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        sharedPreferences.edit()
            .putString(KEY_API_KEY, trimmed)
            .putString(KEY_STATUS, if (trimmed.isBlank()) ApiStatus.UNCONFIGURED.name else ApiStatus.CONNECTED.name)
            .putLong(KEY_LAST_TESTED, System.currentTimeMillis())
            .apply()
        _apiConfig.value = loadConfig()
    }

    fun updateStatus(status: ApiStatus, errorMessage: String? = null) {
        sharedPreferences.edit()
            .putString(KEY_STATUS, status.name)
            .putLong(KEY_LAST_TESTED, System.currentTimeMillis())
            .apply()
        _apiConfig.value = loadConfig().copy(status = status, errorMessage = errorMessage)
    }

    fun removeApiKey() {
        sharedPreferences.edit()
            .remove(KEY_API_KEY)
            .putString(KEY_STATUS, ApiStatus.UNCONFIGURED.name)
            .remove(KEY_LAST_TESTED)
            .apply()
        _apiConfig.value = ApiConfig(apiKey = "", status = ApiStatus.UNCONFIGURED)
    }

    fun getApiKey(): String {
        return sharedPreferences.getString(KEY_API_KEY, "") ?: ""
    }

    companion object {
        private const val KEY_API_KEY = "openrouter_api_key"
        private const val KEY_STATUS = "api_status"
        private const val KEY_LAST_TESTED = "api_last_tested"
    }
}
