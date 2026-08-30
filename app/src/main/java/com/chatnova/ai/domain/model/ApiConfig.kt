package com.chatnova.ai.domain.model

enum class ApiStatus {
    UNCONFIGURED,
    TESTING,
    CONNECTED,
    INVALID_KEY,
    NETWORK_ERROR
}

data class ApiConfig(
    val apiKey: String = "",
    val status: ApiStatus = ApiStatus.UNCONFIGURED,
    val errorMessage: String? = null,
    val lastTested: Long = 0L
)
