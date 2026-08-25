package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AdvisorEntity
import com.example.data.model.MasterFile
import com.example.ui.theme.BoardroomBorder
import com.example.ui.theme.BoardroomSurfaceDark
import com.example.ui.theme.BoardroomSurfaceElevated
import com.example.ui.theme.BoardroomSurfaceHighlight
import com.example.ui.theme.ExecutiveCyan
import com.example.ui.theme.ExecutiveEmerald
import com.example.ui.theme.ExecutiveGold
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun SettingsDialog(
    currentMemoryPath: String,
    masterFiles: List<MasterFile>,
    advisors: List<AdvisorEntity>,
    onDismiss: () -> Unit,
    onSaveMemoryPath: (String) -> Unit,
    onAddMasterDocument: (String, String, String) -> Unit,
    onSetTriageLead: (Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var memoryPathInput by remember { mutableStateOf(currentMemoryPath) }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocContent by remember { mutableStateOf("") }
    var triageDropdownExpanded by remember { mutableStateOf(false) }

    val currentTriageLead = advisors.find { it.isTriageLead } ?: advisors.lastOrNull()

    val tabs = listOf(
        "📚 راهنمای جامع API و مدل‌ها",
        "🗄️ تنظیمات حافظه و فولدر Master",
        "⚙️ کارگروه مسئول تشخیص و ارجاع",
        "ℹ️ معرفی معماری ۲۰ کارگروه"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = BoardroomSurfaceDark,
            border = BorderStroke(1.dp, BoardroomBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(ExecutiveGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = ExecutiveGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مرکز تنظیمات، حافظه و مستندات تخصصی شورا",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = "راهنمای اتصال API، مدل‌های محلی CLI، فریم‌ورک MCP و Agentها",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BoardroomSurfaceElevated,
                    contentColor = ExecutiveGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ExecutiveGold
                        )
                    },
                    edgePadding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) ExecutiveGold else TextSecondaryDark
                                )
                            },
                            modifier = Modifier.testTag("settings_tab_$index")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> ApiAndLocalModelDocsTab()
                        1 -> MemoryAndMasterFolderTab(
                            memoryPath = memoryPathInput,
                            onPathChange = { memoryPathInput = it },
                            onSavePath = { onSaveMemoryPath(memoryPathInput) },
                            masterFiles = masterFiles,
                            newDocTitle = newDocTitle,
                            onTitleChange = { newDocTitle = it },
                            newDocContent = newDocContent,
                            onContentChange = { newDocContent = it },
                            onAddDoc = {
                                if (newDocTitle.isNotBlank() && newDocContent.isNotBlank()) {
                                    onAddMasterDocument(newDocTitle, "سند مستر ثبت شده توسط رییس جلسه", newDocContent)
                                    newDocTitle = ""
                                    newDocContent = ""
                                }
                            }
                        )
                        2 -> TriageCouncilSelectorTab(
                            advisors = advisors,
                            currentLead = currentTriageLead,
                            expanded = triageDropdownExpanded,
                            onExpandChange = { triageDropdownExpanded = it },
                            onSelectTriage = onSetTriageLead
                        )
                        3 -> ArchitectureOverviewTab()
                    }
                }
            }
        }
    }
}

