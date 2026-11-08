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
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import com.example.ui.theme.*

@Composable
fun FinancialReportsScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val accounts by repo.accounts.collectAsState()
    val journalEntries by repo.journalEntries.collectAsState()
    val currencyRates by repo.currencyRates.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scrollable Tabs
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 0.dp,
            containerColor = FundLightSurfaceSecondary,
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
            0 -> TrialBalanceTab(accounts, journalEntries)
            1 -> IncomeStatementTab(accounts)
            2 -> BalanceSheetTab(accounts)
            3 -> AccountLedgerTab(accounts, journalEntries)
            4 -> ReconciliationTab(accounts, onAdjustEntry = { code, name, diff ->
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
                repo.createManualJournalEntry(lines, "قيد تسوية مطابق لحساب $name بفارق $diff ﷼")
                Toast.makeText(context, "تم توليد قيد التسوية وترحيله بنجاح", Toast.LENGTH_SHORT).show()
            })
            5 -> CurrenciesTab(currencyRates, onUpdateRate = { curr, newRate ->
                repo.updateCurrencyRate(curr, newRate)
                Toast.makeText(context, "تم تحديث سعر صرف ${curr.title} إلى $newRate", Toast.LENGTH_SHORT).show()
            })
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 1: Trial Balance
// ----------------------------------------------------------------------------
@Composable
private fun TrialBalanceTab(accounts: List<Account>, entries: List<JournalEntry>) {
    // Calculate debit and credit totals
    var totalDebitBalance = 0.0
    var totalCreditBalance = 0.0

    val rows = accounts.map { acc ->
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

    val isBalanced = Math.abs(totalDebitBalance - totalCreditBalance) < 1.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
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
                    text = "الإجمالي: %,d ﷼".format(totalDebitBalance.toLong()),
                    fontWeight = FontWeight.Black,
                    color = FundPrimary,
                    fontSize = 13.sp
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
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FundLightSurfaceSecondary, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الحساب المحاسبي", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                    Text("رصيد مدين", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("رصيد دائن", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                rows.forEachIndexed { index, (acc, deb, cred) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color.Transparent else FundLightSurfaceSecondary.copy(alpha = 0.5f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(2f)) {
                            Text(acc.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(acc.code, fontSize = 9.sp, color = Color.Gray)
                        }
                        Text(
                            text = if (deb > 0) "%,d".format(deb.toLong()) else "-",
                            fontSize = 11.sp,
                            fontWeight = if (deb > 0) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (cred > 0) "%,d".format(cred.toLong()) else "-",
                            fontSize = 11.sp,
                            fontWeight = if (cred > 0) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = FundPrimary, thickness = 2.dp)

                // Totals Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FundPrimary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الإجماليات الكلية:", fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.weight(2f))
                    Text("%,d ﷼".format(totalDebitBalance.toLong()), fontWeight = FontWeight.Black, fontSize = 12.sp, color = StatusSuccess, modifier = Modifier.weight(1f))
                    Text("%,d ﷼".format(totalCreditBalance.toLong()), fontWeight = FontWeight.Black, fontSize = 12.sp, color = StatusSuccess, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 2: Income Statement
// ----------------------------------------------------------------------------
@Composable
private fun IncomeStatementTab(accounts: List<Account>) {
    val revenues = accounts.filter { it.code.startsWith("4") }
    val expenses = accounts.filter { it.code.startsWith("5") }

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
                    text = "%,d ﷼".format(netSurplus.toLong()),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
            }
        }

        // Revenues Section
        StatementGroup("أولاً: الإيرادات والتبرعات (4000)", revenues, totalRevenues, FundPrimaryMedium)

        // Expenses Section
        StatementGroup("ثانياً: المصروفات والمساعدات (5000)", expenses, totalExpenses, Color(0xFFC62828))
    }
}

