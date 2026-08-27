package com.example.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val system: String? = null,
    val stream: Boolean = false
)

data class OllamaGenerateResponse(
    val response: String?,
    val error: String?
)

/**
 * Client for a locally/network-reachable Ollama server.
 * `endpoint` is whatever the user put in SubAgentSlot.cliOrEndpoint, e.g.
 * "http://192.168.1.50:11434" (a phone can't reach "localhost" of its own
 * host machine — on the Android emulator use "http://10.0.2.2:11434").
 */
object OllamaClient {
    private const val DEFAULT_ENDPOINT = "http://10.0.2.2:11434"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(OllamaGenerateRequest::class.java)
    private val responseAdapter = moshi.adapter(OllamaGenerateResponse::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun callModel(
        prompt: String,
        systemPrompt: String?,
        modelName: String,
        endpoint: String?
    ): String = withContext(Dispatchers.IO) {
        val base = endpoint?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_ENDPOINT
        val url = if (base.contains("/api/")) base else base.trimEnd('/') + "/api/generate"

        // "ollama-local" or "ollama-llama3" -> "llama3"; a bare model name passes through.
        val resolvedModel = modelName.removePrefix("ollama-").ifBlank { "llama3" }

        val bodyJson = requestAdapter.toJson(
            OllamaGenerateRequest(
                model = resolvedModel,
                prompt = prompt,
                system = systemPrompt,
                stream = false
            )
        )

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { httpResponse ->
            val rawBody = httpResponse.body?.string().orEmpty()
            if (!httpResponse.isSuccessful) {
                throw IllegalStateException("خطای سرور Ollama (${httpResponse.code}): $rawBody")
            }
            val parsed = try {
                responseAdapter.fromJson(rawBody)
            } catch (e: Exception) {
                throw IllegalStateException("پاسخ نامعتبر از سرور Ollama: ${e.localizedMessage}")
            }
            parsed?.error?.let { throw IllegalStateException("خطای Ollama: $it") }
            parsed?.response ?: throw IllegalStateException("پاسخی از سرور Ollama دریافت نشد.")
        }
    }
}
