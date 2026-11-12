package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
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
    val regions by repo.regions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRegionFilter by remember { mutableStateOf("كل المناطق") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = beneficiaries.filter { b ->
        val matchesSearch = b.familyHeadName.contains(searchQuery, ignoreCase = true) ||
                b.nationalId.contains(searchQuery) ||
                b.region.contains(searchQuery, ignoreCase = true) ||
                b.village.contains(searchQuery, ignoreCase = true)
        val matchesRegion = selectedRegionFilter == "كل المناطق" || b.region == selectedRegionFilter
        matchesSearch && matchesRegion
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("قاعدة بيانات الأسر المستفيدة (${filtered.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FundPrimary)
                Text("رصد حالات الاحتياج الميداني في المناطق والقرى", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            placeholder = { Text("ابحث برب الأسرة، الهوية، المنطقة أو القرية...") },
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
            FilterChip(
                selected = selectedRegionFilter == "كل المناطق",
                onClick = { selectedRegionFilter = "كل المناطق" },
                label = { Text("كل المناطق", fontSize = 11.sp) }
            )
            regions.forEach { reg ->
                FilterChip(
                    selected = selectedRegionFilter == reg.name,
                    onClick = { selectedRegionFilter = reg.name },
                    label = { Text(reg.name, fontSize = 11.sp) }
                )
            }
        }

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

                        Text("المنطقة: ${b.region} • القرية: ${b.village} | الهاتف: ${b.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("أفراد الأسرة: ${b.membersCount} أفراد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("الدخل الشهري: %,d ﷼".format(b.monthlyIncomeYer.toLong()), fontSize = 12.sp, color = Color.Gray)
                        }

                        if (b.address.isNotEmpty()) {
                            Text("الحالة الاجتماعية: ${b.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                repo.addVillageByRegionName(newB.region, newB.village)
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
    val repo = FundRepository.getInstance()
    val regions by repo.regions.collectAsState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }
    var membersCountStr by remember { mutableStateOf("5") }
    var incomeStr by remember { mutableStateOf("50000") }
    var selectedRegion by remember { mutableStateOf(regions.firstOrNull()?.name ?: "") }
    var selectedVillage by remember { mutableStateOf(regions.firstOrNull()?.villages?.firstOrNull() ?: "") }
    var regionDropdown by remember { mutableStateOf(false) }
    var villageDropdown by remember { mutableStateOf(false) }
    var customVillageMode by remember { mutableStateOf(false) }
    var customVillageInput by remember { mutableStateOf("") }
    var tier by remember { mutableStateOf(PovertyTier.EXTREME) }
    var notes by remember { mutableStateOf("أرملة وأيتام") }

    val activeVillages = regions.find { it.name == selectedRegion }?.villages ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل أسرة مستفيدة جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم رب الأسرة / المعيل") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nationalId, onValueChange = { nationalId = it }, label = { Text("رقم البطاقة الشخصية") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = membersCountStr, onValueChange = { membersCountStr = it }, label = { Text("عدد الأفراد") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = incomeStr, onValueChange = { incomeStr = it }, label = { Text("الدخل التقديري") }, modifier = Modifier.weight(1f))
                }

                // Region Selector
                Text("المنطقة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { regionDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedRegion.ifBlank { "اختر المنطقة..." })
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = regionDropdown, onDismissRequest = { regionDropdown = false }) {
                        regions.forEach { r ->
                            DropdownMenuItem(
                                text = { Text("${r.name} (${r.villages.size} قرى)") },
                                onClick = {
                                    selectedRegion = r.name
                                    selectedVillage = r.villages.firstOrNull() ?: ""
                                    regionDropdown = false
                                }
                            )
                        }
                    }
                }

                // Village Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("القرية:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
                    TextButton(onClick = { customVillageMode = !customVillageMode }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text(if (customVillageMode) "من القائمة" else "+ قرية جديدة", fontSize = 11.sp)
                    }
                }
                if (customVillageMode || activeVillages.isEmpty()) {
                    OutlinedTextField(
                        value = if (customVillageMode) customVillageInput else selectedVillage,
                        onValueChange = { if (customVillageMode) customVillageInput = it else selectedVillage = it },
                        label = { Text("اسم القرية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { villageDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedVillage.ifBlank { "اختر القرية..." })
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = villageDropdown, onDismissRequest = { villageDropdown = false }) {
                            activeVillages.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v) },
                                    onClick = {
                                        selectedVillage = v
                                        villageDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Poverty tier selector
                Text("تصنيف درجة الاحتياج:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PovertyTier.entries.forEach { pt ->
                        FilterChip(
                            selected = tier == pt,
                            onClick = { tier = pt },
                            label = { Text(pt.title, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("وصف الحالة والاحتياج") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = membersCountStr.toIntOrNull() ?: 1
                    val inc = incomeStr.toDoubleOrNull() ?: 0.0
                    val finalVillage = (if (customVillageMode) customVillageInput else selectedVillage).trim()
                    if (name.isNotBlank() && selectedRegion.isNotBlank() && finalVillage.isNotBlank()) {
                        onAdd(
                            Beneficiary(
                                familyHeadName = name.trim(),
                                nationalId = nationalId.trim(),
                                phone = phone.trim(),
                                membersCount = count,
                                monthlyIncomeYer = inc,
                                povertyTier = tier,
                                region = selectedRegion.trim(),
                                village = finalVillage,
                                address = notes.trim()
                            )
                        )
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
