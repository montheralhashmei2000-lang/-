package com.example.fund.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fund.model.AppFontOption
import com.example.fund.model.AppSettings
import com.example.fund.model.CurrencyCode
import com.example.fund.model.DisplayFormatConfig
import com.example.fund.model.FundInfo
import com.example.fund.model.LocalServerConfig
import com.example.fund.ui.viewmodel.SettingsViewModel
import com.example.fund.util.FormatUtils
import com.example.fund.util.LocalScreenFormat
import com.example.ui.theme.*

@Composable
fun AppFontSelectorSection(
    selectedFont: AppFontOption,
    screenFormat: DisplayFormatConfig,
    onSelectFont: (AppFontOption) -> Unit
) {
    val previewFamily = appFontFamilyFor(selectedFont)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.FontDownload, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(18.dp))
                Text(
                    "نوع الخط العربي للواجهة والتطبيق بالكامل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FundPrimary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppFontOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedFont == option,
                        onClick = { onSelectFont(option) },
                        label = {
                            Text(
                                text = option.title,
                                fontFamily = appFontFamilyFor(option),
                                fontSize = 12.sp,
                                fontWeight = if (selectedFont == option) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("app_font_chip_${option.name.lowercase()}")
                    )
                }
            }

            // Live Arabic & Financial Digits Preview Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FundPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_font_preview_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "معاينة فورية للخط المختار (${selectedFont.title}):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FundPrimary
                    )
                    Text(
                        text = "بسم الله الرحمن الرحيم — الصندوق الاجتماعي التنموي يدعم التكافل والمبادرات الخيرية.",
                        fontFamily = previewFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "أرقام مالية متوازنة: ${FormatUtils.formatAmount(1250750.0, screenFormat)} | التاريخ: ${FormatUtils.formatDate("2026-11-12", screenFormat)}",
                        fontFamily = previewFamily,
                        fontSize = 13.sp,
                        color = FundGoldDark,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}

