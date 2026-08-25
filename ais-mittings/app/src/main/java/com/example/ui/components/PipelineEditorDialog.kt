package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AdvisorEntity
import com.example.ui.theme.BoardroomBorder
import com.example.ui.theme.BoardroomSurfaceDark
import com.example.ui.theme.BoardroomSurfaceElevated
import com.example.ui.theme.ExecutiveCyan
import com.example.ui.theme.ExecutiveGold
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

/**
 * ویرایشگر «نظرات پیوسته و زنجیره‌ای» (حالت ۴):
 * کاربر با لمس کارگروه‌ها به‌ترتیب، زنجیره ارجاع را می‌سازد؛
 * مسئله به اولین کارگروه می‌رود، پاسخ او ورودی بعدی می‌شود و ... .
 * لمس یک کارگروه که در زنجیره است، آن را حذف می‌کند.
 */
@Composable
fun PipelineEditorDialog(
    advisors: List<AdvisorEntity>,
    currentSequence: List<Int>,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var sequence by remember { mutableStateOf(currentSequence.toList()) }

    val allowed = advisors.filter { it.isAllowedInMeeting }

    fun toggle(id: Int) {
        sequence = if (sequence.contains(id)) sequence - id else sequence + id
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = BoardroomSurfaceDark,
            border = BorderStroke(1.dp, BoardroomBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // ===== Header =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = ExecutiveGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ترتیب زنجیره مشاوران",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "کارگروه‌ها را به ترتیب لمس کنید تا زنجیره ارجاع ساخته شود. مسئله ابتدا به کارگروه ۱ از زنجیره می‌رود و پاسخ هر مرحله، ورودی مرحله بعد می‌گردد. لمس یک کارگروهِ موجود در زنجیره، آن را حذف می‌کند.",
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ===== Current chain preview =====
                Surface(
                    color = BoardroomSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ExecutiveGold.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pipeline_chain_preview")
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "زنجیره فعلی:",
                            color = ExecutiveGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (sequence.isEmpty()) {
                            Text(
                                text = "خالی — هنوز کارگروهی انتخاب نشده است (پیش‌فرض: ۱ ← ۲ ← ۳)",
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                        } else {
                            val chainText = sequence.mapIndexed { idx, id ->
                                "${idx + 1}) کارگروه #$id"
                            }.joinToString("  ←  ")
                            Text(
                                text = chainText,
                                color = TextPrimaryDark,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ===== Advisor chips (scrollable) =====
                Text(
                    text = "کارگروه‌های مجاز جلسه:",
                    color = ExecutiveCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .height(240.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        allowed.forEach { advisor ->
                            val inChain = sequence.contains(advisor.id)
                            val accent = runCatching {
                                Color(android.graphics.Color.parseColor(advisor.accentColorHex))
                            }.getOrDefault(ExecutiveCyan)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = inChain,
                                    onClick = { toggle(advisor.id) },
                                    label = {
                                        Text(
                                            text = "#${advisor.id} ${advisor.name}",
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(accent, CircleShape)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ExecutiveGold.copy(alpha = 0.25f),
                                        selectedLabelColor = ExecutiveGold,
                                        containerColor = BoardroomSurfaceElevated,
                                        labelColor = TextSecondaryDark
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (inChain) ExecutiveGold else BoardroomBorder
                                    ),
                                    modifier = Modifier.testTag("pipeline_chip_${advisor.id}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (inChain) {
                                    Surface(
                                        color = ExecutiveGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "مرحله ${sequence.indexOf(advisor.id) + 1}",
                                            color = ExecutiveGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ===== Actions =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { sequence = emptyList() },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BoardroomBorder)
                    ) {
                        Text(
                            text = "پاک کردن زنجیره",
                            color = TextSecondaryDark,
                            fontSize = 10.5.sp
                        )
                    }
                    Button(
                        onClick = { onConfirm(sequence) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExecutiveGold.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ExecutiveGold.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("pipeline_save_button")
                    ) {
                        Text(
                            text = "ثبت ترتیب",
                            color = ExecutiveGold,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
