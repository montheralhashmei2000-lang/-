package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import com.example.fund.model.Budget
import com.example.fund.model.UserRole
import com.example.fund.util.FormatUtils
import com.example.fund.util.LocalScreenFormat
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen() {
    val context = LocalContext.current
    val screenFormat = LocalScreenFormat.current
    val repo = FundRepository.getInstance()
    val budgets by repo.budgets.collectAsStateWithLifecycle()
    val settings by repo.appSettings.collectAsStateWithLifecycle()
    val currentUser by repo.currentUser.collectAsStateWithLifecycle()
    val canManage = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.ACCOUNTANT

    var showAddDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<Budget?>(null) }

    val totalBudget = budgets.sumOf { it.budgetedAmount }
    val totalSpent = budgets.sumOf { it.actualSpent }
    val overallRatio = (totalSpent / totalBudget.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Card
        item(key = "budgets_master_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FundPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "الموازنة التقديرية العامة لعام ${FormatUtils.formatCodeOrNumber("2026", screenFormat)}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (canManage) {
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FundGold),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("بند موازنة جديد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المنصرف الفعلي: ${FormatUtils.formatAmount(totalSpent, screenFormat)}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                            Text("من أصل معتمد: ${FormatUtils.formatAmount(totalBudget, screenFormat)}", color = FundGoldLight, fontSize = 12.sp, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                        }
                        Text("${FormatUtils.formatCodeOrNumber((overallRatio * 100).toInt().toString(), screenFormat)}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                    }

                    LinearProgressIndicator(
                        progress = { overallRatio },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = FundGold,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }

        item(key = "budgets_list_header") {
            Text(
                "بنود المصروفات والموازنات الفرعية",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = FundPrimary
            )
        }

        items(budgets, key = { it.id }) { b ->
            val ratio = (b.actualSpent / b.budgetedAmount.coerceAtLeast(1.0)).toFloat()
            val isOver = b.actualSpent > b.budgetedAmount
            val remaining = b.budgetedAmount - b.actualSpent

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isOver) StatusDanger else MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${b.category} (${FormatUtils.formatCodeOrNumber(b.accountCode, screenFormat)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${FormatUtils.formatCodeOrNumber((ratio * 100).toInt().toString(), screenFormat)}%",
                                fontWeight = FontWeight.Black,
                                color = if (isOver) StatusDanger else FundPrimary,
                                fontSize = 13.sp,
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                            if (canManage) {
                                IconButton(
                                    onClick = { budgetToEdit = b },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل السقف", tint = FundPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { ratio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (isOver) StatusDanger else if (ratio >= settings.budgetAlertPercent / 100f) FundGoldDark else FundPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("المنصرف: ${FormatUtils.formatAmount(b.actualSpent, screenFormat)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                        Text("المعتمد: ${FormatUtils.formatAmount(b.budgetedAmount, screenFormat)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                        Text(
                            text = if (remaining >= 0) "المتبقي: ${FormatUtils.formatAmount(remaining, screenFormat)}" else "تجاوز السقف: ${FormatUtils.formatAmount(-remaining, screenFormat)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remaining >= 0) StatusSuccess else StatusDanger,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }
        }
    }

    budgetToEdit?.let { b ->
        var newAmtStr by remember { mutableStateOf(b.budgetedAmount.toLong().toString()) }
        AlertDialog(
            onDismissRequest = { budgetToEdit = null },
            title = { Text("تعديل سقف الموازنة: ${b.category}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = newAmtStr,
                    onValueChange = { newAmtStr = it },
                    label = { Text("المبلغ التقديري المعتمد (${screenFormat.resolvedCurrencySymbol()})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = newAmtStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            repo.updateBudgetAmount(b.id, amt)
                            Toast.makeText(context, "تم تحديث السقف التقديري للموازنة", Toast.LENGTH_SHORT).show()
                            budgetToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToEdit = null }) { Text("إلغاء") }
            }
        )
    }

    if (showAddDialog) {
        var category by remember { mutableStateOf("") }
        var accCode by remember { mutableStateOf("5500") }
        var amountStr by remember { mutableStateOf("1000000") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة بند موازنة تقديرية جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("اسم بند الصرف / النشاط") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = accCode, onValueChange = { accCode = it }, label = { Text("رمز الحساب المحاسبي المرتبط") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("السقف التقديري المعتمد (${screenFormat.resolvedCurrencySymbol()})") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (category.isNotBlank() && amt > 0) {
                            repo.addBudget(category, accCode, amt, "2026")
                            Toast.makeText(context, "تمت إضافة بند الموازنة", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
                ) {
                    Text("حفظ البند")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
