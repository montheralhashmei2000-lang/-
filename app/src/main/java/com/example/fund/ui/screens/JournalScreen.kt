package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.fund.model.Account
import com.example.fund.model.JournalEntry
import com.example.fund.model.JournalLine
import com.example.fund.model.UserRole
import com.example.ui.theme.*

@Composable
fun JournalScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val journalEntries by repo.journalEntries.collectAsState()
    val accounts by repo.accounts.collectAsState()
    val currentUser by repo.currentUser.collectAsState()
    val canAdd = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.ACCOUNTANT

    var searchQuery by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }

    val filteredEntries = journalEntries.filter {
        it.entryNo.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                (it.originalVoucherNo?.contains(searchQuery, ignoreCase = true) ?: false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("دفتر القيود اليومية المزدوجة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FundPrimary)
                Text("توثيق آلي ويدوي لجميع الحركات المحاسبية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (canAdd) {
                Button(
                    onClick = { showManualDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("قيد يدوي", fontSize = 12.sp)
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برقم القيد، البيان، أو رقم السند...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredEntries, key = { it.id }) { entry ->
                JournalEntryCardItem(entry)
            }
        }
    }

    if (showManualDialog) {
        ManualJournalEntryDialog(
            accounts = accounts,
            onDismiss = { showManualDialog = false },
            onSubmit = { lines, desc ->
                val success = repo.createManualJournalEntry(lines, desc)
                if (success) {
                    Toast.makeText(context, "تم حفظ وترحيل القيد بنجاح", Toast.LENGTH_SHORT).show()
                    showManualDialog = false
                } else {
                    Toast.makeText(context, "خطأ: مجموع المدين لا يساوي مجموع الدائن!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
private fun JournalEntryCardItem(entry: JournalEntry) {
    val totalDebit = entry.lines.sumOf { it.debit }
    val totalCredit = entry.lines.sumOf { it.credit }
    val isBalanced = Math.abs(totalDebit - totalCredit) < 0.01

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (entry.isReversed) StatusDanger else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.entryNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FundPrimary)
                    if (entry.isReversed) {
                        Surface(color = StatusDanger.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text("قيد عكسي", color = StatusDanger, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(entry.date, fontSize = 11.sp, color = Color.Gray)
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
                    Text("الحساب", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("مدين", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("دائن", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFD8E4D2))

                entry.lines.forEach { line ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${line.accountName} (${line.accountCode})", fontSize = 10.sp, modifier = Modifier.weight(2f))
                        Text(if (line.debit > 0) "%,d".format(line.debit.toLong()) else "-", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FundPrimary, modifier = Modifier.weight(1f))
                        Text(if (line.credit > 0) "%,d".format(line.credit.toLong()) else "-", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FundGoldDark, modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (isBalanced) "متوازن (Debit = Credit) ✔" else "غير متوازن ❌",
                    fontSize = 10.sp,
                    color = if (isBalanced) StatusSuccess else StatusDanger,
                    fontWeight = FontWeight.Bold
                )
                Text("الإجمالي: %,d ﷼".format(totalDebit.toLong()), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    var desc by remember { mutableStateOf("") }
    var debitCode by remember { mutableStateOf("1100") }
    var creditCode by remember { mutableStateOf("4100") }
    var amountStr by remember { mutableStateOf("25000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء قيد يومية مزدوج يدوي", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("البيان العام للقيد") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ المتطابق (ريال يمني)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("الطرف المدين (Debit):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("1100" to "الخزينة", "1200" to "الكريمي", "5100" to "مساعدات").forEach { (code, name) ->
                        FilterChip(
                            selected = debitCode == code,
                            onClick = { debitCode = code },
                            label = { Text(name, fontSize = 10.sp) }
                        )
                    }
                }

                Text("الطرف الدائن (Credit):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("4100" to "اشتراكات", "4200" to "تبرعات", "1100" to "الخزينة").forEach { (code, name) ->
                        FilterChip(
                            selected = creditCode == code,
                            onClick = { creditCode = code },
                            label = { Text(name, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && desc.isNotBlank()) {
                        val dAcc = accounts.find { it.code == debitCode }?.name ?: "الحساب المدين"
                        val cAcc = accounts.find { it.code == creditCode }?.name ?: "الحساب الدائن"
                        val lines = listOf(
                            JournalLine(debitCode, dAcc, amt, 0.0, "مدين - $desc"),
                            JournalLine(creditCode, cAcc, 0.0, amt, "دائن - $desc")
                        )
                        onSubmit(lines, desc)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("ترحيل القيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
