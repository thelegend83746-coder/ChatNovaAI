package com.chatnova.ai.domain.model

data class AiModel(
    val id: String,
    val name: String,
    val description: String = "",
    val contextLength: Int = 128000,
    val promptPrice: Double = 0.0,
    val completionPrice: Double = 0.0,
    val isFree: Boolean = false,
    val hasVision: Boolean = false,
    val provider: String = "OpenRouter"
)
