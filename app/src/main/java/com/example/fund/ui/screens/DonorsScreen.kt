package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Handshake
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.CurrencyCode
import com.example.fund.model.Donor
import com.example.fund.model.PaymentMethod
import com.example.fund.model.UserRole
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.components.MoneyInput
import com.example.fund.util.FormatUtils
import com.example.fund.util.LocalScreenFormat
import com.example.ui.theme.*

@Composable
fun DonorsScreen() {
    val context = LocalContext.current
    val screenFormat = LocalScreenFormat.current
    val repo = FundRepository.getInstance()
    val donors by repo.donors.collectAsStateWithLifecycle()
    val currentUser by repo.currentUser.collectAsStateWithLifecycle()
    val canManage = currentUser.role != UserRole.VIEWER

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var donorForPayment by remember { mutableStateOf<Donor?>(null) }

    val filtered = donors.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "سجل الداعمين والمانحين",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FundPrimary
                )
                Text("متابعة التعهدات المالية ونسب التحصيل", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (canManage) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("داعم جديد", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث باسم الداعم أو الهاتف...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                FundEmptyState(
                    icon = Icons.Default.Handshake,
                    title = "لا يوجد داعمون مطابقون",
                    subtitle = if (searchQuery.isNotBlank()) "لم نعثر على داعم يطابق عبارة البحث المدخلة." else "سجّل بيانات الداعمين والمانحين لمتابعة تعهداتهم.",
                    actionLabel = if (searchQuery.isNotBlank()) "مسح البحث" else if (canManage) "إضافة داعم" else null,
                    onAction = if (searchQuery.isNotBlank()) ({ searchQuery = "" }) else if (canManage) ({ showAddDialog = true }) else null
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { d ->
                    val pledgePercent = if (d.pledgeAmountYer > 0) ((d.pledgeFulfilledYer / d.pledgeAmountYer) * 100).toInt() else 100
                    val remainingPledge = (d.pledgeAmountYer - d.pledgeFulfilledYer).coerceAtLeast(0.0)

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(d.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("النوع: ${d.type} | هاتف: ${d.phone}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Surface(
                                    color = FundGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "وفاء $pledgePercent%",
                                        color = FundGoldDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "إجمالي التبرعات: ${FormatUtils.formatAmount(d.totalDonatedYer, screenFormat)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FundPrimary,
                                    modifier = Modifier.weight(1f),
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    "التعهد الكلي: ${FormatUtils.formatAmount(d.pledgeAmountYer, screenFormat)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                            }

                            if (d.pledgeAmountYer > 0) {
                                LinearProgressIndicator(
                                    progress = { (d.pledgeFulfilledYer / d.pledgeAmountYer.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = StatusSuccess,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            if (canManage) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (remainingPledge > 0) "متبقي من التعهد: ${FormatUtils.formatAmount(remainingPledge, screenFormat)}" else "مكتمل الوفاء بالتعهد ✔",
                                        fontSize = 11.sp,
                                        color = if (remainingPledge > 0) StatusPending else StatusSuccess,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )

                                    Button(
                                        onClick = { donorForPayment = d },
                                        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسجيل دفعة تعهد", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    donorForPayment?.let { donor ->
        PledgePaymentDialog(
            donor = donor,
            onDismiss = { donorForPayment = null },
            onPay = { amount, curr, method ->
                repo.recordDonorPledgePayment(donor.id, amount, curr, method)
                Toast.makeText(context, "تم تحصيل دفعة التعهد وإصدار سند القبض والقيد المحاسبي", Toast.LENGTH_SHORT).show()
                donorForPayment = null
            }
        )
    }

    if (showAddDialog) {
        AddDonorDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newD ->
                repo.addDonor(newD)
                Toast.makeText(context, "تم حفظ بيانات الداعم", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun PledgePaymentDialog(
    donor: Donor,
    onDismiss: () -> Unit,
    onPay: (Double, CurrencyCode, PaymentMethod) -> Unit
) {
    val remaining = (donor.pledgeAmountYer - donor.pledgeFulfilledYer).coerceAtLeast(100000.0)
    var amountStr by remember { mutableStateOf(remaining.toLong().toString()) }
    var currency by remember { mutableStateOf(CurrencyCode.YER) }
    var method by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحصيل دفعة تعهد: ${donor.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyInput(
                    amount = amountStr,
                    onAmountChange = { amountStr = it },
                    selectedCurrency = currency,
                    onCurrencyChange = { currency = it },
                    label = "مبلغ الدفعة المحصلة"
                )

                Text("طريقة الدفع والإيداع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaymentMethod.values().forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.title, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onPay(amt, currency, method)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("تأكيد التحصيل وإصدار السند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun AddDonorDialog(
    onDismiss: () -> Unit,
    onAdd: (Donor) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("مغترب / فردي") }
    var pledgeStr by remember { mutableStateOf("1000000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة داعم أو مانح جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            val dialogFormat = LocalScreenFormat.current
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الداعم أو المؤسسة") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("صفة الداعم (مغترب، تاجر، مؤسسة)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pledgeStr, onValueChange = { pledgeStr = it }, label = { Text("مبلغ التعهد المالي (${dialogFormat.resolvedCurrencySymbol()})") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = pledgeStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onAdd(Donor(name = name, phone = phone, type = type, pledgeAmountYer = p, totalDonatedYer = 0.0, pledgeFulfilledYer = 0.0))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("حفظ الداعم")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
