package com.chatnova.ai.data.remote.error

sealed class ApiException(message: String, val code: Int? = null) : Exception(message) {
    class InvalidApiKeyException(message: String = "Authentication failed. Please check your OpenRouter API Key in Settings.") : ApiException(message, 401)
    class InsufficientCreditsException(message: String = "Insufficient OpenRouter credits or quota limit reached.") : ApiException(message, 402)
    class ForbiddenException(message: String = "Access forbidden. Verify your OpenRouter account permissions.") : ApiException(message, 403)
    class NotFoundException(message: String = "Requested AI model not found on OpenRouter.") : ApiException(message, 404)
    class RateLimitException(message: String = "Rate limit reached. Please wait a moment before sending another message.") : ApiException(message, 429)
    class ServerException(message: String = "OpenRouter server error. Please try again in a few moments.") : ApiException(message, 500)
    class NetworkException(message: String = "Network connection unavailable. Please check your internet connection.") : ApiException(message)
    class UnknownApiException(message: String, code: Int? = null) : ApiException(message, code)
}
