package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.fund.model.AccountType
import com.example.fund.model.UserRole
import com.example.ui.theme.*

@Composable
fun AccountsScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val accounts by repo.accounts.collectAsState()
    val currentUser by repo.currentUser.collectAsState()
    val canAdd = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.ACCOUNTANT

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("الكل") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = accounts.filter { acc ->
        val matchSearch = acc.name.contains(searchQuery, ignoreCase = true) || acc.code.contains(searchQuery)
        val matchType = when (selectedTypeFilter) {
            "الأصول" -> acc.type == AccountType.ASSET
            "الالتزامات" -> acc.type == AccountType.LIABILITY
            "حقوق الملكية" -> acc.type == AccountType.EQUITY
            "الإيرادات" -> acc.type == AccountType.REVENUE
            "المصروفات" -> acc.type == AccountType.EXPENSE
            else -> true
        }
        matchSearch && matchType
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
                Text(
                    "دليل الحسابات المحاسبي القياسي",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FundPrimary
                )
                Text("شجرة الحسابات والأرصدة الدفترية الحية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (canAdd) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حساب فرعي جديد", fontSize = 12.sp)
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برقم الحساب أو الاسم...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("الكل", "الأصول", "الالتزامات", "حقوق الملكية", "الإيرادات", "المصروفات").forEach { filter ->
                val isSelected = selectedTypeFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTypeFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.code }) { acc ->
                val isParent = acc.parentCode == null
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isParent) FundLightSurfaceSecondary else MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isParent) FundPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.padding(start = if (isParent) 0.dp else 14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(acc.name, fontWeight = if (isParent) FontWeight.Black else FontWeight.Bold, fontSize = 13.sp)
                                if (isParent) {
                                    Surface(color = FundPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                        Text("حساب رئيسي", fontSize = 9.sp, color = FundPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text("رمز الحساب: ${acc.code} | النوع: ${acc.type.title} | الطبيعة: ${if (acc.isDebitNormal) "مدين" else "دائن"}", fontSize = 10.sp, color = Color.Gray)
                        }

                        Text(
                            text = "%,d ﷼".format(acc.balance.toLong()),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = FundPrimary
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubAccountDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { code, name, type, parentCode, openingBal ->
                val ok = repo.addSubAccount(code, name, type, parentCode, openingBal)
                if (ok) {
                    Toast.makeText(context, "تمت إضافة الحساب الفرعي ($code - $name) بنجاح", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                } else {
                    Toast.makeText(context, "خطأ: رمز الحساب موجود مسبقاً!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
private fun AddSubAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, AccountType, String, Double) -> Unit
) {
    var selectedType by remember { mutableStateOf(AccountType.ASSET) }
    var code by remember { mutableStateOf("1500") }
    var name by remember { mutableStateOf("") }
    var openingBalStr by remember { mutableStateOf("0") }

    val parentCode = when (selectedType) {
        AccountType.ASSET -> "1000"
        AccountType.LIABILITY -> "2000"
        AccountType.EQUITY -> "3000"
        AccountType.REVENUE -> "4000"
        AccountType.EXPENSE -> "5000"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة حساب محاسبي فرعي جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("تصنيف الحساب الأب:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AccountType.values().take(3).forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = {
                                selectedType = t
                                code = when (t) {
                                    AccountType.ASSET -> "1500"
                                    AccountType.LIABILITY -> "2300"
                                    AccountType.EQUITY -> "3300"
                                    else -> "4400"
                                }
                            },
                            label = { Text(t.title, fontSize = 10.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AccountType.values().drop(3).forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = {
                                selectedType = t
                                code = if (t == AccountType.REVENUE) "4400" else "5500"
                            },
                            label = { Text(t.title, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("رمز الحساب الفرعي (تحت $parentCode)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الحساب المحاسبي (مثال: بنك التسليف CAC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = openingBalStr,
                    onValueChange = { openingBalStr = it },
                    label = { Text("الرصيد الافتتاحي (ريال يمني)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = openingBalStr.toDoubleOrNull() ?: 0.0
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onAdd(code.trim(), name.trim(), selectedType, parentCode, bal)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("حفظ في شجرة الحسابات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
