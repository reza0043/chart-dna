package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryManager
import com.example.data.local.SecureKeyStore
import com.example.data.model.AdvisorEntity
import com.example.data.model.ChatMessage
import com.example.data.model.CouncilDataConverters
import com.example.data.model.DispatchMode
import com.example.data.model.MasterFile
import com.example.data.model.MeetingSession
import com.example.data.model.SubAgentSlot
import com.example.data.network.ModelRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class BoardroomRepository(
    private val database: AppDatabase,
    private val memoryManager: MemoryManager,
    private val context: Context,
    private val secureKeyStore: SecureKeyStore
) {

    val allAdvisors: Flow<List<AdvisorEntity>> = database.councilDao().getAllAdvisors()
    val allSessions: Flow<List<MeetingSession>> = database.sessionDao().getAllSessions()
    val latestSession: Flow<MeetingSession?> = database.sessionDao().getLatestSession()
    val masterFiles: Flow<List<MasterFile>> = database.masterFileDao().getAllMasterFiles()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return database.messageDao().getMessagesForSession(sessionId)
    }

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val currentAdvisors = database.councilDao().getAllAdvisors().firstOrNull() ?: emptyList()
        if (currentAdvisors.isEmpty()) {
            val defaults = generateDefaultAdvisors()
            database.councilDao().insertAll(defaults)
        }

        val currentSessions = database.sessionDao().getAllSessions().firstOrNull() ?: emptyList()
        if (currentSessions.isEmpty()) {
            val defaultSession = MeetingSession(
                title = "جلسه افتتاحیه راهبردی شورا",
                agenda = "تعیین اهداف کلان سالانه، بررسی فرصت‌های هوش مصنوعی و هماهنگی کارگروه‌های تخصصی",
                dispatchMode = DispatchMode.AUTO_TRIAGE.name,
                executiveSummary = "جلسه با حضور اعضای شورا رسمیت یافت. کارگروه‌ها آماده دریافت موضوعات و تحلیل‌های کارشناسی هستند.",
                finalResolution = "مقرر شد کلیه ارجاعات با توجه به اسناد مستر و سوابق سازمانی، از طریق ۴ حالت تصمیم‌گیری شورا پیگیری شوند."
            )
            val sessionId = database.sessionDao().insertSession(defaultSession)
            val createdSession = defaultSession.copy(id = sessionId)

            // Initial welcome message from System
            database.messageDao().insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    senderName = "دبیرخانه شورای هوش مصنوعی",
                    senderRole = "SYSTEM",
                    text = "جلسه افتتاحیه شورا با موفقیت تشکیل شد. رییس محترم جلسه، لطفاً مسئله یا دستور کار مورد نظر خود را در صفحه چت مطرح فرمایید."
                )
            )

            memoryManager.saveSessionTranscript(createdSession, emptyList())
        }

        // Initialize Master Directory
        memoryManager.getMasterDirectory()
    }

    // ساختار تعریف کارگروه پیش‌فرض (شناسه، نام، رنگ، آیکون)
    private data class AdvisorDef(val id: Int, val name: String, val color: String, val icon: String)

    private fun generateDefaultAdvisors(): List<AdvisorEntity> {
        // پیش‌فرض: ۴ کارگروه (شورای دانش‌آموزی). تعداد کارگروه‌ها پویاست — کاربر می‌تواند از
        // داخل اپ گروه اضافه یا حذف کند؛ هر گروه جدید کپی یکسان همان ساختار ۵ جایگاه را می‌گیرد.
        val advisorDefinitions = listOf(
            AdvisorDef(1, "گروه ۱: رویدادها 🎪", "#A855F7", "people"),
            AdvisorDef(2, "گروه ۲: علم و فن 🔬", "#38BDF8", "science"),
            AdvisorDef(3, "گروه ۳: فرهنگ و هنر 🎨", "#F472B6", "brush"),
            AdvisorDef(4, "گروه ۴: ورزش و نشاط ⚽", "#34D399", "rocket")
        )

        // نقش‌های ۵ جایگاه هوش مصنوعی هر گروه (ported from ai-meeting student council)
        val slotRoles = mapOf(
            1 to listOf(
                "نماینده پایه دهم و یازدهم (صدای دانش‌آموزان)" to "تمرکز بر نیازها و علایق روزمره دانش‌آموزان در محیط مدرسه",
                "کارشناس برنامه‌ریزی و تقویم آموزشی" to "تنظیم زمان‌بندی دقیق رویدادها بدون تداخل با امتحانات و کلاس‌ها",
                "مشاور امور اجرایی و تدارکات مراسم" to "پیش‌بینی هزینه‌ها، تجهیزات و نیازهای پشتیبانی مدرسه",
                "نماینده تعامل با اولیا و مدیریت مدرسه" to "ایجاد هماهنگی و جلب رضایت کادر مدرسه و والدین",
                "ناظر کیفیت و بازخوردسنجی رویدادها" to "ارزیابی میزان رضایت و اثرگذاری برنامه‌ها میان بچه‌ها"
            ),
            2 to listOf(
                "دبیر انجمن نخبگان و المپیاد علمی" to "طراحی کارگاه‌های آمادگی آزمون‌ها و المپیادهای کشوری",
                "مسئول مسابقات هوش مصنوعی و برنامه‌نویسی" to "برگزاری چالش‌های فناورانه و کارگاه‌های هوش مصنوعی دانش‌آموزی",
                "مدیر غرفه‌های آزمایشگاهی و دست‌سازه‌ها" to "برنامه‌ریزی برای نمایشگاه دستاوردهای علمی دانش‌آموزان",
                "مشاور پروژه‌های پژوهشی و خوارزمی" to "هدایت ایده‌های دانش‌آموزی به سمت ثبت اختراع و جشنواره‌ها",
                "مسئول ارتباط با پژوهش‌سراهای دانش‌آموزی" to "تأمین تجهیزات آزمایشگاهی پیشرفته و کارگاه‌های عملی"
            ),
            3 to listOf(
                "سردبیر نشریه و پادکست دانش‌آموزی" to "تولید محتوای جذاب، مصاحبه با معلمان و انعکاس صدای بچه‌ها",
                "مسئول گروه سرود، تئاتر و هنرهای نمایشی" to "آماده‌سازی اجراهای خلاقانه برای جشن‌ها و مناسبت‌های مدرسه",
                "مدیر مسابقات عکاسی، طراحی و گرافیک" to "برگزاری مسابقات هنری و زیباسازی تابلوی اعلانات مدرسه",
                "مشاور کتاب‌خوانی و نقد کتاب" to "توسعه کتابخانه مدرسه و برگزاری دورهمی‌های معرفی کتاب جذاب",
                "روابط عمومی و فضاسازی محیط مدرسه" to "ایجاد فضایی پرانرژی، رنگارنگ و شاداب در راهروها و حیاط"
            ),
            4 to listOf(
                "مسئول برگزاری المپیاد ورزشی درون‌مدرسه‌ای" to "برنامه‌ریزی لیگ فوتبال، والیبال، پینگ‌پنگ و شطرنج",
                "مشاور سلامت و تغذیه سالم بوفه مدرسه" to "پیشنهاد خوراکی‌های سالم، بهداشتی و مورد علاقه نوجوانان",
                "مسئول برنامه‌ریزی اردوهای هیجان‌انگیز" to "شناسایی مقاصد جذاب تفریحی، بوم‌گردی، کمپینگ و کوهپیمایی",
                "سرگروه بازی‌های رومیزی و سرگرمی‌های فکری" to "تجهیز اتاق بازی و اوقات فراغت زنگ تفریح دانش‌آموزان",
                "مشاور روان‌شناختی نشاط و کاهش استرس" to "ارائه راهکارهای کاهش استرس در دوران امتحانات و افزایش انگیزه"
            )
        )

        return advisorDefinitions.map { (id, name, color, icon) ->
            val roles = slotRoles[id] ?: emptyList()
            val slots = (1..5).map { slot ->
                val (roleName, roleDesc) = roles.getOrElse(slot - 1) {
                    "مشاور هوش مصنوعی $slot" to "تحلیل مسائل مرتبط با $name"
                }
                SubAgentSlot(
                    slotNumber = slot,
                    agentName = roleName,
                    modelType = "gemini-3.5-flash",
                    systemPersona = "شما $roleName در «$name» هستید. مأموریت: $roleDesc. پاسخ‌های شما باید صمیمی، سازنده، پرانرژی و متناسب با فضای دانش‌آموزی باشد."
                )
            }
            AdvisorEntity(
                id = id,
                name = name,
                roleTitle = "کارگروه تخصصی شماره $id",
                accentColorHex = color,
                iconName = icon,
                isAllowedInMeeting = true,
                isTriageLead = (id == 4), // پیش‌فرض: آخرین گروه؛ از تنظیمات قابل تغییر است
                subAgentsJson = CouncilDataConverters.subAgentsToJson(slots),
                latestReport = "",
                status = "آماده"
            )
        }
    }

    suspend fun createNewSession(title: String, agenda: String, mode: DispatchMode): MeetingSession = withContext(Dispatchers.IO) {
        val session = MeetingSession(
            title = title.ifBlank { "جلسه جدید شورا - ${java.util.Date()}" },
            agenda = agenda.ifBlank { "بررسی موضوع مطرح شده توسط رییس جلسه" },
            dispatchMode = mode.name
        )
        val id = database.sessionDao().insertSession(session)
        val fullSession = session.copy(id = id)
        memoryManager.getSessionDirectory(fullSession)

        database.messageDao().insertMessage(
            ChatMessage(
                sessionId = id,
                senderName = "منشی جلسه",
                senderRole = "SYSTEM",
                text = "دستور کار جدید اعلام شد: «${fullSession.agenda}». جلسه در حالت [${mode.titleFa}] آماده آغاز است."
            )
        )
        fullSession
    }

    suspend fun updateAdvisor(advisor: AdvisorEntity) = withContext(Dispatchers.IO) {
        database.councilDao().insertOrUpdate(advisor)
    }

    /**
     * افزودن کارگروه جدید: ساختار هر گروه یکسان است (۵ جایگاه مشاور هوش مصنوعی)؛
     * در واقع یک «کپی» از الگوی استاندارد گروه با نام/رنگ/آیکون و عناوین جایگاه‌های دلخواه ساخته می‌شود.
     * شناسهٔ گروه جدید = بزرگ‌ترین شناسهٔ موجود + ۱.
     */
    suspend fun addAdvisor(
        name: String,
        accentColorHex: String,
        iconName: String,
        slotTitles: List<String>
    ): AdvisorEntity = withContext(Dispatchers.IO) {
        val nextId = (database.councilDao().getMaxAdvisorId() ?: 0) + 1
        val slots = CouncilDataConverters.defaultSlots(nextId).mapIndexed { index, slot ->
            slot.copy(agentName = slotTitles.getOrNull(index)?.takeIf { it.isNotBlank() } ?: slot.agentName)
        }
        val advisor = AdvisorEntity(
            id = nextId,
            name = name,
            roleTitle = "کارگروه تخصصی شماره $nextId",
            accentColorHex = accentColorHex,
            iconName = iconName,
            isAllowedInMeeting = true,
            isTriageLead = false,
            subAgentsJson = CouncilDataConverters.subAgentsToJson(slots),
            latestReport = "",
            status = "آماده"
        )
        database.councilDao().insertOrUpdate(advisor)
        advisor
    }

    /**
     * حذف کارگروه + پاک‌سازی کلیدهای API رمزنگاری‌شدهٔ همان گروه. اگر گروه حذف‌شده مسئول
     * ارجاع خودکار بود، مسئولیت ارجاع به آخرین گروه باقی‌مانده منتقل می‌شود.
     */
    suspend fun removeAdvisor(advisorId: Int) = withContext(Dispatchers.IO) {
        secureKeyStore.deleteKeysForAdvisor(advisorId)
        database.councilDao().deleteAdvisorById(advisorId)
        val remaining = database.councilDao().getAllAdvisors().firstOrNull() ?: emptyList()
        if (remaining.none { it.isTriageLead } && remaining.isNotEmpty()) {
            database.councilDao().setTriageLead(remaining.last().id)
        }
    }

    suspend fun updateAdvisorSlots(advisorId: Int, slots: List<SubAgentSlot>) = withContext(Dispatchers.IO) {
        val advisor = database.councilDao().getAdvisorById(advisorId) ?: return@withContext
        val updated = advisor.copy(subAgentsJson = CouncilDataConverters.subAgentsToJson(slots))
        database.councilDao().insertOrUpdate(updated)
    }

    suspend fun postChairmanMessage(
        session: MeetingSession,
        text: String,
        audioPath: String? = null,
        attachmentPath: String? = null,
        attachmentName: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val msg = ChatMessage(
            sessionId = session.id,
            senderName = "رییس جلسه (کاربر)",
            senderRole = "CHAIRMAN",
            text = text,
            audioPath = audioPath,
            attachmentPath = attachmentPath,
            attachmentName = attachmentName
        )
        val msgId = database.messageDao().insertMessage(msg)
        msgId
    }

    // Orchestration Engine: Execute meeting deliberation according to selected mode
    suspend fun executeMeetingDeliberation(
        session: MeetingSession,
        userPrompt: String,
        selectedAdvisorIds: List<Int>,
        pipelineAdvisorIds: List<Int>,
        onProgressUpdate: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val masterMemory = memoryManager.loadMasterContext()
        val sessionMemory = memoryManager.loadHistoricalSessionsSummary()
        val allAdvisorsList = database.councilDao().getAllAdvisors().firstOrNull() ?: emptyList()
        val allowedAdvisors = allAdvisorsList.filter { it.isAllowedInMeeting }

        // هر دور مشورت با گزارش‌های تازه آغاز می‌شود — گزارش‌های کهنه نباید به بیانیه
        // نهایی این جلسه یا صفحه نتایج نشت کنند.
        allowedAdvisors.forEach { advisor ->
            database.councilDao().updateReport(advisor.id, "", "آماده")
        }

        val mode = try {
            DispatchMode.valueOf(session.dispatchMode)
        } catch (e: Exception) {
            DispatchMode.AUTO_TRIAGE
        }

        when (mode) {
            DispatchMode.AUTO_TRIAGE -> {
                onProgressUpdate("در حال تحلیل مسئله توسط کارگروه تشخیص روند و ارجاع...")
                // Find triage lead (configurable from settings; falls back to the last group)
                val triageLead = allAdvisorsList.find { it.isTriageLead } ?: allAdvisorsList.lastOrNull()
                val triagePrompt = """
                شما سرپرست کارگروه تشخیص روند و ارجاع خودکار شورا هستید.
                مسئله مطرح شده توسط رییس جلسه:
                «$userPrompt»
                
                لیست ${allAdvisorsList.size} کارگروه شورا:
                ${allAdvisorsList.joinToString("\n") { "${it.id}. ${it.name}" }}
                
                وظیفه:
                تحلیل کنید کدام ۱ تا ۳ کارگروه برای این مسئله اولویت دارند و چرا.
                
                فرمت خروجی (اجباری):
                فقط و فقط یک شیء JSON معتبر - بدون هیچ متن، توضیح یا Markdown اضافه قبل یا بعد از آن - دقیقاً با این ساختار برگردانید:
                {"selectedIds": [<شناسه‌های عددی کارگروه‌های منتخب از ۱ تا ${allAdvisorsList.size}>], "reasoning": "<توضیح کوتاه دلیل انتخاب>"}
                """.trimIndent()

                // مدلِ مسئول تشخیص روند از تنظیمات خود همان کارگروه (جایگاه فعال اول) خوانده می‌شود؛
                // اگر کاربر آن را تنظیم نکرده باشد، به‌صورت امن روی gemini-3.5-flash برمی‌گردد.
                val triageLeadSlot = triageLead
                    ?.let { CouncilDataConverters.jsonToSubAgents(it.subAgentsJson) }
                    ?.firstOrNull { it.isActive }
                val triageApiKey = triageLead?.let { lead ->
                    triageLeadSlot?.let { slot ->
                        secureKeyStore.getKey(lead.id, slot.slotNumber).takeIf { it.isNotBlank() }
                    }
                }

                val triageResult = ModelRouter.callModel(
                    prompt = triagePrompt,
                    systemPrompt = "تشخیص روند و تحلیل موضوعات شورا",
                    modelName = triageLeadSlot?.modelType ?: "gemini-3.5-flash",
                    customApiKey = triageApiKey,
                    endpoint = triageLeadSlot?.cliOrEndpoint
                )

                // Try the structured JSON contract first; fall back to the older heuristic
                // (scanning the raw text for numbers/names) only if the model didn't comply.
                val triageDecision = CouncilDataConverters.parseTriageDecision(triageResult)
                val matchedIds = triageDecision?.selectedIds
                    ?: parseAdvisorIdsFromText(triageResult, allAdvisorsList)

                // Post triage lead message — show the readable reasoning when we have it,
                // otherwise fall back to showing the raw model output as before.
                val triageDisplayText = triageDecision?.reasoning?.takeIf { it.isNotBlank() }
                    ?: triageResult
                database.messageDao().insertMessage(
                    ChatMessage(
                        sessionId = session.id,
                        senderName = triageLead?.name ?: "کارگروه تشخیص روند",
                        senderRole = "TRIAGE_AGENT",
                        advisorId = triageLead?.id,
                        text = "🔎 گزارش تشخیص روند و ارجاع:\n$triageDisplayText"
                    )
                )

                val targetAdvisors = allowedAdvisors.filter { it.id in matchedIds }.ifEmpty {
                    allowedAdvisors.take(2)
                }

                for (advisor in targetAdvisors) {
                    onProgressUpdate("کارگروه [${advisor.name}] در حال مشورت ۵ عضو هوش مصنوعی...")
                    runCouncilDebateAndReport(session, advisor, userPrompt, masterMemory, sessionMemory)
                }
            }

            DispatchMode.SELECTIVE -> {
                val targets = allowedAdvisors.filter { it.id in selectedAdvisorIds }
                if (targets.isEmpty()) {
                    database.messageDao().insertMessage(
                        ChatMessage(
                            sessionId = session.id,
                            senderName = "منشی شورا",
                            senderRole = "SYSTEM",
                            text = "هشدار: هیچ کارگروهی توسط رییس جلسه انتخاب نشده است. لطفاً حداقل یک کارگروه را برگزینید."
                        )
                    )
                    return@withContext
                }

                for (advisor in targets) {
                    onProgressUpdate("کارگروه [${advisor.name}] در حال بررسی دستور کار...")
                    runCouncilDebateAndReport(session, advisor, userPrompt, masterMemory, sessionMemory)
                }
            }

            DispatchMode.PUBLIC_ASSEMBLY -> {
                onProgressUpdate("جلسه علنی با حضور تمامی ${allowedAdvisors.size} کارگروه مجاز آغاز شد...")
                for (advisor in allowedAdvisors) {
                    onProgressUpdate("در حال دریافت نظرات ${advisor.name}...")
                    runCouncilDebateAndReport(session, advisor, userPrompt, masterMemory, sessionMemory)
                }
            }

            DispatchMode.SEQUENTIAL_PIPELINE -> {
                val pipelineIds = if (pipelineAdvisorIds.isNotEmpty()) pipelineAdvisorIds else listOf(1, 2, 3)
                var runningContext = userPrompt
                var step = 1

                for (advisorId in pipelineIds) {
                    val advisor = allowedAdvisors.find { it.id == advisorId } ?: continue
                    onProgressUpdate("مرحله $step: ارجاع به ${advisor.name} و غنی‌سازی پاسخ...")

                    val report = runCouncilDebateAndReport(
                        session = session,
                        advisor = advisor,
                        topic = "مرحله $step زنجیره پیوسته:\n$runningContext",
                        masterMemory = masterMemory,
                        sessionMemory = sessionMemory
                    )
                    runningContext = "ورودی دریافت شده از ${advisor.name} در مرحله $step:\n$report"
                    step++
                }
            }
        }

        // Generate Final Boardroom Resolution & Executive Summary
        onProgressUpdate("در حال تدوین بیانیه و مصوبه نهایی جلسه شورا...")
        synthesizeBoardroomResolution(session, userPrompt, masterMemory)
        onProgressUpdate("جلسه با موفقیت بررسی و نتایج نهایی ثبت شد.")
    }

    private suspend fun runCouncilDebateAndReport(
        session: MeetingSession,
        advisor: AdvisorEntity,
        topic: String,
        masterMemory: String,
        sessionMemory: String
    ): String {
        database.councilDao().updateStatus(advisor.id, "در حال تحلیل")

        val subAgents = CouncilDataConverters.jsonToSubAgents(advisor.subAgentsJson)
        val activeSlots = subAgents.filter { it.isActive }

        val subTeamDeliberationPrompt = buildString {
            appendLine("شما کارگروه تخصصی «${advisor.name}» (شامل ۵ مشاور هوش مصنوعی) هستید.")
            appendLine("موضوع ارجاعی از رییس جلسه: «$topic»")
            appendLine("حافظه مستر سازمانی:")
            appendLine(masterMemory.take(800))
            appendLine("سوابق جلسات پیشین:")
            appendLine(sessionMemory.take(600))
            appendLine("ترکیب مشاوران این کارگروه:")
            activeSlots.forEach { slot ->
                appendLine("- مشاور ${slot.slotNumber} (${slot.agentName}): ${slot.systemPersona}")
            }
            appendLine("\nلطفاً خروجی و گزارش جامع این کارگروه ۵ نفره را با ساختار زیر تدوین فرمایید:")
            appendLine("۱. جمع‌بندی اعضای کارگروه و تحلیل تخصصی")
            appendLine("۲. نقاط قوت، ضعف و ریسک‌های استراتژیک")
            appendLine("۳. راهکار عملیاتی پیشنهادی کارگروه برای تصویب رییس جلسه")
        }

        val leadSlot = activeSlots.firstOrNull()
        // API keys are never stored in Room / subAgentsJson (see CouncilDataConverters.subAgentsToJson) —
        // the real key lives only in the encrypted SecureKeyStore, keyed by (advisorId, slotNumber).
        val resolvedApiKey = leadSlot?.let { secureKeyStore.getKey(advisor.id, it.slotNumber) }
            ?.takeIf { it.isNotBlank() }
        val report = ModelRouter.callModel(
            prompt = subTeamDeliberationPrompt,
            systemPrompt = "گزارش رسمی کارگروه ${advisor.name}",
            modelName = leadSlot?.modelType ?: "gemini-3.5-flash",
            customApiKey = resolvedApiKey,
            endpoint = leadSlot?.cliOrEndpoint
        )

        // Save report in DB and Memory folder
        database.councilDao().updateReport(advisor.id, report, "گزارش آماده")
        memoryManager.saveCouncilReport(session, advisor.id, advisor.name, report)

        // Post into Chat Room
        database.messageDao().insertMessage(
            ChatMessage(
                sessionId = session.id,
                senderName = advisor.name,
                senderRole = "COUNCIL_LEAD",
                advisorId = advisor.id,
                text = "📋 گزارش کارشناسی کارگروه ${advisor.name}:\n\n$report"
            )
        )

        return report
    }

    private suspend fun synthesizeBoardroomResolution(
        session: MeetingSession,
        agendaTopic: String,
        masterMemory: String
    ) {
        val allAdvisorsList = database.councilDao().getAllAdvisors().firstOrNull() ?: emptyList()
        val reportsWithContent = allAdvisorsList.filter { it.latestReport.isNotBlank() }

        val synthesisPrompt = buildString {
            appendLine("شما دبیرکل و تدوین‌کننده بیانیه نهایی شورای هوش مصنوعی هستید.")
            appendLine("دستور کار جلسه: «${session.title} - $agendaTopic»")
            appendLine("گزارش‌های دریافتی از کارگروه‌های مشاور:")
            reportsWithContent.forEach { adv ->
                appendLine("=== گزارش ${adv.name} ===")
                appendLine(adv.latestReport.take(1200))
                appendLine()
            }
            appendLine("وظیفه:")
            appendLine("تدوین «بیانیه و مصوبه نهایی شورا» شامل:")
            appendLine("۱. خلاصه اجرایی توافقات و دستاوردها (Executive Summary)")
            appendLine("۲. تصمیمات و مصوبات ابلاغی به کارگروه‌ها")
            appendLine("۳. گام‌های اجرایی بعدی و مسئول هر بخش")
        }

        // دبیرخانه تدوین بیانیه: همان کارگروه مسئول ارجاع (یا جایگاه فعال اول آن).
        val secretariat = allAdvisorsList.find { it.isTriageLead } ?: allAdvisorsList.firstOrNull()
        val secretariatSlot = secretariat
            ?.let { CouncilDataConverters.jsonToSubAgents(it.subAgentsJson) }
            ?.firstOrNull { it.isActive }
        val secretariatKey = secretariat?.let { lead ->
            secretariatSlot?.let { slot ->
                secureKeyStore.getKey(lead.id, slot.slotNumber).takeIf { it.isNotBlank() }
            }
        }

        val finalResolutionText = ModelRouter.callModel(
            prompt = synthesisPrompt,
            systemPrompt = "تدوین مصوبات و بیانیه رسمی هیئت مدیره و شورا",
            modelName = secretariatSlot?.modelType ?: "gemini-3.5-flash",
            customApiKey = secretariatKey,
            endpoint = secretariatSlot?.cliOrEndpoint
        )

        val summary = "جلسه با بررسی گزارش‌های ${reportsWithContent.size} کارگروه به جمع‌بندی رسید."
        database.sessionDao().updateResolution(session.id, summary, finalResolutionText)

        // Post resolution to chat
        database.messageDao().insertMessage(
            ChatMessage(
                sessionId = session.id,
                senderName = "دبیرخانه شورا (بیانیه پایانی)",
                senderRole = "SYSTEM",
                text = "🏛️ **بیانیه و مصوبات نهایی جلسه شورا:**\n\n$finalResolutionText"
            )
        )

        // Save transcript
        val allMsgs = database.messageDao().getMessagesForSession(session.id).firstOrNull() ?: emptyList()
        val updatedSession = session.copy(executiveSummary = summary, finalResolution = finalResolutionText)
        memoryManager.saveSessionTranscript(updatedSession, allMsgs)
    }

    private fun parseAdvisorIdsFromText(text: String, advisors: List<AdvisorEntity>): List<Int> {
        val maxId = advisors.maxOfOrNull { it.id } ?: 0
        val triageLeadId = advisors.find { it.isTriageLead }?.id
        val found = mutableSetOf<Int>()
        val regex = Regex("(?:کارگروه|گروه|شماره|مشاور|کد)?\\s*(\\d{1,2})")
        regex.findAll(text).forEach { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..maxId && num != triageLeadId) {
                found.add(num)
            }
        }
        advisors.forEach { advisor ->
            if (text.contains(advisor.name) && advisor.id != triageLeadId) {
                found.add(advisor.id)
            }
        }
        return if (found.isNotEmpty()) found.toList() else advisors.map { it.id }.take(2)
    }

    suspend fun addMasterFile(name: String, description: String, content: String) = withContext(Dispatchers.IO) {
        val file = memoryManager.createMasterFile(name, content)
        val entity = MasterFile(
            fileName = name,
            fileDescription = description,
            contentSummary = content.take(300),
            filePath = file.absolutePath
        )
        database.masterFileDao().insertMasterFile(entity)
    }

    fun getMemoryManager(): MemoryManager = memoryManager
}
