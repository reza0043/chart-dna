package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class DispatchMode(val titleFa: String, val titleEn: String, val description: String) {
    AUTO_TRIAGE(
        "تشخیص خودکار و ارجاع",
        "Auto Triage & Route",
        "مسئله توسط کارگروه ارجاع تحلیل شده و به متناسب‌ترین مشاوران ارسال می‌شود."
    ),
    SELECTIVE(
        "انتخاب دستی مشاوران",
        "Selective Council",
        "رییس جلسه شخصاً تعیین می‌کند کدام مشاوران در بحث شرکت کنند."
    ),
    PUBLIC_ASSEMBLY(
        "جلسه علنی عمومی",
        "Public Assembly",
        "کلیه کارگروه‌های مشاور در جلسه شرکت کرده و نظرات جامع خود را ارائه می‌دهند."
    ),
    SEQUENTIAL_PIPELINE(
        "نظرات پیوسته و زنجیره‌ای",
        "Sequential Cascade",
        "مسئله به مشاور اول ارجاع شده و پاسخ آن به عنوان ورودی به مشاور بعدی ارسال می‌گردد."
    )
}

enum class AgentRole {
    CHAIRMAN,      // رییس جلسه (کاربر)
    COUNCIL_LEAD,  // سرپرست کارگروه مشاور
    SUB_AGENT,     // مدل هوش مصنوعی زیرمجموعه
    SYSTEM,        // سیستم و منشی جلسه
    TRIAGE_AGENT   // کارگروه تشخیص روند
}

data class SubAgentSlot(
    val slotNumber: Int = 1,
    val agentName: String = "مشاور هوش مصنوعی $slotNumber",
    val modelType: String = "gemini-3.5-flash", // e.g. gemini-3.5-flash, gemini-3.1-pro-preview, gpt-4o, claude-3-5-sonnet, ollama-local, mcp-agent
    val customApiKey: String = "",
    val cliOrEndpoint: String = "", // e.g. ollama run llama3 / mcp://localhost:8000
    val systemPersona: String = "شما مشاور متخصص و تحلیل‌گر ارشد در این کارگروه هستید.",
    val isActive: Boolean = true
)

@Entity(tableName = "advisors")
data class AdvisorEntity(
    @PrimaryKey val id: Int, // شناسه یکتا؛ تعداد کارگروه‌ها پویاست (پیش‌فرض ۴ گروه) و کاربر می‌تواند گروه اضافه یا حذف کند
    val name: String,
    val roleTitle: String,
    val accentColorHex: String,
    val iconName: String,
    val isAllowedInMeeting: Boolean = true,
    val isTriageLead: Boolean = false, // کارگروه مسئول ارجاع خودکار؛ از تنظیمات قابل تغییر است
    val subAgentsJson: String = "",
    val latestReport: String = "",
    val status: String = "آماده" // آماده, در حال تحلیل, گزارش آماده, غایب
)

@Entity(tableName = "meetings")
data class MeetingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val agenda: String,
    val createdAt: Long = System.currentTimeMillis(),
    val folderPath: String = "",
    val dispatchMode: String = DispatchMode.AUTO_TRIAGE.name,
    val activeAdvisorIds: String = "", // خالی یعنی همهٔ کارگروه‌های فعال؛ تعداد گروه‌ها پویاست
    val pipelineSequence: String = "1,2",
    val executiveSummary: String = "",
    val finalResolution: String = "",
    val status: String = "فعال" // فعال, پایان یافته, آرشیو
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val senderName: String,
    val senderRole: String, // CHAIRMAN, COUNCIL_LEAD, SUB_AGENT, SYSTEM
    val advisorId: Int? = null,
    val subAgentSlot: Int? = null,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val audioPath: String? = null,
    val attachmentPath: String? = null,
    val attachmentName: String? = null
)

@Entity(tableName = "master_files")
data class MasterFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val fileDescription: String,
    val contentSummary: String,
    val filePath: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Structured contract the Auto-Triage lead model is asked to return, instead of free-form
 * prose that used to be parsed with a fragile "find any 1-2 digit number" regex. Every field
 * has a safe default so a partially-malformed JSON response still deserializes.
 */
data class TriageDecision(
    val selectedIds: List<Int> = emptyList(),
    val reasoning: String = ""
)

object CouncilDataConverters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, SubAgentSlot::class.java)
    private val adapter = moshi.adapter<List<SubAgentSlot>>(listType)
    private val triageAdapter = moshi.adapter(TriageDecision::class.java)

    /**
     * Extracts and parses the JSON object the triage prompt asked for, tolerating models that
     * wrap it in markdown fences or add stray text before/after. Returns null (never throws)
     * if no valid `{...}` with a `selectedIds` array can be found, so the caller can fall back
     * to the older heuristic parser.
     */
    fun parseTriageDecision(rawModelOutput: String): TriageDecision? {
        val start = rawModelOutput.indexOf('{')
        val end = rawModelOutput.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) return null
        val jsonCandidate = rawModelOutput.substring(start, end + 1)
        return try {
            triageAdapter.fromJson(jsonCandidate)?.takeIf { it.selectedIds.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    // customApiKey is NEVER persisted to Room / disk here — it is deliberately stripped before
    // serialization. The real key lives only in SecureKeyStore (Android Keystore-backed
    // EncryptedSharedPreferences), keyed by (advisorId, slotNumber). See BoardroomRepository /
    // BoardroomViewModel for how keys are saved and resolved at call time.
    fun subAgentsToJson(slots: List<SubAgentSlot>): String {
        val sanitized = slots.map { it.copy(customApiKey = "") }
        return adapter.toJson(sanitized)
    }

    fun jsonToSubAgents(json: String): List<SubAgentSlot> {
        if (json.isBlank()) return defaultSlots(1)
        return try {
            adapter.fromJson(json) ?: defaultSlots(1)
        } catch (e: Exception) {
            defaultSlots(1)
        }
    }

    fun defaultSlots(councilId: Int): List<SubAgentSlot> {
        val specialties = listOf(
            "تحلیل‌گر استراتژیک (Strategic Analyst)",
            "ارزیاب ریسک و انطباق (Risk & Compliance)",
            "متخصص داده و پیاده‌سازی (Implementation Spec)",
            "منتقد و تفکر انتقادی (Devil's Advocate)",
            "تلفیق‌کننده و تدوینگر بیانیه (Synthesis Lead)"
        )
        return (1..5).map { slot ->
            SubAgentSlot(
                slotNumber = slot,
                agentName = "مشاور ${councilId}.${slot} - ${specialties[slot - 1].split('(')[0].trim()}",
                modelType = if (slot == 1) "gemini-3.1-pro-preview" else "gemini-3.5-flash",
                systemPersona = "شما به عنوان ${specialties[slot - 1]} در کارگروه تخصصی فعالیت می‌کنید. با دقت بالا و تکیه بر اطلاعات و حافظه سازمانی پاسخ دهید."
            )
        }
    }
}