@Composable
fun ApiAndLocalModelDocsTab() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // API Setup Guide Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ExecutiveGold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.IntegrationInstructions,
                        contentDescription = null,
                        tint = ExecutiveGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "۱. راهنمای اتصال کلیدهای اختصاصی API (Gemini, OpenAI, Claude)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ExecutiveGold,
                            fontSize = 12.sp
                        )
                    )
                }
                Text(
                    text = """
                    • هر یک از ۲۰ پنجره مشاور شامل ۵ جایگاه هوش مصنوعی است. کاربر می‌تواند در هر جایگاه کلید API اختصاصی خود را وارد کند یا از کلید سرور مرکزی استفاده نماید.
                    • برای مدل‌های Gemini: از فرمت کلیدهای Google AI Studio (مدل‌های gemini-3.5-flash و gemini-3.1-pro-preview) استفاده نمایید.
                    • برای مدل‌های OpenAI و Anthropic Claude: نام مدل و کلید اختصاصی را در پنجره تنظیمات آن جایگاه ثبت فرمایید.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimaryDark,
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        // Local Models & CLI (Ollama, LM Studio)
        Card(
            colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ExecutiveCyan.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = ExecutiveCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "۲. روش راه‌اندازی مدل‌های محلی سیستم با CLI و Ollama",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ExecutiveCyan,
                            fontSize = 12.sp
                        )
                    )
                }
                Text(
                    text = """
                    • در صورتی که تمایل دارید مدل‌های آفلاین روی سیستم شما پاسخگو باشند:
                    ۱) نرم‌افزار Ollama را روی سیستم نصب کرده و دستور زیر را اجرا کنید:
                       ollama run llama3 (یا mistral / qwen2.5)
                    ۲) در فیلد «دستور CLI / Endpoint» جایگاه مورد نظر، آدرس محلی را وارد کنید:
                       http://localhost:11434/api/generate یا دستور cli مربوطه.
                    ۳) شورا به صورت خودکار پیام‌ها را به انجین محلی ارسال می‌کند.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimaryDark,
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        // MCP & Node.js Agent Protocol
        Card(
            colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ExecutiveEmerald.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ExecutiveEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "۳. اتصال به پروتکل MCP (Model Context Protocol) و Agentهای Node.js",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ExecutiveEmerald,
                            fontSize = 12.sp
                        )
                    )
                }
                Text(
                    text = """
                    • جهت اتصال به Agentهای پیشرفته تحت پروتکل MCP یا سرورهای Node.js:
                    ۱) سرور MCP خود را روی سیستم با پورت مشخص اجرا کنید (مثلاً mcp://localhost:8000).
                    ۲) در جایگاه مشاور، نام مدل را mcp-agent قرار دهید.
                    ۳) Agentهای MCP به حافظه مستر و اسناد متصل جلسه دسترسی کامل خواهند داشت.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimaryDark,
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
fun MemoryAndMasterFolderTab(
    memoryPath: String,
    onPathChange: (String) -> Unit,
    onSavePath: () -> Unit,
    masterFiles: List<MasterFile>,
    newDocTitle: String,
    onTitleChange: (String) -> Unit,
    newDocContent: String,
    onContentChange: (String) -> Unit,
    onAddDoc: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Memory Path Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BoardroomBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "آدرس دایرکتوری حافظه داخلی شورا",
                    fontWeight = FontWeight.Bold,
                    color = ExecutiveGold,
                    fontSize = 12.sp
                )
                Text(
                    text = "تمامی صورت‌جلسات، فایل‌ها، صداها و پوشه Master در این مسیر نگهداری می‌شوند.",
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = memoryPath,
                        onValueChange = onPathChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("memory_path_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ExecutiveGold,
                            unfocusedBorderColor = BoardroomBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = onSavePath,
                        colors = ButtonDefaults.buttonColors(containerColor = ExecutiveGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_memory_path_button")
                    ) {
                        Text("ثبت مسیر", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Master Folder Manager Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BoardroomBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "پوشه Master (اسناد و حافظه مشترک میان کلیه مدل‌ها)",
                    fontWeight = FontWeight.Bold,
                    color = ExecutiveCyan,
                    fontSize = 12.sp
                )
                Text(
                    text = "فایل‌ها و داده‌های قرار گرفته در این پوشه، در تمامی جلسات توسط هر ۲۰ کارگروه مشاور تحلیل و استناد می‌شوند.",
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )

                // Add Master Document Form
                OutlinedTextField(
                    value = newDocTitle,
                    onValueChange = onTitleChange,
                    placeholder = { Text("عنوان سند مستر (مثلاً استراتژی سالانه یا کتابچه فنی)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExecutiveGold,
                        unfocusedBorderColor = BoardroomBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    )
                )

                OutlinedTextField(
                    value = newDocContent,
                    onValueChange = onContentChange,
                    placeholder = { Text("متن و محتوای سند مستر...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExecutiveGold,
                        unfocusedBorderColor = BoardroomBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    )
                )

                Button(
                    onClick = onAddDoc,
                    enabled = newDocTitle.isNotBlank() && newDocContent.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ExecutiveCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("add_master_file_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افزودن به پوشه Master", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // Existing Master Files List
                if (masterFiles.isNotEmpty()) {
                    Text(text = "اسناد مستر ثبت شده در حافظه:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimaryDark)
                    masterFiles.forEach { file ->
                        Surface(
                            color = BoardroomSurfaceHighlight,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ExecutiveCyan)
                                Text(file.contentSummary, fontSize = 10.sp, color = TextSecondaryDark, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TriageCouncilSelectorTab(
    advisors: List<AdvisorEntity>,
    currentLead: AdvisorEntity?,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelectTriage: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BoardroomBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "تعیین کارگروه مسئول تشخیص روند و ارجاع خودکار",
                    fontWeight = FontWeight.Bold,
                    color = ExecutiveGold,
                    fontSize = 12.sp
                )
                Text(
                    text = "در حالت اول (تشخیص خودکار)، کارگروه تعیین‌شده در این بخش ابتدا مسئله رییس جلسه را تحلیل کرده و سپس آن را به متناسب‌ترین کارگروه‌های شورا ارجاع می‌دهد.",
                    fontSize = 10.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )

                Box {
                    Surface(
                        color = BoardroomSurfaceHighlight,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ExecutiveGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExpandChange(true) }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentLead?.let { "#${it.id} - ${it.name}" } ?: "انتخاب کارگروه ارجاع",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ExecutiveGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandChange(false) }
                    ) {
                        advisors.forEach { advisor ->
                            DropdownMenuItem(
                                text = { Text("#${advisor.id} - ${advisor.name}", fontSize = 11.sp) },
                                onClick = {
                                    onSelectTriage(advisor.id)
                                    onExpandChange(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchitectureOverviewTab() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BoardroomBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ساختار هیئت مدیره و شورای ۲۰ کارگروه هوش مصنوعی",
                    fontWeight = FontWeight.Bold,
                    color = ExecutiveGold,
                    fontSize = 12.sp
                )
                Text(
                    text = """
                    • نوار افقی بالا: شامل پنجره‌های کارگروه ۱ تا ۱۰ (استراتژی، معماری، مالی، حقوقی، محصول، مارکت، امنیت، عملیات، منابع انسانی، اخلاق AI).
                    • نوار افقی پایین: شامل پنجره‌های کارگروه ۱۱ تا ۲۰ (تحقیق و توسعه، روابط عمومی، بین‌الملل، CRM، ابری، کیفیت، رقبا، بحران، پایداری، تشخیص روند).
                    • هر پنجره دارای ۵ مشاور تخصصی هوش مصنوعی با قابلیت انتساب اختصاصی کلید API، مدل، و پرامپت است.
                    • صفحه سمت چپ: اتاق گفتگوی بزرگ رییس جلسه با امکان ارسال متن، صوت و فایل.
                    • صفحه سمت راست: خروجی، مصوبات و سنتز نهایی جلسه.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimaryDark,
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}
