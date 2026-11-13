package com.example.fund.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.ui.components.FundEmptyState
import com.example.ui.theme.*

@Composable
fun AuditScreen() {
    val repo = FundRepository.getInstance()
    val logs by repo.auditLogs.collectAsStateWithLifecycle()
    val currentUser by repo.currentUser.collectAsStateWithLifecycle()
    val serverConfig by repo.serverConfig.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }

    val filteredLogs = logs.filter { log ->
        val matchesSearch = log.actionType.contains(searchQuery, ignoreCase = true) ||
                log.description.contains(searchQuery, ignoreCase = true) ||
                log.userName.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "سندات وقيود" -> log.actionType.contains("سند") || log.actionType.contains("قيد") || log.actionType.contains("تحويل") || log.actionType.contains("اشتراك")
            "مساعدات" -> log.actionType.contains("مساعدة") || log.actionType.contains("صرف") || log.actionType.contains("عينية")
            "مزامنة وخادم" -> log.actionType.contains("مزامنة") || log.actionType.contains("خادم") || log.actionType.contains("شبكة")
            "دخول وأمان" -> log.actionType.contains("دخول") || log.actionType.contains("صلاحية") || log.actionType.contains("تصفير") || log.actionType.contains("استعادة")
            else -> true
        }
        matchesSearch && matchesFilter
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
                    "سجل العمليات والرقابة المحاسبية (Audit Trail)",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FundPrimary
                )
                Text(
                    "سجل زمني محمي وغير قابل للتعديل يوثق كل حركة في النظام",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.Security, contentDescription = null, tint = FundGoldDark, modifier = Modifier.size(26.dp))
        }

        // Security Summary Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = FundDeepHeader)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = FundGoldLight, modifier = Modifier.size(20.dp))
                    Column {
                        Text("إجمالي العمليات الموثقة: ${logs.size} حركة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("الجلسة النشطة: ${currentUser.fullName} | العقدة: ${serverConfig.serverHost}", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                    }
                }
                Surface(color = FundGold.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "SHA-256 مؤمّن",
                        color = FundGoldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث بنوع العملية، اسم الموظف، أو تفاصيل الحركة...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("الكل", "سندات وقيود", "مساعدات", "مزامنة وخادم", "دخول وأمان").forEach { filter ->
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

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                FundEmptyState(
                    icon = Icons.Default.Security,
                    title = "لا توجد سجلات رقابية مطابقة",
                    subtitle = "جرّب تغيير تصنيف السجل أو مسح عبارة البحث.",
                    actionLabel = "إعادة ضبط الفلترة",
                    onAction = {
                        searchQuery = ""
                        selectedFilter = "الكل"
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    val badgeColor = when {
                        log.actionType.contains("إلغاء") || log.actionType.contains("تصفير") || log.actionType.contains("رفض") -> StatusDanger
                        log.actionType.contains("مزامنة") || log.actionType.contains("خادم") -> StatusInfo
                        log.actionType.contains("سند") || log.actionType.contains("اعتماد") -> StatusSuccess
                        else -> FundPrimary
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = badgeColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = log.actionType,
                                        color = badgeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    log.timestamp,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                            }

                            Text(log.description, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الموظف المنفذ: ${log.userName}", fontSize = 10.sp, color = FundPrimary, fontWeight = FontWeight.SemiBold)
                                Text("العقدة: ${log.ipOrDevice}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
