package com.example.data.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeminiPart(
    val text: String? = null
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

data class GeminiCandidate(
    val content: GeminiContent?
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun callModel(
        prompt: String,
        systemPrompt: String? = null,
        modelName: String = "gemini-3.5-flash",
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val key = customApiKey?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY

        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            // Simulated intelligent response for offline/preview environments
            return@withContext simulateAdvisorResponse(prompt, systemPrompt, modelName)
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            systemInstruction = systemPrompt?.let {
                GeminiContent(
                    parts = listOf(GeminiPart(text = it))
                )
            }
        )

        try {
            val response = apiService.generateContent(
                model = modelName.ifBlank { "gemini-3.5-flash" },
                apiKey = key,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "پاسخی از مدل دریافت نشد."
        } catch (e: Exception) {
            // If network fails or invalid key, return structured fallback with note
            simulateAdvisorResponse(prompt, systemPrompt, modelName, errorMessage = e.localizedMessage)
        }
    }

    /**
     * Builds a clearly-labeled placeholder response. This is template text matched on a few
     * keywords — never a real model call — so every branch is prefixed with an explicit warning
     * banner (phase 6 hardening) to avoid it being mistaken for genuine AI advisor output.
     */
    private fun simulateAdvisorResponse(
        prompt: String,
        systemPrompt: String?,
        modelName: String,
        errorMessage: String? = null
    ): String {
        val warningBanner = if (errorMessage != null) {
            "⚠️ خطا در اتصال به مدل «$modelName» — پاسخ زیر یک متن نمایشی جایگزین است، نه پاسخ واقعی مدل.\n" +
                "جزئیات خطا: $errorMessage\n" +
                "──────────────────────────────\n"
        } else {
            "⚠️ کلید API برای «$modelName» تنظیم نشده — پاسخ زیر صرفاً یک متن نمونه‌ی از پیش نوشته‌شده است، نه پاسخ واقعی هوش مصنوعی. برای دریافت تحلیل واقعی، کلید API را در تنظیمات این جایگاه وارد کنید.\n" +
                "──────────────────────────────\n"
        }
        return warningBanner + buildSimulatedBody(prompt, systemPrompt, modelName)
    }

    private fun buildSimulatedBody(
        prompt: String,
        systemPrompt: String?,
        modelName: String
    ): String {
        val persona = systemPrompt ?: "دستیار هوشمند مستقر نرم‌افزار"
        val lowerPrompt = prompt.trim().lowercase()

        // 1. Simple Persian conversational greetings & chit-chat
        val isGreeting = lowerPrompt.contains("سلام") || lowerPrompt.contains("درود") || lowerPrompt.contains("صبح بخیر") || lowerPrompt.contains("عصر بخیر")
        val isIdentityQuestion = lowerPrompt.contains("کیستی") || lowerPrompt.contains("شما کی هستید") || lowerPrompt.contains("معرفی") || lowerPrompt.contains("چیکار میکنی")
        val isThanks = lowerPrompt.contains("ممنون") || lowerPrompt.contains("تشکر") || lowerPrompt.contains("مرسی") || lowerPrompt.contains("سپاس")
        val isHelp = lowerPrompt.contains("راهنما") || lowerPrompt.contains("کمک") || lowerPrompt.contains("چطور کار")

        return when {
            isGreeting -> {
                """
                سلام! این یک پاسخ نمونه و از پیش نوشته‌شده است (متن ثابت، نه پاسخ زنده‌ی یک مدل هوش مصنوعی)، چون کلید API این جایگاه هنوز متصل نیست.
                
                برای دریافت پاسخ واقعی، لطفاً کلید API معتبر را در تنظیمات این جایگاه مشاور وارد کنید.
                """.trimIndent()
            }
            isIdentityQuestion -> {
                """
                من یک مدل هوش مصنوعی واقعی نیستم؛ چون کلید API این جایگاه متصل نیست، این متن یک پاسخ نمونه‌ی ثابت (template) است که صرفاً بر اساس چند کلیدواژه‌ی ساده انتخاب می‌شود.
                برای فعال‌سازی پاسخ واقعی از یک مدل هوش مصنوعی واقعی (Gemini/OpenAI/Claude/Ollama/...)، کلید API یا آدرس CLI/سرور محلی را در تنظیمات این جایگاه ثبت کنید.
                """.trimIndent()
            }
            isThanks -> {
                """
                خواهش می‌کنم! انجام وظیفه است. در صورتی که نکته یا دستور کار دیگری برای بررسی دارید، در خدمت شما هستم.
                """.trimIndent()
            }
            isHelp -> {
                """
                راهنمای سریع استفاده:
                ۱. متن پرسش یا موضوع جلسه را در پنجره پایین تایپ کرده یا از دکمه ضبط صدا استفاده فرمایید.
                ۲. در صورت تمایل فایل یا سند مورد نظر را پیوست کنید.
                ۳. می‌توانید نحوه ارجاع (هوشمند، انتخابی، عمومی، زنجیره‌ای) را با کلیدهای بالای پنجره تغییر دهید.
                ۴. دکمه «ارسال پیام» را بزنید تا پاسخ تحلیلی را در همین پنجره بالا دریافت کنید.
                """.trimIndent()
            }
            else -> {
                // Intelligent thematic domain response based on keywords
                val isFinancial = lowerPrompt.contains("مالی") || lowerPrompt.contains("بودجه") || lowerPrompt.contains("سرمایه") || lowerPrompt.contains("هزینه")
                val isTech = lowerPrompt.contains("نرم‌افزار") || lowerPrompt.contains("هوش مصنوعی") || lowerPrompt.contains("فناوری") || lowerPrompt.contains("سیستم") || lowerPrompt.contains("امنیت")
                val isMarketing = lowerPrompt.contains("بازاریابی") || lowerPrompt.contains("فروش") || lowerPrompt.contains("مشتری") || lowerPrompt.contains("تبلیغات")

                val specificDomainAnalysis = when {
                    isFinancial -> "در حوزه مدیریت مالی و تخصیص منابع، اولویت با بهینه‌سازی جریان نقدینگی، ارزیابی بازگشت سرمایه (ROI) و پایش هزینه‌های عملیاتی به صورت مرحله‌ای است."
                    isTech -> "در بعد فنی و زیرساختی، توصیه بر رعایت استانداردهای مقیاس‌پذیری، امنیت چندلایه داده‌ها و استفاده از معماری ماژولار و تاب‌آور می‌باشد."
                    isMarketing -> "از دیدگاه راهبرد بازار و مخاطبان، تمرکز بر تحلیل رفتار کاربران، شخصی‌سازی تجربه کاربری و اجرای کمپین‌های چابک و مبتنی بر بازخورد پیشنهاد می‌شود."
                    else -> "موضوع مطرح شده به دقت توسط هوش مصنوعی بررسی گردید. با در نظر گرفتن ابعاد کلیدی و اهداف کاربردی، اجرای گام‌به‌گام به همراه ثبت بازخورد موثرترین رویکرد خواهد بود."
                }

                """
                تحلیل هوش مصنوعی مستقر بر اساس موضوع: «${prompt.take(60)}»
                
                ۱. بررسی اولیه: $specificDomainAnalysis
                ۲. نکات کلیدی و ریسک‌ها: هماهنگی میان اجزای عملیاتی، تعیین شاخص‌های شفاف عملکرد (KPI) و پایش مستمر از ارکان کلیدی موفقیت است.
                ۳. راهکار پیشنهادی: پیشنهاد می‌شود فاز نخست به صورت پایلوت اجرا شده و نتایج جهت تصمیم‌گیری نهایی مورد ارزیابی قرار گیرد.
                
                (این یک متن نمونه‌ی ثابت است، تولیدشده بر اساس الگوی «$persona» — نه پاسخ واقعی مدل «$modelName»)
                """.trimIndent()
            }
        }
    }
}
