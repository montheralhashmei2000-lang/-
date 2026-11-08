package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import com.example.fund.ui.components.MoneyInput
import com.example.fund.ui.components.VoucherPrintDialog
import com.example.ui.theme.*

@Composable
fun VouchersScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val vouchers by repo.vouchers.collectAsState()
    val accounts by repo.accounts.collectAsState()
    val currentUser by repo.currentUser.collectAsState()
    val canIssue = currentUser.role != UserRole.VIEWER

    var selectedFilter by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var showNewVoucherDialog by remember { mutableStateOf(false) }
    var voucherToPrint by remember { mutableStateOf<Voucher?>(null) }

    val filteredVouchers = vouchers.filter { v ->
        val matchesSearch = v.voucherNo.contains(searchQuery, ignoreCase = true) ||
                v.partyName.contains(searchQuery, ignoreCase = true) ||
                v.description.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "قبض" -> v.type == VoucherType.RECEIPT && v.status == VoucherStatus.POSTED
            "صرف" -> v.type == VoucherType.PAYMENT && v.status == VoucherStatus.POSTED
            "ملغي" -> v.status == VoucherStatus.VOID
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
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "سندات القبض والصرف الرسمية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = FundPrimary
                )
                Text(
                    text = "إصدار القيود والطباعة الرسمية برمز الـ QR",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (canIssue) {
                Button(
                    onClick = { showNewVoucherDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إصدار سند", fontSize = 12.sp)
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برقم السند، اسم المكرم، أو البيان...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("الكل", "قبض", "صرف", "ملغي").forEach { filter ->
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

        // Vouchers List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredVouchers, key = { it.id }) { voucher ->
                VoucherListItem(
                    voucher = voucher,
                    canIssue = canIssue,
                    onPrint = { voucherToPrint = voucher },
                    onVoid = {
                        repo.voidVoucher(voucher.id)
                        Toast.makeText(context, "تم إلغاء السند وتوليد قيد عكسي آلي بنجاح", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Print & Preview Official Dialog
    voucherToPrint?.let { voucher ->
        VoucherPrintDialog(
            voucher = voucher,
            onDismiss = { voucherToPrint = null }
        )
    }

    // New Voucher Dialog
    if (showNewVoucherDialog) {
        NewVoucherDialog(
            accounts = accounts,
            onDismiss = { showNewVoucherDialog = false },
            onSubmit = { type, party, amount, curr, method, accCode, counterpartCode, desc ->
                val v = repo.issueVoucher(
                    type = type,
                    partyName = party,
                    amountOriginal = amount,
                    currency = curr,
                    paymentMethod = method,
                    accountCode = accCode,
                    counterpartAccountCode = counterpartCode,
                    description = desc
                )
                Toast.makeText(context, "تم إصدار السند ${v.voucherNo} وترحيل القيد المحاسبي", Toast.LENGTH_SHORT).show()
                showNewVoucherDialog = false
                voucherToPrint = v // directly open preview for user
            }
        )
    }
}

@Composable
private fun VoucherListItem(
    voucher: Voucher,
    canIssue: Boolean,
    onPrint: () -> Unit,
    onVoid: () -> Unit
) {
    val isReceipt = voucher.type == VoucherType.RECEIPT
    val isVoid = voucher.status == VoucherStatus.VOID
    val color = if (isVoid) Color.Gray else if (isReceipt) StatusSuccess else StatusDanger

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isVoid) Color.LightGray else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isVoid) "ملغي - VOID" else voucher.type.title,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = voucher.voucherNo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "%,d ﷼".format(voucher.amountYer.toLong()),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = color
                )
            }

            Text(
                text = "${if (isReceipt) "استلمنا من:" else "يصرف للمكرم:"} ${voucher.partyName}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Text(
                text = "البيان: ${voucher.description}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الحساب: ${voucher.accountName}", fontSize = 11.sp, color = Color.Gray)
                Text("القيد: ${voucher.linkedJournalEntryId}", fontSize = 11.sp, color = FundPrimary)
                Text(voucher.date, fontSize = 11.sp, color = Color.Gray)
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Actions: Print / Void
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canIssue && !isVoid) {
                    TextButton(onClick = onVoid) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إلغاء السند (Void)", color = StatusDanger, fontSize = 11.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = onPrint,
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("معاينة / طباعة PDF", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun NewVoucherDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSubmit: (VoucherType, String, Double, CurrencyCode, PaymentMethod, String, String, String) -> Unit
) {
    var type by remember { mutableStateOf(VoucherType.RECEIPT) }
    var partyName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("50000") }
    var currency by remember { mutableStateOf(CurrencyCode.YER) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var description by remember { mutableStateOf("") }

    val treasuryAccounts = accounts.filter { it.code in listOf("1100", "1200", "1300") }
    val counterpartAccounts = accounts.filter { it.code.startsWith("4") || it.code.startsWith("5") || it.code.startsWith("2") }

    var selectedTreasuryCode by remember { mutableStateOf("1100") }
    var selectedCounterpartCode by remember { mutableStateOf(if (type == VoucherType.RECEIPT) "4100" else "5400") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إصدار سند رسمي جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            type = VoucherType.RECEIPT
                            selectedCounterpartCode = "4100"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == VoucherType.RECEIPT) StatusSuccess else Color.LightGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("سند قبض (+)")
                    }
                    Button(
                        onClick = {
                            type = VoucherType.PAYMENT
                            selectedCounterpartCode = "5100"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == VoucherType.PAYMENT) StatusDanger else Color.LightGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("سند صرف (-)")
                    }
                }

                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text(if (type == VoucherType.RECEIPT) "استلمنا من المكرم:" else "يصرف للمكرم:") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                MoneyInput(
                    amount = amountStr,
                    onAmountChange = { amountStr = it },
                    selectedCurrency = currency,
                    onCurrencyChange = { currency = it },
                    label = "المبلغ"
                )

                // Payment Method
                Text("طريقة الدفع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaymentMethod.values().forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = {
                                method = m
                                selectedTreasuryCode = when (m) {
                                    PaymentMethod.CASH -> "1100"
                                    PaymentMethod.BANK_TRANSFER -> "1200"
                                    PaymentMethod.ELECTRONIC_WALLET -> "1300"
                                }
                            },
                            label = { Text(m.title, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وذلك مقابل (البيان)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (partyName.isNotBlank() && amount > 0) {
                        onSubmit(type, partyName, amount, currency, method, selectedTreasuryCode, selectedCounterpartCode, description)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("إصدار السند وترحيل القيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
