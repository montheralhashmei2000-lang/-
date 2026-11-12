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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.data.FundRepository
import com.example.fund.model.CurrencyCode
import com.example.fund.model.Member
import com.example.fund.model.PaymentMethod
import com.example.fund.model.UserRole
import com.example.fund.ui.components.MoneyInput
import com.example.ui.theme.*

@Composable
fun SubscriptionsScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val subscriptions by repo.subscriptions.collectAsState()
    val members by repo.members.collectAsState()
    val currentUser by repo.currentUser.collectAsState()
    val canCollect = currentUser.role != UserRole.VIEWER

    var showCollectDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
                    Text("%,d ﷼".format(totalCollected.toLong()), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("عدد العمليات: ${subscriptions.size} اشتراك", color = FundGoldLight, fontSize = 11.sp)
                }

                if (canCollect) {
                    Button(
                        onClick = { showCollectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FundGold),
                        shape = RoundedCornerShape(10.dp)
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

        // Subscriptions List
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
                                fontSize = 14.sp
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
                                color = Color.Gray
                            )
                        }

                        if (sub.notes.isNotEmpty()) {
                            Text("ملاحظة: ${sub.notes}", fontSize = 11.sp, color = Color.DarkGray)
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
    onDismiss: () -> Unit,
    onCollect: (String, Double, CurrencyCode, String, PaymentMethod, String, String) -> Unit
) {
    var selectedMember by remember { mutableStateOf(members.firstOrNull()) }
    var memberDropdownExpanded by remember { mutableStateOf(false) }

    val defaultFee = FundRepository.getInstance().appSettings.collectAsState().value.monthlySubscriptionFeeYer.toLong().toString()
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
                    if (m != null && amount > 0) {
                        onCollect(m.id, amount, selectedCurrency, monthYear, selectedMethod, refNo, notes)
                    }
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
