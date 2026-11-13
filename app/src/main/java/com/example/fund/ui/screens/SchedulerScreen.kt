package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.SchedulerEvent
import com.example.fund.ui.components.FundEmptyState
import com.example.ui.theme.*

@Composable
fun SchedulerScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val events by repo.schedulerEvents.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var completedIds by remember { mutableStateOf(setOf<String>()) }

    val filteredEvents = events.filter { ev ->
        when (selectedFilter) {
            "مجلس الإدارة" -> ev.type.contains("مجلس") || ev.type.contains("إدارة")
            "لجنة المساعدات" -> ev.type.contains("مساعدات") || ev.type.contains("لجنة")
            "حملات وتحصيل" -> ev.type.contains("حملة") || ev.type.contains("تحصيل") || ev.type.contains("ميداني")
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "جدول المواعيد والفعاليات",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FundPrimary
                )
                Text(
                    "اجتماعات مجلس الإدارة ولجنة الضمان والحملات الميدانية",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("موعد جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("الكل", "مجلس الإدارة", "لجنة المساعدات", "حملات وتحصيل").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = FundPrimary
                    )
                )
            }
        }

        if (filteredEvents.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                FundEmptyState(
                    icon = Icons.Default.CalendarMonth,
                    title = "لا توجد مواعيد في هذا التصنيف",
                    subtitle = "يمكنك إضافة موعد جديد أو استعراض جميع المواعيد المجدولة.",
                    actionLabel = if (selectedFilter != "الكل") "عرض الكل" else "موعد جديد",
                    onAction = {
                        if (selectedFilter != "الكل") selectedFilter = "الكل" else showAddDialog = true
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredEvents, key = { it.id }) { ev ->
                    val isDone = completedIds.contains(ev.id)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDone) StatusSuccess else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    ev.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = FundPrimary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = ev.type,
                                        color = FundPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = FundPrimary)
                                    Text(ev.date, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = FundGoldDark)
                                    Text(ev.time, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = FundGoldDark)
                                    Text(ev.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                TextButton(
                                    onClick = {
                                        completedIds = if (isDone) completedIds - ev.id else completedIds + ev.id
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isDone) StatusSuccess else Color.Gray,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isDone) "مكتمل ✔" else "تحديد كمكتمل",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDone) StatusSuccess else FundPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSchedulerEventDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newEv ->
                repo.addSchedulerEvent(newEv)
                Toast.makeText(context, "تمت جدولة الفعالية بنجاح", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddSchedulerEventDialog(
    onDismiss: () -> Unit,
    onAdd: (SchedulerEvent) -> Unit
) {
    val defaultLocation = FundRepository.getInstance().appSettings.collectAsStateWithLifecycle().value.defaultEventLocation
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-11-25") }
    var time by remember { mutableStateOf("04:00 م") }
    var location by remember(defaultLocation) { mutableStateOf(defaultLocation) }
    var type by remember { mutableStateOf("مجلس الإدارة") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("جدولة موعد أو اجتماع جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FundPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان الاجتماع أو الفعالية") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("التاريخ") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("الوقت") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("المكان / القاعة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Text("نوع الفعالية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("مجلس الإدارة", "لجنة المساعدات", "حملة ميدانية", "تحصيل وجرد").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(SchedulerEvent(title = title, date = date, time = time, location = location, type = type))
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("حفظ الموعد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
