package com.chatnova.ai.data.local.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.chatnova.ai.domain.model.ChatSettings
import com.chatnova.ai.domain.model.CustomInstructions
import com.chatnova.ai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chatnova_settings")

class DataStoreManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEND_ON_ENTER = booleanPreferencesKey("send_on_enter")
        val ENABLE_MARKDOWN = booleanPreferencesKey("enable_markdown")
        val SHOW_TIMESTAMPS = booleanPreferencesKey("show_timestamps")
        val STREAM_RESPONSE = booleanPreferencesKey("stream_response")
        val DEFAULT_MODEL_ID = stringPreferencesKey("default_model_id")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TOP_P = floatPreferencesKey("top_p")
        val MAX_TOKENS = intPreferencesKey("max_tokens")

        val INSTRUCTIONS_ENABLED = booleanPreferencesKey("instructions_enabled")
        val GLOBAL_INSTRUCTIONS = stringPreferencesKey("global_instructions")
        val RESPONSE_STYLE = stringPreferencesKey("response_style")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    val chatSettings: Flow<ChatSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            ChatSettings(
                themeMode = try { ThemeMode.valueOf(themeStr) } catch (e: Exception) { ThemeMode.SYSTEM },
                sendOnEnter = prefs[Keys.SEND_ON_ENTER] ?: false,
                enableMarkdown = prefs[Keys.ENABLE_MARKDOWN] ?: true,
                showTimestamps = prefs[Keys.SHOW_TIMESTAMPS] ?: true,
                streamResponse = prefs[Keys.STREAM_RESPONSE] ?: true,
                defaultModelId = prefs[Keys.DEFAULT_MODEL_ID] ?: "stealth/ox-alpha",
                temperature = prefs[Keys.TEMPERATURE] ?: 0.7f,
                topP = prefs[Keys.TOP_P] ?: 0.9f,
                maxTokens = prefs[Keys.MAX_TOKENS] ?: 4096
            )
        }

    suspend fun updateChatSettings(settings: ChatSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.SEND_ON_ENTER] = settings.sendOnEnter
            prefs[Keys.ENABLE_MARKDOWN] = settings.enableMarkdown
            prefs[Keys.SHOW_TIMESTAMPS] = settings.showTimestamps
            prefs[Keys.STREAM_RESPONSE] = settings.streamResponse
            prefs[Keys.DEFAULT_MODEL_ID] = settings.defaultModelId
            prefs[Keys.TEMPERATURE] = settings.temperature
            prefs[Keys.TOP_P] = settings.topP
            prefs[Keys.MAX_TOKENS] = settings.maxTokens
        }
    }

    val customInstructions: Flow<CustomInstructions> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            CustomInstructions(
                isEnabled = prefs[Keys.INSTRUCTIONS_ENABLED] ?: true,
                globalInstructions = prefs[Keys.GLOBAL_INSTRUCTIONS] ?: "",
                responseStyle = prefs[Keys.RESPONSE_STYLE] ?: "Balanced"
            )
        }

    suspend fun updateCustomInstructions(instructions: CustomInstructions) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INSTRUCTIONS_ENABLED] = instructions.isEnabled
            prefs[Keys.GLOBAL_INSTRUCTIONS] = instructions.globalInstructions
            prefs[Keys.RESPONSE_STYLE] = instructions.responseStyle
        }
    }

    suspend fun isFirstLaunch(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.FIRST_LAUNCH] = false
        }
    }
}
