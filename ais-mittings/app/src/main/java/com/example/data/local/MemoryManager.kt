package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ChatMessage
import com.example.data.model.MeetingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MemoryManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("boardroom_memory_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_MEMORY_PATH = "custom_memory_path"
        private const val DEFAULT_FOLDER_NAME = "boardroom_memory"
    }

    fun getMemoryRootPath(): String {
        val saved = prefs.getString(KEY_CUSTOM_MEMORY_PATH, null)
        if (!saved.isNullOrBlank()) {
            return saved
        }
        val defaultDir = File(context.filesDir, DEFAULT_FOLDER_NAME)
        if (!defaultDir.exists()) {
            defaultDir.mkdirs()
        }
        return defaultDir.absolutePath
    }

    fun setMemoryRootPath(path: String) {
        val f = File(path)
        if (!f.exists()) {
            f.mkdirs()
        }
        prefs.edit().putString(KEY_CUSTOM_MEMORY_PATH, path).apply()
    }

    fun getMasterDirectory(): File {
        val masterDir = File(getMemoryRootPath(), "master")
        if (!masterDir.exists()) {
            masterDir.mkdirs()
            // Create a default master overview file if not exists
            val defaultMasterDoc = File(masterDir, "general_charter.txt")
            if (!defaultMasterDoc.exists()) {
                defaultMasterDoc.writeText(
                    """
                    # اساسنامه و خط مشی کلان شورای مشاوران هوش مصنوعی
                    ۱. این شورا از کارگروه‌های تخصصی پویا تشکیل شده است؛ هر کارگروه ۵ جایگاه مشاور هوش مصنوعی دارد و تعداد گروه‌ها را رییس جلسه تعیین می‌کند.
                    ۲. کلیه اعضا موظفند با بررسی حافظه مستر و سوابق جلسات گذشته، تحلیل‌های دقیق ارائه دهند.
                    ۳. حفظ یکپارچگی استراتژیک و ارائه راهکارهای عملیاتی اولویت اصلی تصمیم‌گیری‌هاست.
                    """.trimIndent()
                )
            }
        }
        return masterDir
    }

    fun getSessionDirectory(session: MeetingSession): File {
        val sanitizedTitle = session.title.replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_").take(30)
        val dirName = "session_${session.id}_$sanitizedTitle"
        val sessionDir = File(getMemoryRootPath(), "sessions/$dirName")
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
            File(sessionDir, "attachments").mkdirs()
            File(sessionDir, "reports").mkdirs()
        }
        return sessionDir
    }

    suspend fun saveSessionTranscript(session: MeetingSession, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        try {
            val sessionDir = getSessionDirectory(session)
            val transcriptFile = File(sessionDir, "transcript.txt")
            val content = buildString {
                appendLine("=== صورت جلسه شورای مشاوران هوش مصنوعی ===")
                appendLine("موضوع: ${session.title}")
                appendLine("دستور کار: ${session.agenda}")
                appendLine("حالت برگزاری: ${session.dispatchMode}")
                appendLine("تاریخ: ${java.util.Date(session.createdAt)}")
                appendLine("--------------------------------------------------")
                messages.forEach { msg ->
                    appendLine("[${java.util.Date(msg.timestamp)}] ${msg.senderName} (${msg.senderRole}):")
                    appendLine(msg.text)
                    if (!msg.attachmentName.isNullOrBlank()) {
                        appendLine("-> پیوست فایل: ${msg.attachmentName}")
                    }
                    appendLine()
                }
                if (session.finalResolution.isNotBlank()) {
                    appendLine("--------------------------------------------------")
                    appendLine("=== بیانیه و مصوبه نهایی جلسه ===")
                    appendLine(session.finalResolution)
                }
            }
            transcriptFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveCouncilReport(session: MeetingSession, councilId: Int, councilName: String, report: String) = withContext(Dispatchers.IO) {
        try {
            val reportsDir = File(getSessionDirectory(session), "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()
            val reportFile = File(reportsDir, "council_${councilId}_report.txt")
            reportFile.writeText(
                """
                === گزارش کارگروه: $councilName (شناسه: $councilId) ===
                جلسه: ${session.title}
                زمان: ${java.util.Date()}
                --------------------------------------------------
                $report
                """.trimIndent()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadMasterContext(): String = withContext(Dispatchers.IO) {
        val masterDir = getMasterDirectory()
        val files = masterDir.listFiles() ?: return@withContext "هیچ سند مستری یافت نشد."
        buildString {
            appendLine("[حافظه کلان سازمانی - پوشه Master]")
            files.filter { it.isFile }.forEach { file ->
                appendLine("--- سند مستر: ${file.name} ---")
                appendLine(file.readText().take(1500))
                appendLine()
            }
        }
    }

    suspend fun loadHistoricalSessionsSummary(limit: Int = 3): String = withContext(Dispatchers.IO) {
        val sessionsDir = File(getMemoryRootPath(), "sessions")
        if (!sessionsDir.exists()) return@withContext "سابقه جلسه قبلی ثبت نشده است."
        val folders = sessionsDir.listFiles { f -> f.isDirectory }?.sortedByDescending { it.lastModified() }?.take(limit)
            ?: return@withContext "سابقه جلسه قبلی ثبت نشده است."

        buildString {
            appendLine("[سوابق جلسات پیشین در حافظه]")
            folders.forEach { folder ->
                val transcript = File(folder, "transcript.txt")
                if (transcript.exists()) {
                    appendLine("--- ${folder.name} ---")
                    appendLine(transcript.readLines().take(15).joinToString("\n"))
                    appendLine()
                }
            }
        }
    }

    suspend fun createMasterFile(name: String, content: String): File = withContext(Dispatchers.IO) {
        val masterDir = getMasterDirectory()
        val file = File(masterDir, name)
        file.writeText(content)
        file
    }

    suspend fun saveAttachment(session: MeetingSession, fileName: String, bytes: ByteArray): File = withContext(Dispatchers.IO) {
        val attachmentsDir = File(getSessionDirectory(session), "attachments")
        if (!attachmentsDir.exists()) attachmentsDir.mkdirs()
        val file = File(attachmentsDir, fileName)
        file.writeBytes(bytes)
        file
    }
}
