package com.example.fund.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.model.*
import com.example.fund.ui.components.DisplayFormatSettingsCard
import com.example.fund.ui.components.VoucherPrintDialog
import com.example.fund.util.FormatUtils
import com.example.ui.theme.*

@Composable
fun PrintSettingsTab(
    fundInfo: FundInfo,
    printSettings: PrintSettings,
    sampleVoucher: Voucher?,
    onSave: (PrintSettings) -> Unit,
    onResetDefault: () -> Unit
) {
    var draft by remember(printSettings) { mutableStateOf(printSettings) }
    var showFullPreviewDialog by remember { mutableStateOf(false) }
    var activeFormatProfile by remember { mutableIntStateOf(0) } // 0 = Screen, 1 = Print/PDF

    val accentColors = listOf(
        "#1B5E20" to "أخضر الصندوق",
        "#003300" to "أخضر ملكي داكن",
        "#0D47A1" to "أزرق رسمي",
        "#4E342E" to "بني كلاسيكي",
        "#263238" to "فحمي مالي",
        "#6A1B9A" to "بنفسجي وقور"
    )
    val parsedAccent = remember(draft.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(draft.accentColorHex))
        } catch (_: Exception) {
            FundPrimary
        }
    }
    val previewFont = printFontFamilyFor(draft.fontFamily)

    // 0. Live Mini Preview + Full Preview Button
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, parsedAccent.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "معاينة فورية لقالب السندات والتقارير",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = parsedAccent
                )
                Button(
                    onClick = {
                        onSave(draft)
                        showFullPreviewDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = parsedAccent),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("preview_print_template_button")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("معاينة سند كامل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Mini Stationery Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, parsedAccent, RoundedCornerShape(8.dp))
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (draft.showCountryHeader && draft.countryHeaderText.isNotBlank()) {
                    Text(
                        draft.countryHeaderText,
                        fontFamily = previewFont,
                        fontSize = (draft.headerFontSizeSp - 4).coerceAtLeast(9).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003300)
                    )
                }
                Text(
                    fundInfo.name,
                    fontFamily = previewFont,
                    fontSize = (draft.headerFontSizeSp - 1).coerceAtLeast(11).sp,
                    fontWeight = FontWeight.Black,
                    color = parsedAccent
                )
                if (draft.showDepartmentHeader && draft.departmentHeaderText.isNotBlank()) {
                    Text(draft.departmentHeaderText, fontFamily = previewFont, fontSize = 10.sp, color = Color.DarkGray)
                }
                Surface(
                    color = parsedAccent.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, parsedAccent)
                ) {
                    Text(
                        draft.receiptVoucherTitle,
                        fontFamily = previewFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = parsedAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Text(
                    "سند رقم: ${FormatUtils.formatCodeOrNumber("V-2026-0042", draft.printFormat)} • ${draft.receiptPartyLabel} نموذج تجريبي • ${FormatUtils.formatAmount(50000.0, draft.printFormat)}",
                    fontFamily = previewFont,
                    fontSize = draft.bodyFontSizeSp.coerceAtMost(14).sp,
                    color = Color(0xFF14210F)
                )
                Text(
                    "تاريخ الطباعة: ${FormatUtils.formatDate("2026-11-12", draft.printFormat)}",
                    fontFamily = previewFont,
                    fontSize = 10.sp,
                    color = Color.DarkGray
                )
                if (draft.showSignaturesSection) {
                    val activeTitles = listOfNotNull(
                        if (draft.sig1Enabled) draft.sig1Title else null,
                        if (draft.sig2Enabled) draft.sig2Title else null,
                        if (draft.sig3Enabled) draft.sig3Title else null,
                        if (draft.sig4Enabled) draft.sig4Title else null
                    )
                    Text(
                        "التوقيعات المفعلة (${activeTitles.size}): ${activeTitles.joinToString(" • ")}",
                        fontFamily = previewFont,
                        fontSize = 9.sp,
                        color = parsedAccent
                    )
                }
                if (draft.footerNote.isNotBlank()) {
                    HorizontalDivider(color = Color(0xFFD8E4D2))
                    Text(draft.footerNote, fontFamily = previewFont, fontSize = 9.sp, color = Color.Gray)
                }
            }

            // Live Side-by-Side Comparison: Screen Format vs Print Format
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, parsedAccent.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dual_format_live_preview_card")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "معاينة حية مقارنة (الشاشات مقابل المطبوعات و PDF):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = parsedAccent
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, FundPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("على الشاشة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
                                Text(
                                    FormatUtils.formatAmount(1250450.75, draft.screenFormat),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    "سند: ${FormatUtils.formatCodeOrNumber("V-2026-0042", draft.screenFormat)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FundPrimary,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    FormatUtils.formatDate("2026-11-12 14:35", draft.screenFormat),
                                    fontSize = 10.sp,
                                    color = FundGoldDark
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, parsedAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("في المطبوعة / PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = parsedAccent)
                                Text(
                                    FormatUtils.formatAmount(1250450.75, draft.printFormat),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    "سند: ${FormatUtils.formatCodeOrNumber("V-2026-0042", draft.printFormat)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = parsedAccent,
                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    FormatUtils.formatDate("2026-11-12 14:35", draft.printFormat),
                                    fontSize = 10.sp,
                                    color = FundGoldDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Headers & Letterhead Settings
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("1. رؤوس النماذج والترويسة الرسمية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = parsedAccent)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار ترويسة الدولة / المرجعية العليا", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = draft.showCountryHeader, onCheckedChange = { draft = draft.copy(showCountryHeader = it) })
            }
            if (draft.showCountryHeader) {
                OutlinedTextField(
                    value = draft.countryHeaderText,
                    onValueChange = { draft = draft.copy(countryHeaderText = it) },
                    label = { Text("نص الترويسة العليا (مثلاً: الجمهورية اليمنية / اسم المرجعية)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار اسم الإدارة / القسم المالي", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = draft.showDepartmentHeader, onCheckedChange = { draft = draft.copy(showDepartmentHeader = it) })
            }
            if (draft.showDepartmentHeader) {
                OutlinedTextField(
                    value = draft.departmentHeaderText,
                    onValueChange = { draft = draft.copy(departmentHeaderText = it) },
                    label = { Text("مسمى الإدارة في الترويسة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("طباعة شعار الصندوق في الترويسة", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = draft.showLogoInPrint, onCheckedChange = { draft = draft.copy(showLogoInPrint = it) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار رقم الترخيص والعنوان أسفل الترويسة", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = draft.showLicenseAndAddress, onCheckedChange = { draft = draft.copy(showLicenseAndAddress = it) })
            }
        }
    }

    // 2. Document Titles & Field Labels
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("2. عناوين السندات والتقارير وتسميات الحقول", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = parsedAccent)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.receiptVoucherTitle,
                    onValueChange = { draft = draft.copy(receiptVoucherTitle = it) },
                    label = { Text("عنوان سند القبض") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.paymentVoucherTitle,
                    onValueChange = { draft = draft.copy(paymentVoucherTitle = it) },
                    label = { Text("عنوان سند الصرف") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = draft.journalVoucherTitle,
                onValueChange = { draft = draft.copy(journalVoucherTitle = it) },
                label = { Text("عنوان سند القيد المحاسبي") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = draft.financialStatementSubtitle,
                onValueChange = { draft = draft.copy(financialStatementSubtitle = it) },
                label = { Text("العنوان الفرعي للقوائم المالية الختامية") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = draft.analyticalReportSubtitle,
                onValueChange = { draft = draft.copy(analyticalReportSubtitle = it) },
                label = { Text("عنوان التقرير المالي والإحصائي") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("تسميات الحقول داخل السندات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = parsedAccent)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.receiptPartyLabel,
                    onValueChange = { draft = draft.copy(receiptPartyLabel = it) },
                    label = { Text("تسمية دافع القبض") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.paymentPartyLabel,
                    onValueChange = { draft = draft.copy(paymentPartyLabel = it) },
                    label = { Text("تسمية مستفيد الصرف") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.amountWordsLabel,
                    onValueChange = { draft = draft.copy(amountWordsLabel = it) },
                    label = { Text("تسمية التفقيط") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.descriptionLabel,
                    onValueChange = { draft = draft.copy(descriptionLabel = it) },
                    label = { Text("تسمية البيان") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }

    // 3. Fonts, Sizes, Paper Size, Border Style & Accent Color
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("3. الخطوط والأحجام ونمط الإطار وحجم الورق", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = parsedAccent)

            Text("نوع الخط العربي للمستندات والطباعة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrintFontOption.entries.forEach { fOpt ->
                    FilterChip(
                        selected = draft.fontFamily == fOpt,
                        onClick = { draft = draft.copy(fontFamily = fOpt) },
                        label = {
                            Text(
                                fOpt.title,
                                fontSize = 12.sp,
                                fontFamily = printFontFamilyFor(fOpt),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            // Font sizes stepper row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontSizeStepperCard(
                    title = "حجم العنوان",
                    valueSp = draft.headerFontSizeSp,
                    min = 12,
                    max = 22,
                    onChange = { draft = draft.copy(headerFontSizeSp = it) },
                    modifier = Modifier.weight(1f)
                )
                FontSizeStepperCard(
                    title = "حجم المتن",
                    valueSp = draft.bodyFontSizeSp,
                    min = 10,
                    max = 18,
                    onChange = { draft = draft.copy(bodyFontSizeSp = it) },
                    modifier = Modifier.weight(1f)
                )
                FontSizeStepperCard(
                    title = "حجم التوقيع",
                    valueSp = draft.signatureFontSizeSp,
                    min = 9,
                    max = 15,
                    onChange = { draft = draft.copy(signatureFontSizeSp = it) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("حجم الورق المستهدف:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrintPaperSize.entries.forEach { pSize ->
                    FilterChip(
                        selected = draft.paperSize == pSize,
                        onClick = { draft = draft.copy(paperSize = pSize) },
                        label = { Text(pSize.title, fontSize = 11.sp) }
                    )
                }
            }

            Text("نمط إطار الورقة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrintBorderStyle.entries.forEach { bStyle ->
                    FilterChip(
                        selected = draft.borderStyle == bStyle,
                        onClick = { draft = draft.copy(borderStyle = bStyle) },
                        label = { Text(bStyle.title, fontSize = 11.sp) }
                    )
                }
            }

            Text("اللون الرئيسي للنماذج والأختام:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                accentColors.forEach { (hex, label) ->
                    val swatch = Color(android.graphics.Color.parseColor(hex))
                    val selected = draft.accentColorHex.equals(hex, ignoreCase = true)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { draft = draft.copy(accentColorHex = hex) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(if (selected) 3.dp else 1.dp, if (selected) FundGold else MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(label, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }

    // 4. Signatures & Official Stamp Settings
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("4. حقول التوقيعات والختم الرسمي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = parsedAccent)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار قسم التوقيعات في السندات والتقارير", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(checked = draft.showSignaturesSection, onCheckedChange = { draft = draft.copy(showSignaturesSection = it) })
            }

            if (draft.showSignaturesSection) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("إظهار خط التوقيع المتقطع داخل كل خانة", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(checked = draft.showSignatureLine, onCheckedChange = { draft = draft.copy(showSignatureLine = it) })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("إظهار مربع الختم الرسمي عند المعتمد النهائي", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(checked = draft.showOfficialStampBox, onCheckedChange = { draft = draft.copy(showOfficialStampBox = it) })
                }

                if (draft.showOfficialStampBox) {
                    OutlinedTextField(
                        value = draft.officialStampLabel,
                        onValueChange = { draft = draft.copy(officialStampLabel = it) },
                        label = { Text("نص الختم الرسمي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                SignatureConfigCard(
                    slotIndex = 1,
                    enabled = draft.sig1Enabled,
                    title = draft.sig1Title,
                    defaultName = draft.sig1DefaultName,
                    namePlaceholderHint = "اتركه فارغاً ليأخذ اسم الطرف في السند تلقائياً",
                    onEnabledChange = { draft = draft.copy(sig1Enabled = it) },
                    onTitleChange = { draft = draft.copy(sig1Title = it) },
                    onNameChange = { draft = draft.copy(sig1DefaultName = it) }
                )
                SignatureConfigCard(
                    slotIndex = 2,
                    enabled = draft.sig2Enabled,
                    title = draft.sig2Title,
                    defaultName = draft.sig2DefaultName,
                    onEnabledChange = { draft = draft.copy(sig2Enabled = it) },
                    onTitleChange = { draft = draft.copy(sig2Title = it) },
                    onNameChange = { draft = draft.copy(sig2DefaultName = it) }
                )
                SignatureConfigCard(
                    slotIndex = 3,
                    enabled = draft.sig3Enabled,
                    title = draft.sig3Title,
                    defaultName = draft.sig3DefaultName,
                    onEnabledChange = { draft = draft.copy(sig3Enabled = it) },
                    onTitleChange = { draft = draft.copy(sig3Title = it) },
                    onNameChange = { draft = draft.copy(sig3DefaultName = it) }
                )
                SignatureConfigCard(
                    slotIndex = 4,
                    enabled = draft.sig4Enabled,
                    title = draft.sig4Title,
                    defaultName = draft.sig4DefaultName,
                    onEnabledChange = { draft = draft.copy(sig4Enabled = it) },
                    onTitleChange = { draft = draft.copy(sig4Title = it) },
                    onNameChange = { draft = draft.copy(sig4DefaultName = it) }
                )
            }
        }
    }

    // 5. Footer & Verification Options
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("5. التذييل ورمز التحقق QR والتفاصيل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = parsedAccent)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار رمز التحقق الرقمي (QR)", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = draft.showQrVerification, onCheckedChange = { draft = draft.copy(showQrVerification = it) })
            }
            if (draft.showQrVerification) {
                OutlinedTextField(
                    value = draft.qrCaptionText,
                    onValueChange = { draft = draft.copy(qrCaptionText = it) },
                    label = { Text("تسمية أسفل رمز QR") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار رقم القيد المحاسبي الآلي في السند", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = draft.showJournalReference, onCheckedChange = { draft = draft.copy(showJournalReference = it) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار تفاصيل العملة وسعر الصرف", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = draft.showCurrencyDetails, onCheckedChange = { draft = draft.copy(showCurrencyDetails = it) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إظهار بيانات التواصل في التذييل", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = draft.showFundContactInFooter, onCheckedChange = { draft = draft.copy(showFundContactInFooter = it) })
            }

            OutlinedTextField(
                value = draft.footerNote,
                onValueChange = { draft = draft.copy(footerNote = it) },
                label = { Text("نص التذييل الرئيسي (الملاحظة القانونية)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.footerSubNote,
                onValueChange = { draft = draft.copy(footerSubNote = it) },
                label = { Text("نص التذييل الإضافي / رسالة الشكر") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 6. Unified Screen & Print Numbers, Currency & Date Formatting
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, parsedAccent.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "6. تنسيق الأرقام والعملات والتاريخ (الشاشات والمطبوعات)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = parsedAccent
            )
            Text(
                "اضبط تنسيق عرض الشاشات وتنسيق المطبوعات وملفات PDF بشكل مستقل أو موحّد من مكان واحد.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFormatProfile == 0,
                    onClick = { activeFormatProfile = 0 },
                    leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("تنسيق الشاشات", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f).testTag("profile_tab_screen_format")
                )
                FilterChip(
                    selected = activeFormatProfile == 1,
                    onClick = { activeFormatProfile = 1 },
                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("تنسيق المطبوعات و PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f).testTag("profile_tab_print_format")
                )
            }

            OutlinedButton(
                onClick = {
                    draft = if (activeFormatProfile == 0) {
                        draft.copy(screenFormat = draft.printFormat)
                    } else {
                        draft.copy(printFormat = draft.screenFormat)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (activeFormatProfile == 0) "نسخ إعدادات المطبوعة إلى الشاشات"
                    else "نسخ إعدادات الشاشات إلى المطبوعات",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeFormatProfile == 0) {
                DisplayFormatSettingsCard(
                    title = "ملف تنسيق الشاشات وحقول الإدخال",
                    subtitle = "يطبّق على لوحة المعلومات، الجداول، السندات، القيود، وحقول الإدخال",
                    config = draft.screenFormat,
                    onConfigChange = { draft = draft.copy(screenFormat = it) },
                    accentColor = FundPrimary,
                    testTagPrefix = "screen_format"
                )
            } else {
                DisplayFormatSettingsCard(
                    title = "ملف تنسيق المطبوعات وملفات PDF",
                    subtitle = "مستقل عن تنسيق الشاشات؛ يطبّق على السندات المطبوعة وكشوفات PDF",
                    config = draft.printFormat,
                    onConfigChange = { draft = draft.copy(printFormat = it) },
                    accentColor = parsedAccent,
                    testTagPrefix = "print_format"
                )
            }
        }
    }

    // Save & Reset Buttons
    Button(
        onClick = { onSave(draft) },
        colors = ButtonDefaults.buttonColors(containerColor = parsedAccent),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("save_print_settings_button")
    ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("حفظ إعدادات الطباعة والسندات", fontWeight = FontWeight.Bold)
    }

    OutlinedButton(
        onClick = {
            onResetDefault()
            draft = PrintSettings()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger)
    ) {
        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("استعادة القالب الافتراضي للطباعة")
    }

    if (showFullPreviewDialog) {
        val previewVoucher = sampleVoucher ?: Voucher(
            voucherNo = "V-2026-001",
            type = VoucherType.RECEIPT,
            status = VoucherStatus.POSTED,
            partyName = "أحمد محمد صالح (نموذج معاينة)",
            amountOriginal = 50000.0,
            currency = CurrencyCode.YER,
            exchangeRate = 1.0,
            amountYer = 50000.0,
            amountWords = "فقط خمسون ألف ريال يمني لا غير",
            paymentMethod = PaymentMethod.CASH,
            accountCode = "1100",
            accountName = "الخزينة النقدية الرئيسية",
            counterpartAccountCode = "4100",
            counterpartAccountName = "إيرادات الاشتراكات",
            description = "معاينة تجريبية لإعدادات الترويسة والتذييل والخطوط وحقول التوقيعات",
            date = "2026-11-12",
            linkedJournalEntryId = "JE-000001"
        )
        VoucherPrintDialog(
            voucher = previewVoucher,
            onDismiss = { showFullPreviewDialog = false }
        )
    }
}

@Composable
private fun FontSizeStepperCard(
    title: String,
    valueSp: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { if (valueSp > min) onChange(valueSp - 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "تصغير", modifier = Modifier.size(14.dp))
                }
                Text("$valueSp", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                IconButton(
                    onClick = { if (valueSp < max) onChange(valueSp + 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "تكبير", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SignatureConfigCard(
    slotIndex: Int,
    enabled: Boolean,
    title: String,
    defaultName: String,
    namePlaceholderHint: String = "الاسم المطبوع أسفل التوقيع",
    onEnabledChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onNameChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (enabled) FundPrimary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "خانة التوقيع #$slotIndex",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (enabled) FundPrimary else Color.Gray
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            if (enabled) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("الصفة / المسمى الوظيفي") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = defaultName,
                    onValueChange = onNameChange,
                    label = { Text(namePlaceholderHint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}
