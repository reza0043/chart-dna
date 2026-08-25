package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdvisorEntity
import com.example.data.model.DispatchMode
import com.example.ui.components.AdvisorConfigDialog
import com.example.ui.components.AdvisorReportDialog
import com.example.ui.components.AdvisorStrip
import com.example.ui.components.ChatPane
import com.example.ui.components.NewSessionDialog
import com.example.ui.components.PipelineEditorDialog
import com.example.ui.components.ResultsPane
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.BoardroomBorder
import com.example.ui.theme.BoardroomDarkBg
import com.example.ui.theme.BoardroomSurfaceDark
import com.example.ui.theme.BoardroomSurfaceElevated
import com.example.ui.theme.ExecutiveCyan
import com.example.ui.theme.ExecutiveGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.BoardroomViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BoardroomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BoardroomMainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BoardroomMainScreen(viewModel: BoardroomViewModel) {
    val context = LocalContext.current

    val advisors by viewModel.advisors.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val masterFiles by viewModel.masterFiles.collectAsStateWithLifecycle()
    val dispatchMode by viewModel.dispatchMode.collectAsStateWithLifecycle()
    val selectedAdvisorIds by viewModel.selectedAdvisorIds.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val progressMessage by viewModel.progressMessage.collectAsStateWithLifecycle()
    val selectedForEdit by viewModel.selectedAdvisorForEdit.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isNewSessionOpen by viewModel.isNewSessionDialogOpen.collectAsStateWithLifecycle()
    val isVoiceListening by viewModel.isVoiceListening.collectAsStateWithLifecycle()
    val attachedFile by viewModel.attachedFile.collectAsStateWithLifecycle()
    val pipelineSequence by viewModel.pipelineSequence.collectAsStateWithLifecycle()
    val isPipelineEditorOpen by viewModel.isPipelineEditorOpen.collectAsStateWithLifecycle()

    // پنجره گزارش کارگروه انتخاب‌شده (زیر پنجره‌های نوار بالا/پایین)
    var reportAdvisor by remember { mutableStateOf<AdvisorEntity?>(null) }

    // ===== انتخاب فایل واقعی از دستگاه (متن، صدا، تصویر، سند و ...) =====
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val resolver = context.contentResolver
                var fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
                var fileSize = -1L
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) cursor.getString(nameIdx)?.let { fileName = it }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) fileSize = cursor.getLong(sizeIdx)
                    }
                }
                if (fileSize > 5 * 1024 * 1024) {
                    Toast.makeText(context, "حجم فایل پیوست باید کمتر از ۵ مگابایت باشد", Toast.LENGTH_LONG).show()
                } else {
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        viewModel.attachFile(fileName, bytes)
                        Toast.makeText(context, "فایل «$fileName» به جلسه پیوست شد", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "خواندن فایل انتخاب‌شده ممکن نشد", Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure {
                Toast.makeText(context, "خطا در خواندن فایل انتخاب‌شده", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ===== مجوز میکروفون برای ورودی صوتی (اندروید ۶ به بالا) =====
    var pendingVoiceTranscript by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val callback = pendingVoiceTranscript
        pendingVoiceTranscript = null
        if (granted && callback != null) {
            viewModel.startSpeechRecognition(callback)
        } else if (!granted) {
            Toast.makeText(
                context,
                "برای استفاده از ورودی صوتی، لطفاً مجوز دسترسی به میکروفون را بدهید",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BoardroomDarkBg),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = BoardroomDarkBg,
        topBar = {
            // Top Bar with Settings on top-left as requested
            Surface(
                color = BoardroomSurfaceDark,
                border = BorderStroke(1.dp, BoardroomBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-Left Action Button: Settings & Documentation Dialog
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.openSettings() },
                            modifier = Modifier
                                .size(34.dp)
                                .background(BoardroomSurfaceElevated, CircleShape)
                                .testTag("settings_top_left_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "تنظیمات، حافظه و راهنمای مدل‌ها",
                                tint = ExecutiveGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تنظیمات و حافظه",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                        )
                    }

                    // Center App Identity
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(ExecutiveGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "شورای مشاوران هوش مصنوعی (۲۰ کارگروه تخصصی / ۱۰۰ مدل مشاور)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Right Side: Active Session Indicator
                    Surface(
                        color = ExecutiveCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ExecutiveCyan.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ExecutiveCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "رییس جلسه (کاربر) آنلاین",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ExecutiveCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Horizontal Strip: 10 Advisor Windows (Advisors 1 to 10)
            AdvisorStrip(
                advisors = advisors,
                startIndex = 1,
                endIndex = 10,
                selectedIds = selectedAdvisorIds,
                isSelectiveMode = (dispatchMode == DispatchMode.SELECTIVE),
                onAdvisorClick = { adv ->
                    if (dispatchMode == DispatchMode.SELECTIVE) {
                        viewModel.toggleAdvisorSelection(adv.id)
                    } else {
                        viewModel.openAdvisorEdit(adv)
                    }
                },
                onOpenEdit = { adv -> viewModel.openAdvisorEdit(adv) },
                onOpenReport = { adv -> reportAdvisor = adv },
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Main Middle Workspace:
            //    سمت چپ: پنجره بزرگ گفتگو (متن، صدا، فایل)
            //    سمت راست: صفحه پاسخ‌ها و نتایج کلی جلسه
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.weight(0.62f)) {
                    ChatPane(
                        session = currentSession,
                        sessions = sessions,
                        messages = messages,
                        currentMode = dispatchMode,
                        isProcessing = isProcessing,
                        progressMessage = progressMessage,
                        attachedFile = attachedFile,
                        isVoiceListening = isVoiceListening,
                        pipelineSequence = pipelineSequence,
                        onSelectSession = { s -> viewModel.selectSession(s) },
                        onOpenNewSessionDialog = { viewModel.openNewSessionDialog() },
                        onModeSelected = { mode -> viewModel.setDispatchMode(mode) },
                        onSendMessage = { text -> viewModel.sendChairmanMessage(text) },
                        onAttachFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                        onRemoveAttachment = { viewModel.removeAttachment() },
                        onToggleVoice = { onTranscript ->
                            if (isVoiceListening) {
                                viewModel.stopSpeechRecognition()
                            } else {
                                val micGranted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (micGranted) {
                                    viewModel.startSpeechRecognition { result -> onTranscript(result) }
                                } else {
                                    pendingVoiceTranscript = onTranscript
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onEditPipeline = { viewModel.openPipelineEditor() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(modifier = Modifier.weight(0.38f)) {
                    ResultsPane(
                        session = currentSession,
                        advisors = advisors,
                        isProcessing = isProcessing,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. Bottom Horizontal Strip: 10 Advisor Windows (Advisors 11 to 20)
            AdvisorStrip(
                advisors = advisors,
                startIndex = 11,
                endIndex = 20,
                selectedIds = selectedAdvisorIds,
                isSelectiveMode = (dispatchMode == DispatchMode.SELECTIVE),
                onAdvisorClick = { adv ->
                    if (dispatchMode == DispatchMode.SELECTIVE) {
                        viewModel.toggleAdvisorSelection(adv.id)
                    } else {
                        viewModel.openAdvisorEdit(adv)
                    }
                },
                onOpenEdit = { adv -> viewModel.openAdvisorEdit(adv) },
                onOpenReport = { adv -> reportAdvisor = adv },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Modal Dialogs
    selectedForEdit?.let { advisor ->
        AdvisorConfigDialog(
            advisor = advisor,
            initialApiKeys = viewModel.getApiKeysForAdvisor(advisor.id),
            onDismiss = { viewModel.closeAdvisorEdit() },
            onSave = { adv, name, role, color, icon, slots ->
                viewModel.saveAdvisorEdit(adv, name, role, color, icon, slots)
            },
            onSetTriageLead = { id -> viewModel.setTriageLead(id) }
        )
    }

    // پنجره گزارش کارگروه (خروجی کارگروه ۵ نفره زیر هر پنجره مشاور)
    reportAdvisor?.let { advisor ->
        AdvisorReportDialog(
            advisor = advisor,
            onDismiss = { reportAdvisor = null }
        )
    }

    // ویرایشگر ترتیب زنجیره برای حالت «نظرات پیوسته و زنجیره‌ای»
    if (isPipelineEditorOpen) {
        PipelineEditorDialog(
            advisors = advisors,
            currentSequence = pipelineSequence,
            onConfirm = { order ->
                viewModel.setPipelineSequence(order)
                viewModel.closePipelineEditor()
                Toast.makeText(context, "ترتیب زنجیره مشاوران ثبت شد", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { viewModel.closePipelineEditor() }
        )
    }

    if (isSettingsOpen) {
        SettingsDialog(
            currentMemoryPath = viewModel.getMemoryRootPath(),
            masterFiles = masterFiles,
            advisors = advisors,
            onDismiss = { viewModel.closeSettings() },
            onSaveMemoryPath = { path ->
                viewModel.updateMemoryRootPath(path)
                Toast.makeText(context, "مسیر حافظه ذخیره شد", Toast.LENGTH_SHORT).show()
            },
            onAddMasterDocument = { name, desc, text ->
                viewModel.addMasterDocument(name, desc, text)
                Toast.makeText(context, "سند به پوشه Master اضافه شد", Toast.LENGTH_SHORT).show()
            },
            onSetTriageLead = { id ->
                viewModel.setTriageLead(id)
                Toast.makeText(context, "کارگروه #$id به عنوان مسئول ارجاع تعیین شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (isNewSessionOpen) {
        NewSessionDialog(
            onDismiss = { viewModel.closeNewSessionDialog() },
            onCreate = { title, agenda, mode ->
                viewModel.createNewSession(title, agenda, mode)
                Toast.makeText(context, "جلسه جدید تشکیل و پوشه حافظه باز شد", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
