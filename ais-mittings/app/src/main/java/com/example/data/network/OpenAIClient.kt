package com.example.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>
)

data class OpenAIChoice(
    val message: OpenAIMessage?
)

data class OpenAIErrorBody(
    val message: String?
)

data class OpenAIChatResponse(
    val choices: List<OpenAIChoice>?,
    val error: OpenAIErrorBody?
)

interface OpenAIApiService {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse
}

/**
 * Client for the OpenAI Chat Completions API (also compatible with many
 * OpenAI-compatible providers that share the same request/response shape:
 * LM Studio, llama.cpp server, vLLM, LocalAI, LiteLLM proxies, ...).
 * Pass `baseUrlOverride` (e.g. "http://192.168.1.50:1234/v1/") to target a
 * local/CLI-launched OpenAI-compatible server instead of api.openai.com.
 */
object OpenAIClient {
    private const val BASE_URL = "https://api.openai.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val serviceCache = mutableMapOf<String, OpenAIApiService>()

    private fun serviceFor(baseUrlOverride: String?): OpenAIApiService {
        val normalized = baseUrlOverride?.trim()?.takeIf { it.isNotBlank() }
            ?.let { if (it.endsWith("/")) it else "$it/" }
            ?: BASE_URL
        return synchronized(serviceCache) {
            serviceCache.getOrPut(normalized) {
                Retrofit.Builder()
                    .baseUrl(normalized)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(OpenAIApiService::class.java)
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun callModel(
        prompt: String,
        systemPrompt: String?,
        modelName: String,
        apiKey: String,
        baseUrlOverride: String? = null
    ): String = withContext(Dispatchers.IO) {
        val messages = buildList {
            systemPrompt?.let { add(OpenAIMessage(role = "system", content = it)) }
            add(OpenAIMessage(role = "user", content = prompt))
        }

        val response = serviceFor(baseUrlOverride).chatCompletion(
            authorization = "Bearer $apiKey",
            request = OpenAIChatRequest(model = modelName, messages = messages)
        )

        response.error?.let { err ->
            throw IllegalStateException("خطای OpenAI: ${err.message ?: "نامشخص"}")
        }

        response.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("پاسخی از OpenAI دریافت نشد.")
    }
}
