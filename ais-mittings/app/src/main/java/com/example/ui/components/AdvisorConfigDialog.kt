package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.data.model.CouncilDataConverters
import com.example.data.model.SubAgentSlot
import com.example.ui.theme.BoardroomBorder
import com.example.ui.theme.BoardroomDarkBg
import com.example.ui.theme.BoardroomSurfaceDark
import com.example.ui.theme.BoardroomSurfaceElevated
import com.example.ui.theme.BoardroomSurfaceHighlight
import com.example.ui.theme.ExecutiveCyan
import com.example.ui.theme.ExecutiveGold
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun AdvisorConfigDialog(
    advisor: AdvisorEntity,
    onDismiss: () -> Unit,
    onSave: (AdvisorEntity, String, String, String, String, List<SubAgentSlot>) -> Unit,
    onSetTriageLead: (Int) -> Unit,
    // API keys are stored encrypted outside Room and are NOT part of advisor.subAgentsJson —
    // the caller (MainActivity, via viewModel.getApiKeysForAdvisor) supplies the decrypted
    // values so the dialog can pre-fill each slot's key field. Keyed by slotNumber (1..5).
    initialApiKeys: Map<Int, String> = emptyMap()
) {
    var councilName by remember { mutableStateOf(advisor.name) }
    var roleTitle by remember { mutableStateOf(advisor.roleTitle) }
    var selectedColor by remember { mutableStateOf(advisor.accentColorHex) }
    var selectedIcon by remember { mutableStateOf(advisor.iconName) }
    var isAllowed by remember { mutableStateOf(advisor.isAllowedInMeeting) }
    var isTriage by remember { mutableStateOf(advisor.isTriageLead) }

    var slots by remember {
        mutableStateOf(
            CouncilDataConverters.jsonToSubAgents(advisor.subAgentsJson).map { slot ->
                slot.copy(customApiKey = initialApiKeys[slot.slotNumber] ?: "")
            }
        )
    }
    var activeSlotTab by remember { mutableStateOf(0) }

    val presetColors = listOf(
        "#3B82F6", "#6366F1", "#10B981", "#EC4899", "#F59E0B",
        "#8B5CF6", "#EF4444", "#14B8A6", "#F97316", "#06B6D4",
        "#84CC16", "#E11D48", "#0284C7", "#D97706", "#4F46E5",
        "#059669", "#7C3AED", "#DC2626", "#16A34A", "#D946EF"
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
                .testTag("advisor_config_dialog")
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
                                imageVector = CouncilIconHelper.getIcon(selectedIcon),
                                contentDescription = null,
                                tint = ExecutiveGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "تنظیمات کارگروه مشاور #${advisor.id}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = "شخصی‌سازی نام، رنگ، لوگو و ۵ مدل هوش مصنوعی زیرمجموعه",
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

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Council General Info Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BoardroomBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "مشخصات عمومی کارگروه مشاور",
                                fontWeight = FontWeight.Bold,
                                color = ExecutiveGold,
                                fontSize = 12.sp
                            )

                            OutlinedTextField(
                                value = councilName,
                                onValueChange = { councilName = it },
                                label = { Text("نام کارگروه مشاور") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("advisor_name_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ExecutiveGold,
                                    unfocusedBorderColor = BoardroomBorder,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )

                            // Color Picker Row
                            Text(text = "رنگ اختصاصی پنجره:", fontSize = 11.sp, color = TextSecondaryDark)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetColors.forEach { colorHex ->
                                    val c = Color(android.graphics.Color.parseColor(colorHex))
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(c, CircleShape)
                                            .clickable { selectedColor = colorHex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedColor.equals(colorHex, ignoreCase = true)) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Icon Selector Row
                            Text(text = "لوگو / آیکون پنجره:", fontSize = 11.sp, color = TextSecondaryDark)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CouncilIconHelper.availableIconNames.forEach { (iconKey, label) ->
                                    val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)
                                    Surface(
                                        color = if (isSelected) ExecutiveGold.copy(alpha = 0.25f) else BoardroomSurfaceHighlight,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isSelected) ExecutiveGold else BoardroomBorder),
                                        modifier = Modifier.clickable { selectedIcon = iconKey }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = CouncilIconHelper.getIcon(iconKey),
                                                contentDescription = label,
                                                tint = if (isSelected) ExecutiveGold else TextSecondaryDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = label,
                                                fontSize = 9.sp,
                                                color = if (isSelected) ExecutiveGold else TextSecondaryDark
                                            )
                                        }
                                    }
                                }
                            }

                            // Permissions and Triage Switches
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مجوز حضور و مشارکت در جلسه",
                                    fontSize = 11.sp,
                                    color = TextPrimaryDark
                                )
                                Switch(
                                    checked = isAllowed,
                                    onCheckedChange = { isAllowed = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = ExecutiveGold)
                                )
                            }
                        }
                    }

                    // 5 Sub-Agents AI Configuration Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BoardroomBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = ExecutiveCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "۵ جایگاه مشاور هوش مصنوعی زیرمجموعه",
                                        fontWeight = FontWeight.Bold,
                                        color = ExecutiveCyan,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = "استخدام مدل با API / CLI / MCP",
                                    fontSize = 9.sp,
                                    color = TextSecondaryDark
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 5 Slots Tabs
                            TabRow(
                                selectedTabIndex = activeSlotTab,
                                containerColor = BoardroomSurfaceHighlight,
                                contentColor = ExecutiveGold
                            ) {
                                (0..4).forEach { index ->
                                    Tab(
                                        selected = activeSlotTab == index,
                                        onClick = { activeSlotTab = index },
                                        text = {
                                            Text(
                                                text = "مدل ${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = if (activeSlotTab == index) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Active Slot Editor
                            val currentSlot = slots.getOrNull(activeSlotTab) ?: SubAgentSlot(slotNumber = activeSlotTab + 1)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = currentSlot.agentName,
                                    onValueChange = { newName ->
                                        slots = slots.mapIndexed { idx, s ->
                                            if (idx == activeSlotTab) s.copy(agentName = newName) else s
                                        }
                                    },
                                    label = { Text("نام اختصاصی مدل هوش مصنوعی") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ExecutiveGold,
                                        unfocusedBorderColor = BoardroomBorder,
                                        focusedTextColor = TextPrimaryDark,
                                        unfocusedTextColor = TextPrimaryDark
                                    )
                                )

                                OutlinedTextField(
                                    value = currentSlot.modelType,
                                    onValueChange = { newModel ->
                                        slots = slots.mapIndexed { idx, s ->
                                            if (idx == activeSlotTab) s.copy(modelType = newModel) else s
                                        }
                                    },
                                    label = { Text("نوع و نام مدل (gemini-3.5-flash, gemini-3.1-pro-preview, gpt-4o, claude...)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ExecutiveGold,
                                        unfocusedBorderColor = BoardroomBorder,
                                        focusedTextColor = TextPrimaryDark,
                                        unfocusedTextColor = TextPrimaryDark
                                    )
                                )

                                OutlinedTextField(
                                    value = currentSlot.customApiKey,
                                    onValueChange = { newKey ->
                                        slots = slots.mapIndexed { idx, s ->
                                            if (idx == activeSlotTab) s.copy(customApiKey = newKey) else s
                                        }
                                    },
                                    label = { Text("API Key اختصاصی این مدل (اختیاری - در صورت خالی بودن از کلید پیش‌فرض استفاده می‌شود)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ExecutiveGold,
                                        unfocusedBorderColor = BoardroomBorder,
                                        focusedTextColor = TextPrimaryDark,
                                        unfocusedTextColor = TextPrimaryDark
                                    )
                                )

                                OutlinedTextField(
                                    value = currentSlot.cliOrEndpoint,
                                    onValueChange = { newCli ->
                                        slots = slots.mapIndexed { idx, s ->
                                            if (idx == activeSlotTab) s.copy(cliOrEndpoint = newCli) else s
                                        }
                                    },
                                    label = { Text("دستور CLI / پروتکل MCP / آدرس سرور محلی (مثلاً ollama run llama3)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ExecutiveGold,
                                        unfocusedBorderColor = BoardroomBorder,
                                        focusedTextColor = TextPrimaryDark,
                                        unfocusedTextColor = TextPrimaryDark
                                    )
                                )

                                OutlinedTextField(
                                    value = currentSlot.systemPersona,
                                    onValueChange = { newPersona ->
                                        slots = slots.mapIndexed { idx, s ->
                                            if (idx == activeSlotTab) s.copy(systemPersona = newPersona) else s
                                        }
                                    },
                                    label = { Text("شرح وظیفه، پرامپت و تخصص مشاور") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ExecutiveGold,
                                        unfocusedBorderColor = BoardroomBorder,
                                        focusedTextColor = TextPrimaryDark,
                                        unfocusedTextColor = TextPrimaryDark
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BoardroomSurfaceElevated),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("انصراف", color = TextSecondaryDark)
                    }

                    Button(
                        onClick = {
                            val updatedAdvisor = advisor.copy(isAllowedInMeeting = isAllowed)
                            onSave(updatedAdvisor, councilName, roleTitle, selectedColor, selectedIcon, slots)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExecutiveGold),
                        modifier = Modifier.testTag("save_advisor_button")
                    ) {
                        Text("ذخیره تغییرات کارگروه", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
