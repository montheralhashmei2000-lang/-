package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fund.model.Account
import com.example.fund.model.JournalEntry
import com.example.fund.model.JournalLine
import com.example.fund.model.UserRole
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.components.SensitiveActionConfirmDialog
import com.example.fund.ui.viewmodel.AccountingViewModel
import com.example.fund.util.OfficialPrintHelper
import com.example.ui.theme.*
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalScreen(accountingViewModel: AccountingViewModel = viewModel()) {
    val context = LocalContext.current
    val journalEntries by accountingViewModel.journalEntries.collectAsStateWithLifecycle()
    val accounts by accountingViewModel.accounts.collectAsStateWithLifecycle()
    val fundInfo by accountingViewModel.fundInfo.collectAsStateWithLifecycle()
    val currentUser by accountingViewModel.currentUser.collectAsStateWithLifecycle()
    val canAdd = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.ACCOUNTANT

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var selectedAccountCode by remember { mutableStateOf("الكل") }
    var showManualDialog by remember { mutableStateOf(false) }
    var pendingManualPost by remember { mutableStateOf<Pair<List<JournalLine>, String>?>(null) }

    val totalAllDebits = journalEntries.flatMap { it.lines }.sumOf { it.debit }
    val totalAllCredits = journalEntries.flatMap { it.lines }.sumOf { it.credit }
    val isLedgerBalanced = abs(totalAllDebits - totalAllCredits) < 1.0

    val filteredEntries = remember(journalEntries, searchQuery, selectedFilter, selectedAccountCode) {
        journalEntries.filter { entry ->
            val matchesSearch = entry.entryNo.contains(searchQuery, ignoreCase = true) ||
                entry.description.contains(searchQuery, ignoreCase = true) ||
                (entry.originalVoucherNo?.contains(searchQuery, ignoreCase = true) ?: false) ||
                entry.lines.any { it.accountName.contains(searchQuery, ignoreCase = true) || it.accountCode.contains(searchQuery) }

            val matchesFilter = when (selectedFilter) {
                "قيود السندات" -> entry.originalVoucherNo != null && !entry.isReversed
                "قيود يدوية وتسويات" -> entry.originalVoucherNo == null && !entry.isReversed
                "قيود عكسية" -> entry.isReversed
                else -> true
            }

            val matchesAccount = selectedAccountCode == "الكل" ||
                entry.lines.any { it.accountCode == selectedAccountCode }

            matchesSearch && matchesFilter && matchesAccount
        }
    }

    val groupedByDate = remember(filteredEntries) {
        filteredEntries.groupBy { it.date }
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "دفتر القيود اليومية المزدوجة",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FundPrimary
                )
                Text(
                    "توثيق آلي ويدوي لكافة الحركات المالية بنظام القيد المزدوج",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (canAdd) {
                Button(
                    onClick = { showManualDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("قيد تسوية يدوي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Executive Double-Entry Balance Banner
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
                Column {
                    Text("إجمالي المدين (Debit)", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                    Text(
                        "%,d ﷼".format(totalAllDebits.toLong()),
                        color = FundGoldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
                Column {
                    Text("إجمالي الدائن (Credit)", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                    Text(
                        "%,d ﷼".format(totalAllCredits.toLong()),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
                Surface(
                    color = if (isLedgerBalanced) StatusSuccess.copy(alpha = 0.25f) else StatusDanger.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isLedgerBalanced) Color(0xFF81C784) else StatusDanger)
                ) {
                    Text(
                        text = if (isLedgerBalanced) "متوازن (${journalEntries.size} قيد) ✔" else "غير متوازن ❌",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }

        // Instant Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برقم القيد، البيان، الحساب، أو رقم السند...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Entry Type + Account Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("الكل", "قيود السندات", "قيود يدوية وتسويات", "قيود عكسية").forEach { filter ->
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
            VerticalDivider(modifier = Modifier.height(22.dp))
            listOf("الكل" to "كل الحسابات", "1100" to "الخزينة", "1200" to "الكريمي", "4100" to "الإيرادات", "5100" to "المساعدات").forEach { (code, title) ->
                FilterChip(
                    selected = selectedAccountCode == code,
                    onClick = { selectedAccountCode = code },
                    label = { Text(title, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundGold.copy(alpha = 0.22f),
                        selectedLabelColor = FundGoldDark
                    )
                )
            }
        }

        // Empty State or Sticky-Date Grouped Journal Entries
        if (filteredEntries.isEmpty()) {
            FundEmptyState(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "لا توجد قيود محاسبية مطابقة",
                subtitle = "جرب مسح البحث أو إزالة فلاتر النوع والحساب لعرض القيود اليومية.",
                secondaryActionLabel = "إلغاء الفلاتر",
                onSecondaryAction = {
                    searchQuery = ""
                    selectedFilter = "الكل"
                    selectedAccountCode = "الكل"
                },
                actionLabel = if (canAdd) "قيد تسوية يدوي" else null,
                onAction = if (canAdd) { { showManualDialog = true } } else null,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedByDate.forEach { (date, dayEntries) ->
                    stickyHeader(key = "journal_header_$date") {
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
                                        text = "📅 قيود تاريخ: $date",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FundPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )
                                }
                                Text(
                                    text = "${dayEntries.size} قيد",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                            }
                        }
                    }

                    items(dayEntries, key = { it.id }) { entry ->
                        JournalEntryCardItem(
                            entry = entry,
                            onPrint = {
                                OfficialPrintHelper.printJournalEntryPdf(context, entry, fundInfo)
                                Toast.makeText(context, "جاري فتح معاينة طباعة سند القيد ${entry.entryNo}...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualJournalEntryDialog(
            accounts = accounts,
            onDismiss = { showManualDialog = false },
            onSubmit = { lines, desc ->
                pendingManualPost = lines to desc
            }
        )
    }

    pendingManualPost?.let { (lines, desc) ->
        val total = lines.sumOf { it.debit }.toLong()
        SensitiveActionConfirmDialog(
            title = "تأكيد ترحيل قيد التسوية اليدوي",
            message = "سيتم ترحيل قيد محاسبي مزدوج بمبلغ ${"%,d".format(total)} ﷼ ($desc) وتحديث أرصدة الحسابات مباشرة.",
            confirmText = "اعتماد وترحيل القيد",
            confirmColor = FundPrimary,
            icon = Icons.Default.Verified,
            onConfirm = {
                val success = accountingViewModel.createManualJournalEntry(lines, desc)
                if (success) {
                    Toast.makeText(context, "تم حفظ وترحيل القيد المحاسبي بنجاح", Toast.LENGTH_SHORT).show()
                    pendingManualPost = null
                    showManualDialog = false
                } else {
                    Toast.makeText(context, "خطأ: يجب أن يكون القيد متوازناً (المدين = الدائن) وبقيمة موجبة!", Toast.LENGTH_LONG).show()
                    pendingManualPost = null
                }
            },
            onDismiss = { pendingManualPost = null }
        )
    }
}

@Composable
private fun JournalEntryCardItem(
    entry: JournalEntry,
    onPrint: () -> Unit
) {
    val totalDebit = entry.lines.sumOf { it.debit }
    val totalCredit = entry.lines.sumOf { it.credit }
    val isBalanced = abs(totalDebit - totalCredit) < 0.01

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (entry.isReversed) StatusDanger else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.entryNo, fontWeight = FontWeight.Black, fontSize = 13.sp, color = FundPrimary)
                    if (entry.originalVoucherNo != null) {
                        Surface(color = FundPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "سند: ${entry.originalVoucherNo}",
                                color = FundPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(color = FundGold.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "قيد تسوية يدوي",
                                color = FundGoldDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (entry.isReversed) {
                        Surface(color = StatusDanger.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "قيد عكسي",
                                color = StatusDanger,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.date, fontSize = 11.sp, color = Color.Gray)
                    IconButton(
                        onClick = onPrint,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "طباعة سند القيد",
                            tint = FundPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(entry.description, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            // Lines Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FundLightSurfaceSecondary, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الحساب المحاسبي", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                    Text("مدين (له)", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("دائن (منه)", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFD8E4D2))

                entry.lines.forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${line.accountName} (${line.accountCode})", fontSize = 10.sp, modifier = Modifier.weight(2.2f))
                        Text(
                            if (line.debit > 0) "%,d".format(line.debit.toLong()) else "-",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FundPrimary,
                            modifier = Modifier.weight(1f),
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                        Text(
                            if (line.credit > 0) "%,d".format(line.credit.toLong()) else "-",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FundGoldDark,
                            modifier = Modifier.weight(1f),
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBalanced) "متوازن (المدين = الدائن) ✔" else "غير متوازن ❌",
                    fontSize = 10.sp,
                    color = if (isBalanced) StatusSuccess else StatusDanger,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "إجمالي القيد: %,d ﷼".format(totalDebit.toLong()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = FundPrimary,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }
        }
    }
}

@Composable
private fun ManualJournalEntryDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSubmit: (List<JournalLine>, String) -> Unit
) {
    val leafAccounts = remember(accounts) { accounts.filter { it.parentCode != null } }
    var desc by remember { mutableStateOf("قيد تسوية محاسبية ومطابقة شهرية") }
    var debitCode by remember { mutableStateOf(leafAccounts.firstOrNull { it.code == "1100" }?.code ?: "1100") }
    var creditCode by remember { mutableStateOf(leafAccounts.firstOrNull { it.code == "4100" }?.code ?: "4100") }
    var debitAmountStr by remember { mutableStateOf("50000") }
    var creditAmountStr by remember { mutableStateOf("50000") }

    val debitAmt = debitAmountStr.toDoubleOrNull() ?: 0.0
    val creditAmt = creditAmountStr.toDoubleOrNull() ?: 0.0
    val diff = abs(debitAmt - creditAmt)
    val isBalanced = debitAmt > 0 && diff < 0.01 && debitCode != creditCode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "إنشاء قيد يومية مزدوج (يدوي / تسوية)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = FundPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("البيان المحاسبي العام للقيد") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("الطرف المدين (Debit):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    leafAccounts.forEach { acc ->
                        FilterChip(
                            selected = debitCode == acc.code,
                            onClick = { debitCode = acc.code },
                            label = { Text("${acc.name} (${acc.code})", fontSize = 10.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = debitAmountStr,
                    onValueChange = {
                        debitAmountStr = it
                        creditAmountStr = it
                    },
                    label = { Text("مبلغ المدين (ريال يمني)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("الطرف الدائن (Credit):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FundGoldDark)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    leafAccounts.forEach { acc ->
                        FilterChip(
                            selected = creditCode == acc.code,
                            onClick = { creditCode = acc.code },
                            label = { Text("${acc.name} (${acc.code})", fontSize = 10.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = creditAmountStr,
                    onValueChange = { creditAmountStr = it },
                    label = { Text("مبلغ الدائن المطابق (ريال يمني)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Surface(
                    color = if (isBalanced) StatusSuccess.copy(alpha = 0.12f) else StatusDanger.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            debitCode == creditCode -> "تنبيه: يجب اختيار حسابين مختلفين للطرفين المدين والدائن"
                            isBalanced -> "القيد متوازن تماماً (%,d ﷼) وجاهز للترحيل ✔".format(debitAmt.toLong())
                            else -> "الفرق بين المدين والدائن: %,d ﷼ (يجب أن يكون صفراً)".format(diff.toLong())
                        },
                        color = if (isBalanced) StatusSuccess else StatusDanger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isBalanced && desc.isNotBlank()) {
                        val dAcc = accounts.find { it.code == debitCode }?.name ?: "الحساب المدين"
                        val cAcc = accounts.find { it.code == creditCode }?.name ?: "الحساب الدائن"
                        val lines = listOf(
                            JournalLine(debitCode, dAcc, debitAmt, 0.0, "مدين - $desc"),
                            JournalLine(creditCode, cAcc, 0.0, creditAmt, "دائن - $desc")
                        )
                        onSubmit(lines, desc)
                    }
                },
                enabled = isBalanced && desc.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("اعتماد وترحيل القيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
