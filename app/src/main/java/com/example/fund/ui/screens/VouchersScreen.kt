package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fund.model.*
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.components.MoneyInput
import com.example.fund.ui.components.SensitiveActionConfirmDialog
import com.example.fund.ui.components.VoucherPrintDialog
import com.example.fund.ui.viewmodel.AccountingViewModel
import com.example.fund.util.FormatUtils
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VouchersScreen(
    initialFilter: String = "الكل",
    initialNewVoucherType: String = "",
    accountingViewModel: AccountingViewModel = viewModel()
) {
    val context = LocalContext.current
    val vouchers by accountingViewModel.vouchers.collectAsStateWithLifecycle()
    val accounts by accountingViewModel.accounts.collectAsStateWithLifecycle()
    val currentUser by accountingViewModel.currentUser.collectAsStateWithLifecycle()
    val screenFormat by accountingViewModel.screenFormat.collectAsStateWithLifecycle()
    val canIssue = currentUser.role != UserRole.VIEWER

    var selectedFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var selectedAccountFilter by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var showNewVoucherDialog by remember(initialNewVoucherType) {
        mutableStateOf(initialNewVoucherType.isNotBlank() && canIssue)
    }
    var voucherToPrint by remember { mutableStateOf<Voucher?>(null) }
    var pendingVoidVoucher by remember { mutableStateOf<Voucher?>(null) }

    val filteredVouchers = remember(vouchers, searchQuery, selectedFilter, selectedAccountFilter) {
        vouchers.filter { v ->
            val matchesSearch = v.voucherNo.contains(searchQuery, ignoreCase = true) ||
                v.partyName.contains(searchQuery, ignoreCase = true) ||
                v.description.contains(searchQuery, ignoreCase = true) ||
                v.accountName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "قبض" -> v.type == VoucherType.RECEIPT && v.status == VoucherStatus.POSTED
                "صرف" -> v.type == VoucherType.PAYMENT && v.status == VoucherStatus.POSTED
                "ملغي" -> v.status == VoucherStatus.VOID
                else -> true
            }
            val matchesAccount = when (selectedAccountFilter) {
                "كل الحسابات", "الكل" -> true
                else -> v.accountCode == selectedAccountFilter || v.accountName.contains(selectedAccountFilter)
            }
            matchesSearch && matchesFilter && matchesAccount
        }
    }

    val groupedByDate = remember(filteredVouchers) {
        filteredVouchers.groupBy { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "سندات القبض والصرف الرسمية (${FormatUtils.formatCodeOrNumber(filteredVouchers.size.toString(), screenFormat)})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = FundPrimary
                )
                Text(
                    text = "إصدار القيود والطباعة الرسمية ومشاركة PDF",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (canIssue) {
                Button(
                    onClick = { showNewVoucherDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إصدار سند", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        // Instant Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برقم السند، اسم المكرم، الحساب أو البيان...") },
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

        // Type + Account Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
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
            VerticalDivider(modifier = Modifier.height(22.dp))
            listOf("الكل" to "كل الحسابات", "1100" to "الخزينة", "1200" to "الكريمي", "1300" to "المحافظ").forEach { (code, label) ->
                val isSelected = selectedAccountFilter == code
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedAccountFilter = code },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundGold.copy(alpha = 0.22f),
                        selectedLabelColor = FundGoldDark
                    )
                )
            }
        }

        // Empty State or Sticky-Date Grouped Vouchers List
        if (filteredVouchers.isEmpty()) {
            FundEmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "لا توجد سندات مطابقة للبحث",
                subtitle = "جرب تغيير كلمات البحث أو إعادة ضبط فلاتر النوع والحساب لعرض السندات.",
                secondaryActionLabel = "إلغاء الفلاتر",
                onSecondaryAction = {
                    searchQuery = ""
                    selectedFilter = "الكل"
                    selectedAccountFilter = "الكل"
                },
                actionLabel = if (canIssue) "إصدار سند جديد" else null,
                onAction = if (canIssue) { { showNewVoucherDialog = true } } else null,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedByDate.forEach { (date, dayVouchers) ->
                    stickyHeader(key = "header_$date") {
                        Surface(
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = FundPrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "📅 تاريخ: ${FormatUtils.formatDate(date, screenFormat)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FundPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )
                                }
                                val dayTotal = dayVouchers.filter { it.status == VoucherStatus.POSTED }.sumOf { it.amountYer }
                                Text(
                                    text = "${FormatUtils.toArabicDigitsIfNeeded(dayVouchers.size.toString(), screenFormat.digitLanguage)} سند • إجمالي ${FormatUtils.formatAmount(dayTotal, screenFormat)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                            }
                        }
                    }

                    items(dayVouchers, key = { it.id }) { voucher ->
                        VoucherListItem(
                            voucher = voucher,
                            screenFormat = screenFormat,
                            canIssue = canIssue,
                            onPrint = { voucherToPrint = voucher },
                            onVoidRequest = { pendingVoidVoucher = voucher }
                        )
                    }
                }
            }
        }
    }

    // Confirmation before Voiding Voucher
    pendingVoidVoucher?.let { voucher ->
        SensitiveActionConfirmDialog(
            title = "تأكيد إلغاء السند المالي",
            message = "هل أنت متأكد من إلغاء السند رقم (${FormatUtils.formatCodeOrNumber(voucher.voucherNo, screenFormat)}) بمبلغ ${FormatUtils.formatAmount(voucher.amountYer, screenFormat)}؟ سيتم توليد قيد عكسي آلي وتعديل الأرصدة.",
            confirmText = "تأكيد الإلغاء والقيد العكسي",
            confirmColor = StatusDanger,
            icon = Icons.Default.Cancel,
            onConfirm = {
                accountingViewModel.voidVoucher(voucher.id)
                Toast.makeText(context, "تم إلغاء السند وتوليد قيد عكسي آلي بنجاح", Toast.LENGTH_SHORT).show()
                pendingVoidVoucher = null
            },
            onDismiss = { pendingVoidVoucher = null }
        )
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
            defaultType = if (initialNewVoucherType == "PAYMENT") VoucherType.PAYMENT else VoucherType.RECEIPT,
            onDismiss = { showNewVoucherDialog = false },
            onSubmit = { type, party, amount, curr, method, accCode, counterpartCode, desc ->
                if (amount <= 0.0) {
                    Toast.makeText(context, "يرجى إدخال مبلغ صحيح أكبر من صفر", Toast.LENGTH_SHORT).show()
                } else {
                    val v = accountingViewModel.issueVoucher(
                        type = type,
                        partyName = party,
                        amountOriginal = amount,
                        currency = curr,
                        paymentMethod = method,
                        accountCode = accCode,
                        counterpartAccountCode = counterpartCode,
                        description = desc
                    )
                    Toast.makeText(context, "تم إصدار السند ${FormatUtils.formatCodeOrNumber(v.voucherNo, screenFormat)} وترحيل القيد المحاسبي", Toast.LENGTH_SHORT).show()
                    showNewVoucherDialog = false
                    voucherToPrint = v
                }
            }
        )
    }
}

