package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DispatchMode
import com.example.ui.theme.BoardroomBorder
import com.example.ui.theme.BoardroomSurfaceDark
import com.example.ui.theme.BoardroomSurfaceElevated
import com.example.ui.theme.ExecutiveGold
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun NewSessionDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, agenda: String, mode: DispatchMode) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var agenda by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(DispatchMode.AUTO_TRIAGE) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = BoardroomSurfaceDark,
            border = BorderStroke(1.dp, BoardroomBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("new_session_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اعلان صورت جلسه و دستور کار جدید",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ExecutiveGold,
                            fontSize = 13.sp
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextSecondaryDark)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان جلسه (نام فولدر حافظه)") },
                    placeholder = { Text("مثال: جلسه تصمیم‌گیری ورود به بازار بین‌الملل") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_session_title_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExecutiveGold,
                        unfocusedBorderColor = BoardroomBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    )
                )

                OutlinedTextField(
                    value = agenda,
                    onValueChange = { agenda = it },
                    label = { Text("دستور کار و شرح مسئله رییس جلسه") },
                    placeholder = { Text("طرح مسئله اصلی که باید توسط مشاوران شورا بررسی شود...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_session_agenda_field"),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExecutiveGold,
                        unfocusedBorderColor = BoardroomBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    )
                )

                Text(
                    text = "روش تصمیم‌گیری و ارجاع جلسه:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark, fontSize = 10.sp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DispatchMode.values().forEach { mode ->
                        val isSelected = selectedMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMode = mode },
                            label = {
                                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(mode.titleFa, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(mode.description, fontSize = 9.sp, color = TextSecondaryDark)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExecutiveGold.copy(alpha = 0.2f),
                                selectedLabelColor = ExecutiveGold,
                                containerColor = BoardroomSurfaceElevated,
                                labelColor = TextPrimaryDark
                            ),
                            border = BorderStroke(1.dp, if (isSelected) ExecutiveGold else BoardroomBorder),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BoardroomSurfaceElevated)
                    ) {
                        Text("انصراف", color = TextSecondaryDark)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() || agenda.isNotBlank()) {
                                onCreate(title, agenda, selectedMode)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExecutiveGold),
                        modifier = Modifier.testTag("confirm_create_session_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ایجاد و آغاز جلسه", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
