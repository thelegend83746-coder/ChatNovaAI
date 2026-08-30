package com.chatnova.ai.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String = "stealth/ox-alpha",
    val systemPrompt: String? = null
)
