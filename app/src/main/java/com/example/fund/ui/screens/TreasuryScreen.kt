package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.Voucher
import com.example.fund.model.VoucherStatus
import com.example.fund.model.VoucherType
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.components.SensitiveActionConfirmDialog
import com.example.fund.ui.components.VoucherPrintDialog
import com.example.fund.ui.navigation.Screen
import com.example.ui.theme.*

@Composable
fun TreasuryScreen(
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val accounts by repo.accounts.collectAsStateWithLifecycle()
    val vouchers by repo.vouchers.collectAsStateWithLifecycle()

    var showTransferDialog by remember { mutableStateOf(false) }
    var pendingTransfer by remember { mutableStateOf<Triple<String, String, Pair<Double, String>>?>(null) }
    var selectedVoucherForPrint by remember { mutableStateOf<Voucher?>(null) }

    val cashAcc = accounts.find { it.code == "1100" }
    val bankAcc = accounts.find { it.code == "1200" }
    val walletAcc = accounts.find { it.code == "1300" }

    val totalCash = cashAcc?.balance ?: 0.0
    val totalBank = bankAcc?.balance ?: 0.0
    val totalWallet = walletAcc?.balance ?: 0.0
    val totalLiquidity = totalCash + totalBank + totalWallet

    val validVouchers = vouchers.filter { it.status == VoucherStatus.POSTED }
    val totalReceipts = validVouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amountYer }
    val totalPayments = validVouchers.filter { it.type == VoucherType.PAYMENT }.sumOf { it.amountYer }

    var selectedAccountFilter by remember { mutableStateOf("الكل") }

    val filteredVouchers = validVouchers.filter { v ->
        when (selectedAccountFilter) {
            "الصندوق النقدي" -> v.accountCode == "1100" || v.counterpartAccountCode == "1100"
            "بنك الكريمي" -> v.accountCode == "1200" || v.counterpartAccountCode == "1200"
            "المحافظ الإلكترونية" -> v.accountCode == "1300" || v.counterpartAccountCode == "1300"
            else -> true
        }
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
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إجمالي السيولة النقدية والمصرفية", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text(
                            text = "%,d ﷼".format(totalLiquidity.toLong()),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { showTransferDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FundGoldLight),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تحويل داخلي", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onNavigate(Screen.VOUCHERS) },
                            colors = ButtonDefaults.buttonColors(containerColor = FundGold),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("إصدار سند", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("إجمالي المقبوضات", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                        Text("%,d ﷼".format(totalReceipts.toLong()), color = FundGoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                    }
                    Column {
                        Text("إجمالي المدفوعات", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                        Text("%,d ﷼".format(totalPayments.toLong()), color = Color(0xFFFFCDD2), fontWeight = FontWeight.Bold, fontSize = 12.sp, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                    }
                    Column {
                        Text("صافي التدفق النقدي", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                        Text("%,d ﷼".format((totalReceipts - totalPayments).toLong()), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                    }
                }
            }
        }

        // Account Balances 3 Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountBalancePill("الخزينة النقدية (1100)", totalCash, Icons.Default.Savings, Modifier.weight(1f))
            AccountBalancePill("بنك الكريمي (1200)", totalBank, Icons.Default.AccountBalance, Modifier.weight(1f))
            AccountBalancePill("المحافظ (1300)", totalWallet, Icons.Default.PhoneIphone, Modifier.weight(1f))
        }

        // Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("الكل", "الصندوق النقدي", "بنك الكريمي", "المحافظ الإلكترونية").forEach { filter ->
                val isSelected = selectedAccountFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedAccountFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = FundPrimary
                    )
                )
            }
        }

        Text(
            "سجل حركات الخزينة والحسابات (${filteredVouchers.size} حركة - اضغط على أي حركة لمعاينة وطباعة السند)",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Transactions List
        if (filteredVouchers.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                FundEmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "لا توجد حركات مالية مطابقة",
                    subtitle = "لم يتم تسجيل سندات على الحساب المحدد بعد.",
                    actionLabel = if (selectedAccountFilter != "الكل") "عرض كل الحسابات" else null,
                    onAction = if (selectedAccountFilter != "الكل") ({ selectedAccountFilter = "الكل" }) else null
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredVouchers, key = { it.id }) { v ->
                    val isReceipt = v.type == VoucherType.RECEIPT
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVoucherForPrint = v }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (isReceipt) StatusSuccess.copy(alpha = 0.12f) else StatusDanger.copy(alpha = 0.12f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isReceipt) StatusSuccess else StatusDanger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(v.partyName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${v.voucherNo} | ${v.date} | ${v.paymentMethod.title}", fontSize = 11.sp, color = Color.Gray)
                                    Text(v.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isReceipt) "+" else "-"} %,d ﷼".format(v.amountYer.toLong()),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReceipt) StatusSuccess else StatusDanger,
                                    fontSize = 13.sp,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                                Text("معاينة السند 🖨", fontSize = 10.sp, color = FundPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTransferDialog) {
        TreasuryInternalTransferDialog(
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromCode, toCode, amount, notes ->
                showTransferDialog = false
                pendingTransfer = Triple(fromCode, toCode, amount to notes)
            }
        )
    }

    pendingTransfer?.let { (fromCode, toCode, amountNotes) ->
        val (amount, notes) = amountNotes
        SensitiveActionConfirmDialog(
            title = "تأكيد التحويل الداخلي وترحيل القيد",
            message = "سيتم تحويل مبلغ %,d ﷼ من حساب ($fromCode) إلى حساب ($toCode) وترحيل القيد المزدوج في الدفتر المحاسبي.".format(amount.toLong()),
            confirmText = "تأكيد وترحيل التحويل",
            confirmColor = FundPrimary,
            icon = Icons.Default.SwapHoriz,
            onConfirm = {
                val ok = repo.transferBetweenTreasuryAccounts(fromCode, toCode, amount, notes)
                if (ok) {
                    Toast.makeText(context, "تم تنفيذ التحويل الداخلي وترحيل القيد المزدوج بنجاح ✔", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تأكد من اختلاف الحسابين وكفاية الرصيد المتاح!", Toast.LENGTH_LONG).show()
                }
                pendingTransfer = null
            },
            onDismiss = { pendingTransfer = null }
        )
    }

    selectedVoucherForPrint?.let { v ->
        VoucherPrintDialog(
            voucher = v,
            onDismiss = { selectedVoucherForPrint = null }
        )
    }
}

