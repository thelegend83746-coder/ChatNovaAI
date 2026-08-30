package com.chatnova.ai.di

import android.content.Context
import com.chatnova.ai.data.local.ChatDatabase
import com.chatnova.ai.data.local.preference.DataStoreManager
import com.chatnova.ai.data.local.preference.EncryptedPreferenceManager
import com.chatnova.ai.data.remote.OpenRouterApi
import com.chatnova.ai.data.remote.sse.SseStreamParser
import com.chatnova.ai.data.repository.ChatRepositoryImpl
import com.chatnova.ai.data.repository.ModelRepositoryImpl
import com.chatnova.ai.data.repository.SettingsRepositoryImpl
import com.chatnova.ai.domain.repository.ChatRepository
import com.chatnova.ai.domain.repository.ModelRepository
import com.chatnova.ai.domain.repository.SettingsRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    val database: ChatDatabase by lazy {
        ChatDatabase.getInstance(context)
    }

    val dataStoreManager: DataStoreManager by lazy {
        DataStoreManager(context)
    }

    val encryptedPreferenceManager: EncryptedPreferenceManager by lazy {
        EncryptedPreferenceManager(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // Keep logging basic to never log Authorization tokens
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val openRouterApi: OpenRouterApi by lazy {
        retrofit.create(OpenRouterApi::class.java)
    }

    val sseStreamParser: SseStreamParser by lazy {
        SseStreamParser(gson)
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            openRouterApi = openRouterApi,
            encryptedPreferenceManager = encryptedPreferenceManager,
            sseStreamParser = sseStreamParser,
            gson = gson
        )
    }

    val modelRepository: ModelRepository by lazy {
        ModelRepositoryImpl(
            modelCacheDao = database.modelCacheDao(),
            openRouterApi = openRouterApi,
            encryptedPreferenceManager = encryptedPreferenceManager
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(
            context = context,
            dataStoreManager = dataStoreManager,
            encryptedPreferenceManager = encryptedPreferenceManager,
            openRouterApi = openRouterApi
        )
    }
}
