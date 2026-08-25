package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdvisorEntity
import com.example.data.model.CouncilDataConverters
import com.example.ui.theme.BoardroomBorder
import com.example.ui.theme.BoardroomSurfaceDark
import com.example.ui.theme.BoardroomSurfaceElevated
import com.example.ui.theme.ExecutiveCyan
import com.example.ui.theme.ExecutiveGold
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusReportReady
import com.example.ui.theme.StatusThinking
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun AdvisorStrip(
    advisors: List<AdvisorEntity>,
    startIndex: Int,
    endIndex: Int,
    selectedIds: Set<Int>,
    isSelectiveMode: Boolean,
    onAdvisorClick: (AdvisorEntity) -> Unit,
    onOpenEdit: (AdvisorEntity) -> Unit,
    onOpenReport: (AdvisorEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedAdvisors = advisors.filter { it.id in startIndex..endIndex }

    Surface(
        color = BoardroomSurfaceDark.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, BoardroomBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("advisor_strip_${startIndex}_${endIndex}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                displayedAdvisors.forEach { advisor ->
                    AdvisorWindowCard(
                        advisor = advisor,
                        isSelected = selectedIds.contains(advisor.id),
                        isSelectiveMode = isSelectiveMode,
                        onClick = { onAdvisorClick(advisor) },
                        onEdit = { onOpenEdit(advisor) },
                        onOpenReport = { onOpenReport(advisor) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdvisorWindowCard(
    advisor: AdvisorEntity,
    isSelected: Boolean,
    isSelectiveMode: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onOpenReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(advisor.accentColorHex))
    } catch (e: Exception) {
        ExecutiveCyan
    }

    val statusColor = when (advisor.status) {
        "در حال تحلیل" -> StatusThinking
        "گزارش آماده" -> StatusReportReady
        "آماده" -> StatusReady
        else -> StatusAbsent
    }

    val subAgents = CouncilDataConverters.jsonToSubAgents(advisor.subAgentsJson)
    val activeCount = subAgents.count { it.isActive }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected && isSelectiveMode) ExecutiveGold else if (advisor.status == "در حال تحلیل") StatusThinking else BoardroomBorder,
        label = "border_color"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (advisor.isAllowedInMeeting) BoardroomSurfaceElevated else BoardroomSurfaceDark.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (isSelected && isSelectiveMode) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .width(175.dp)
            .height(68.dp)
            .clickable { onClick() }
            .testTag("advisor_card_${advisor.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Top Row: Icon + Name beside it (single line, small text) + Badges/Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CouncilIconHelper.getIcon(advisor.iconName),
                            contentDescription = advisor.name,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = advisor.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 8.5.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "#${advisor.id} • ${advisor.roleTitle}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = accentColor.copy(alpha = 0.9f),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (advisor.isTriageLead) {
                        Surface(
                            color = ExecutiveGold.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 2.dp)
                        ) {
                            Text(
                                text = "ارجاع",
                                color = ExecutiveGold,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 1.dp)
                            )
                        }
                    } else if (!advisor.isAllowedInMeeting) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "غیرمجاز در جلسه",
                            tint = StatusAbsent,
                            modifier = Modifier.size(12.dp).padding(end = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenReport,
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("report_advisor_${advisor.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "گزارش کارگروه",
                            tint = if (advisor.latestReport.isNotBlank()) accentColor else TextSecondaryDark,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("edit_advisor_${advisor.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "تنظیم مدل‌ها",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Bottom Sub Row: Status indicator dot + sub-agents count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.5.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = advisor.status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Text(
                    text = "$activeCount/۵ مدل",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondaryDark,
                        fontSize = 7.5.sp
                    )
                )
            }
        }
    }
}
