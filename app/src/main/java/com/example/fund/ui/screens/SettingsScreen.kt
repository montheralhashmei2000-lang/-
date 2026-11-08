package com.example.fund.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.fund.data.FundRepository
import com.example.fund.model.CurrencyCode
import com.example.fund.model.FundInfo
import com.example.ui.theme.*

@Composable
fun SettingsScreen() {
    val repo = FundRepository.getInstance()
    val fundInfo by repo.fundInfo.collectAsState()
    val rates by repo.currencyRates.collectAsState()
    val isDark by repo.isDarkMode.collectAsState()
    val fontScale by repo.fontScale.collectAsState()
    val context = LocalContext.current

    var nameAr by remember(fundInfo) { mutableStateOf(fundInfo.name) }
    var license by remember(fundInfo) { mutableStateOf(fundInfo.licenseNo) }
    var address by remember(fundInfo) { mutableStateOf(fundInfo.address) }
    var phone by remember(fundInfo) { mutableStateOf(fundInfo.phone) }
    var bankKareemi by remember(fundInfo) { mutableStateOf(fundInfo.bankAccountKareemi) }
    var bankCAC by remember(fundInfo) { mutableStateOf(fundInfo.bankAccountCAC) }

    var usdRate by remember(rates) { mutableStateOf(((rates[CurrencyCode.USD] ?: 2530.0).toLong()).toString()) }
    var sarRate by remember(rates) { mutableStateOf(((rates[CurrencyCode.SAR] ?: 665.0).toLong()).toString()) }

    var jsonBackupText by remember { mutableStateOf("") }
    var showBackupModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "الإعدادات العامة والنسخ الاحتياطي",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Appearance & Font Scale
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("المظهر وحجم الخط المحاسبي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الوضع الليلي الداكن (Dark Mode)")
                    Switch(
                        checked = isDark,
                        onCheckedChange = { repo.setDarkModePreference(it) },
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("مقياس حجم الخط الفوري في جميع الجداول والسندات:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.9f to "صغير 0.9x", 1.0f to "عادي 1.0x", 1.15f to "كبير 1.15x", 1.3f to "كبير جداً 1.3x").forEach { (scale, label) ->
                        FilterChip(
                            selected = fontScale == scale,
                            onClick = { repo.setFontScalePreference(scale) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Exchange rates
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("أسعار الصرف المعتمدة مقابل الريال اليمني (YER)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = usdRate,
                        onValueChange = { usdRate = it.filter { c -> c.isDigit() } },
                        label = { Text("سعر الدولار (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sarRate,
                        onValueChange = { sarRate = it.filter { c -> c.isDigit() } },
                        label = { Text("سعر السعودي (SAR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        usdRate.toDoubleOrNull()?.let { repo.updateCurrencyRate(CurrencyCode.USD, it) }
                        sarRate.toDoubleOrNull()?.let { repo.updateCurrencyRate(CurrencyCode.SAR, it) }
                        Toast.makeText(context, "تم تحديث وحفظ أسعار الصرف محلياً", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("save_rates_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("حفظ أسعار الصرف")
                }
            }
        }

        // Official Fund Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("البيانات الرسمية للصندوق والحسابات البنكية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("اسم الصندوق الرسمي") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = license, onValueChange = { license = it }, label = { Text("رقم الترخيص الرسمي") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان والمقر") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("أرقام التواصل") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = bankKareemi, onValueChange = { bankKareemi = it }, label = { Text("حساب بنك الكريمي") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = bankCAC, onValueChange = { bankCAC = it }, label = { Text("حساب كاك بنك CAC") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(
                    onClick = {
                        repo.updateFundInfo(
                            fundInfo.copy(
                                name = nameAr,
                                licenseNo = license,
                                address = address,
                                phone = phone,
                                bankAccountKareemi = bankKareemi,
                                bankAccountCAC = bankCAC
                            )
                        )
                        Toast.makeText(context, "تم حفظ البيانات الرسمية للصندوق", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("save_fund_info_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("حفظ البيانات الرسمية")
                }
            }
        }

        // JSON Backup & Restore
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("النسخ الاحتياطي الكامل واستعادة البيانات (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "جميع التعديلات تُحفظ تلقائياً في الذاكرة المستديمة للجهاز (Local-First Storage). يمكنك تصدير أو استيراد نسخة JSON كاملة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            jsonBackupText = repo.exportDatabaseJson()
                            showBackupModal = true
                        },
                        modifier = Modifier.testTag("export_json_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تصدير / استيراد JSON")
                    }
                    OutlinedButton(
                        onClick = {
                            val json = repo.exportDatabaseJson()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - ${fundInfo.name}")
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة النسخة الاحتياطية JSON"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("مشاركة النسخة")
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        repo.resetToDefaultSeedData()
                        Toast.makeText(context, "تمت إعادة ضبط النظام إلى البيانات الأولية المعتمدة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                    modifier = Modifier.testTag("reset_seed_data_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("إعادة ضبط المصنع (استعادة البيانات الأولية الافتراضية)")
                }
            }
        }
    }

    if (showBackupModal) {
        Dialog(onDismissRequest = { showBackupModal = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("النسخة الاحتياطية الكاملة للنظام (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "يمكنك نسخ النص أو تعديله ولصق نسخة احتياطية لاستعادتها:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonBackupText,
                        onValueChange = { jsonBackupText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showBackupModal = false }) { Text("إغلاق") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val ok = repo.importDatabaseFromJson(jsonBackupText)
                                if (ok) {
                                    Toast.makeText(context, "تم فحص واستيراد النسخة الاحتياطية بنجاح", Toast.LENGTH_SHORT).show()
                                    showBackupModal = false
                                } else {
                                    Toast.makeText(context, "ملف JSON غير صالح أو تالف", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("استعادة هذه النسخة")
                        }
                    }
                }
            }
        }
    }
}
