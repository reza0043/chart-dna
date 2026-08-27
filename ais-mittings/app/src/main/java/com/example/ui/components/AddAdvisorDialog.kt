package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.ui.theme.BoardroomBorder
import com.example.ui.theme.BoardroomSurfaceDark
import com.example.ui.theme.BoardroomSurfaceElevated
import com.example.ui.theme.ExecutiveCyan
import com.example.ui.theme.ExecutiveGold
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

/**
 * دیالوگ افزودن کارگروه جدید.
 * ساختار هر گروه — فارغ از نام، رنگ و آیکون — دقیقاً یکسان است: ۵ جایگاه مشاور هوش مصنوعی
 * که بعداً از «تنظیمات کارگروه» قابل ویرایش‌اند (نوع مدل، کلید API، سرور Ollama و ...).
 * بنابراین افزودن گروه جدید عملاً یک کپی از الگوی استاندارد گروه است و برنامه را سنگین نمی‌کند.
 */
@Composable
fun AddAdvisorDialog(
    nextId: Int,
    onDismiss: () -> Unit,
    onAddAdvisor: (name: String, colorHex: String, iconName: String, slotTitles: List<String>) -> Unit
) {
    // پیش‌تنظیم‌های آماده (شورای دانش‌آموزی) — با یک کلیک همهٔ فیلدها پر می‌شوند
    val presets = listOf(
        PresetGroup("گروه $nextId: انضباط 🕊️", "#818CF8", "balance", listOf("نماینده انضباطی", "مشاور حل اختلاف", "ناظر حیاط و راهروها", "رابط معلمین", "سفیر دوستی")),
        PresetGroup("گروه $nextId: اردو 🏕️", "#34D399", "public", listOf("مسئول برنامه‌ریزی اردو", "مسئول ایمنی و سلامت", "عکاس و مستندساز", "مسئول تدارکات", "مشاور بازی‌های گروهی")),
        PresetGroup("گروه $nextId: نشریه 🎙️", "#F472B6", "campaign", listOf("سردبیر نشریه", "گوینده رادیو مدرسه", "طراح و گرافیست", "خبرنگار رویدادها", "مسئول مصاحبه‌ها")),
        PresetGroup("گروه $nextId: نوآوری 💻", "#22D3EE", "code", listOf("مربی هوش مصنوعی", "توسعه‌دهنده وب مدرسه", "طراح رباتیک", "مسئول چالش‌های کدنویسی", "پشتیبان نرم‌افزار")),
        PresetGroup("گروه $nextId: نیکوکاری 💖", "#FB7185", "eco", listOf("مسئول صندوق نیکوکاری", "شناسایی نیازهای مدرسه", "برگزارکننده بازارچه خیریه", "رابط با خیریه‌ها", "سفیر مهربانی"))
    )

    var name by remember { mutableStateOf("گروه $nextId") }
    var selectedColor by remember { mutableStateOf("#A855F7") }
    var selectedIcon by remember { mutableStateOf("auto_awesome") }
    var slot1 by remember { mutableStateOf("مشاور ۱") }
    var slot2 by remember { mutableStateOf("مشاور ۲") }
    var slot3 by remember { mutableStateOf("مشاور ۳") }
    var slot4 by remember { mutableStateOf("مشاور ۴") }
    var slot5 by remember { mutableStateOf("مشاور ۵") }

    val colorOptions = listOf(
        "#A855F7", "#38BDF8", "#F472B6", "#34D399", "#FBBF24",
        "#818CF8", "#22D3EE", "#FB923C", "#FB7185", "#C084FC"
    )

    fun applyPreset(p: PresetGroup) {
        name = p.name
        selectedColor = p.color
        selectedIcon = p.icon
        slot1 = p.slots[0]; slot2 = p.slots[1]; slot3 = p.slots[2]; slot4 = p.slots[3]; slot5 = p.slots[4]
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("add_advisor_dialog")
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, BoardroomBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BoardroomSurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "➕ افزودن کارگروه جدید",
                        color = ExecutiveGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextSecondaryDark)
                    }
                }

                Text(
                    text = "هر کارگروه دقیقاً همان ساختار استاندارد را دارد: ۵ جایگاه مشاور هوش مصنوعی که بعداً از تنظیمات کارگروه قابل پیکربندی‌اند (مدل، کلید API، سرور Ollama و...).",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )

                // پیش‌تنظیم‌های آماده
                Text("⚡ پیش‌تنظیم‌های آماده (اختیاری):", fontSize = 12.sp, color = TextPrimaryDark, fontWeight = FontWeight.Medium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { p ->
                        OutlinedButton(onClick = { applyPreset(p) }) {
                            Text(p.name.substringAfter(": "), fontSize = 11.sp, color = ExecutiveCyan, maxLines = 1)
                        }
                    }
                }

                // نام کارگروه
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام کارگروه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_advisor_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = ExecutiveGold,
                        unfocusedBorderColor = BoardroomBorder,
                        cursorColor = ExecutiveGold,
                        focusedLabelColor = ExecutiveGold,
                        unfocusedLabelColor = TextSecondaryDark
                    )
                )

                // انتخاب رنگ
                Text("🎨 رنگ نشانگر کارگروه:", fontSize = 12.sp, color = TextPrimaryDark, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { ExecutiveCyan }
                        Box(
                            modifier = Modifier
                                .size(if (selectedColor == hex) 34.dp else 28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (selectedColor == hex) 3.dp else 1.dp,
                                    if (selectedColor == hex) ExecutiveGold else BoardroomBorder,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                // انتخاب آیکون
                Text("🏷️ آیکون کارگروه:", fontSize = 12.sp, color = TextPrimaryDark, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CouncilIconHelper.availableIconNames.forEach { (iconKey, iconLabel) ->
                        Surface(
                            color = if (selectedIcon == iconKey) ExecutiveGold.copy(alpha = 0.2f) else BoardroomSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selectedIcon == iconKey) ExecutiveGold else BoardroomBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { selectedIcon = iconKey }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = CouncilIconHelper.getIcon(iconKey),
                                    contentDescription = iconLabel,
                                    tint = if (selectedIcon == iconKey) ExecutiveGold else TextSecondaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(iconLabel, fontSize = 10.sp, color = if (selectedIcon == iconKey) ExecutiveGold else TextSecondaryDark)
                            }
                        }
                    }
                }

                // عناوین ۵ جایگاه
                Text("👥 عناوین ۵ جایگاه مشاور (بعداً هم قابل تغییر است):", fontSize = 12.sp, color = TextPrimaryDark, fontWeight = FontWeight.Medium)
                SlotTitleField("جایگاه ۱", slot1, { slot1 = it }, "add_advisor_slot_0")
                SlotTitleField("جایگاه ۲", slot2, { slot2 = it }, "add_advisor_slot_1")
                SlotTitleField("جایگاه ۳", slot3, { slot3 = it }, "add_advisor_slot_2")
                SlotTitleField("جایگاه ۴", slot4, { slot4 = it }, "add_advisor_slot_3")
                SlotTitleField("جایگاه ۵", slot5, { slot5 = it }, "add_advisor_slot_4")

                Spacer(modifier = Modifier.height(2.dp))

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
                            val titles = listOf(slot1, slot2, slot3, slot4, slot5)
                            onAddAdvisor(
                                name.ifBlank { "گروه $nextId" },
                                selectedColor,
                                selectedIcon,
                                titles
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExecutiveGold),
                        modifier = Modifier.testTag("confirm_add_advisor_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ایجاد کارگروه", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class PresetGroup(
    val name: String,
    val color: String,
    val icon: String,
    val slots: List<String>
)

@Composable
private fun SlotTitleField(label: String, value: String, onValueChange: (String) -> Unit, tag: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedBorderColor = ExecutiveCyan,
            unfocusedBorderColor = BoardroomBorder,
            cursorColor = ExecutiveCyan,
            focusedLabelColor = ExecutiveCyan,
            unfocusedLabelColor = TextSecondaryDark
        )
    )
}
