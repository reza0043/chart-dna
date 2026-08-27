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

data class AnthropicMessage(
    val role: String,
    val content: String
)

data class AnthropicMessageRequest(
    val model: String,
    val max_tokens: Int = 1024,
    val system: String? = null,
    val messages: List<AnthropicMessage>
)

data class AnthropicContentBlock(
    val type: String?,
    val text: String?
)

data class AnthropicErrorBody(
    val message: String?
)

data class AnthropicMessageResponse(
    val content: List<AnthropicContentBlock>?,
    val error: AnthropicErrorBody?
)

interface AnthropicApiService {
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String,
        @Body request: AnthropicMessageRequest
    ): AnthropicMessageResponse
}

/**
 * Client for the Anthropic Messages API.
 * NOTE: `modelName` must be an exact Anthropic model id
 * (e.g. "claude-3-5-sonnet-20241022"), not a loose alias.
 */
object AnthropicClient {
    private const val BASE_URL = "https://api.anthropic.com/"
    private const val API_VERSION = "2023-06-01"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: AnthropicApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AnthropicApiService::class.java)
    }

    suspend fun callModel(
        prompt: String,
        systemPrompt: String?,
        modelName: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val response = apiService.createMessage(
            apiKey = apiKey,
            version = API_VERSION,
            request = AnthropicMessageRequest(
                model = modelName,
                system = systemPrompt,
                messages = listOf(AnthropicMessage(role = "user", content = prompt))
            )
        )

        response.error?.let { err ->
            throw IllegalStateException("خطای Anthropic: ${err.message ?: "نامشخص"}")
        }

        response.content
            ?.firstOrNull { it.type == "text" }
            ?.text
            ?: throw IllegalStateException("پاسخی از Anthropic دریافت نشد.")
    }
}
