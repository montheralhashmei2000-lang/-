package com.example.fund.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import com.example.fund.util.OfficialPrintHelper
import com.example.fund.util.QrCodeView
import com.example.ui.theme.*

@Composable
fun VoucherPrintDialog(
    voucher: Voucher,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val fundInfo by repo.fundInfo.collectAsStateWithLifecycle()
    val ps by repo.printSettings.collectAsStateWithLifecycle()

    var showQuickCustomize by remember { mutableStateOf(false) }
    var isMiniPreview by remember { mutableStateOf(false) }

    val isReceipt = voucher.type == VoucherType.RECEIPT
    val accentColor = remember(ps.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(ps.accentColorHex))
        } catch (_: Exception) {
            FundPrimary
        }
    }
    val badgeColor = if (isReceipt) accentColor else Color(0xFFB71C1C)
    val voucherTitle = if (isReceipt) ps.receiptVoucherTitle else ps.paymentVoucherTitle
    val isVoid = voucher.status == VoucherStatus.VOID
    val docFontFamily = printFontFamilyFor(ps.fontFamily)

    // Active signature slots resolved from PrintSettings + Voucher names
    val activeSignatures = remember(ps, voucher) {
        listOfNotNull(
            if (ps.sig1Enabled) SignatureSlotData(
                title = ps.sig1Title,
                name = ps.sig1DefaultName.ifBlank { voucher.receivedOrPaidBy.ifBlank { voucher.partyName } },
                isFinalApprover = false
            ) else null,
            if (ps.sig2Enabled) SignatureSlotData(
                title = ps.sig2Title,
                name = ps.sig2DefaultName.ifBlank { voucher.accountantName },
                isFinalApprover = false
            ) else null,
            if (ps.sig3Enabled) SignatureSlotData(
                title = ps.sig3Title,
                name = ps.sig3DefaultName.ifBlank { voucher.auditorName },
                isFinalApprover = false
            ) else null,
            if (ps.sig4Enabled) SignatureSlotData(
                title = ps.sig4Title,
                name = ps.sig4DefaultName.ifBlank { voucher.managerName },
                isFinalApprover = true
            ) else null
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 1. Top Action Toolbar (Print, Share PDF, Toggle Mini Preview, Customize)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                OfficialPrintHelper.printVoucherPdf(context, voucher, fundInfo, ps)
                                Toast.makeText(context, "جاري فتح نافذة الطباعة وحفظ PDF الرسمي...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "طباعة", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("طباعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val shared = OfficialPrintHelper.generateAndShareVoucherPdf(context, voucher, fundInfo, ps)
                                if (!shared) {
                                    val shareText = """
                                        ${fundInfo.name}
                                        $voucherTitle رقم: ${voucher.voucherNo}
                                        التاريخ: ${voucher.date}
                                        ${if (isReceipt) ps.receiptPartyLabel else ps.paymentPartyLabel} ${voucher.partyName}
                                        المبلغ: ${"%,d".format(voucher.amountYer.toLong())} ﷼ (${voucher.amountWords})
                                        ${ps.descriptionLabel} ${voucher.description}
                                        القيد المحاسبي: ${voucher.linkedJournalEntryId}
                                    """.trimIndent()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "مشاركة بيانات السند"))
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "مشاركة PDF", modifier = Modifier.size(16.dp), tint = StatusDanger)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        FilterChip(
                            selected = isMiniPreview,
                            onClick = { isMiniPreview = !isMiniPreview },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isMiniPreview) Icons.Default.Fullscreen else Icons.Default.Preview,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = { Text(if (isMiniPreview) "معاينة كاملة" else "معاينة مصغرة", fontSize = 11.sp) }
                        )

                        FilledTonalIconButton(
                            onClick = { showQuickCustomize = !showQuickCustomize },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "تخصيص سريع للقالب",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Active Print Profile Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${ps.paperSize.title} • خط ${ps.fontFamily.title} • ${activeSignatures.size} توقيع",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Collapsible Quick Customizer Panel
                AnimatedVisibility(visible = showQuickCustomize) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "تخصيص فوري لمعاينة الطباعة (وللإعدادات الكاملة افتح تبويب الطباعة في الإعدادات):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PrintFontOption.entries.forEach { fOpt ->
                                    FilterChip(
                                        selected = ps.fontFamily == fOpt,
                                        onClick = { repo.updatePrintSettings(ps.copy(fontFamily = fOpt)) },
                                        label = {
                                            Text(
                                                fOpt.title.substringBefore(" "),
                                                fontSize = 11.sp,
                                                fontFamily = printFontFamilyFor(fOpt)
                                            )
                                        }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = ps.showSignaturesSection,
                                    onClick = { repo.updatePrintSettings(ps.copy(showSignaturesSection = !ps.showSignaturesSection)) },
                                    label = { Text("التوقيعات", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = ps.showOfficialStampBox,
                                    onClick = { repo.updatePrintSettings(ps.copy(showOfficialStampBox = !ps.showOfficialStampBox)) },
                                    label = { Text("الختم الرسمي", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = ps.showQrVerification,
                                    onClick = { repo.updatePrintSettings(ps.copy(showQrVerification = !ps.showQrVerification)) },
                                    label = { Text("رمز QR", fontSize = 11.sp) }
                                )
                                PrintBorderStyle.entries.forEach { bStyle ->
                                    FilterChip(
                                        selected = ps.borderStyle == bStyle,
                                        onClick = { repo.updatePrintSettings(ps.copy(borderStyle = bStyle)) },
                                        label = { Text(bStyle.title.substringBefore(" "), fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Scrollable Official Voucher Sheet Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE7EDE5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val sheetWidthModifier = when {
                        isMiniPreview -> Modifier.widthIn(max = 310.dp).fillMaxWidth()
                        ps.paperSize == PrintPaperSize.THERMAL_80 -> Modifier.widthIn(max = 320.dp).fillMaxWidth()
                        else -> Modifier.fillMaxWidth()
                    }

                    val outerBorderModifier = when (ps.borderStyle) {
                        PrintBorderStyle.DOUBLE_CLASSIC -> Modifier
                            .border(2.5.dp, accentColor, RoundedCornerShape(10.dp))
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .padding(4.dp)
                            .border(1.2.dp, FundGold, RoundedCornerShape(7.dp))
                            .padding(14.dp)

                        PrintBorderStyle.MODERN_MINIMAL -> Modifier
                            .border(1.dp, Color(0xFFD0DDD0), RoundedCornerShape(12.dp))
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp)

                        PrintBorderStyle.OFFICIAL_BOXED -> Modifier
                            .border(2.dp, Color(0xFF212121), RoundedCornerShape(4.dp))
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .padding(3.dp)
                            .border(1.dp, Color(0xFF212121), RoundedCornerShape(2.dp))
                            .padding(14.dp)
                    }

                    Box(
                        modifier = sheetWidthModifier.then(outerBorderModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (ps.borderStyle == PrintBorderStyle.MODERN_MINIMAL) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .background(accentColor, RoundedCornerShape(3.dp))
                                )
                                Spacer(Modifier.height(10.dp))
                            }

                            // A. Clean Executive Two-Tier Header (No cramped columns!)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (ps.showCountryHeader && ps.countryHeaderText.isNotBlank()) {
                                        Text(
                                            text = ps.countryHeaderText,
                                            fontFamily = docFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (ps.headerFontSizeSp - 3).coerceAtLeast(10).sp,
                                            color = FundDeepHeader
                                        )
                                    }
                                    Text(
                                        text = fundInfo.name,
                                        fontFamily = docFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = ps.headerFontSizeSp.sp,
                                        color = accentColor
                                    )
                                    if (ps.showDepartmentHeader && ps.departmentHeaderText.isNotBlank()) {
                                        Text(
                                            text = ps.departmentHeaderText,
                                            fontFamily = docFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = (ps.bodyFontSizeSp - 1).coerceAtLeast(10).sp,
                                            color = Color(0xFF44553F)
                                        )
                                    }
                                    if (ps.showLicenseAndAddress) {
                                        Text(
                                            text = "${fundInfo.licenseNo} • ${fundInfo.address}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF5F6D59),
                                            maxLines = 2
                                        )
                                    }
                                }

                                if (ps.showLogoInPrint) {
                                    Spacer(Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF5FAF6))
                                            .border(2.dp, FundGold, CircleShape)
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        FundLogoImage(
                                            customLogoBase64 = fundInfo.customLogoBase64,
                                            contentDescription = "شعار الصندوق",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                thickness = 1.5.dp,
                                color = Color(0xFFD8E4D2)
                            )

                            // B. Voucher Title Ribbon + Serial + Amount Card
                            Surface(
                                color = Color(0xFFF6FAF5),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFD8E4D2)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(2.dp, badgeColor)
                                        ) {
                                            Text(
                                                text = voucherTitle,
                                                fontFamily = docFontFamily,
                                                fontWeight = FontWeight.Black,
                                                fontSize = (ps.headerFontSizeSp + 1).sp,
                                                color = badgeColor,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                            )
                                        }

                                        // Highlighted Amount Pill
                                        Surface(
                                            color = Color(0xFFFFF9C4),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.5.dp, FundGoldDark)
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = "# %,d # ﷼".format(voucher.amountYer.toLong()),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = (ps.headerFontSizeSp).sp,
                                                    color = Color(0xFF003300),
                                                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                                )
                                                if (ps.showCurrencyDetails && voucher.currency != CurrencyCode.YER) {
                                                    Text(
                                                        text = "${voucher.amountOriginal} ${voucher.currency.name} @ %,.0f".format(voucher.exchangeRate),
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF44553F),
                                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Metadata chips line
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "رقم السند: ${voucher.voucherNo}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = badgeColor
                                        )
                                        Text(
                                            text = "التاريخ: ${voucher.date}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF333333)
                                        )
                                        if (ps.showJournalReference && voucher.linkedJournalEntryId.isNotBlank()) {
                                            Text(
                                                text = "القيد: ${voucher.linkedJournalEntryId}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // C. Voucher Structured Body Ledger Card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFD8E4D2), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                VoucherFieldRow(
                                    label = if (isReceipt) ps.receiptPartyLabel else ps.paymentPartyLabel,
                                    value = voucher.partyName,
                                    accentColor = accentColor,
                                    fontFamily = docFontFamily,
                                    bodySizeSp = ps.bodyFontSizeSp,
                                    isBold = true
                                )
                                HorizontalDivider(color = Color(0xFFE6EEE3))
                                VoucherFieldRow(
                                    label = ps.amountWordsLabel,
                                    value = voucher.amountWords,
                                    accentColor = accentColor,
                                    fontFamily = docFontFamily,
                                    bodySizeSp = ps.bodyFontSizeSp,
                                    isHighlight = true
                                )
                                HorizontalDivider(color = Color(0xFFE6EEE3))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ps.paymentMethodLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                        Text(voucher.paymentMethod.title, fontSize = ps.bodyFontSizeSp.sp, fontFamily = docFontFamily, color = Color(0xFF14210F))
                                    }
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(ps.accountLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                        Text(voucher.accountName, fontSize = ps.bodyFontSizeSp.sp, fontFamily = docFontFamily, color = Color(0xFF14210F))
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFE6EEE3))
                                VoucherFieldRow(
                                    label = ps.descriptionLabel,
                                    value = voucher.description,
                                    accentColor = accentColor,
                                    fontFamily = docFontFamily,
                                    bodySizeSp = ps.bodyFontSizeSp
                                )
                            }

                            // D. Responsive Signature Fields Grid (2x2 on mobile / up to 4 cols on wide screens)
                            if (ps.showSignaturesSection && activeSignatures.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val columns = if (maxWidth < 500.dp || ps.paperSize == PrintPaperSize.THERMAL_80) {
                                        if (activeSignatures.size == 1) 1 else 2
                                    } else {
                                        activeSignatures.size.coerceIn(1, 4)
                                    }
                                    val rows = activeSignatures.chunked(columns)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rows.forEach { rowSlots ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowSlots.forEach { slot ->
                                                    SignatureCardBox(
                                                        slot = slot,
                                                        accentColor = accentColor,
                                                        fontFamily = docFontFamily,
                                                        fontSizeSp = ps.signatureFontSizeSp,
                                                        showSignatureLine = ps.showSignatureLine,
                                                        showOfficialStampBox = ps.showOfficialStampBox,
                                                        officialStampLabel = ps.officialStampLabel,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                repeat(columns - rowSlots.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFD8E4D2))
                            Spacer(modifier = Modifier.height(10.dp))

                            // E. Dedicated QR & Footer Strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (ps.showQrVerification) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        QrCodeView(
                                            content = "SOCIALFUND|${voucher.voucherNo}|${voucher.amountYer.toLong()}|${voucher.date}|${voucher.status.name}",
                                            sizeDp = 72.dp,
                                            dotColor = accentColor
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = ps.qrCaptionText,
                                            fontSize = 9.sp,
                                            color = Color(0xFF44553F),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (ps.footerNote.isNotBlank()) {
                                        Text(
                                            text = ps.footerNote,
                                            fontFamily = docFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF14210F)
                                        )
                                    }
                                    if (ps.footerSubNote.isNotBlank()) {
                                        Text(
                                            text = ps.footerSubNote,
                                            fontFamily = docFontFamily,
                                            fontSize = 9.sp,
                                            color = Color(0xFF44553F)
                                        )
                                    }
                                    if (ps.showFundContactInFooter) {
                                        Text(
                                            text = "${fundInfo.name} | هاتف: ${fundInfo.phone} | ${fundInfo.email}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF6B7966)
                                        )
                                    }
                                }
                            }
                        }

                        // Void Watermark if Voided
                        if (isVoid) {
                            Box(
                                modifier = Modifier
                                    .rotate(-30f)
                                    .border(5.dp, Color(0xFFD32F2F).copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "سند ملغي - VOID",
                                    fontFamily = docFontFamily,
                                    color = Color(0xFFD32F2F).copy(alpha = 0.75f),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SignatureSlotData(
    val title: String,
    val name: String,
    val isFinalApprover: Boolean
)

@Composable
private fun VoucherFieldRow(
    label: String,
    value: String,
    accentColor: Color,
    fontFamily: FontFamily,
    bodySizeSp: Int,
    isBold: Boolean = false,
    isHighlight: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlight) Color(0xFFFFFDF0) else Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Text(
            text = value,
            fontFamily = fontFamily,
            fontSize = if (isHighlight || isBold) (bodySizeSp + 1).sp else bodySizeSp.sp,
            fontWeight = if (isBold || isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) Color(0xFF003300) else Color(0xFF14210F),
            lineHeight = (bodySizeSp + 6).sp
        )
    }
}

@Composable
private fun SignatureCardBox(
    slot: SignatureSlotData,
    accentColor: Color,
    fontFamily: FontFamily,
    fontSizeSp: Int,
    showSignatureLine: Boolean,
    showOfficialStampBox: Boolean,
    officialStampLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFFD2DFCD), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFAFCFA))
    ) {
        // Role Header Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor.copy(alpha = 0.09f))
                .padding(horizontal = 6.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = slot.title,
                fontFamily = fontFamily,
                fontSize = fontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(color = Color(0xFFD8E4D2))

        // Signing Area + Optional Stamp Seal + Signature Dotted Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (slot.isFinalApprover && showOfficialStampBox) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(42.dp)
                        .rotate(-12f)
                        .drawBehind {
                            drawCircle(
                                color = accentColor.copy(alpha = 0.45f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = officialStampLabel,
                        fontSize = 7.sp,
                        lineHeight = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showSignatureLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(1.dp)
                            .drawBehind {
                                drawLine(
                                    color = Color(0xFF91A38A),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                                )
                            }
                    )
                }
                Text(
                    text = slot.name,
                    fontFamily = fontFamily,
                    fontSize = (fontSizeSp - 1).coerceAtLeast(9).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF283724),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
