package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
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
import com.example.fund.model.CurrencyCode
import com.example.fund.model.Member
import com.example.fund.model.PaymentMethod
import com.example.fund.model.UserRole
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.components.MoneyInput
import com.example.ui.theme.*

@Composable
fun SubscriptionsScreen(
    initialSearch: String = "",
    preselectedMemberId: String = ""
) {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val subscriptions by repo.subscriptions.collectAsStateWithLifecycle()
    val members by repo.members.collectAsStateWithLifecycle()
    val currentUser by repo.currentUser.collectAsStateWithLifecycle()
    val canCollect = currentUser.role != UserRole.VIEWER

    var showCollectDialog by remember { mutableStateOf(false) }
    var searchQuery by remember(initialSearch) { mutableStateOf(initialSearch) }
    var handledPreselect by remember { mutableStateOf("") }

    LaunchedEffect(preselectedMemberId, members, canCollect) {
        if (preselectedMemberId.isNotBlank() && preselectedMemberId != handledPreselect && canCollect) {
            if (members.any { it.id == preselectedMemberId }) {
                handledPreselect = preselectedMemberId
                showCollectDialog = true
            }
        }
    }

    val totalCollected = subscriptions.sumOf { it.amountYer }

    val filteredSubs = subscriptions.filter {
        it.memberName.contains(searchQuery, ignoreCase = true) ||
                it.receiptNo.contains(searchQuery, ignoreCase = true) ||
                it.monthYear.contains(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FundPrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إجمالي تحصيلات الاشتراكات المسجلة", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Text(
                        "%,d ﷼".format(totalCollected.toLong()),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                    Text(
                        "عدد العمليات: ${subscriptions.size} اشتراك",
                        color = FundGoldLight,
                        fontSize = 11.sp,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }

                if (canCollect) {
                    Button(
                        onClick = { showCollectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FundGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("collect_subscription_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحصيل اشتراك", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برقم الإيصال، اسم العضو، أو الشهر...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Subscriptions List or Empty State
        if (filteredSubs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                FundEmptyState(
                    icon = Icons.Default.Payments,
                    title = "لا توجد تحصيلات اشتراكات مطابقة",
                    subtitle = "لم يتم العثور على إيصالات تحصيل مطابقة لبحثك، يمكنك مسح البحث أو تسجيل تحصيل جديد.",
                    actionLabel = if (canCollect) "تحصيل اشتراك جديد" else null,
                    onAction = if (canCollect) ({ showCollectDialog = true }) else null,
                    secondaryActionLabel = if (searchQuery.isNotBlank()) "مسح البحث" else null,
                    onSecondaryAction = if (searchQuery.isNotBlank()) ({ searchQuery = "" }) else null
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSubs, key = { it.id }) { sub ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sub.memberName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "%,d ﷼".format(sub.amountYer.toLong()),
                                    fontWeight = FontWeight.Bold,
                                    color = FundPrimary,
                                    fontSize = 14.sp,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إيصال: ${sub.receiptNo} | شهر: ${sub.monthYear}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(sub.paymentMethod.title, fontSize = 11.sp, color = FundGoldDark)
                            }

                            if (sub.currency != CurrencyCode.YER) {
                                Text(
                                    "أصل المبلغ: ${sub.amountOriginal} ${sub.currency.name} (سعر الصرف: %,.0f)".format(sub.exchangeRate),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                            }

                            if (sub.notes.isNotEmpty()) {
                                Text("ملاحظة: ${sub.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // Collect Subscription Dialog
    if (showCollectDialog) {
        CollectSubscriptionDialog(
            members = members,
            preselectedMemberId = preselectedMemberId,
            onDismiss = { showCollectDialog = false },
            onCollect = { memberId, amount, curr, month, method, ref, notes ->
                repo.addSubscription(
                    memberId = memberId,
                    amountOriginal = amount,
                    currency = curr,
                    monthYear = month,
                    paymentMethod = method,
                    referenceNo = ref,
                    notes = notes
                )
                Toast.makeText(context, "تم تحصيل الاشتراك وإصدار السند والقيد المحاسبي بنجاح", Toast.LENGTH_SHORT).show()
                showCollectDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectSubscriptionDialog(
    members: List<Member>,
    preselectedMemberId: String = "",
    onDismiss: () -> Unit,
    onCollect: (String, Double, CurrencyCode, String, PaymentMethod, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedMember by remember(preselectedMemberId, members) {
        mutableStateOf(members.find { it.id == preselectedMemberId } ?: members.firstOrNull())
    }
    var memberDropdownExpanded by remember { mutableStateOf(false) }

    val defaultFee = FundRepository.getInstance().appSettings.collectAsStateWithLifecycle().value.monthlySubscriptionFeeYer.toLong().toString()
    var amountStr by remember(defaultFee) { mutableStateOf(defaultFee) }
    var selectedCurrency by remember { mutableStateOf(CurrencyCode.YER) }
    var monthYear by remember { mutableStateOf("2026-11") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var refNo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحصيل اشتراك شهري جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Member Selector
                Text("اختر العضو:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(
                        onClick = { memberDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(selectedMember?.fullName ?: "اختر العضو...", maxLines = 1)
                    }

                    DropdownMenu(
                        expanded = memberDropdownExpanded,
                        onDismissRequest = { memberDropdownExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.fullName} (${m.memberNo})") },
                                onClick = {
                                    selectedMember = m
                                    memberDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Money Input
                MoneyInput(
                    amount = amountStr,
                    onAmountChange = { amountStr = it },
                    selectedCurrency = selectedCurrency,
                    onCurrencyChange = { selectedCurrency = it },
                    label = "مبلغ الاشتراك"
                )

                // Month
                OutlinedTextField(
                    value = monthYear,
                    onValueChange = { monthYear = it },
                    label = { Text("شهر الاشتراك (سنة-شهر)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Method
                Text("طريقة الدفع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PaymentMethod.values().forEach { method ->
                        val isSelected = selectedMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMethod = method },
                            label = { Text(method.title, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = refNo,
                    onValueChange = { refNo = it },
                    label = { Text("رقم المرجع / الحوالة البنكية (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val m = selectedMember
                    if (m == null) {
                        Toast.makeText(context, "يرجى اختيار العضو أولاً", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (amount <= 0.0) {
                        Toast.makeText(context, "يرجى إدخال مبلغ صحيح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onCollect(m.id, amount, selectedCurrency, monthYear, selectedMethod, refNo, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("تأكيد السداد وتوليد السند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
