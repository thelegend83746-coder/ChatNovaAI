package com.chatnova.ai.data.remote

import com.chatnova.ai.data.remote.dto.ChatCompletionRequestDto
import com.chatnova.ai.data.remote.dto.ChatCompletionResponseDto
import com.chatnova.ai.data.remote.dto.ModelListResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface OpenRouterApi {
    @GET("api/v1/models")
    suspend fun getModels(
        @Header("Authorization") authHeader: String
    ): Response<ModelListResponseDto>

    @POST("api/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/chatnova/ai",
        @Header("X-Title") title: String = "ChatNova AI",
        @Body request: ChatCompletionRequestDto
    ): Response<ChatCompletionResponseDto>

    @Streaming
    @POST("api/v1/chat/completions")
    suspend fun createStreamingChatCompletion(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/chatnova/ai",
        @Header("X-Title") title: String = "ChatNova AI",
        @Body request: ChatCompletionRequestDto
    ): Response<ResponseBody>
}
