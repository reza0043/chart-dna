package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.AdvisorEntity
import com.example.data.model.CouncilDataConverters
import com.example.data.model.MeetingSession
import com.example.ui.theme.BoardroomBorder
import com.example.ui.theme.BoardroomSurfaceDark
import com.example.ui.theme.BoardroomSurfaceElevated
import com.example.ui.theme.ExecutiveCyan
import com.example.ui.theme.ExecutiveEmerald
import com.example.ui.theme.ExecutiveGold
import com.example.ui.theme.StatusReportReady
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

/**
 * «صفحه پاسخ و نتایج کلی جلسه» — پنل سمت راست صفحه اصلی.
 *
 * خروجی نهایی جلسه را در سه بخش نمایش می‌دهد:
 *  ۱. اطلاعات جلسه و خلاصه اجرایی (Executive Summary)
 *  ۲. بیانیه و مصوبه نهایی شورا
 *  ۳. گزارش تازه‌ی هر کارگروه مشاور (باز/بسته‌شونده) — همان «صفحه ارائه گزارش» که
 *     خروجی کارگروه ۵ نفره را زیر پنجره هر مشاور نشان می‌دهد.
 */
@Composable
fun ResultsPane(
    session: MeetingSession?,
    advisors: List<AdvisorEntity>,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BoardroomSurfaceDark,
        border = BorderStroke(1.dp, BoardroomBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("results_pane")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // ===== Header =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(ExecutiveGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = ExecutiveGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "نتایج و مصوبات جلسه",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            fontSize = 12.sp
                        )
                    )
                }

                if (isProcessing) {
                    Surface(
                        color = ExecutiveGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ExecutiveGold.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "در حال تدوین...",
                            color = ExecutiveGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = ExecutiveEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ExecutiveEmerald.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "به‌روز",
                            color = ExecutiveEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (session == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = TextSecondaryDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "جلسه‌ای انتخاب نشده است. پس از ارسال دستور کار، نتایج و مصوبات این جلسه در همین پنل نمایش داده می‌شود.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryDark,
                                fontSize = 10.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ===== 1. Session info + executive summary =====
                    item {
                        ResultsSectionCard(title = "📋 اطلاعات و خلاصه اجرایی جلسه", accent = ExecutiveCyan) {
                            Text(
                                text = "موضوع: ${session.title}",
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "دستور کار: ${session.agenda}",
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = session.executiveSummary.ifBlank {
                                    "هنوز خلاصه اجرایی برای این جلسه ثبت نشده است."
                                },
                                color = TextPrimaryDark.copy(alpha = 0.9f),
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // ===== 2. Final resolution =====
                    item {
                        ExpandableResultsCard(
                            title = "🏛️ بیانیه و مصوبه نهایی شورا",
                            accent = ExecutiveGold,
                            previewText = session.finalResolution.ifBlank {
                                "بیانیه نهایی پس از اتمام مشورت کارگروه‌ها در این بخش ثبت می‌شود."
                            },
                            initiallyExpanded = true,
                            testTag = "final_resolution_card"
                        )
                    }

                    // ===== 3. Council reports =====
                    item {
                        Text(
                            text = "📊 گزارش کارگروه‌های مشاور (این جلسه)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        )
                    }

                    val withReports = advisors.filter { it.latestReport.isNotBlank() }
                    if (withReports.isEmpty()) {
                        item {
                            Surface(
                                color = BoardroomSurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BoardroomBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "هنوز کارگروهی گزارشی ارائه نکرده است. با ارسال مسئله در پنجره گفتگو (سمت چپ)، گزارش هر کارگروه اینجا نمایش داده می‌شود.",
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    } else {
                        items(withReports, key = { it.id }) { advisor ->
                            ExpandableResultsCard(
                                title = "#${advisor.id} ${advisor.name}",
                                accent = runCatching {
                                    Color(android.graphics.Color.parseColor(advisor.accentColorHex))
                                }.getOrDefault(StatusReportReady),
                                previewText = advisor.latestReport,
                                initiallyExpanded = false,
                                testTag = "advisor_report_card_${advisor.id}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsSectionCard(
    title: String,
    accent: Color,
    content: @Composable () -> Unit
) {
    Surface(
        color = BoardroomSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BoardroomBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(accent, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    color = accent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            content()
        }
    }
}

/** کارت باز/بسته‌شونده: پیش‌نمایش ۳ خطی در حالت بسته، متن کامل در حالت باز. */
@Composable
private fun ExpandableResultsCard(
    title: String,
    accent: Color,
    previewText: String,
    initiallyExpanded: Boolean,
    testTag: String
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Surface(
        color = BoardroomSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(accent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = title,
                        color = TextPrimaryDark,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "بستن" else "نمایش کامل",
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (expanded) {
                Text(
                    text = previewText,
                    color = TextPrimaryDark.copy(alpha = 0.92f),
                    fontSize = 10.sp,
                    lineHeight = 15.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp) // حداکثر ارتفاع؛ متن بلند داخل خودش اسکرول می‌شود
                        .verticalScroll(rememberScrollState())
                )
            } else {
                Text(
                    text = previewText,
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
