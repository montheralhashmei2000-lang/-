package com.example.fund.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fund.model.RegionGroup
import com.example.fund.ui.components.FundLogoImage
import com.example.fund.ui.components.encodeImageUriToBase64
import com.example.fund.ui.viewmodel.SettingsViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    initialTab: Int = 0,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val fundInfo by settingsViewModel.fundInfo.collectAsStateWithLifecycle()
    val regions by settingsViewModel.regions.collectAsStateWithLifecycle()
    val members by settingsViewModel.members.collectAsStateWithLifecycle()
    val beneficiaries by settingsViewModel.beneficiaries.collectAsStateWithLifecycle()
    val appSettings by settingsViewModel.appSettings.collectAsStateWithLifecycle()
    val printSettings by settingsViewModel.printSettings.collectAsStateWithLifecycle()
    val vouchers by settingsViewModel.vouchers.collectAsStateWithLifecycle()
    val rates by settingsViewModel.currencyRates.collectAsStateWithLifecycle()
    val isDark by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle()
    val appFont by settingsViewModel.appFontFamily.collectAsStateWithLifecycle()
    val screenFormat by settingsViewModel.screenFormat.collectAsStateWithLifecycle()
    val serverConfig by settingsViewModel.serverConfig.collectAsStateWithLifecycle()
    val autoSync by settingsViewModel.autoSyncOnConnect.collectAsStateWithLifecycle()
    val isOnline by settingsViewModel.isOnline.collectAsStateWithLifecycle()
    val syncQueue by settingsViewModel.syncQueue.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 5)) }
    val tabTitles = listOf(
        "المظهر والخطوط",
        "المناطق والقرى",
        "الأعضاء والمساعدات",
        "المالية والعملات",
        "الطباعة والسندات",
        "النظام والنسخ"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // Header + Tab Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 12.dp, start = 14.dp, end = 14.dp)
        ) {
            Text(
                "الإعدادات الشاملة لجميع الشاشات والبيانات",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = FundPrimary
            )
            Text(
                "تخصيص هوية الصندوق والشعار، المناطق والقرى، القواعد المحاسبية والرقابية، والنسخ الاحتياطي",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FundPrimary
            ) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> AppearanceAndFontsTab(
                    isDark = isDark,
                    fontScale = fontScale,
                    appFont = appFont,
                    screenFormat = screenFormat,
                    fundInfo = fundInfo,
                    onDarkChange = { settingsViewModel.setDarkModePreference(it) },
                    onFontScaleChange = { settingsViewModel.setFontScalePreference(it) },
                    onAppFontChange = { fontOpt ->
                        settingsViewModel.setAppFontPreference(fontOpt)
                        Toast.makeText(context, "تم تفعيل خط ${fontOpt.title} عبر التطبيق", Toast.LENGTH_SHORT).show()
                    }
                )

                1 -> RegionsAndVillagesTab(
                    regions = regions,
                    membersCountByRegion = { rName -> members.count { it.region == rName } },
                    beneficiariesCountByRegion = { rName -> beneficiaries.count { it.region == rName } },
                    onAddRegion = { name ->
                        if (settingsViewModel.addRegion(name)) {
                            Toast.makeText(context, "تمت إضافة المنطقة: $name", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "اسم المنطقة موجود مسبقاً أو فارغ", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteRegion = { reg ->
                        settingsViewModel.deleteRegion(reg.id)
                        Toast.makeText(context, "تم حذف المنطقة ${reg.name}", Toast.LENGTH_SHORT).show()
                    },
                    onAddVillage = { regId, vName ->
                        if (settingsViewModel.addVillageToRegion(regId, vName)) {
                            Toast.makeText(context, "تمت إضافة القرية ($vName)", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "القرية موجودة مسبقاً أو فارغة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRemoveVillage = { regId, vName ->
                        settingsViewModel.removeVillageFromRegion(regId, vName)
                    }
                )

                2 -> MembersAidsWarehouseTab(
                    appSettings = appSettings,
                    maxAidCeilingYer = fundInfo.maxAidCeilingYer,
                    onSave = { newSettings, newCeiling ->
                        settingsViewModel.updateAppSettings(newSettings)
                        settingsViewModel.updateFundInfo(fundInfo.copy(maxAidCeilingYer = newCeiling))
                        Toast.makeText(context, "تم حفظ إعدادات الأعضاء والمساعدات والمستودع", Toast.LENGTH_SHORT).show()
                    }
                )

                3 -> FinanceCurrenciesBudgetsTab(
                    rates = rates,
                    appSettings = appSettings,
                    onSaveRates = { usd, sar ->
                        settingsViewModel.updateCurrencyRate(com.example.fund.model.CurrencyCode.USD, usd)
                        settingsViewModel.updateCurrencyRate(com.example.fund.model.CurrencyCode.SAR, sar)
                        Toast.makeText(context, "تم تحديث وحفظ أسعار الصرف", Toast.LENGTH_SHORT).show()
                    },
                    onSaveSettings = { newSettings ->
                        settingsViewModel.updateAppSettings(newSettings)
                        Toast.makeText(context, "تم حفظ الإعدادات المالية والموازنات", Toast.LENGTH_SHORT).show()
                    }
                )

                4 -> PrintSettingsTab(
                    fundInfo = fundInfo,
                    printSettings = printSettings,
                    sampleVoucher = vouchers.firstOrNull(),
                    onSave = { updated ->
                        settingsViewModel.updatePrintSettings(updated)
                        Toast.makeText(context, "تم حفظ إعدادات الطباعة والسندات والأرقام بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    onResetDefault = {
                        settingsViewModel.resetPrintSettingsToDefault()
                        Toast.makeText(context, "تمت استعادة القالب الافتراضي للطباعة والأرقام", Toast.LENGTH_SHORT).show()
                    }
                )

                5 -> SystemServerBackupTab(
                    settingsViewModel = settingsViewModel,
                    fundInfo = fundInfo,
                    appSettings = appSettings,
                    serverConfig = serverConfig,
                    autoSync = autoSync,
                    isOnline = isOnline,
                    pendingSyncCount = syncQueue.size
                )
            }
        }
    }
}

@Composable
private fun AppearanceAndFontsTab(
    isDark: Boolean,
    fontScale: Float,
    appFont: com.example.fund.model.AppFontOption,
    screenFormat: com.example.fund.model.DisplayFormatConfig,
    fundInfo: com.example.fund.model.FundInfo,
    onDarkChange: (Boolean) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onAppFontChange: (com.example.fund.model.AppFontOption) -> Unit
) {
    // Appearance & Font Scale Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("المظهر وحجم الخط المحاسبي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الوضع الليلي الداكن (Dark Mode)")
                Switch(
                    checked = isDark,
                    onCheckedChange = onDarkChange,
                    modifier = Modifier.testTag("settings_dark_mode_switch")
                )
            }
            Text("مقياس حجم الخط الفوري في جميع الشاشات:", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0.9f to "0.9x", 1.0f to "1.0x", 1.15f to "1.15x", 1.3f to "1.3x").forEach { (scale, label) ->
                    FilterChip(
                        selected = fontScale == scale,
                        onClick = { onFontScaleChange(scale) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }
    }

    // App-Wide Arabic Font Family Selector + Live Preview
    AppFontSelectorSection(
        selectedFont = appFont,
        screenFormat = screenFormat,
        onSelectFont = onAppFontChange
    )

    // Read-only Fund Identity Summary pointing to dedicated FundInfoScreen (no duplicate form)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, FundGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(FundDeepHeader)
                    .border(1.5.dp, FundGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                FundLogoImage(
                    customLogoBase64 = fundInfo.customLogoBase64,
                    contentDescription = "شعار الصندوق",
                    modifier = Modifier.size(44.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(fundInfo.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FundPrimary)
                Text(
                    "لتعديل بيانات الصندوق الرسمية والشعار والحسابات البنكية، افتح «بيانات الصندوق والشعار» من القائمة الجانبية.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RegionsAndVillagesTab(
    regions: List<RegionGroup>,
    membersCountByRegion: (String) -> Int,
    beneficiariesCountByRegion: (String) -> Int,
    onAddRegion: (String) -> Unit,
    onDeleteRegion: (RegionGroup) -> Unit,
    onAddVillage: (String, String) -> Unit,
    onRemoveVillage: (String, String) -> Unit
) {
    var newRegionName by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, FundPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إدارة المناطق والقرى (كل منطقة تضم عدة قرى)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FundPrimary)
            Text(
                "أضف المناطق الإدارية ثم أضف القرى التابعة لكل منطقة لتظهر تلقائياً عند تسجيل الأعضاء والأسر المستفيدة.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newRegionName,
                    onValueChange = { newRegionName = it },
                    label = { Text("اسم المنطقة الجديدة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newRegionName.isNotBlank()) {
                            onAddRegion(newRegionName)
                            newRegionName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة")
                }
            }
        }
    }

    regions.forEach { region ->
        var newVillageInput by remember(region.id) { mutableStateOf("") }
        val mCount = membersCountByRegion(region.name)
        val bCount = beneficiariesCountByRegion(region.name)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(region.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FundPrimary)
                        Text(
                            "${region.villages.size} قرى • $mCount عضو • $bCount أسرة مستفيدة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (regions.size > 1) {
                        IconButton(onClick = { onDeleteRegion(region) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف المنطقة", tint = StatusDanger)
                        }
                    }
                }

                // Villages Chips Wrap
                if (region.villages.isEmpty()) {
                    Text("لا توجد قرى مضافة بعد في هذه المنطقة.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        region.villages.forEach { village ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(village, fontSize = 12.sp) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onRemoveVillage(region.id, village) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "إزالة القرية", modifier = Modifier.size(14.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                // Add Village Row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newVillageInput,
                        onValueChange = { newVillageInput = it },
                        label = { Text("أضف قرية إلى ${region.name}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            if (newVillageInput.isNotBlank()) {
                                onAddVillage(region.id, newVillageInput)
                                newVillageInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("قرية", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
