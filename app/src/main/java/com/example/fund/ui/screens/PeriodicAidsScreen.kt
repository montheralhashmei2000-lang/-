package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.Beneficiary
import com.example.fund.model.UserRole
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.components.SensitiveActionConfirmDialog
import com.example.fund.util.FormatUtils
import com.example.fund.util.LocalScreenFormat
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun shiftMonthYear(current: String, deltaMonths: Int): String {
    val parts = current.split("-")
    val cal = Calendar.getInstance()
    val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
    val m = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
    cal.set(y, m, 1)
    cal.add(Calendar.MONTH, deltaMonths)
    return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeriodicAidsScreen() {
    val context = LocalContext.current
    val screenFormat = LocalScreenFormat.current
    val repo = FundRepository.getInstance()
    val periodicAids by repo.periodicAids.collectAsStateWithLifecycle()
    val beneficiaries by repo.beneficiaries.collectAsStateWithLifecycle()
    val appSettings by repo.appSettings.collectAsStateWithLifecycle()
    val currentUser by repo.currentUser.collectAsStateWithLifecycle()
    val canManage = currentUser.role != UserRole.VIEWER

    val totalMonthly = periodicAids.filter { it.status == "نشطة" }.sumOf { it.monthlyAmountYer }
    var currentDisbursingMonth by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.US).format(Date()))
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchConfirmDialog by remember { mutableStateOf(false) }
    val pendingForMonth = periodicAids.filter { it.status == "نشطة" && it.lastDisbursedMonth != currentDisbursingMonth }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("periodic_aids_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Summary Card (scrolls naturally with the list so list items are never compressed)
        item(key = "periodic_master_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FundPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المساعدات والكفالات الدورية الشهرية",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (canManage) {
                            OutlinedButton(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FundGoldLight),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.heightIn(min = 38.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة كفالة", fontSize = 11.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = "إجمالي الكفالات: ${FormatUtils.formatAmount(totalMonthly, screenFormat)} / شهرياً",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                            Text(
                                text = "لعدد ${FormatUtils.formatCodeOrNumber(periodicAids.size.toString(), screenFormat)} أسرة ويتيم • يوم الصرف: ${appSettings.periodicAidDisburseDay} شهرياً",
                                color = FundGoldLight,
                                fontSize = 11.sp,
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                        }

                        if (canManage) {
                            Button(
                                onClick = { showBatchConfirmDialog = true },
                                enabled = pendingForMonth.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = FundGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.Black, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pendingForMonth.isEmpty()) "تم الصرف للشهر" else "صرف دفعة الشهر (${pendingForMonth.size})",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Month Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "شهر الاستحقاق المستهدف:",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { currentDisbursingMonth = shiftMonthYear(currentDisbursingMonth, -1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "الشهر السابق", tint = FundGoldLight)
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = FormatUtils.formatCodeOrNumber(currentDisbursingMonth, screenFormat),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                            }
                            IconButton(
                                onClick = { currentDisbursingMonth = shiftMonthYear(currentDisbursingMonth, 1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "الشهر التالي", tint = FundGoldLight)
                            }
                        }
                    }
                }
            }
        }

        item(key = "periodic_list_header") {
            Text(
                text = "قائمة الكفالات الشهرية المسجلة (${FormatUtils.formatCodeOrNumber(periodicAids.size.toString(), screenFormat)})",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = FundPrimary,
                modifier = Modifier.padding(top = 2.dp),
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
            )
        }

        if (periodicAids.isEmpty()) {
            item(key = "periodic_empty_state") {
                FundEmptyState(
                    icon = Icons.Default.VolunteerActivism,
                    title = "لا توجد كفالات دورية مسجلة",
                    subtitle = "يمكنك جدولة كفالة أيتام أو مساعدة شهرية ثابتة للأسر المسجلة.",
                    actionLabel = if (canManage) "إضافة كفالة" else null,
                    onAction = if (canManage) ({ showAddDialog = true }) else null
                )
            }
        } else {
            items(periodicAids, key = { it.id }) { pa ->
                val isPaidThisMonth = pa.lastDisbursedMonth == currentDisbursingMonth
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pa.beneficiaryName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "نوع الكفالة: ${pa.sponsorshipType} | آخر شهر مصروف: ${FormatUtils.formatCodeOrNumber(pa.lastDisbursedMonth, screenFormat)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    FormatUtils.formatAmount(pa.monthlyAmountYer, screenFormat),
                                    fontWeight = FontWeight.Bold,
                                    color = FundPrimary,
                                    fontSize = 13.sp,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                                Surface(
                                    color = if (isPaidThisMonth) StatusSuccess.copy(alpha = 0.12f) else StatusPending.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isPaidThisMonth) "مصروفة لشهر ${FormatUtils.formatCodeOrNumber(currentDisbursingMonth, screenFormat)} ✔" else "مستحقة الصرف",
                                        color = if (isPaidThisMonth) StatusSuccess else StatusPending,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )
                                }
                            }
                        }

                        if (canManage && !isPaidThisMonth) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            repo.disburseSinglePeriodicAid(pa.id, currentDisbursingMonth)
                                        }.onSuccess {
                                            Toast.makeText(context, "تم صرف الكفالة الفردية وإصدار سند الصرف", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.message ?: "تعذر صرف الكفالة الفردية", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 38.dp)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp), tint = FundPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("صرف فردي فوري", fontSize = 11.sp, color = FundPrimary, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBatchConfirmDialog) {
        val activeList = pendingForMonth
        val batchAmount = activeList.sumOf { it.monthlyAmountYer }
        SensitiveActionConfirmDialog(
            title = "تأكيد صرف دفعة الشهر ($currentDisbursingMonth)",
            message = "سيتم صرف ${activeList.size} كفالة مستحقة لشهر $currentDisbursingMonth بإجمالي ${FormatUtils.formatAmount(batchAmount, screenFormat)} وتوليد سند الصرف والقيد المحاسبي تلقائياً.",
            confirmText = "اعتماد وصرف الدفعة",
            confirmColor = FundPrimary,
            icon = Icons.Default.DoneAll,
            onConfirm = {
                runCatching {
                    repo.disbursePeriodicAidsBatch(currentDisbursingMonth, activeList.map { it.id })
                }.onSuccess {
                    Toast.makeText(context, "تم صرف الكفالات لشهر $currentDisbursingMonth وتوليد سند الصرف والقيد بنجاح", Toast.LENGTH_SHORT).show()
                    showBatchConfirmDialog = false
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "تعذر صرف دفعة الكفالات", Toast.LENGTH_LONG).show()
                    showBatchConfirmDialog = false
                }
            },
            onDismiss = { showBatchConfirmDialog = false }
        )
    }

    if (showAddDialog) {
        AddPeriodicAidDialog(
            beneficiaries = beneficiaries,
            onDismiss = { showAddDialog = false },
            onAdd = { benId, benName, amount, type ->
                runCatching {
                    repo.addPeriodicAid(benId, benName, amount, type)
                }.onSuccess {
                    Toast.makeText(context, "تمت جدولة الكفالة الدورية بنجاح", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "تعذر إضافة الكفالة", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
private fun AddPeriodicAidDialog(
    beneficiaries: List<Beneficiary>,
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, String) -> Unit
) {
    var selectedBen by remember { mutableStateOf(beneficiaries.firstOrNull()) }
    var dropdownOpen by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf("60000") }
    var sponsorshipType by remember { mutableStateOf("كفالة أيتام") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("جدولة كفالة أو مساعدة دورية جديدة", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("اختر الأسرة المستفيدة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(onClick = { dropdownOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedBen?.familyHeadName ?: "اختر الأسرة...")
                    }
                    DropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                        beneficiaries.forEach { b ->
                            DropdownMenuItem(
                                text = { Text("${b.familyHeadName} (${b.povertyTier.title})") },
                                onClick = {
                                    selectedBen = b
                                    dropdownOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = sponsorshipType,
                    onValueChange = { sponsorshipType = it },
                    label = { Text("نوع الكفالة (كفالة أيتام / إعانة أسرة / مرض مزمن)") },
                    modifier = Modifier.fillMaxWidth()
                )

                val dialogFormat = LocalScreenFormat.current
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ الشهري الثابت (${dialogFormat.resolvedCurrencySymbol()})") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val b = selectedBen
                    if (b != null && amt > 0) {
                        onAdd(b.id, "${b.familyHeadName} ($sponsorshipType)", amt, sponsorshipType)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("حفظ وجدولة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
