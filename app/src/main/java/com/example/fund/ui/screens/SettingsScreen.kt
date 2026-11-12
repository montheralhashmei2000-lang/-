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
import com.example.fund.data.FundRepository
import com.example.fund.model.RegionGroup
import com.example.fund.ui.components.FundLogoImage
import com.example.fund.ui.components.encodeImageUriToBase64
import com.example.ui.theme.*

@Composable
fun SettingsScreen() {
    val repo = FundRepository.getInstance()
    val fundInfo by repo.fundInfo.collectAsState()
    val regions by repo.regions.collectAsState()
    val members by repo.members.collectAsState()
    val beneficiaries by repo.beneficiaries.collectAsState()
    val appSettings by repo.appSettings.collectAsState()
    val rates by repo.currencyRates.collectAsState()
    val isDark by repo.isDarkMode.collectAsState()
    val fontScale by repo.fontScale.collectAsState()
    val serverConfig by repo.serverConfig.collectAsState()
    val autoSync by repo.autoSyncOnConnect.collectAsState()
    val isOnline by repo.isOnline.collectAsState()
    val syncQueue by repo.syncQueue.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "المظهر والهوية",
        "المناطق والقرى",
        "الأعضاء والمساعدات",
        "المالية والعملات",
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
                0 -> GeneralAndIdentityTab(
                    isDark = isDark,
                    fontScale = fontScale,
                    onDarkChange = { repo.setDarkModePreference(it) },
                    onFontScaleChange = { repo.setFontScalePreference(it) },
                    onLogoBase64Selected = { base64 ->
                        repo.updateFundLogo(base64)
                        Toast.makeText(context, "تم تحديث شعار الصندوق بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    onSaveFundInfo = { updated ->
                        repo.updateFundInfo(updated)
                        Toast.makeText(context, "تم حفظ البيانات الرسمية للصندوق", Toast.LENGTH_SHORT).show()
                    },
                    fundInfo = fundInfo
                )

                1 -> RegionsAndVillagesTab(
                    regions = regions,
                    membersCountByRegion = { rName -> members.count { it.region == rName } },
                    beneficiariesCountByRegion = { rName -> beneficiaries.count { it.region == rName } },
                    onAddRegion = { name ->
                        if (repo.addRegion(name)) {
                            Toast.makeText(context, "تمت إضافة المنطقة: $name", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "اسم المنطقة موجود مسبقاً أو فارغ", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteRegion = { reg ->
                        repo.deleteRegion(reg.id)
                        Toast.makeText(context, "تم حذف المنطقة ${reg.name}", Toast.LENGTH_SHORT).show()
                    },
                    onAddVillage = { regId, vName ->
                        if (repo.addVillageToRegion(regId, vName)) {
                            Toast.makeText(context, "تمت إضافة القرية ($vName)", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "القرية موجودة مسبقاً أو فارغة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRemoveVillage = { regId, vName ->
                        repo.removeVillageFromRegion(regId, vName)
                    }
                )

                2 -> MembersAidsWarehouseTab(
                    appSettings = appSettings,
                    maxAidCeilingYer = fundInfo.maxAidCeilingYer,
                    onSave = { newSettings, newCeiling ->
                        repo.updateAppSettings(newSettings)
                        repo.updateFundInfo(fundInfo.copy(maxAidCeilingYer = newCeiling))
                        Toast.makeText(context, "تم حفظ إعدادات الأعضاء والمساعدات والمستودع", Toast.LENGTH_SHORT).show()
                    }
                )

                3 -> FinanceCurrenciesBudgetsTab(
                    rates = rates,
                    appSettings = appSettings,
                    onSaveRates = { usd, sar ->
                        repo.updateCurrencyRate(com.example.fund.model.CurrencyCode.USD, usd)
                        repo.updateCurrencyRate(com.example.fund.model.CurrencyCode.SAR, sar)
                        Toast.makeText(context, "تم تحديث وحفظ أسعار الصرف", Toast.LENGTH_SHORT).show()
                    },
                    onSaveSettings = { newSettings ->
                        repo.updateAppSettings(newSettings)
                        Toast.makeText(context, "تم حفظ الإعدادات المالية والموازنات", Toast.LENGTH_SHORT).show()
                    }
                )

                4 -> SystemServerBackupTab(
                    repo = repo,
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
private fun GeneralAndIdentityTab(
    isDark: Boolean,
    fontScale: Float,
    onDarkChange: (Boolean) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onLogoBase64Selected: (String) -> Unit,
    onSaveFundInfo: (com.example.fund.model.FundInfo) -> Unit,
    fundInfo: com.example.fund.model.FundInfo
) {
    val context = LocalContext.current
    var nameAr by remember(fundInfo) { mutableStateOf(fundInfo.name) }
    var subtitle by remember(fundInfo) { mutableStateOf(fundInfo.subtitle) }
    var license by remember(fundInfo) { mutableStateOf(fundInfo.licenseNo) }
    var address by remember(fundInfo) { mutableStateOf(fundInfo.address) }
    var phone by remember(fundInfo) { mutableStateOf(fundInfo.phone) }
    var email by remember(fundInfo) { mutableStateOf(fundInfo.email) }
    var bankKareemi by remember(fundInfo) { mutableStateOf(fundInfo.bankAccountKareemi) }
    var bankCAC by remember(fundInfo) { mutableStateOf(fundInfo.bankAccountCAC) }
    var fiscalYear by remember(fundInfo) { mutableStateOf(fundInfo.fiscalYear) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = encodeImageUriToBase64(context, uri)
            if (!encoded.isNullOrBlank()) {
                onLogoBase64Selected(encoded)
            } else {
                Toast.makeText(context, "تعذر معالجة الصورة المختارة", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    // Logo Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FundGold.copy(alpha = 0.6f))
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
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(FundDeepHeader)
                    .border(2.dp, FundGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                FundLogoImage(
                    customLogoBase64 = fundInfo.customLogoBase64,
                    contentDescription = "شعار الصندوق",
                    modifier = Modifier.size(50.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("شعار الصندوق الرسمي", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FundPrimary)
                Text("اختر شعار قريتك أو صندوقك ليظهر في الترويسات والسندات وبطاقات العضوية.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تغيير الشعار", fontSize = 12.sp)
                    }
                    if (fundInfo.customLogoBase64.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onLogoBase64Selected("") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("استعادة الافتراضي", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Fund Identity Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("البيانات الرسمية للصندوق والحسابات البنكية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FundPrimary)
            OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("اسم الصندوق / القرية / المنطقة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("الوصف الفرعي") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = license, onValueChange = { license = it }, label = { Text("رقم الترخيص الرسمي") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان والمقر العام") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("الهاتف") }, modifier = Modifier.weight(1.3f), singleLine = true)
                OutlinedTextField(value = fiscalYear, onValueChange = { fiscalYear = it }, label = { Text("السنة المالية") }, modifier = Modifier.weight(0.7f), singleLine = true)
            }
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = bankKareemi, onValueChange = { bankKareemi = it }, label = { Text("حساب بنك الكريمي") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = bankCAC, onValueChange = { bankCAC = it }, label = { Text("حساب كاك بنك CAC") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(
                onClick = {
                    onSaveFundInfo(
                        fundInfo.copy(
                            name = nameAr,
                            subtitle = subtitle,
                            licenseNo = license,
                            address = address,
                            phone = phone,
                            email = email,
                            bankAccountKareemi = bankKareemi,
                            bankAccountCAC = bankCAC,
                            fiscalYear = fiscalYear
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                modifier = Modifier.fillMaxWidth().testTag("save_fund_info_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("حفظ البيانات الرسمية")
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
        colors = CardDefaults.cardColors(containerColor = FundLightSurfaceSecondary),
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
