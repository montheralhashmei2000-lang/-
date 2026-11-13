package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.viewmodel.AccountingViewModel
import com.example.fund.util.FormatUtils
import com.example.fund.util.OfficialPrintHelper
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun FinancialReportsScreen(accountingViewModel: AccountingViewModel = viewModel()) {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val accounts by accountingViewModel.accounts.collectAsStateWithLifecycle()
    val journalEntries by accountingViewModel.journalEntries.collectAsStateWithLifecycle()
    val currencyRates by repo.currencyRates.collectAsStateWithLifecycle()
    val fundInfo by accountingViewModel.fundInfo.collectAsStateWithLifecycle()
    val screenFormat by accountingViewModel.screenFormat.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "ميزان المراجعة",
        "قائمة الدخل",
        "المركز المالي",
        "كشف حركة حساب",
        "تسوية ومطابقة الخزينة",
        "أسعار الصرف"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "القوائم المالية الختامية والعملات",
                    fontFamily = AmiriFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FundPrimary
                )
                Text(
                    "تقارير ختامية مطابقة للمعايير المحاسبية",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    OfficialPrintHelper.printFinancialStatementPdf(
                        context = context,
                        statementTitle = "القائمة المالية المعتمدة: ${tabTitles[activeTab]}",
                        accounts = accounts,
                        fundInfo = fundInfo
                    )
                    Toast.makeText(context, "جاري فتح نافذة الطباعة الرسمية وحفظ PDF...", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("طباعة القائمة PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }

        // Scrollable Tabs
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = FundPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        when (activeTab) {
            0 -> TrialBalanceTab(accounts, journalEntries, screenFormat)
            1 -> IncomeStatementTab(accounts, screenFormat)
            2 -> BalanceSheetTab(accounts, screenFormat)
            3 -> AccountLedgerTab(accounts, journalEntries, screenFormat)
            4 -> ReconciliationTab(accounts, screenFormat, onAdjustEntry = { code, name, diff ->
                val lines = if (diff > 0) {
                    listOf(
                        JournalLine(code, name, diff, 0.0, "مدين - إثبات فائض تسوية جرد الخزينة"),
                        JournalLine("4200", "التبرعات والمنح العامة", 0.0, diff, "دائن - تسوية فروق الجرد")
                    )
                } else {
                    val absDiff = Math.abs(diff)
                    listOf(
                        JournalLine("5400", "المصروفات التشغيلية والإدارية", absDiff, 0.0, "مدين - إثبات عجز تسوية الخزينة"),
                        JournalLine(code, name, 0.0, absDiff, "دائن - تسوية عجز الجرد")
                    )
                }
                repo.createManualJournalEntry(lines, "قيد تسوية مطابق لحساب $name بفارق ${FormatUtils.formatAmount(diff, screenFormat)}")
                Toast.makeText(context, "تم توليد قيد التسوية وترحيله بنجاح", Toast.LENGTH_SHORT).show()
            })
            5 -> CurrenciesTab(currencyRates, screenFormat, onUpdateRate = { curr, newRate ->
                repo.updateCurrencyRate(curr, newRate)
                Toast.makeText(context, "تم تحديث سعر صرف ${curr.title} إلى ${FormatUtils.formatNumber(newRate, screenFormat, decimalsOverride = 0)}", Toast.LENGTH_SHORT).show()
            })
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 1: Trial Balance
// ----------------------------------------------------------------------------
@Composable
private fun TrialBalanceTab(accounts: List<Account>, entries: List<JournalEntry>, screenFormat: DisplayFormatConfig) {
    var accountQuery by remember { mutableStateOf("") }
    var totalDebitBalance = 0.0
    var totalCreditBalance = 0.0

    val allRows = accounts.map { acc ->
        val lines = entries.flatMap { it.lines }.filter { it.accountCode == acc.code }
        val debits = lines.sumOf { it.debit }
        val credits = lines.sumOf { it.credit }
        val net = debits - credits

        val debitBal = if (net > 0) net else 0.0
        val creditBal = if (net < 0) -net else 0.0

        totalDebitBalance += debitBal
        totalCreditBalance += creditBal

        Triple(acc, debitBal, creditBal)
    }

    val rows = remember(allRows, accountQuery) {
        if (accountQuery.isBlank()) allRows
        else allRows.filter { (acc, _, _) ->
            acc.name.contains(accountQuery, ignoreCase = true) || acc.code.contains(accountQuery)
        }
    }

    val isBalanced = Math.abs(totalDebitBalance - totalCreditBalance) < 1.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = accountQuery,
            onValueChange = { accountQuery = it },
            placeholder = { Text("تصفية حسابات ميزان المراجعة بالاسم أو الكود...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (accountQuery.isNotEmpty()) {
                    IconButton(onClick = { accountQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        // Balance Badge
        Surface(
            color = if (isBalanced) StatusSuccess.copy(alpha = 0.12f) else StatusDanger.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isBalanced) StatusSuccess else StatusDanger),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isBalanced) StatusSuccess else StatusDanger
                    )
                    Text(
                        text = if (isBalanced) "ميزان المراجعة متوازن تماماً ✔" else "تنبيه: ميزان المراجعة غير متوازن ❌",
                        fontWeight = FontWeight.Bold,
                        color = if (isBalanced) StatusSuccess else StatusDanger,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "الإجمالي: ${FormatUtils.formatAmount(totalDebitBalance, screenFormat)}",
                    fontWeight = FontWeight.Black,
                    color = FundPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }
        }

        // Table
        Card(
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الحساب المحاسبي", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                    Text("رصيد مدين", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("رصيد دائن", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                if (rows.isEmpty()) {
                    FundEmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "لا توجد حسابات مطابقة",
                        subtitle = "جرّب مسح عبارة البحث لعرض جميع حسابات ميزان المراجعة.",
                        actionLabel = "مسح البحث",
                        onAction = { accountQuery = "" }
                    )
                } else {
                    rows.forEachIndexed { index, (acc, deb, cred) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(acc.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(FormatUtils.formatCodeOrNumber(acc.code, screenFormat), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                            }
                            Text(
                                text = if (deb > 0) FormatUtils.formatAmount(deb, screenFormat, inTable = true) else "-",
                                fontSize = 11.sp,
                                fontWeight = if (deb > 0) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                            Text(
                                text = if (cred > 0) FormatUtils.formatAmount(cred, screenFormat, inTable = true) else "-",
                                fontSize = 11.sp,
                                fontWeight = if (cred > 0) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }

                HorizontalDivider(color = FundPrimary, thickness = 2.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FundPrimary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الإجماليات الكلية:", fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.weight(2f))
                    Text(
                        FormatUtils.formatAmount(totalDebitBalance, screenFormat),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = StatusSuccess,
                        modifier = Modifier.weight(1f),
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                    Text(
                        FormatUtils.formatAmount(totalCreditBalance, screenFormat),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = StatusSuccess,
                        modifier = Modifier.weight(1f),
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 2: Income Statement
// ----------------------------------------------------------------------------
@Composable
private fun IncomeStatementTab(accounts: List<Account>, screenFormat: DisplayFormatConfig) {
    val revenues = accounts.filter { it.code.startsWith("4") && it.parentCode != null }
        .ifEmpty { accounts.filter { it.code.startsWith("4") } }
    val expenses = accounts.filter { it.code.startsWith("5") && it.parentCode != null }
        .ifEmpty { accounts.filter { it.code.startsWith("5") } }

    val totalRevenues = revenues.sumOf { it.balance }
    val totalExpenses = expenses.sumOf { it.balance }
    val netSurplus = totalRevenues - totalExpenses

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Net Result Card
        Card(
            colors = CardDefaults.cardColors(containerColor = if (netSurplus >= 0) StatusSuccess else StatusDanger),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (netSurplus >= 0) "صافي الفائض التشغيلي التنموي" else "صافي العجز للفترة",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
                Text(
                    text = FormatUtils.formatAmount(netSurplus, screenFormat),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }
        }

        // Revenues Section
        StatementGroup("أولاً: الإيرادات والتبرعات (4000)", revenues, totalRevenues, FundPrimaryMedium, screenFormat)

        // Expenses Section
        StatementGroup("ثانياً: المصروفات والمساعدات (5000)", expenses, totalExpenses, Color(0xFFC62828), screenFormat)
    }
}

@Composable
private fun StatementGroup(
    title: String,
    accounts: List<Account>,
    total: Double,
    color: Color,
    screenFormat: DisplayFormatConfig
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color, modifier = Modifier.weight(1f))
                Text(
                    FormatUtils.formatAmount(total, screenFormat),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            accounts.forEach { acc ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${acc.name} (${FormatUtils.formatCodeOrNumber(acc.code, screenFormat)})",
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        FormatUtils.formatAmount(acc.balance, screenFormat),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 3: Balance Sheet
// ----------------------------------------------------------------------------
@Composable
private fun BalanceSheetTab(accounts: List<Account>, screenFormat: DisplayFormatConfig) {
    val assets = accounts.filter { it.code.startsWith("1") && it.parentCode != null }
        .ifEmpty { accounts.filter { it.code.startsWith("1") } }
    val liabilities = accounts.filter { it.code.startsWith("2") && it.parentCode != null }
        .ifEmpty { accounts.filter { it.code.startsWith("2") } }
    val equity = accounts.filter { it.code.startsWith("3") && it.parentCode != null }
        .ifEmpty { accounts.filter { it.code.startsWith("3") } }

    val totalAssets = assets.sumOf { it.balance }
    val totalLiabilities = liabilities.sumOf { it.balance }
    val totalEquity = equity.sumOf { it.balance }
    val totalLiabEquity = totalLiabilities + totalEquity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatementGroup("الأصول والموجودات (1000)", assets, totalAssets, FundPrimary, screenFormat)
        StatementGroup("الالتزامات والذمم الدائنة (2000)", liabilities, totalLiabilities, StatusPending, screenFormat)
        StatementGroup("حقوق الملكية والاحتياطي (3000)", equity, totalEquity, FundGoldDark, screenFormat)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, FundPrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مجموع الالتزامات وحقوق الملكية:", fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    FormatUtils.formatAmount(totalLiabEquity, screenFormat),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = FundPrimary,
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 4: Account Ledger (Running Balance)
// ----------------------------------------------------------------------------
@Composable
private fun AccountLedgerTab(
    accounts: List<Account>,
    entries: List<JournalEntry>,
    screenFormat: DisplayFormatConfig
) {
    var selectedCode by remember { mutableStateOf("1100") }
    var dropdownOpen by remember { mutableStateOf(false) }

    val activeAccount = accounts.find { it.code == selectedCode } ?: accounts.first()

    // Filter lines and calculate running balance according to account normal side
    var running = 0.0
    val ledgerRows = entries.sortedBy { it.date }.flatMap { entry ->
        entry.lines.filter { it.accountCode == selectedCode }.map { line ->
            val delta = if (activeAccount.isDebitNormal) (line.debit - line.credit) else (line.credit - line.debit)
            running += delta
            Quadruple(entry.date, entry.description, line.debit, line.credit, running)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Account Picker Dropdown
        Box {
            OutlinedButton(
                onClick = { dropdownOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("الحساب: ${activeAccount.name} (${FormatUtils.formatCodeOrNumber(activeAccount.code, screenFormat)})", fontWeight = FontWeight.Bold)
            }

            DropdownMenu(
                expanded = dropdownOpen,
                onDismissRequest = { dropdownOpen = false }
            ) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = { Text("${acc.name} (${FormatUtils.formatCodeOrNumber(acc.code, screenFormat)})") },
                        onClick = {
                            selectedCode = acc.code
                            dropdownOpen = false
                        }
                    )
                }
            }
        }

        // Ledger Card
        Card(
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("التاريخ والبيان", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                    Text("مدين", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("دائن", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("الرصيد", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                if (ledgerRows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد حركات مسجلة لهذا الحساب حالياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    ledgerRows.forEachIndexed { index, (date, desc, deb, cred, bal) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(desc, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                Text(FormatUtils.formatDate(date, screenFormat), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                if (deb > 0) FormatUtils.formatAmount(deb, screenFormat, inTable = true) else "-",
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                            Text(
                                if (cred > 0) FormatUtils.formatAmount(cred, screenFormat, inTable = true) else "-",
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                            Text(
                                FormatUtils.formatAmount(bal, screenFormat, inTable = true),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FundPrimary,
                                modifier = Modifier.weight(1f),
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

// ----------------------------------------------------------------------------
// Tab 5: Bank & Treasury Reconciliation
// ----------------------------------------------------------------------------
@Composable
private fun ReconciliationTab(
    accounts: List<Account>,
    screenFormat: DisplayFormatConfig,
    onAdjustEntry: (String, String, Double) -> Unit
) {
    var selectedCode by remember { mutableStateOf("1100") }
    val activeAcc = accounts.find { it.code == selectedCode } ?: accounts.first()
    val bookBalance = activeAcc.balance
    var actualCountStr by remember(selectedCode, bookBalance) {
        mutableStateOf(bookBalance.toLong().toString())
    }

    val actualBalance = actualCountStr.toDoubleOrNull() ?: 0.0
    val difference = actualBalance - bookBalance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("مطابقة وتسوية أرصدة الخزينة والبنوك", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FundPrimary)

                // Select Cash / Bank
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1100" to "الخزينة النقدية", "1200" to "بنك الكريمي", "1300" to "المحافظ").forEach { (code, name) ->
                        FilterChip(
                            selected = selectedCode == code,
                            onClick = { selectedCode = code },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }

                Text(
                    "الرصيد الدفتري الحالي: ${FormatUtils.formatAmount(bookBalance, screenFormat)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )

                OutlinedTextField(
                    value = actualCountStr,
                    onValueChange = { input -> actualCountStr = FormatUtils.toLatinDigits(input).filter { it.isDigit() || it == '.' } },
                    label = { Text("الرصيد الفعلي بعد الجرد / كشف البنك (${screenFormat.resolvedCurrencySymbol()})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (abs(difference) < 0.01) MaterialTheme.colorScheme.surfaceVariant
                            else StatusPending.copy(alpha = 0.14f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("فارق المطابقة والتسوية:", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(
                        text = if (abs(difference) < 0.01) {
                            "مطابق تماماً ✔ (صفر)"
                        } else {
                            "${FormatUtils.formatAmount(difference, screenFormat)} ${if (difference > 0) "(فائض)" else "(عجز)"}"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (abs(difference) < 0.01) StatusSuccess else StatusDanger,
                        fontSize = 13.sp,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }

                if (abs(difference) >= 1.0) {
                    Button(
                        onClick = { onAdjustEntry(activeAcc.code, activeAcc.name, difference) },
                        colors = ButtonDefaults.buttonColors(containerColor = FundGoldDark),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("توليد قيد تسوية آلي بالفارق")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 6: Currencies
// ----------------------------------------------------------------------------
@Composable
private fun CurrenciesTab(
    rates: Map<CurrencyCode, Double>,
    screenFormat: DisplayFormatConfig,
    onUpdateRate: (CurrencyCode, Double) -> Unit
) {
    var calcAmountStr by remember { mutableStateOf("1000") }
    var calcCurrency by remember { mutableStateOf(CurrencyCode.SAR) }

    val calcAmount = calcAmountStr.toDoubleOrNull() ?: 0.0
    val selectedRate = rates[calcCurrency] ?: 1.0
    val equivalentYer = calcAmount * selectedRate
    val usdRate = (rates[CurrencyCode.USD] ?: 2530.0).coerceAtLeast(1.0)
    val sarRate = (rates[CurrencyCode.SAR] ?: 665.0).coerceAtLeast(1.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("إدارة أسعار صرف العملات الأجنبية مقابل العملة الأساسية (${screenFormat.resolvedCurrencySymbol()})", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        rates.forEach { (curr, rate) ->
            if (curr != CurrencyCode.YER) {
                var newRateStr by remember(rate) { mutableStateOf(rate.toString()) }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${curr.title} (${curr.symbol})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("السعر الحالي: ${FormatUtils.formatAmount(rate, screenFormat, decimalsOverride = 0)}", fontSize = 11.sp, color = FundPrimary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newRateStr,
                                onValueChange = { newRateStr = FormatUtils.toLatinDigits(it).filter { c -> c.isDigit() || c == '.' } },
                                singleLine = true,
                                modifier = Modifier.width(100.dp)
                            )
                            Button(
                                onClick = {
                                    val r = newRateStr.toDoubleOrNull()
                                    if (r != null && r > 0) onUpdateRate(curr, r)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("تحديث", fontSize = 11.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }

        // Instant Multi-Currency Calculator Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, FundGold)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(18.dp))
                    Text("حاسبة المعادلة الفورية للعملات", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FundPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyCode.entries.forEach { code ->
                        FilterChip(
                            selected = calcCurrency == code,
                            onClick = { calcCurrency = code },
                            label = { Text("${code.title} (${code.symbol})", fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = calcAmountStr,
                    onValueChange = { calcAmountStr = FormatUtils.toLatinDigits(it).filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("أدخل المبلغ المراد تحويله (${calcCurrency.symbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = FundDeepHeader,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "المعادل بالعملة الأساسية: ${FormatUtils.formatAmount(equivalentYer, screenFormat)}",
                            color = FundGoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                        val eqUsd = FormatUtils.formatNumber(equivalentYer / usdRate, screenFormat, decimalsOverride = 1)
                        val eqSar = FormatUtils.formatNumber(equivalentYer / sarRate, screenFormat, decimalsOverride = 1)
                        Text(
                            text = "المعادل بالدولار: $eqUsd $  |  بالريال السعودي: $eqSar ر.س",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }
        }
    }
}
