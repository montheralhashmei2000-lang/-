package com.example.fund.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fund.data.FundRepository
import com.example.fund.model.Member
import com.example.fund.model.Voucher
import com.example.ui.theme.*

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit
) {
    val repo = FundRepository.getInstance()
    val members by repo.members.collectAsState()
    val vouchers by repo.vouchers.collectAsState()

    var searchQuery by remember { mutableStateOf("M-1001") }
    var matchedMember by remember { mutableStateOf<Member?>(members.find { it.memberNo == "M-1001" }) }
    var matchedVoucher by remember { mutableStateOf<Voucher?>(null) }
    var hasSearched by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = FundGoldDark)
                        Text(
                            text = "التحقق الميداني من الـ QR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FundDeepHeader
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "أدخل رقم العضوية (مثل M-1001) أو رقم السند (مثل V-2026-001) للمطابقة الميدانية الفورية:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input + Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("رقم العضوية أو السند") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            hasSearched = true
                            val query = searchQuery.trim()
                            matchedMember = members.find { it.memberNo.equals(query, ignoreCase = true) || it.nationalId == query }
                            matchedVoucher = vouchers.find { it.voucherNo.equals(query, ignoreCase = true) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "فحص")
                    }
                }

                // Quick selector chips for demo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("M-1001", "M-1002", "M-1006", "V-2026-001").forEach { sample ->
                        AssistChip(
                            onClick = {
                                searchQuery = sample
                                val query = sample
                                matchedMember = members.find { it.memberNo.equals(query, ignoreCase = true) }
                                matchedVoucher = vouchers.find { it.voucherNo.equals(query, ignoreCase = true) }
                                hasSearched = true
                            },
                            label = { Text(sample, fontSize = 11.sp) }
                        )
                    }
                }

                // Verification Results Card
                if (hasSearched) {
                    when {
                        matchedMember != null -> {
                            val m = matchedMember!!
                            val isActive = m.status == "نشط"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isActive) FundLightSurfaceSecondary else Color(0xFFFFEBEE)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) StatusSuccess else StatusDanger),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(m.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Surface(
                                            color = if (isActive) StatusSuccess else StatusDanger,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (isActive) "عضوية سارية ✔" else "عضوية معلقة ⚠",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text("رقم العضوية: ${m.memberNo} | الهوية: ${m.nationalId}", fontSize = 12.sp)
                                    Text("المهنة: ${m.job} | المنطقة: ${m.region} | القرية: ${m.village}", fontSize = 12.sp)
                                    Text("إجمالي المدفوع: %,d ﷼".format(m.totalPaidYer.toLong()), fontWeight = FontWeight.Bold, color = FundPrimary, fontSize = 12.sp)
                                    Text("المتأخرات المستحقة: %,d ﷼".format(m.dueBalanceYer.toLong()), color = if (m.dueBalanceYer > 0) StatusDanger else StatusSuccess, fontSize = 12.sp)
                                    Text(
                                        text = if (isActive && m.dueBalanceYer == 0.0) "أهلية الاستفادة: مؤهل بالكامل لجميع المساعدات" else "أهلية الاستفادة: يلزم تسوية المتأخرات أولاً",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isActive && m.dueBalanceYer == 0.0) StatusSuccess else StatusPending
                                    )
                                }
                            }
                        }

                        matchedVoucher != null -> {
                            val v = matchedVoucher!!
                            Card(
                                colors = CardDefaults.cardColors(containerColor = FundLightSurfaceSecondary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FundGoldDark),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${v.type.title} - ${v.voucherNo}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(v.status.title, color = if (v.status.name == "POSTED") StatusSuccess else StatusDanger, fontWeight = FontWeight.Bold)
                                    }
                                    Text("المكرم: ${v.partyName}", fontSize = 12.sp)
                                    Text("المبلغ: %,d ﷼".format(v.amountYer.toLong()), fontWeight = FontWeight.Bold, color = FundPrimary)
                                    Text("البيان: ${v.description}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF3E0), RoundedCornerShape(10.dp))
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لم يتم العثور على سجل مطابق للرقم المدخل.", color = Color(0xFFE65100), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
