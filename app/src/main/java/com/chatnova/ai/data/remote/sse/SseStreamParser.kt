package com.chatnova.ai.data.remote.sse

import com.chatnova.ai.data.remote.dto.ChatCompletionChunkDto
import com.chatnova.ai.data.remote.error.ApiException
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.InputStreamReader

class SseStreamParser(private val gson: Gson) {

    suspend fun parseStream(
        responseBody: ResponseBody,
        onChunk: suspend (String) -> Unit,
        onComplete: suspend (String) -> Unit
    ) {
        val reader = BufferedReader(InputStreamReader(responseBody.byteStream(), Charsets.UTF_8))
        val fullContent = StringBuilder()

        try {
            var line: String? = reader.readLine()
            while (line != null && currentCoroutineContext().isActive) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        break
                    }
                    if (data.isNotEmpty()) {
                        try {
                            val chunk = gson.fromJson(data, ChatCompletionChunkDto::class.java)
                            if (chunk.error != null) {
                                throw ApiException.UnknownApiException(
                                    chunk.error.message ?: "Stream API Error"
                                )
                            }
                            val textDelta = chunk.choices?.firstOrNull()?.delta?.content
                            if (!textDelta.isNullOrEmpty()) {
                                fullContent.append(textDelta)
                                onChunk(textDelta)
                            }
                        } catch (e: Exception) {
                            if (e is ApiException) throw e
                            // Skip JSON parse noise on keep-alive/heartbeat comments
                        }
                    }
                }
                line = reader.readLine()
            }
            onComplete(fullContent.toString())
        } catch (e: CancellationException) {
            // User cancelled generation via Stop button
            onComplete(fullContent.toString())
            throw e
        } finally {
            try {
                reader.close()
                responseBody.close()
            } catch (ignored: Exception) {}
        }
    }
}
