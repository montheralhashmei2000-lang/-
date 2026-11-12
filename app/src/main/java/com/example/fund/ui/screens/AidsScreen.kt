package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.ai.GeminiFundAdvisor
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AidsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = FundRepository.getInstance()
    val aidRequests by repo.aidRequests.collectAsState()
    val members by repo.members.collectAsState()
    val currentUser by repo.currentUser.collectAsState()
    val canManage = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.ACCOUNTANT || currentUser.role == UserRole.REVIEWER

    var selectedFilter by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var showNewAidDialog by remember { mutableStateOf(false) }

    var aiEvaluationText by remember { mutableStateOf<String?>(null) }
    var evaluatingRequestNo by remember { mutableStateOf<String?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }

    val filteredAids = aidRequests.filter { req ->
        val matchesSearch = req.memberName.contains(searchQuery, ignoreCase = true) ||
                req.requestNo.contains(searchQuery, ignoreCase = true) ||
                req.aidType.title.contains(searchQuery)
        val matchesFilter = when (selectedFilter) {
            "قيد المراجعة" -> req.status == AidStatus.IN_REVIEW
            "معتمدة" -> req.status == AidStatus.APPROVED
            "مصروفة" -> req.status == AidStatus.DISBURSED
            "مرفوضة" -> req.status == AidStatus.REJECTED
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("طلبات المساعدة والضمان الاجتماعي", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FundPrimary)
                Text("إدارة مراحل الاعتماد والصرف المالي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (canManage) {
                Button(
                    onClick = { showNewAidDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طلب جديد", fontSize = 12.sp)
                }
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برقم الطلب، اسم العضو، أو نوع المساعدة...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("الكل", "قيد المراجعة", "معتمدة", "مصروفة", "مرفوضة").forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = FundPrimary
                    )
                )
            }
        }

        // AI Inline Advisor Banner if active
        if (aiEvaluationText != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = FundLightSurfaceSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, FundGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = FundGoldDark)
                            Text("توصية ذكية من مستشار Gemini ($evaluatingRequestNo)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        IconButton(onClick = { aiEvaluationText = null }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(aiEvaluationText!!, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredAids, key = { it.id }) { req ->
                AidRequestCardItem(
                    req = req,
                    canManage = canManage,
                    onStatusChange = { newStatus, approvedAmt ->
                        repo.updateAidStatus(req.id, newStatus, approvedAmt)
                        Toast.makeText(context, "تم تحديث حالة الطلب إلى ${newStatus.title}", Toast.LENGTH_SHORT).show()
                    },
                    onConsultAi = {
                        evaluatingRequestNo = req.requestNo
                        isAiLoading = true
                        coroutineScope.launch {
                            val member = members.find { it.id == req.memberId }
                            aiEvaluationText = GeminiFundAdvisor.evaluateAidEligibility(req, member)
                            isAiLoading = false
                        }
                    }
                )
            }
        }
    }

    // New Aid Dialog
    if (showNewAidDialog) {
        NewAidRequestDialog(
            members = members,
            onDismiss = { showNewAidDialog = false },
            onSubmit = { memberId, aidType, amount, notes ->
                repo.addAidRequest(memberId, aidType, amount, notes)
                Toast.makeText(context, "تم تقديم طلب المساعدة بنجاح", Toast.LENGTH_SHORT).show()
                showNewAidDialog = false
            }
        )
    }
}

@Composable
private fun AidRequestCardItem(
    req: AidRequest,
    canManage: Boolean,
    onStatusChange: (AidStatus, Double) -> Unit,
    onConsultAi: () -> Unit
) {
    val statusColor = when (req.status) {
        AidStatus.IN_REVIEW -> StatusPending
        AidStatus.APPROVED -> StatusInfo
        AidStatus.DISBURSED -> StatusSuccess
        AidStatus.REJECTED -> StatusDanger
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(req.memberName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("طلب رقم: ${req.requestNo} | التاريخ: ${req.requestDate}", fontSize = 11.sp, color = Color.Gray)
                }

                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = req.status.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("نوع المساعدة: ${req.aidType.title}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "المبلغ المطلوب: %,d ﷼".format(req.requestedAmountYer.toLong()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FundPrimary
                )
            }

            if (req.approvedAmountYer > 0) {
                Text(
                    "المبلغ المعتمد: %,d ﷼".format(req.approvedAmountYer.toLong()),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusSuccess
                )
            }

            if (req.voucherId != null) {
                Text("سند الصرف المرتبط: ${req.voucherId}", fontSize = 11.sp, color = StatusInfo)
            }

            if (req.notes.isNotEmpty()) {
                Text("الملاحظات: ${req.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Action workflow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI Consult button
                TextButton(onClick = onConsultAi) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = FundGoldDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تقييم الذكاء الاصطناعي", fontSize = 11.sp, color = FundGoldDark)
                }

                if (canManage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (req.status == AidStatus.IN_REVIEW) {
                            Button(
                                onClick = { onStatusChange(AidStatus.APPROVED, req.requestedAmountYer) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusInfo),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("اعتماد", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { onStatusChange(AidStatus.REJECTED, 0.0) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("رفض", fontSize = 11.sp, color = StatusDanger)
                            }
                        } else if (req.status == AidStatus.APPROVED) {
                            Button(
                                onClick = { onStatusChange(AidStatus.DISBURSED, req.approvedAmountYer) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("صرف نقدي فوري", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewAidRequestDialog(
    members: List<Member>,
    onDismiss: () -> Unit,
    onSubmit: (String, AidType, Double, String) -> Unit
) {
    var selectedMember by remember { mutableStateOf(members.firstOrNull()) }
    var memberDropdownExpanded by remember { mutableStateOf(false) }

    var selectedAidType by remember { mutableStateOf(AidType.MEDICAL) }
    var amountStr by remember { mutableStateOf("150000") }
    var notes by remember { mutableStateOf("") }

    val ceiling = FundRepository.getInstance().fundInfo.collectAsState().value.maxAidCeilingYer
    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val isOverCeiling = amount > ceiling

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تقديم طلب مساعدة جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المستفيد / العضو:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(
                        onClick = { memberDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedMember?.fullName ?: "اختر العضو...")
                    }

                    DropdownMenu(
                        expanded = memberDropdownExpanded,
                        onDismissRequest = { memberDropdownExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.fullName) },
                                onClick = {
                                    selectedMember = m
                                    memberDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("نوع المساعدة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AidType.values().take(3).forEach { type ->
                        FilterChip(
                            selected = selectedAidType == type,
                            onClick = { selectedAidType = type },
                            label = { Text(type.title, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AidType.values().drop(3).forEach { type ->
                        FilterChip(
                            selected = selectedAidType == type,
                            onClick = { selectedAidType = type },
                            label = { Text(type.title, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ المطلوب (ريال يمني)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isOverCeiling) {
                    Text(
                        "تنبيه: المبلغ يتجاوز السقف الأعلى المسموح به (${"%,d".format(ceiling.toLong())} ﷼) ويلزم استثناء خاص من مجلس الإدارة.",
                        color = StatusDanger,
                        fontSize = 11.sp
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("البيان والمبررات والتقارير الطبية") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = selectedMember
                    if (m != null && amount > 0) {
                        onSubmit(m.id, selectedAidType, amount, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("إرسال الطلب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