@Composable
private fun StatementGroup(title: String, accounts: List<Account>, total: Double, color: Color) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
                Text("%,d ﷼".format(total.toLong()), fontWeight = FontWeight.Black, fontSize = 13.sp, color = color)
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            accounts.forEach { acc ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${acc.name} (${acc.code})", fontSize = 11.sp)
                    Text("%,d ﷼".format(acc.balance.toLong()), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 3: Balance Sheet
// ----------------------------------------------------------------------------
@Composable
private fun BalanceSheetTab(accounts: List<Account>) {
    val assets = accounts.filter { it.code.startsWith("1") }
    val liabilities = accounts.filter { it.code.startsWith("2") }
    val equity = accounts.filter { it.code.startsWith("3") }

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
        StatementGroup("الأصول والموجودات (1000)", assets, totalAssets, FundPrimary)
        StatementGroup("الالتزامات والذمم الدائنة (2000)", liabilities, totalLiabilities, StatusPending)
        StatementGroup("حقوق الملكية والاحتياطي (3000)", equity, totalEquity, FundGoldDark)

        Card(
            colors = CardDefaults.cardColors(containerColor = FundLightSurfaceSecondary),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, FundPrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("مجموع الالتزامات وحقوق الملكية:", fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("%,d ﷼".format(totalLiabEquity.toLong()), fontWeight = FontWeight.Black, fontSize = 13.sp, color = FundPrimary)
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 4: Account Ledger (Running Balance)
// ----------------------------------------------------------------------------
@Composable
private fun AccountLedgerTab(accounts: List<Account>, entries: List<JournalEntry>) {
    var selectedCode by remember { mutableStateOf("1100") }
    var dropdownOpen by remember { mutableStateOf(false) }

    val activeAccount = accounts.find { it.code == selectedCode } ?: accounts.first()

    // Filter lines and calculate running balance
    var running = 0.0
    val ledgerRows = entries.sortedBy { it.date }.flatMap { entry ->
        entry.lines.filter { it.accountCode == selectedCode }.map { line ->
            running += (line.debit - line.credit)
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
                Text("الحساب: ${activeAccount.name} (${activeAccount.code})", fontWeight = FontWeight.Bold)
            }

            DropdownMenu(
                expanded = dropdownOpen,
                onDismissRequest = { dropdownOpen = false }
            ) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = { Text("${acc.name} (${acc.code})") },
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
                        .background(FundLightSurfaceSecondary, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("التاريخ والبيان", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                    Text("مدين", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("دائن", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("الرصيد", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                if (ledgerRows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد حركات مسجلة لهذا الحساب حالياً.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    ledgerRows.forEachIndexed { index, (date, desc, deb, cred, bal) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 0) Color.Transparent else FundLightSurfaceSecondary.copy(alpha = 0.5f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(desc, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                Text(date, fontSize = 9.sp, color = Color.Gray)
                            }
                            Text(if (deb > 0) "%,d".format(deb.toLong()) else "-", fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text(if (cred > 0) "%,d".format(cred.toLong()) else "-", fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text(
                                "%,d".format(bal.toLong()),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FundPrimary,
                                modifier = Modifier.weight(1f)
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
private fun ReconciliationTab(accounts: List<Account>, onAdjustEntry: (String, String, Double) -> Unit) {
    var selectedCode by remember { mutableStateOf("1100") }
    var actualCountStr by remember { mutableStateOf("4850000") }

    val activeAcc = accounts.find { it.code == selectedCode } ?: accounts.first()
    val bookBalance = activeAcc.balance
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1100" to "الخزينة النقدية", "1200" to "بنك الكريمي", "1300" to "المحافظ").forEach { (code, name) ->
                        FilterChip(
                            selected = selectedCode == code,
                            onClick = {
                                selectedCode = code
                                val b = accounts.find { it.code == code }?.balance ?: 0.0
                                actualCountStr = b.toLong().toString()
                            },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }

                Text("الرصيد الدفتري الحالي: %,d ﷼".format(bookBalance.toLong()), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                OutlinedTextField(
                    value = actualCountStr,
                    onValueChange = { actualCountStr = it },
                    label = { Text("الرصيد الفعلي بعد الجرد / كشف البنك (﷼)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (difference == 0.0) FundLightSurfaceSecondary else Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("فارق المطابقة والتسوية:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = if (difference == 0.0) "مطابق تماماً ✔ (صفر)" else "%,d ﷼ ${if (difference > 0) "(فائض)" else "(عجز)"}".format(difference.toLong()),
                        fontWeight = FontWeight.Bold,
                        color = if (difference == 0.0) StatusSuccess else StatusDanger,
                        fontSize = 13.sp
                    )
                }

                if (difference != 0.0) {
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
private fun CurrenciesTab(rates: Map<CurrencyCode, Double>, onUpdateRate: (CurrencyCode, Double) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("إدارة أسعار صرف العملات الأجنبية مقابل الريال اليمني (YER)", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${curr.title} (${curr.symbol})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("السعر الحالي: %,.0f ﷼".format(rate), fontSize = 11.sp, color = FundPrimary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newRateStr,
                                onValueChange = { newRateStr = it },
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
                                Text("تحديث", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
