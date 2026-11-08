package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FamilyRestroom
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
import com.example.fund.model.Beneficiary
import com.example.fund.model.PovertyTier
import com.example.ui.theme.*

@Composable
fun BeneficiariesScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val beneficiaries by repo.beneficiaries.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = beneficiaries.filter {
        it.familyHeadName.contains(searchQuery, ignoreCase = true) ||
                it.nationalId.contains(searchQuery) ||
                it.villageOrArea.contains(searchQuery)
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
                Text("قاعدة بيانات الأسر المستفيدة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FundPrimary)
                Text("رصد حالات الفقر والاحتياج الميداني بردفان", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة أسرة", fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برب الأسرة، الهوية، أو المنطقة...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { b ->
                val tierColor = when (b.povertyTier) {
                    PovertyTier.EXTREME -> StatusDanger
                    PovertyTier.HIGH -> Color(0xFFE65100)
                    PovertyTier.MEDIUM -> FundGoldDark
                    PovertyTier.LOW -> StatusSuccess
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(b.familyHeadName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                color = tierColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, tierColor)
                            ) {
                                Text(
                                    text = b.povertyTier.title,
                                    color = tierColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text("المنطقة: ${b.villageOrArea} | الهاتف: ${b.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("أفراد الأسرة: ${b.membersCount} أفراد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("الدخل الشهري: %,d ﷼".format(b.monthlyIncomeYer.toLong()), fontSize = 12.sp, color = Color.Gray)
                        }

                        if (b.address.isNotEmpty()) {
                            Text("الحالة الاجتماعية: ${b.address}", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBeneficiaryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newB ->
                repo.addBeneficiary(newB)
                Toast.makeText(context, "تم حفظ بيانات الأسرة المستفيدة", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddBeneficiaryDialog(
    onDismiss: () -> Unit,
    onAdd: (Beneficiary) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }
    var membersCountStr by remember { mutableStateOf("5") }
    var incomeStr by remember { mutableStateOf("50000") }
    var village by remember { mutableStateOf("الحبيلين") }
    var tier by remember { mutableStateOf(PovertyTier.EXTREME) }
    var notes by remember { mutableStateOf("أرملة وأيتام") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل أسرة مستفيدة جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم رب الأسرة / المعيل") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nationalId, onValueChange = { nationalId = it }, label = { Text("رقم البطاقة الشخصية") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = membersCountStr, onValueChange = { membersCountStr = it }, label = { Text("عدد الأفراد") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = incomeStr, onValueChange = { incomeStr = it }, label = { Text("الدخل التقديري") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("القرية / المحلة") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("وصف الحالة والاحتياج") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = membersCountStr.toIntOrNull() ?: 1
                    val inc = incomeStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onAdd(Beneficiary(
                            familyHeadName = name,
                            nationalId = nationalId,
                            phone = phone,
                            membersCount = count,
                            monthlyIncomeYer = inc,
                            povertyTier = tier,
                            villageOrArea = village,
                            address = notes
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("حفظ الأسرة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
