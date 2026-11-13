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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fund.ai.GeminiFundAdvisor
import com.example.fund.model.*
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.components.SensitiveActionConfirmDialog
import com.example.fund.ui.components.ThousandSeparatorTransformation
import com.example.fund.ui.viewmodel.AidsViewModel
import com.example.fund.util.TafqeetEngine
import com.example.ui.theme.*
import kotlinx.coroutines.launch

private data class PendingAidAction(
    val request: AidRequest,
    val targetStatus: AidStatus,
    val approvedAmountYer: Double
)

@Composable
fun AidsScreen(
    initialStatusFilter: String = "الكل",
    aidsViewModel: AidsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aidRequests by aidsViewModel.aidRequests.collectAsStateWithLifecycle()
    val members by aidsViewModel.members.collectAsStateWithLifecycle()
    val fundInfo by aidsViewModel.fundInfo.collectAsStateWithLifecycle()
    val currentUser by aidsViewModel.currentUser.collectAsStateWithLifecycle()
    val canManage = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.ACCOUNTANT || currentUser.role == UserRole.REVIEWER

    var selectedFilter by remember(initialStatusFilter) { mutableStateOf(initialStatusFilter) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewAidDialog by remember { mutableStateOf(false) }
    var pendingAidAction by remember { mutableStateOf<PendingAidAction?>(null) }

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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("new_aid_request_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طلب جديد", fontSize = 12.sp)
                }
            }
        }

        if (!canManage) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        "وضع القراءة فقط: صلاحية (${currentUser.role.title}) تتيح المتابعة دون الاعتماد أو الصرف.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برقم الطلب، اسم العضو، أو نوع المساعدة...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                    }
                }
            },
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
        aiEvaluationText?.let { evalText ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    Text(evalText, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        if (isAiLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = FundGoldDark
            )
        }

        // List or Empty State
        if (filteredAids.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                FundEmptyState(
                    icon = Icons.Default.VolunteerActivism,
                    title = "لا توجد طلبات مساعدة مطابقة",
                    subtitle = "جرّب تغيير فلتر الحالة أو البحث، أو قدّم طلب مساعدة جديد للأعضاء المستحقين.",
                    actionLabel = if (canManage) "تقديم طلب جديد" else null,
                    onAction = if (canManage) ({ showNewAidDialog = true }) else null,
                    secondaryActionLabel = "إعادة ضبط الفلتر",
                    onSecondaryAction = {
                        searchQuery = ""
                        selectedFilter = "الكل"
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAids, key = { it.id }) { req ->
                    AidRequestCardItem(
                        req = req,
                        canManage = canManage,
                        onStatusChange = { newStatus, approvedAmt ->
                            pendingAidAction = PendingAidAction(req, newStatus, approvedAmt)
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
    }

    // Confirm sensitive Aid workflow action (Approve / Disburse / Reject)
    pendingAidAction?.let { action ->
        val (dialogTitle, dialogMsg, confirmLabel, confirmColor, dialogIcon) = when (action.targetStatus) {
            AidStatus.APPROVED -> listOf(
                "تأكيد اعتماد طلب المساعدة",
                "هل أنت متأكد من اعتماد طلب (${action.request.requestNo}) للمستفيد ${action.request.memberName} بمبلغ ${"%,d".format(action.approvedAmountYer.toLong())} ﷼؟",
                "اعتماد الطلب",
                StatusInfo,
                Icons.Default.Verified
            )
            AidStatus.DISBURSED -> listOf(
                "تأكيد الصرف النقدي الفوري",
                "سيتم صرف مبلغ ${"%,d".format(action.approvedAmountYer.toLong())} ﷼ لطلب (${action.request.requestNo}) وتوليد سند صرف وقيد محاسبي آلي. هل تود المتابعة؟",
                "صرف فوري وإصدار السند",
                StatusSuccess,
                Icons.Default.Payments
            )
            else -> listOf(
                "تأكيد رفض طلب المساعدة",
                "هل أنت متأكد من رفض طلب (${action.request.requestNo}) الخاص بالعضو ${action.request.memberName}؟",
                "تأكيد الرفض",
                StatusDanger,
                Icons.Default.Cancel
            )
        }
        @Suppress("UNCHECKED_CAST")
        SensitiveActionConfirmDialog(
            title = dialogTitle as String,
            message = dialogMsg as String,
            confirmText = confirmLabel as String,
            confirmColor = confirmColor as Color,
            icon = dialogIcon as androidx.compose.ui.graphics.vector.ImageVector,
            onConfirm = {
                aidsViewModel.updateAidStatus(action.request.id, action.targetStatus, action.approvedAmountYer)
                Toast.makeText(context, "تم تحديث حالة الطلب إلى ${action.targetStatus.title}", Toast.LENGTH_SHORT).show()
                pendingAidAction = null
            },
            onDismiss = { pendingAidAction = null }
        )
    }

    // New Aid Dialog
    if (showNewAidDialog) {
        NewAidRequestDialog(
            members = members,
            ceiling = fundInfo.maxAidCeilingYer,
            onDismiss = { showNewAidDialog = false },
            onSubmit = { memberId, aidType, amount, notes ->
                aidsViewModel.addAidRequest(memberId, aidType, amount, notes)
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
                    color = FundPrimary,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }

            if (req.approvedAmountYer > 0) {
                Text(
                    "المبلغ المعتمد: %,d ﷼".format(req.approvedAmountYer.toLong()),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusSuccess,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }

            if (req.voucherId != null) {
                Text("سند الصرف المرتبط: ${req.voucherId}", fontSize = 11.sp, color = StatusInfo)
            }

            if (req.notes.isNotEmpty()) {
                Text("الملاحظات: ${req.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

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
    ceiling: Double,
    onDismiss: () -> Unit,
    onSubmit: (String, AidType, Double, String) -> Unit
) {
    var selectedMember by remember { mutableStateOf(members.firstOrNull()) }
    var memberDropdownExpanded by remember { mutableStateOf(false) }

    var selectedAidType by remember { mutableStateOf(AidType.MEDICAL) }
    var amountStr by remember { mutableStateOf("150000") }
    var notes by remember { mutableStateOf("") }

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
                    onValueChange = { input ->
                        val cleaned = input.replace(",", "").filter { it.isDigit() }
                        amountStr = cleaned
                    },
                    label = { Text("المبلغ المطلوب (ريال يمني)") },
                    visualTransformation = ThousandSeparatorTransformation(),
                    textStyle = LocalTextStyle.current.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (amount > 0) {
                    Text(
                        text = TafqeetEngine.convertToWords(amount, "ريال يمني", "فلس"),
                        fontSize = 11.sp,
                        color = FundPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isOverCeiling) {
                    Text(
                        "تنبيه: المبلغ يتجاوز السقف الأعلى المسموح به (${"%,d".format(ceiling.toLong())} ﷼) ويلزم استثناء خاص من مجلس الإدارة.",
                        color = StatusDanger,
                        fontSize = 11.sp,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
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
