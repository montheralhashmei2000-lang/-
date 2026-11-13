package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.Beneficiary
import com.example.fund.model.UserRole
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.components.SensitiveActionConfirmDialog
import com.example.ui.theme.*

@Composable
fun PeriodicAidsScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val periodicAids by repo.periodicAids.collectAsStateWithLifecycle()
    val beneficiaries by repo.beneficiaries.collectAsStateWithLifecycle()
    val currentUser by repo.currentUser.collectAsStateWithLifecycle()
    val canManage = currentUser.role != UserRole.VIEWER

    val totalMonthly = periodicAids.filter { it.status == "نشطة" }.sumOf { it.monthlyAmountYer }
    var currentDisbursingMonth by remember { mutableStateOf("2026-11") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FundPrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المساعدات والكفالات الدورية الشهرية", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    if (canManage) {
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FundGoldLight),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة كفالة", fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "إجمالي الكفالات: %,d ﷼ / شهرياً".format(totalMonthly.toLong()),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                        Text("لعدد ${periodicAids.size} أسرة ويتيم", color = FundGoldLight, fontSize = 11.sp)
                    }

                    if (canManage) {
                        Button(
                            onClick = { showBatchConfirmDialog = true },
                            enabled = periodicAids.any { it.status == "نشطة" },
                            colors = ButtonDefaults.buttonColors(containerColor = FundGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("صرف دفعة الشهر", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Text(
            "قائمة الكفالات الشهرية المسجلة",
            fontFamily = AmiriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = FundPrimary
        )

        if (periodicAids.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                FundEmptyState(
                    icon = Icons.Default.VolunteerActivism,
                    title = "لا توجد كفالات دورية مسجلة",
                    subtitle = "يمكنك جدولة كفالة أيتام أو مساعدة شهرية ثابتة للأسر المسجلة.",
                    actionLabel = if (canManage) "إضافة كفالة" else null,
                    onAction = if (canManage) ({ showAddDialog = true }) else null
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(pa.beneficiaryName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("نوع الكفالة: ${pa.sponsorshipType} | آخر شهر مصروف: ${pa.lastDisbursedMonth}", fontSize = 11.sp, color = Color.Gray)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "%,d ﷼".format(pa.monthlyAmountYer.toLong()),
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
                                            text = if (isPaidThisMonth) "مصروفة لشهر $currentDisbursingMonth ✔" else "مستحقة الصرف",
                                            color = if (isPaidThisMonth) StatusSuccess else StatusPending,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
                                            repo.disburseSinglePeriodicAid(pa.id, currentDisbursingMonth)
                                            Toast.makeText(context, "تم صرف الكفالة الفردية وإصدار سند الصرف", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp), tint = FundPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("صرف فردي فوري", fontSize = 11.sp, color = FundPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBatchConfirmDialog) {
        val activeList = periodicAids.filter { it.status == "نشطة" }
        val batchAmount = activeList.sumOf { it.monthlyAmountYer }
        SensitiveActionConfirmDialog(
            title = "تأكيد صرف دفعة الشهر ($currentDisbursingMonth)",
            message = "سيتم صرف ${activeList.size} كفالة نشطة بإجمالي %,d ﷼ وتوليد سندات الصرف والقيود المحاسبية تلقائياً.".format(batchAmount.toLong()),
            confirmText = "اعتماد وصرف الدفعة",
            confirmColor = FundPrimary,
            icon = Icons.Default.DoneAll,
            onConfirm = {
                repo.disbursePeriodicAidsBatch(currentDisbursingMonth, activeList.map { it.id })
                Toast.makeText(context, "تم صرف الكفالات لشهر $currentDisbursingMonth وتوليد سند الصرف والقيد بنجاح", Toast.LENGTH_SHORT).show()
                showBatchConfirmDialog = false
            },
            onDismiss = { showBatchConfirmDialog = false }
        )
    }

    if (showAddDialog) {
        AddPeriodicAidDialog(
            beneficiaries = beneficiaries,
            onDismiss = { showAddDialog = false },
            onAdd = { benId, benName, amount, type ->
                repo.addPeriodicAid(benId, benName, amount, type)
                Toast.makeText(context, "تمت جدولة الكفالة الدورية بنجاح", Toast.LENGTH_SHORT).show()
                showAddDialog = false
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

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ الشهري الثابت (ريال يمني)") },
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