@Composable
fun MembersAidsWarehouseTab(
    appSettings: AppSettings,
    maxAidCeilingYer: Double,
    onSave: (AppSettings, Double) -> Unit
) {
    val currencySym = LocalScreenFormat.current.resolvedCurrencySymbol()
    var feeStr by remember(appSettings) { mutableStateOf(appSettings.monthlySubscriptionFeeYer.toLong().toString()) }
    var suspendMonthsStr by remember(appSettings) { mutableStateOf(appSettings.autoSuspendMonths.toString()) }
    var memberPrefix by remember(appSettings) { mutableStateOf(appSettings.memberNoPrefix) }
    var requireNationalId by remember(appSettings) { mutableStateOf(appSettings.requireNationalId) }

    var ceilingStr by remember(maxAidCeilingYer) { mutableStateOf(maxAidCeilingYer.toLong().toString()) }
    var requireReviewer by remember(appSettings) { mutableStateOf(appSettings.requireReviewerApproval) }
    var periodicDayStr by remember(appSettings) { mutableStateOf(appSettings.periodicAidDisburseDay.toString()) }

    var lowStockStr by remember(appSettings) { mutableStateOf(appSettings.lowStockThreshold.toString()) }
    var warehouseName by remember(appSettings) { mutableStateOf(appSettings.defaultWarehouseName) }

    // Members & Subscriptions Card
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إعدادات الأعضاء والاشتراكات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = feeStr,
                    onValueChange = { feeStr = it.filter { c -> c.isDigit() } },
                    label = { Text("الاشتراك الشهري ($currencySym)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = suspendMonthsStr,
                    onValueChange = { suspendMonthsStr = it.filter { c -> c.isDigit() } },
                    label = { Text("أشهر التأخير للتعليق") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = memberPrefix,
                onValueChange = { memberPrefix = it },
                label = { Text("بادئة ترقيم العضوية (مثل M-)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إلزامية رقم الهوية الوطنية عند إضافة عضو", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = requireNationalId, onCheckedChange = { requireNationalId = it })
            }
        }
    }

    // Aids & Periodic Sponsorships Card
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إعدادات المساعدات والكفالات والأسر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ceilingStr,
                    onValueChange = { ceilingStr = it.filter { c -> c.isDigit() } },
                    label = { Text("سقف المساعدة الفردية ($currencySym)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.3f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = periodicDayStr,
                    onValueChange = { periodicDayStr = it.filter { c -> c.isDigit() } },
                    label = { Text("يوم صرف الكفالات") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.7f),
                    singleLine = true
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("اشتراط اعتماد المراجع المالي قبل الصرف", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = requireReviewer, onCheckedChange = { requireReviewer = it })
            }
        }
    }

    // In-Kind Warehouse Card
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إعدادات المستودع والمساعدات العينية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            OutlinedTextField(
                value = warehouseName,
                onValueChange = { warehouseName = it },
                label = { Text("اسم / موقع المستودع الافتراضي") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = lowStockStr,
                onValueChange = { lowStockStr = it.filter { c -> c.isDigit() } },
                label = { Text("حد التنبيه الافتراضي لنقص المخزون") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }

    Button(
        onClick = {
            val newFee = feeStr.toDoubleOrNull() ?: appSettings.monthlySubscriptionFeeYer
            val newSuspend = (suspendMonthsStr.toIntOrNull() ?: appSettings.autoSuspendMonths).coerceIn(1, 24)
            val newDay = (periodicDayStr.toIntOrNull() ?: appSettings.periodicAidDisburseDay).coerceIn(1, 28)
            val newLowStock = (lowStockStr.toIntOrNull() ?: appSettings.lowStockThreshold).coerceAtLeast(1)
            val newCeiling = ceilingStr.toDoubleOrNull() ?: maxAidCeilingYer

            onSave(
                appSettings.copy(
                    monthlySubscriptionFeeYer = newFee,
                    autoSuspendMonths = newSuspend,
                    requireNationalId = requireNationalId,
                    memberNoPrefix = memberPrefix.ifBlank { "M-" },
                    requireReviewerApproval = requireReviewer,
                    periodicAidDisburseDay = newDay,
                    lowStockThreshold = newLowStock,
                    defaultWarehouseName = warehouseName.ifBlank { "المستودع المركزي للصندوق" }
                ),
                newCeiling
            )
        },
        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("حفظ إعدادات الأعضاء والمساعدات والمستودع", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FinanceCurrenciesBudgetsTab(
    rates: Map<CurrencyCode, Double>,
    appSettings: AppSettings,
    onSaveRates: (Double, Double) -> Unit,
    onSaveSettings: (AppSettings) -> Unit
) {
    var usdRate by remember(rates) { mutableStateOf(((rates[CurrencyCode.USD] ?: 2530.0).toLong()).toString()) }
    var sarRate by remember(rates) { mutableStateOf(((rates[CurrencyCode.SAR] ?: 665.0).toLong()).toString()) }

    var voucherPrefix by remember(appSettings) { mutableStateOf(appSettings.voucherNoPrefix) }
    var defaultTreasury by remember(appSettings) { mutableStateOf(appSettings.defaultTreasuryAccountCode) }
    var allowNegative by remember(appSettings) { mutableStateOf(appSettings.allowNegativeTreasury) }
    var enforceBalanced by remember(appSettings) { mutableStateOf(appSettings.enforceBalancedJournal) }
    var autoPrint by remember(appSettings) { mutableStateOf(appSettings.autoPrintVoucher) }
    var budgetAlertStr by remember(appSettings) { mutableStateOf(appSettings.budgetAlertPercent.toString()) }
    var allowAnonDonors by remember(appSettings) { mutableStateOf(appSettings.allowAnonymousDonors) }

    // Exchange Rates Card
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("أسعار الصرف المعتمدة مقابل الريال اليمني (YER)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
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
            Button(
                onClick = {
                    val u = usdRate.toDoubleOrNull() ?: 2530.0
                    val s = sarRate.toDoubleOrNull() ?: 665.0
                    onSaveRates(u, s)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                modifier = Modifier.testTag("save_rates_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("حفظ أسعار الصرف")
            }
        }
    }

    // Vouchers, Treasury & Journal Card
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إعدادات السندات والخزينة والقيود المزدوجة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            OutlinedTextField(
                value = voucherPrefix,
                onValueChange = { voucherPrefix = it },
                label = { Text("بادئة ترقيم السندات (مثل V أو REC)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("حساب الخزينة الافتراضي:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("1100" to "الصندوق (1100)", "1200" to "البنك (1200)", "1300" to "المحافظ (1300)").forEach { (code, label) ->
                    FilterChip(
                        selected = defaultTreasury == code,
                        onClick = { defaultTreasury = code },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("السماح بالرصيد السالب عند التحويل من الخزينة", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = allowNegative, onCheckedChange = { allowNegative = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إلزامية توازن القيد المزدوج (مدين = دائن)", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = enforceBalanced, onCheckedChange = { enforceBalanced = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("فتح معاينة PDF تلقائياً بعد إصدار السند", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = autoPrint, onCheckedChange = { autoPrint = it })
            }
        }
    }

    // Budgets, Campaigns & Donors Card
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إعدادات الموازنة والحملات والمانحين", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            OutlinedTextField(
                value = budgetAlertStr,
                onValueChange = { budgetAlertStr = it.filter { c -> c.isDigit() } },
                label = { Text("نسبة تنبيه اقتراب سقف الموازنة (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("السماح بتبرعات فاعل خير (مانح غير مسمى)", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = allowAnonDonors, onCheckedChange = { allowAnonDonors = it })
            }
        }
    }

    Button(
        onClick = {
            val alertPct = (budgetAlertStr.toIntOrNull() ?: appSettings.budgetAlertPercent).coerceIn(10, 100)
            onSaveSettings(
                appSettings.copy(
                    voucherNoPrefix = voucherPrefix.ifBlank { "V" },
                    defaultTreasuryAccountCode = defaultTreasury,
                    allowNegativeTreasury = allowNegative,
                    enforceBalancedJournal = enforceBalanced,
                    autoPrintVoucher = autoPrint,
                    budgetAlertPercent = alertPct,
                    allowAnonymousDonors = allowAnonDonors
                )
            )
        },
        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("حفظ الإعدادات المالية والموازنات", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SystemServerBackupTab(
    settingsViewModel: SettingsViewModel,
    fundInfo: FundInfo,
    appSettings: AppSettings,
    serverConfig: LocalServerConfig,
    autoSync: Boolean,
    isOnline: Boolean,
    pendingSyncCount: Int
) {
    val context = LocalContext.current
    var serverName by remember(serverConfig) { mutableStateOf(serverConfig.serverName) }
    var serverHost by remember(serverConfig) { mutableStateOf(serverConfig.serverHost) }
    var eventLocation by remember(appSettings) { mutableStateOf(appSettings.defaultEventLocation) }
    var strictRoles by remember(appSettings) { mutableStateOf(appSettings.strictRolePermissions) }
    var enableAudit by remember(appSettings) { mutableStateOf(appSettings.enableAuditTrail) }

    var jsonBackupText by remember { mutableStateOf("") }
    var showBackupModal by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Server & Sync Settings
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إعدادات الخادم المحلي والمزامنة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("حالة الاتصال بالخادم", fontSize = 13.sp)
                Switch(checked = isOnline, onCheckedChange = { settingsViewModel.toggleOnline() })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("المزامنة التلقائية فور الاتصال بالشبكة", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = autoSync, onCheckedChange = { settingsViewModel.setAutoSyncOnConnect(it) })
            }
            OutlinedTextField(value = serverName, onValueChange = { serverName = it }, label = { Text("اسم عقدة الخادم") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = serverHost, onValueChange = { serverHost = it }, label = { Text("عنوان الخادم والمنفذ (Host:Port)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        settingsViewModel.updateServerNode(serverName.trim(), serverHost.trim())
                        Toast.makeText(context, "تم تحديث إعدادات الخادم المحلي", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
                ) {
                    Text("حفظ العقدة")
                }
                OutlinedButton(onClick = {
                    settingsViewModel.syncNow()
                    Toast.makeText(context, "تم تشغيل المزامنة الفورية (المعلق: $pendingSyncCount)", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مزامنة الآن ($pendingSyncCount)")
                }
            }
        }
    }

    // Scheduler, Roles & Audit Settings
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إعدادات المواعيد والصلاحيات والرقابة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            OutlinedTextField(
                value = eventLocation,
                onValueChange = { eventLocation = it },
                label = { Text("المقر الافتراضي للاجتماعات والمواعيد") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("تطبيق الصلاحيات الصارمة للأدوار الوظيفية", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = strictRoles, onCheckedChange = { strictRoles = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("تفعيل التوثيق في سجل التدقيق والرقابة", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = enableAudit, onCheckedChange = { enableAudit = it })
            }
            Button(
                onClick = {
                    settingsViewModel.updateAppSettings(
                        appSettings.copy(
                            defaultEventLocation = eventLocation.ifBlank { "مقر الصندوق - قاعة الاجتماعات" },
                            strictRolePermissions = strictRoles,
                            enableAuditTrail = enableAudit
                        )
                    )
                    Toast.makeText(context, "تم حفظ إعدادات المواعيد والرقابة", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("حفظ إعدادات الرقابة والمواعيد")
            }
        }
    }

    // JSON Backup & Restore + Factory Reset
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("النسخ الاحتياطي الكامل واستعادة البيانات (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            Text(
                "إجمالي السجلات المحفوظة محلياً: ${settingsViewModel.getTotalLocalRecordsCount()} سجل (تشمل الشعار والمناطق والقرى وجميع الإعدادات).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        jsonBackupText = settingsViewModel.exportDatabaseJson()
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
                        val json = settingsViewModel.exportDatabaseJson()
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
            OutlinedButton(
                onClick = { showResetConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                modifier = Modifier.fillMaxWidth().testTag("reset_seed_data_button")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("إعادة ضبط المصنع (استعادة البيانات الأولية الافتراضية)")
            }
        }
    }

    if (showBackupModal) {
        Dialog(onDismissRequest = { showBackupModal = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("النسخة الاحتياطية الكاملة للنظام (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "يمكنك نسخ النص أو لصق نسخة احتياطية سابقة لاستعادتها:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonBackupText,
                        onValueChange = { jsonBackupText = it },
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showBackupModal = false }) { Text("إغلاق") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val ok = settingsViewModel.importDatabaseFromJson(jsonBackupText)
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

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("تأكيد إعادة ضبط المصنع", fontWeight = FontWeight.Bold, color = StatusDanger) },
            text = { Text("سيتم مسح التعديلات المحلية واستعادة البيانات الأولية الافتراضية للصندوق والمناطق والقرى. هل تريد المتابعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.resetToDefaultSeedData()
                        showResetConfirm = false
                        Toast.makeText(context, "تمت إعادة ضبط النظام بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) { Text("نعم، استعادة الافتراضي") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("إلغاء") }
            }
        )
    }
}
