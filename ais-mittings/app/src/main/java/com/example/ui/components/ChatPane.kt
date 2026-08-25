package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.DispatchMode
import com.example.data.model.MeetingSession
import com.example.ui.theme.BoardroomBorder
import com.example.ui.theme.BoardroomDarkBg
import com.example.ui.theme.BoardroomSurfaceDark
import com.example.ui.theme.BoardroomSurfaceElevated
import com.example.ui.theme.BoardroomSurfaceHighlight
import com.example.ui.theme.ExecutiveCyan
import com.example.ui.theme.ExecutiveGold
import com.example.ui.theme.ExecutiveIndigo
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatPane(
    session: MeetingSession?,
    sessions: List<MeetingSession>,
    messages: List<ChatMessage>,
    currentMode: DispatchMode,
    isProcessing: Boolean,
    progressMessage: String,
    attachedFile: Pair<String, ByteArray>?,
    isVoiceListening: Boolean,
    pipelineSequence: List<Int>,
    onSelectSession: (MeetingSession) -> Unit,
    onOpenNewSessionDialog: () -> Unit,
    onModeSelected: (DispatchMode) -> Unit,
    onSendMessage: (String) -> Unit,
    onAttachFile: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onToggleVoice: (onTranscript: (String) -> Unit) -> Unit,
    onEditPipeline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var sessionDropdownExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_pane"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ==========================================
        // 1. TOP PANE (نصف بالا): پنجره بازخورد گفتگو و پیام‌های جلسه
        // ==========================================
        Surface(
            color = BoardroomSurfaceDark,
            border = BorderStroke(1.dp, BoardroomBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("chat_feedback_window")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Header Bar: Session Switcher, New Meeting, Title & Agenda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Session Selector
                    Box {
                        Surface(
                            color = BoardroomSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BoardroomBorder),
                            modifier = Modifier
                                .clickable { sessionDropdownExpanded = true }
                                .testTag("session_selector_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "فولدر جلسه",
                                    tint = ExecutiveGold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = session?.title ?: "انتخاب یا شروع جلسه",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sessionDropdownExpanded,
                            onDismissRequest = { sessionDropdownExpanded = false }
                        ) {
                            sessions.forEach { s ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(s.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(s.agenda, fontSize = 10.sp, color = TextSecondaryDark)
                                        }
                                    },
                                    onClick = {
                                        onSelectSession(s)
                                        sessionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Window Title Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(ExecutiveCyan, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "بازخورد گفتگو و مذاکرات جلسه",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ExecutiveCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        )
                    }

                    // New Session Button
                    Button(
                        onClick = onOpenNewSessionDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = ExecutiveGold.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ExecutiveGold.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("new_meeting_button"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "جلسه جدید",
                            tint = ExecutiveGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "جلسه جدید",
                            color = ExecutiveGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Messages Stream (LazyColumn)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    if (messages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = TextSecondaryDark.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "گفتگو آماده است. پیام خود را در پنجره پایین بنویسید تا پاسخ هوشمند در اینجا نمایش یابد.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                ChatMessageItem(message = msg)
                            }
                        }
                    }
                }

                // Processing Progress Animation
                AnimatedVisibility(visible = isProcessing) {
                    Surface(
                        color = ExecutiveGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, ExecutiveGold.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = ExecutiveGold,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = progressMessage.ifBlank { "کارگروه‌های شورا در حال تحلیل و پاسخ هستند..." },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ExecutiveGold,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. BOTTOM PANE (نصف پایین): پنجره گفتگو، تایپ متن، پیوست فایل و صدا (به سبک ChatGPT)
        // ==========================================
        Surface(
            color = BoardroomSurfaceDark,
            border = BorderStroke(1.dp, BoardroomBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("chatgpt_composer_window")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Sub-Header: Title & Mode Chips
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ExecutiveGold, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ارسال گفتگو و پیام",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ExecutiveGold,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4 Dispatch Mode Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DispatchMode.values().forEach { mode ->
                            val isSelected = currentMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onModeSelected(mode) },
                                label = {
                                    Text(
                                        text = mode.titleFa,
                                        fontSize = 9.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    val icon = when (mode) {
                                        DispatchMode.AUTO_TRIAGE -> Icons.Default.AutoAwesome
                                        DispatchMode.SELECTIVE -> Icons.Default.TableChart
                                        DispatchMode.PUBLIC_ASSEMBLY -> Icons.Default.RecordVoiceOver
                                        DispatchMode.SEQUENTIAL_PIPELINE -> Icons.Default.ViewStream
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = if (isSelected) ExecutiveGold else TextSecondaryDark
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ExecutiveGold.copy(alpha = 0.25f),
                                    selectedLabelColor = ExecutiveGold,
                                    containerColor = BoardroomSurfaceElevated,
                                    labelColor = TextSecondaryDark
                                ),
                                border = BorderStroke(1.dp, if (isSelected) ExecutiveGold else BoardroomBorder),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("dispatch_mode_${mode.name}")
                            )
                        }
                    }

                    // نوار ترتیب زنجیره — فقط در حالت «نظرات پیوسته و زنجیره‌ای»
                    if (currentMode == DispatchMode.SEQUENTIAL_PIPELINE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = ExecutiveIndigo.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ExecutiveIndigo.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pipeline_order_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⛓️ ترتیب زنجیره: " + (
                                        pipelineSequence.takeIf { it.isNotEmpty() }
                                            ?.joinToString(" ← ") { id -> "کارگروه #$id" }
                                            ?: "پیش‌فرض (۱ ← ۲ ← ۳)"
                                        ),
                                    color = TextPrimaryDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                OutlinedButton(
                                    onClick = onEditPipeline,
                                    shape = RoundedCornerShape(7.dp),
                                    border = BorderStroke(1.dp, ExecutiveIndigo.copy(alpha = 0.5f)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 8.dp, vertical = 2.dp
                                    ),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(
                                        text = "ویرایش ترتیب",
                                        color = ExecutiveIndigo,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Middle: Attachment Preview + ChatGPT Large Multiline Text Field
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    if (attachedFile != null) {
                        Surface(
                            color = BoardroomSurfaceHighlight,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = ExecutiveCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "فایل پیوست: ${attachedFile.first} (${attachedFile.second.size / 1024} KB)",
                                        color = TextPrimaryDark,
                                        fontSize = 10.sp
                                    )
                                }
                                IconButton(
                                    onClick = onRemoveAttachment,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف فایل",
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ChatGPT Multiline Text Area
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isVoiceListening)
                                    "🎤 در حال شنیدن و ضبط صدای شما..."
                                else
                                    "پیام یا گفتگوی خود را اینجا بنویسید یا از دکمه صدا و پیوست فایل استفاده فرمایید...",
                                fontSize = 11.5.sp,
                                color = TextSecondaryDark,
                                lineHeight = 17.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chairman_input_field"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BoardroomSurfaceElevated,
                            unfocusedContainerColor = BoardroomSurfaceElevated,
                            focusedBorderColor = ExecutiveGold,
                            unfocusedBorderColor = BoardroomBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                if (!isProcessing && (inputText.isNotBlank() || attachedFile != null)) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        )
                    )
                }

                // Bottom Action Dock: File Attachment, Voice Mic, Clear, and Send Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Actions: Attach File + Voice Mic + Clear
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Attach File button (real file picker)
                        IconButton(
                            onClick = onAttachFile,
                            modifier = Modifier
                                .size(36.dp)
                                .background(BoardroomSurfaceElevated, CircleShape)
                                .testTag("attach_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "پیوست فایل به جلسه",
                                tint = ExecutiveCyan,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Voice Recording Button
                        IconButton(
                            onClick = {
                                onToggleVoice { transcript ->
                                    inputText = if (inputText.isBlank()) transcript else "$inputText $transcript"
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isVoiceListening) ExecutiveGold else BoardroomSurfaceElevated,
                                    CircleShape
                                )
                                .testTag("voice_input_button")
                        ) {
                            Icon(
                                imageVector = if (isVoiceListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                contentDescription = "صحبت و ضبط صدا",
                                tint = if (isVoiceListening) Color.Black else ExecutiveGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = { inputText = "" },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(BoardroomSurfaceElevated, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "پاک کردن متن",
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Right Action: Prominent Send / Dispatch Button
                    Button(
                        onClick = {
                            if (inputText.isNotBlank() || attachedFile != null) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = !isProcessing && (inputText.isNotBlank() || attachedFile != null),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExecutiveGold,
                            disabledContainerColor = BoardroomSurfaceElevated
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("send_button"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ارسال پیام",
                            tint = if (!isProcessing && (inputText.isNotBlank() || attachedFile != null)) Color.Black else TextSecondaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ارسال پیام",
                            color = if (!isProcessing && (inputText.isNotBlank() || attachedFile != null)) Color.Black else TextSecondaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isChairman = message.senderRole == "CHAIRMAN"
    val isSystem = message.senderRole == "SYSTEM"
    val isTriage = message.senderRole == "TRIAGE_AGENT"

    val bubbleBg = when {
        isChairman -> BoardroomSurfaceHighlight
        isSystem -> BoardroomSurfaceDark.copy(alpha = 0.8f)
        isTriage -> ExecutiveIndigo.copy(alpha = 0.15f)
        else -> BoardroomSurfaceElevated
    }

    val roleColor = when {
        isChairman -> ExecutiveGold
        isSystem -> ExecutiveCyan
        isTriage -> ExecutiveIndigo
        else -> ExecutiveCyan
    }

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = if (isChairman) Alignment.End else Alignment.Start
    ) {
        // Sender Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = roleColor,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondaryDark,
                    fontSize = 9.sp
                )
            )
        }

        // Message Body
        Surface(
            color = bubbleBg,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (isChairman) ExecutiveGold.copy(alpha = 0.4f) else BoardroomBorder.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                )

                if (!message.attachmentName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = BoardroomDarkBg.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = ExecutiveCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = message.attachmentName,
                                color = ExecutiveCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