@Composable
private fun TreasuryInternalTransferDialog(
    onDismiss: () -> Unit,
    onTransfer: (String, String, Double, String) -> Unit
) {
    val treasuryAccounts = listOf(
        "1100" to "الخزينة النقدية (1100)",
        "1200" to "بنك الكريمي الإسلامي (1200)",
        "1300" to "المحافظ الإلكترونية (1300)"
    )

    var fromCode by remember { mutableStateOf("1100") }
    var toCode by remember { mutableStateOf("1200") }
    var amountStr by remember { mutableStateOf("250000") }
    var notes by remember { mutableStateOf("توريد نقدية من الخزينة إلى حساب الصندوق ببنك الكريمي") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحويل سيولة داخلي بين الخزينة والبنك", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("من حساب (السحب):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    treasuryAccounts.forEach { (code, title) ->
                        FilterChip(
                            selected = fromCode == code,
                            onClick = { fromCode = code },
                            label = { Text(title.substringBefore(" ("), fontSize = 10.sp) }
                        )
                    }
                }

                Text("إلى حساب (الإيداع):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    treasuryAccounts.forEach { (code, title) ->
                        FilterChip(
                            selected = toCode == code,
                            onClick = { toCode = code },
                            label = { Text(title.substringBefore(" ("), fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
                    label = { Text("المبلغ المحول بالريال اليمني (﷼)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("البيان / سبب التحويل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onTransfer(fromCode, toCode, amt, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("اعتماد التحويل وترحيل القيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun AccountBalancePill(label: String, balance: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text("%,d ﷼".format(balance.toLong()), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FundPrimary, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
        }
    }
}
