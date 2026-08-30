package com.chatnova.ai.domain.model

data class CustomInstructions(
    val isEnabled: Boolean = true,
    val globalInstructions: String = "",
    val responseStyle: String = "Balanced",
    val chatSpecificInstructions: String = ""
)