@Composable
private fun VoucherListItem(
    voucher: Voucher,
    screenFormat: DisplayFormatConfig,
    canIssue: Boolean,
    onPrint: () -> Unit,
    onVoidRequest: () -> Unit
) {
    val isReceipt = voucher.type == VoucherType.RECEIPT
    val isVoid = voucher.status == VoucherStatus.VOID
    val color = if (isVoid) Color.Gray else if (isReceipt) StatusSuccess else StatusDanger

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isVoid) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Text(
                        text = FormatUtils.formatCodeOrNumber(voucher.voucherNo, screenFormat),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }

                Text(
                    text = FormatUtils.formatAmount(voucher.amountYer, screenFormat),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "الحساب: ${voucher.accountName}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "القيد: ${FormatUtils.formatCodeOrNumber(voucher.linkedJournalEntryId, screenFormat)}",
                    fontSize = 11.sp,
                    color = FundPrimary,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
                Text(
                    FormatUtils.formatDate(voucher.date, screenFormat),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canIssue && !isVoid) {
                    TextButton(
                        onClick = onVoidRequest,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("معاينة / طباعة ومشاركة PDF", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun NewVoucherDialog(
    accounts: List<Account>,
    defaultType: VoucherType = VoucherType.RECEIPT,
    onDismiss: () -> Unit,
    onSubmit: (VoucherType, String, Double, CurrencyCode, PaymentMethod, String, String, String) -> Unit
) {
    var type by remember(defaultType) { mutableStateOf(defaultType) }
    var partyName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("50000") }
    var currency by remember { mutableStateOf(CurrencyCode.YER) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var description by remember { mutableStateOf("") }

    var selectedTreasuryCode by remember { mutableStateOf("1100") }
    var selectedCounterpartCode by remember(defaultType) {
        mutableStateOf(if (defaultType == VoucherType.RECEIPT) "4100" else "5100")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إصدار سند رسمي جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            type = VoucherType.RECEIPT
                            selectedCounterpartCode = "4100"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == VoucherType.RECEIPT) StatusSuccess else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == VoucherType.RECEIPT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("سند قبض (+)")
                    }
                    Button(
                        onClick = {
                            type = VoucherType.PAYMENT
                            selectedCounterpartCode = "5100"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == VoucherType.PAYMENT) StatusDanger else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == VoucherType.PAYMENT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
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

                Text("طريقة الدفع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.entries.forEach { m ->
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
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("إصدار السند وترحيل القيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("إلغاء") }
        }
    )
}
