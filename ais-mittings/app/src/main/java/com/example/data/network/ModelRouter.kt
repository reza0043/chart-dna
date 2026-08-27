package com.example.data.network

import com.example.BuildConfig

/**
 * Central dispatcher for the "multi-model combo" feature.
 *
 * Every advisor sub-agent slot stores a free-text `modelType` (e.g.
 * "gemini-3.5-flash", "gpt-4o", "claude-3-5-sonnet-20241022", "ollama-llama3",
 * "mcp-agent") plus an optional per-slot `customApiKey` and `cliOrEndpoint`.
 * Previously ALL of these were sent to the Gemini REST endpoint regardless of
 * what the user typed, so anything non-Gemini silently failed and fell back
 * to a canned Persian response. This router actually looks at modelType and
 * routes the call to the right provider client.
 */
object ModelRouter {

    enum class Provider { GEMINI, OPENAI, ANTHROPIC, OLLAMA, MCP }

    fun detectProvider(modelName: String, endpoint: String?): Provider {
        val m = modelName.trim().lowercase()
        val e = endpoint?.trim()?.lowercase().orEmpty()

        return when {
            e.startsWith("mcp://") || m == "mcp-agent" || m.startsWith("mcp-") -> Provider.MCP
            m.startsWith("ollama") || e.contains("ollama") || e.contains(":11434") ||
                m.startsWith("llama") || m.startsWith("mistral") || m.startsWith("qwen") ||
                m.startsWith("phi") || m.startsWith("gemma") -> Provider.OLLAMA
            m.startsWith("gpt") || m.startsWith("o1") || m.startsWith("o3") || m.startsWith("o4") || m.startsWith("chatgpt") || m.startsWith("openai") -> Provider.OPENAI
            m.startsWith("claude") || m.startsWith("anthropic") -> Provider.ANTHROPIC
            m.startsWith("gemini") -> Provider.GEMINI
            else -> Provider.GEMINI // safe default: previous behavior for unrecognized names
        }
    }

    /**
     * Same call signature the rest of the app already used for GeminiClient,
     * plus an optional `endpoint` for Ollama/MCP slots (SubAgentSlot.cliOrEndpoint).
     *
     * On failure this returns a clear Persian error string identifying the
     * provider and the real error message — it does NOT fall back to a fake
     * "offline AI" persona response the way GeminiClient's internal fallback
     * still does for missing/invalid Gemini keys (that's issue #2, tracked
     * separately).
     */
    suspend fun callModel(
        prompt: String,
        systemPrompt: String? = null,
        modelName: String = "gemini-3.5-flash",
        customApiKey: String? = null,
        endpoint: String? = null
    ): String {
        val provider = detectProvider(modelName, endpoint)

        return try {
            when (provider) {
                Provider.OPENAI -> {
                    // اندپوینت محلی/سازگار با OpenAI (LM Studio، llama.cpp، vLLM، ...):
                    // این سرورها معمولاً کلید واقعی نمی‌خواهند، پس با کلید نمایشی ادامه می‌دهیم.
                    val localEndpoint = endpoint?.takeIf { it.isNotBlank() && !it.startsWith("mcp://") }
                    OpenAIClient.callModel(
                        prompt = prompt,
                        systemPrompt = systemPrompt,
                        modelName = modelName,
                        apiKey = if (localEndpoint != null) {
                            customApiKey?.takeIf { it.isNotBlank() } ?: "local-openai-compatible-server"
                        } else {
                            resolveKey(customApiKey, BuildConfig.OPENAI_API_KEY, "OpenAI")
                        },
                        baseUrlOverride = localEndpoint
                    )
                }

                Provider.ANTHROPIC -> AnthropicClient.callModel(
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    modelName = modelName,
                    apiKey = resolveKey(customApiKey, BuildConfig.ANTHROPIC_API_KEY, "Anthropic")
                )

                Provider.OLLAMA -> OllamaClient.callModel(
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    modelName = modelName,
                    endpoint = endpoint
                )

                Provider.MCP -> throw UnsupportedOperationException(
                    "اتصال به عامل‌های MCP هنوز پیاده‌سازی نشده است (اندپوینت وارد شده: ${endpoint ?: "نامشخص"})."
                )

                Provider.GEMINI -> GeminiClient.callModel(
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    modelName = modelName,
                    customApiKey = customApiKey
                )
            }
        } catch (e: Exception) {
            "⚠️ خطا در فراخوانی مدل «$modelName» (ارائه‌دهنده: ${provider.name}):\n${e.localizedMessage ?: e.toString()}"
        }
    }

    private fun resolveKey(customKey: String?, fallbackKey: String, providerLabel: String): String {
        val key = customKey?.takeIf { it.isNotBlank() } ?: fallbackKey
        if (key.isBlank() || key.startsWith("MY_")) {
            throw IllegalStateException("کلید API برای $providerLabel تنظیم نشده است. آن را در تنظیمات این جایگاه مشاور وارد کنید.")
        }
        return key
    }
}
