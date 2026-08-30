package com.chatnova.ai.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class ChatSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sendOnEnter: Boolean = false,
    val enableMarkdown: Boolean = true,
    val showTimestamps: Boolean = true,
    val streamResponse: Boolean = true,
    val defaultModelId: String = "stealth/ox-alpha",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 4096
)
