package com.example.fund.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.R
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

object OfficialPrintHelper {

    private val fontBase64Cache = ConcurrentHashMap<PrintFontOption, String>()
    private var defaultLogoBase64Cache: String? = null

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun resolveFontFaceCss(context: Context, fontOption: PrintFontOption): String {
        val b64 = fontBase64Cache.getOrPut(fontOption) {
            try {
                val resId = when (fontOption) {
                    PrintFontOption.AMIRI -> R.font.amiri
                    PrintFontOption.TAJAWAL -> R.font.tajawal
                    PrintFontOption.CAIRO -> R.font.cairo
                    PrintFontOption.NASKH -> R.font.noto_naskh_arabic
                }
                context.resources.openRawResource(resId).use { input ->
                    Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
                }
            } catch (_: Exception) {
                ""
            }
        }
        return if (b64.isNotBlank()) {
            """
            @font-face {
                font-family: 'CustomPrintFont';
                src: url('data:font/ttf;base64,$b64') format('truetype');
                font-weight: normal;
                font-style: normal;
            }
            @font-face {
                font-family: 'CustomPrintFont';
                src: url('data:font/ttf;base64,$b64') format('truetype');
                font-weight: bold;
                font-style: normal;
            }
            """.trimIndent()
        } else ""
    }

    private fun resolveLogoBase64(context: Context, fundInfo: FundInfo): String {
        if (fundInfo.customLogoBase64.isNotBlank()) return fundInfo.customLogoBase64
        defaultLogoBase64Cache?.let { return it }
        return try {
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.ic_fund_logo)
            if (bmp != null) {
                val out = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                defaultLogoBase64Cache = encoded
                encoded
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun buildLogoHtml(context: Context, fundInfo: FundInfo, ps: PrintSettings, sizePx: Int = 58): String {
        if (!ps.showLogoInPrint) return ""
        val b64 = resolveLogoBase64(context, fundInfo)
        return if (b64.isNotBlank()) {
            """<img src="data:image/png;base64,$b64" style="width:${sizePx}px;height:${sizePx}px;border-radius:50%;object-fit:cover;border:2px solid #F9A825;background:#FFFFFF;" />"""
        } else ""
    }

    private fun buildSheetWrapperStyles(ps: PrintSettings, accent: String): Pair<String, String> {
        val pageWidth = when (ps.paperSize) {
            PrintPaperSize.A4 -> "100%"
            PrintPaperSize.A5 -> "680px"
            PrintPaperSize.THERMAL_80 -> "310px"
        }
        val outerCss = when (ps.borderStyle) {
            PrintBorderStyle.DOUBLE_CLASSIC -> "max-width:$pageWidth;margin:0 auto;border:3px solid $accent;padding:5px;border-radius:12px;background:#FFFFFF;"
            PrintBorderStyle.MODERN_MINIMAL -> "max-width:$pageWidth;margin:0 auto;border:1px solid #D8E4D2;border-top:6px solid $accent;padding:0;border-radius:12px;background:#FFFFFF;"
            PrintBorderStyle.OFFICIAL_BOXED -> "max-width:$pageWidth;margin:0 auto;border:2.5px solid #212121;padding:3px;border-radius:4px;background:#FFFFFF;"
        }
        val innerCss = when (ps.borderStyle) {
            PrintBorderStyle.DOUBLE_CLASSIC -> "border:1.5px solid #F9A825;padding:20px;border-radius:8px;position:relative;"
            PrintBorderStyle.MODERN_MINIMAL -> "padding:22px;position:relative;"
            PrintBorderStyle.OFFICIAL_BOXED -> "border:1px solid #212121;padding:18px;position:relative;"
        }
        return outerCss to innerCss
    }

    private fun buildOfficialHeaderHtml(
        context: Context,
        fundInfo: FundInfo,
        ps: PrintSettings,
        accent: String
    ): String {
        val logoHtml = buildLogoHtml(context, fundInfo, ps, if (ps.paperSize == PrintPaperSize.THERMAL_80) 44 else 58)
        val countryLine = if (ps.showCountryHeader && ps.countryHeaderText.isNotBlank()) {
            """<div style="font-weight:bold;font-size:${(ps.headerFontSizeSp - 2).coerceAtLeast(10)}px;color:#003300;">${escapeHtml(ps.countryHeaderText)}</div>"""
        } else ""
        val deptLine = if (ps.showDepartmentHeader && ps.departmentHeaderText.isNotBlank()) {
            """<div style="font-size:${(ps.bodyFontSizeSp - 1).coerceAtLeast(10)}px;color:#44553F;font-weight:600;">${escapeHtml(ps.departmentHeaderText)}</div>"""
        } else ""
        val licenseLine = if (ps.showLicenseAndAddress) {
            """
            <div style="font-size:11px;color:#555;margin-top:2px;">${escapeHtml(fundInfo.licenseNo)}</div>
            <div style="font-size:11px;color:#555;">${escapeHtml(fundInfo.address)}</div>
            """.trimIndent()
        } else ""

        return """
            <div style="display:flex;justify-content:space-between;align-items:center;border-bottom:2px solid #D8E4D2;padding-bottom:14px;margin-bottom:16px;gap:12px;">
                <div style="flex:1;">
                    $countryLine
                    <div style="font-weight:900;font-size:${ps.headerFontSizeSp + 1}px;color:$accent;margin-top:2px;">${escapeHtml(fundInfo.name)}</div>
                    $deptLine
                    $licenseLine
                </div>
                <div style="flex-shrink:0;text-align:center;">
                    $logoHtml
                </div>
            </div>
        """.trimIndent()
    }

    private fun buildSignaturesHtml(
        ps: PrintSettings,
        accent: String,
        sig1NameFallback: String,
        sig2NameFallback: String,
        sig3NameFallback: String,
        sig4NameFallback: String
    ): String {
        if (!ps.showSignaturesSection) return ""
        val slots = listOfNotNull(
            if (ps.sig1Enabled) Triple(ps.sig1Title, ps.sig1DefaultName.ifBlank { sig1NameFallback }, false) else null,
            if (ps.sig2Enabled) Triple(ps.sig2Title, ps.sig2DefaultName.ifBlank { sig2NameFallback }, false) else null,
            if (ps.sig3Enabled) Triple(ps.sig3Title, ps.sig3DefaultName.ifBlank { sig3NameFallback }, false) else null,
            if (ps.sig4Enabled) Triple(ps.sig4Title, ps.sig4DefaultName.ifBlank { sig4NameFallback }, true) else null
        )
        if (slots.isEmpty()) return ""

        val colPercent = when {
            ps.paperSize == PrintPaperSize.THERMAL_80 -> "100%"
            slots.size <= 2 -> "48%"
            else -> "${(96 / slots.size)}%"
        }

        val sigLineHtml = if (ps.showSignatureLine) {
            """<div style="border-bottom:1.5px dashed #87977F;margin:24px 12px 6px 12px;"></div>"""
        } else {
            """<div style="height:28px;"></div>"""
        }

        val cardsHtml = slots.joinToString("\n") { (title, name, isFinalApprover) ->
            val stampHtml = if (isFinalApprover && ps.showOfficialStampBox) {
                """
                <div style="position:absolute;left:8px;bottom:22px;width:46px;height:46px;border:1.5px dashed $accent;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:8px;color:$accent;opacity:0.55;transform:rotate(-12deg);text-align:center;line-height:1.1;">
                    ${escapeHtml(ps.officialStampLabel)}
                </div>
                """.trimIndent()
            } else ""

            """
            <div style="width:$colPercent;box-sizing:border-box;border:1px solid #D8E4D2;border-radius:8px;background:#FAFCFA;overflow:hidden;position:relative;margin-bottom:8px;">
                <div style="background:#F0F6EF;border-bottom:1px solid #D8E4D2;padding:5px 6px;font-weight:bold;font-size:${ps.signatureFontSizeSp}px;color:$accent;text-align:center;">
                    ${escapeHtml(title)}
                </div>
                <div style="padding:8px 6px 8px 6px;text-align:center;min-height:56px;display:flex;flex-direction:column;justify-content:flex-end;">
                    $stampHtml
                    $sigLineHtml
                    <div style="font-size:${(ps.signatureFontSizeSp - 1).coerceAtLeast(9)}px;color:#2E3B2B;font-weight:600;">${escapeHtml(name)}</div>
                </div>
            </div>
            """.trimIndent()
        }

        return """
            <div style="display:flex;flex-wrap:wrap;justify-content:space-between;gap:8px;margin-top:22px;">
                $cardsHtml
            </div>
        """.trimIndent()
    }

    private fun buildFooterHtml(
        ps: PrintSettings,
        fundInfo: FundInfo,
        qrContent: String,
        accent: String
    ): String {
        val qrHtml = if (ps.showQrVerification) {
            val svg = generateQrSvg(qrContent, sizePx = 68, dotColorHex = accent)
            """
            <div style="text-align:center;flex-shrink:0;">
                $svg
                <div style="font-size:9px;color:#666;margin-top:3px;">${escapeHtml(ps.qrCaptionText)}</div>
            </div>
            """.trimIndent()
        } else ""

        val noteHtml = if (ps.footerNote.isNotBlank()) {
            """<div style="font-weight:bold;color:#14210F;font-size:11px;">${escapeHtml(ps.footerNote)}</div>"""
        } else ""
        val subNoteHtml = if (ps.footerSubNote.isNotBlank()) {
            """<div style="color:#44553F;font-size:10px;margin-top:2px;">${escapeHtml(ps.footerSubNote)}</div>"""
        } else ""
        val contactHtml = if (ps.showFundContactInFooter) {
            """<div style="color:#666;font-size:10px;margin-top:4px;">${escapeHtml(fundInfo.name)} | هاتف: ${escapeHtml(fundInfo.phone)} | ${escapeHtml(fundInfo.email)}</div>"""
        } else ""

        return """
            <div style="display:flex;justify-content:space-between;align-items:center;gap:14px;margin-top:18px;padding-top:12px;border-top:1.5px dashed #C8D6C3;">
                $qrHtml
                <div style="flex:1;">
                    $noteHtml
                    $subNoteHtml
                    $contactHtml
                </div>
            </div>
        """.trimIndent()
    }

    fun printVoucherPdf(
        context: Context,
        voucher: Voucher,
        fundInfo: FundInfo,
        printSettings: PrintSettings = FundRepository.getInstance().printSettings.value
    ) {
        val ps = printSettings
        val isReceipt = voucher.type == VoucherType.RECEIPT
        val titleText = if (isReceipt) ps.receiptVoucherTitle else ps.paymentVoucherTitle
        val accent = ps.accentColorHex.ifBlank { "#1B5E20" }
        val badgeColor = if (isReceipt) accent else "#B71C1C"
        val isVoid = voucher.status == VoucherStatus.VOID

        val (outerCss, innerCss) = buildSheetWrapperStyles(ps, accent)
        val fontFaceCss = resolveFontFaceCss(context, ps.fontFamily)
        val headerHtml = buildOfficialHeaderHtml(context, fundInfo, ps, accent)

        val watermarkHtml = if (isVoid) {
            """<div style="position:fixed;top:40%;left:20%;transform:rotate(-30deg);border:6px solid rgba(211,47,47,0.45);color:rgba(211,47,47,0.45);font-size:44px;font-weight:900;padding:12px 36px;border-radius:14px;z-index:99;">سند ملغي - VOID</div>"""
        } else ""

        val currencySubHtml = if (ps.showCurrencyDetails && voucher.currency != CurrencyCode.YER) {
            """<div style="font-size:11px;color:#44553F;margin-top:2px;">(${voucher.amountOriginal} ${voucher.currency.name} @ ${"%,.0f".format(voucher.exchangeRate)})</div>"""
        } else ""

        val journalRefHtml = if (ps.showJournalReference && voucher.linkedJournalEntryId.isNotBlank()) {
            """<div style="font-size:11px;color:$accent;margin-top:2px;">القيد المحاسبي: <b>${escapeHtml(voucher.linkedJournalEntryId)}</b></div>"""
        } else ""

        val partyLabel = if (isReceipt) ps.receiptPartyLabel else ps.paymentPartyLabel

        val signaturesHtml = buildSignaturesHtml(
            ps = ps,
            accent = accent,
            sig1NameFallback = voucher.receivedOrPaidBy.ifBlank { voucher.partyName },
            sig2NameFallback = voucher.accountantName,
            sig3NameFallback = voucher.auditorName,
            sig4NameFallback = voucher.managerName
        )

        val qrPayload = "SOCIALFUND|${voucher.voucherNo}|${voucher.amountYer.toLong()}|${voucher.date}|${voucher.status.name}"
        val footerHtml = buildFooterHtml(ps, fundInfo, qrPayload, accent)

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8" />
                <style>
                    $fontFaceCss
                    body {
                        font-family: 'CustomPrintFont', '${ps.fontFamily.cssName}', 'Tahoma', 'Arial', sans-serif;
                        padding: 16px;
                        color: #14210F;
                        background: #FFFFFF;
                        font-size: ${ps.bodyFontSizeSp}px;
                    }
                    .banner {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        flex-wrap: wrap;
                        gap: 10px;
                        background: #F5FAF6;
                        border: 1px solid #D8E4D2;
                        border-radius: 10px;
                        padding: 12px 14px;
                        margin-bottom: 16px;
                    }
                    .title-pill {
                        border: 2px solid $badgeColor;
                        background: #FFFFFF;
                        color: $badgeColor;
                        padding: 6px 22px;
                        font-size: ${ps.headerFontSizeSp + 3}px;
                        font-weight: 900;
                        border-radius: 8px;
                    }
                    .amount-box {
                        border: 2px solid #BF6000;
                        background: #FFF9C4;
                        padding: 6px 16px;
                        border-radius: 8px;
                        text-align: center;
                    }
                    .field-card {
                        border: 1px solid #D8E4D2;
                        border-radius: 8px;
                        overflow: hidden;
                    }
                    .field-row {
                        display: flex;
                        align-items: center;
                        padding: 10px 12px;
                        border-bottom: 1px solid #E8EFE5;
                        font-size: ${ps.bodyFontSizeSp}px;
                    }
                    .field-row:last-child { border-bottom: none; }
                    .field-label {
                        font-weight: bold;
                        color: $accent;
                        width: 155px;
                        flex-shrink: 0;
                    }
                    .highlight-row {
                        background: #FFFDF0;
                    }
                </style>
            </head>
            <body>
                $watermarkHtml
                <div style="$outerCss">
                    <div style="$innerCss">
                        $headerHtml

                        <div class="banner">
                            <div>
                                <div style="font-weight:bold;font-size:${ps.bodyFontSizeSp + 1}px;color:$badgeColor;">رقم السند: ${escapeHtml(voucher.voucherNo)}</div>
                                <div style="font-size:12px;color:#44553F;margin-top:2px;">التاريخ: ${escapeHtml(voucher.date)}</div>
                                $journalRefHtml
                            </div>
                            <div class="title-pill">${escapeHtml(titleText)}</div>
                            <div class="amount-box">
                                <div style="font-size:${ps.headerFontSizeSp + 2}px;font-weight:900;color:#003300;"># ${"%,d".format(voucher.amountYer.toLong())} # ﷼</div>
                                $currencySubHtml
                            </div>
                        </div>

                        <div class="field-card">
                            <div class="field-row">
                                <span class="field-label">${escapeHtml(partyLabel)}</span>
                                <span style="font-weight:900;font-size:${ps.bodyFontSizeSp + 2}px;">${escapeHtml(voucher.partyName)}</span>
                            </div>
                            <div class="field-row highlight-row">
                                <span class="field-label">${escapeHtml(ps.amountWordsLabel)}</span>
                                <span style="font-weight:bold;color:#003300;">${escapeHtml(voucher.amountWords)}</span>
                            </div>
                            <div class="field-row">
                                <span class="field-label">${escapeHtml(ps.paymentMethodLabel)}</span>
                                <span style="flex:1;">${escapeHtml(voucher.paymentMethod.title)}</span>
                                <span style="font-weight:bold;color:$accent;margin-left:8px;">${escapeHtml(ps.accountLabel)}</span>
                                <span>${escapeHtml(voucher.accountName)}</span>
                            </div>
                            <div class="field-row">
                                <span class="field-label">${escapeHtml(ps.descriptionLabel)}</span>
                                <span style="line-height:1.5;">${escapeHtml(voucher.description)}</span>
                            </div>
                        </div>

                        $signaturesHtml
                        $footerHtml
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        launchAndroidPrintJob(context, html, "Voucher_${voucher.voucherNo}", ps.paperSize)
    }

    fun printMemberCardPdf(
        context: Context,
        member: Member,
        fundInfo: FundInfo,
        printSettings: PrintSettings = FundRepository.getInstance().printSettings.value
    ) {
        val ps = printSettings
        val accent = ps.accentColorHex.ifBlank { "#1B5E20" }
        val fontFaceCss = resolveFontFaceCss(context, ps.fontFamily)
        val logoHtml = buildLogoHtml(context, fundInfo, ps, 46)
        val qrSvg = if (ps.showQrVerification) {
            generateQrSvg("MEMBER|${member.memberNo}|${member.nationalId}|${member.status}", 64, "#003300")
        } else ""

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8" />
                <style>
                    $fontFaceCss
                    body { font-family: 'CustomPrintFont', '${ps.fontFamily.cssName}', 'Tahoma', sans-serif; padding: 28px; }
                    .card {
                        width: 480px;
                        border: 3px solid #F9A825;
                        border-radius: 18px;
                        background: linear-gradient(135deg, #003300, $accent);
                        color: white;
                        padding: 22px;
                    }
                    .badge {
                        background: #F9A825;
                        color: black;
                        padding: 4px 12px;
                        border-radius: 12px;
                        font-weight: bold;
                        font-size: 13px;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <div style="display:flex;justify-content:space-between;align-items:center;">
                        <div style="display:flex;align-items:center;gap:10px;">
                            $logoHtml
                            <div>
                                <div style="font-size:18px;font-weight:bold;">${escapeHtml(fundInfo.name)}</div>
                                <div style="font-size:12px;color:#FDD835;">بطاقة عضوية رسمية معتمدة</div>
                            </div>
                        </div>
                        <span class="badge">${escapeHtml(member.status)}</span>
                    </div>
                    <hr style="border-color:rgba(255,255,255,0.2);margin:14px 0;" />
                    <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;">
                        <div style="flex:1;">
                            <div style="font-size:20px;font-weight:bold;">${escapeHtml(member.fullName)}</div>
                            <div style="font-size:15px;color:#FDD835;margin-top:6px;">رقم العضوية: ${escapeHtml(member.memberNo)}</div>
                            <div style="font-size:12px;margin-top:4px;">رقم الهوية: ${escapeHtml(member.nationalId)} | الهاتف: ${escapeHtml(member.phone)}</div>
                            <div style="font-size:12px;margin-top:4px;">المهنة: ${escapeHtml(member.job)} | المنطقة: ${escapeHtml(member.region)} | القرية: ${escapeHtml(member.village)}</div>
                        </div>
                        $qrSvg
                    </div>
                    <hr style="border-color:rgba(255,255,255,0.2);margin:14px 0;" />
                    <div style="font-size:12px;display:flex;justify-content:space-between;">
                        <span>تاريخ الانتساب: ${escapeHtml(member.joinDate)}</span>
                        <span>إجمالي السداد: ${"%,d".format(member.totalPaidYer.toLong())} ﷼</span>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        launchAndroidPrintJob(context, html, "MemberCard_${member.memberNo}", ps.paperSize)
    }

    fun printJournalEntryPdf(
        context: Context,
        entry: JournalEntry,
        fundInfo: FundInfo,
        printSettings: PrintSettings = FundRepository.getInstance().printSettings.value
    ) {
        val ps = printSettings
        val accent = ps.accentColorHex.ifBlank { "#1B5E20" }
        val (outerCss, innerCss) = buildSheetWrapperStyles(ps, accent)
        val fontFaceCss = resolveFontFaceCss(context, ps.fontFamily)
        val headerHtml = buildOfficialHeaderHtml(context, fundInfo, ps, accent)

        val totalDebit = entry.lines.sumOf { it.debit }.toLong()
        val totalCredit = entry.lines.sumOf { it.credit }.toLong()
        val rowsHtml = entry.lines.joinToString("\n") { line ->
            val debStr = if (line.debit > 0) "%,d ﷼".format(line.debit.toLong()) else "-"
            val credStr = if (line.credit > 0) "%,d ﷼".format(line.credit.toLong()) else "-"
            """
            <tr>
                <td><b>${escapeHtml(line.accountCode)}</b></td>
                <td>${escapeHtml(line.accountName)}</td>
                <td style="color:$accent;font-weight:bold;">$debStr</td>
                <td style="color:#BF6000;font-weight:bold;">$credStr</td>
                <td>${escapeHtml(line.description)}</td>
            </tr>
            """.trimIndent()
        }

        val signaturesHtml = buildSignaturesHtml(
            ps = ps,
            accent = accent,
            sig1NameFallback = "مُعد القيد",
            sig2NameFallback = "أمين الصندوق / المحاسب",
            sig3NameFallback = "المراجع المالي",
            sig4NameFallback = "رئيس مجلس الإدارة"
        )
        val footerHtml = buildFooterHtml(ps, fundInfo, "JOURNAL|${entry.entryNo}|${entry.date}|$totalDebit", accent)

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8" />
                <style>
                    $fontFaceCss
                    body { font-family: 'CustomPrintFont', '${ps.fontFamily.cssName}', 'Tahoma', sans-serif; padding: 18px; color: #14210F; font-size: ${ps.bodyFontSizeSp}px; }
                    .badge { border: 2px solid $accent; background: #F2F6F1; color: $accent; padding: 6px 20px; border-radius: 8px; font-weight: bold; font-size: ${ps.headerFontSizeSp + 1}px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: ${ps.bodyFontSizeSp}px; }
                    th { background: $accent; color: white; padding: 9px; border: 1px solid $accent; text-align: right; }
                    td { padding: 8px; border: 1px solid #D8E4D2; text-align: right; }
                    .total-row { background: #FFF9C4; font-weight: bold; }
                </style>
            </head>
            <body>
                <div style="$outerCss">
                    <div style="$innerCss">
                        $headerHtml
                        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
                            <div class="badge">${escapeHtml(ps.journalVoucherTitle)}</div>
                            <div style="text-align:left;font-size:13px;">
                                <div><b>رقم القيد:</b> ${escapeHtml(entry.entryNo)}</div>
                                <div><b>التاريخ:</b> ${escapeHtml(entry.date)}</div>
                                <div><b>السند المرتبط:</b> ${escapeHtml(entry.originalVoucherNo ?: "قيد يدوي")}</div>
                            </div>
                        </div>
                        <div style="margin-bottom:10px;"><b>البيان المحاسبي:</b> ${escapeHtml(entry.description)}</div>
                        <table>
                            <thead>
                                <tr>
                                    <th>رقم الحساب</th>
                                    <th>اسم الحساب</th>
                                    <th>مدين (Debit)</th>
                                    <th>دائن (Credit)</th>
                                    <th>البيان الفرعي</th>
                                </tr>
                            </thead>
                            <tbody>
                                $rowsHtml
                                <tr class="total-row">
                                    <td colspan="2">الإجمالي المطابق (توازن القيد المزدوج ✔)</td>
                                    <td>${"%,d".format(totalDebit)} ﷼</td>
                                    <td>${"%,d".format(totalCredit)} ﷼</td>
                                    <td>متوازن</td>
                                </tr>
                            </tbody>
                        </table>
                        $signaturesHtml
                        $footerHtml
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        launchAndroidPrintJob(context, html, "JournalEntry_${entry.entryNo}", ps.paperSize)
    }

    fun printFinancialStatementPdf(
        context: Context,
        statementTitle: String,
        accounts: List<Account>,
        fundInfo: FundInfo,
        printSettings: PrintSettings = FundRepository.getInstance().printSettings.value
    ) {
        val ps = printSettings
        val accent = ps.accentColorHex.ifBlank { "#1B5E20" }
        val (outerCss, innerCss) = buildSheetWrapperStyles(ps, accent)
        val fontFaceCss = resolveFontFaceCss(context, ps.fontFamily)
        val headerHtml = buildOfficialHeaderHtml(context, fundInfo, ps, accent)

        val rowsHtml = accounts.joinToString("\n") { acc ->
            """
            <tr>
                <td><b>${escapeHtml(acc.code)}</b></td>
                <td>${escapeHtml(acc.name)}</td>
                <td>${escapeHtml(acc.type.title)}</td>
                <td style="font-weight:bold;color:$accent;">${"%,d".format(acc.balance.toLong())} ﷼</td>
            </tr>
            """.trimIndent()
        }
        val totalAssets = accounts.filter { it.parentCode != null && it.code.startsWith("1") }.sumOf { it.balance }.toLong()
        val totalRev = accounts.filter { it.parentCode != null && it.code.startsWith("4") }.sumOf { it.balance }.toLong()
        val totalExp = accounts.filter { it.parentCode != null && it.code.startsWith("5") }.sumOf { it.balance }.toLong()

        val signaturesHtml = buildSignaturesHtml(
            ps = ps,
            accent = accent,
            sig1NameFallback = "مُعد التقرير",
            sig2NameFallback = "أمين الصندوق / المحاسب",
            sig3NameFallback = "المراجع المالي",
            sig4NameFallback = "رئيس مجلس الإدارة"
        )
        val footerHtml = buildFooterHtml(ps, fundInfo, "STATEMENT|${fundInfo.fiscalYear}|$totalAssets|$totalRev", accent)

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8" />
                <style>
                    $fontFaceCss
                    body { font-family: 'CustomPrintFont', '${ps.fontFamily.cssName}', 'Tahoma', sans-serif; padding: 18px; color: #14210F; font-size: ${ps.bodyFontSizeSp}px; }
                    .kpi-row { display: flex; justify-content: space-between; margin-bottom: 14px; gap: 10px; }
                    .kpi { flex: 1; border: 1px solid #D8E4D2; background: #F5FAF6; padding: 10px; border-radius: 8px; text-align: center; }
                    table { width: 100%; border-collapse: collapse; font-size: ${(ps.bodyFontSizeSp - 1).coerceAtLeast(10)}px; }
                    th { background: $accent; color: white; padding: 8px; border: 1px solid $accent; text-align: right; }
                    td { padding: 7px; border: 1px solid #D8E4D2; text-align: right; }
                </style>
            </head>
            <body>
                <div style="$outerCss">
                    <div style="$innerCss">
                        $headerHtml
                        <div style="text-align:center;margin-bottom:14px;">
                            <div style="font-size:${ps.headerFontSizeSp + 2}px;font-weight:bold;color:$accent;">${escapeHtml(statementTitle)}</div>
                            <div style="font-size:11px;color:#666;">${escapeHtml(ps.financialStatementSubtitle)}</div>
                        </div>
                        <div class="kpi-row">
                            <div class="kpi"><b>إجمالي الأصول والسيولة</b><br/>${"%,d".format(totalAssets)} ﷼</div>
                            <div class="kpi"><b>إجمالي الإيرادات</b><br/>${"%,d".format(totalRev)} ﷼</div>
                            <div class="kpi"><b>إجمالي المصروفات</b><br/>${"%,d".format(totalExp)} ﷼</div>
                            <div class="kpi"><b>صافي الفائض التشغيلي</b><br/>${"%,d".format(totalRev - totalExp)} ﷼</div>
                        </div>
                        <table>
                            <thead>
                                <tr>
                                    <th>كود الحساب</th>
                                    <th>اسم الحساب المحاسبي</th>
                                    <th>التصنيف</th>
                                    <th>الرصيد الدفتري المعتمد</th>
                                </tr>
                            </thead>
                            <tbody>
                                $rowsHtml
                            </tbody>
                        </table>
                        $signaturesHtml
                        $footerHtml
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        launchAndroidPrintJob(context, html, "FinancialStatement_${System.currentTimeMillis()}", ps.paperSize)
    }

    fun printAnalyticalReportPdf(
        context: Context,
        fundInfo: FundInfo,
        totalRev: Long,
        totalExp: Long,
        membersCount: Int,
        disbursedAidsCount: Int,
        printSettings: PrintSettings = FundRepository.getInstance().printSettings.value
    ) {
        val ps = printSettings
        val accent = ps.accentColorHex.ifBlank { "#1B5E20" }
        val (outerCss, innerCss) = buildSheetWrapperStyles(ps, accent)
        val fontFaceCss = resolveFontFaceCss(context, ps.fontFamily)
        val headerHtml = buildOfficialHeaderHtml(context, fundInfo, ps, accent)
        val signaturesHtml = buildSignaturesHtml(
            ps = ps,
            accent = accent,
            sig1NameFallback = "مُعد التقرير",
            sig2NameFallback = "أمين الصندوق / المحاسب",
            sig3NameFallback = "المراجع المالي",
            sig4NameFallback = "رئيس مجلس الإدارة"
        )
        val footerHtml = buildFooterHtml(ps, fundInfo, "REPORT|${fundInfo.fiscalYear}|$totalRev|$totalExp", accent)

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8"/>
                <style>
                    $fontFaceCss
                    body { font-family: 'CustomPrintFont', '${ps.fontFamily.cssName}', sans-serif; direction: rtl; padding: 18px; color: #14210F; font-size: ${ps.bodyFontSizeSp}px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 14px; }
                    th, td { border: 1px solid #D8E4D2; padding: 11px; text-align: right; }
                    th { background: $accent; color: white; }
                </style>
            </head>
            <body>
                <div style="$outerCss">
                    <div style="$innerCss">
                        $headerHtml
                        <div style="text-align:center;margin-bottom:14px;">
                            <div style="font-size:${ps.headerFontSizeSp + 2}px;font-weight:bold;color:$accent;">${escapeHtml(ps.analyticalReportSubtitle)}</div>
                        </div>
                        <table>
                            <tr><th>البيان الإحصائي / المالي</th><th>القيمة المعتمدة</th></tr>
                            <tr><td>إجمالي المقبوضات المعتمدة</td><td>${"%,d ﷼".format(totalRev)}</td></tr>
                            <tr><td>إجمالي المصروفات المعتمدة</td><td>${"%,d ﷼".format(totalExp)}</td></tr>
                            <tr><td>صافي الفائض النقدي والبنكي</td><td><b>${"%,d ﷼".format(totalRev - totalExp)}</b></td></tr>
                            <tr><td>إجمالي الأعضاء المسجلين</td><td>$membersCount عضو</td></tr>
                            <tr><td>عدد حالات المساعدات المنفذة</td><td>$disbursedAidsCount حالة</td></tr>
                        </table>
                        $signaturesHtml
                        $footerHtml
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        launchAndroidPrintJob(context, html, "Analytical_Report_${System.currentTimeMillis()}", ps.paperSize)
    }

    private fun launchAndroidPrintJob(
        context: Context,
        htmlContent: String,
        jobName: String,
        paperSize: PrintPaperSize = PrintPaperSize.A4
    ) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val adapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter(jobName)
                val mediaSize = when (paperSize) {
                    PrintPaperSize.A4 -> PrintAttributes.MediaSize.ISO_A4
                    PrintPaperSize.A5 -> PrintAttributes.MediaSize.ISO_A5
                    PrintPaperSize.THERMAL_80 -> PrintAttributes.MediaSize.ISO_C7
                }
                printManager?.print(
                    jobName,
                    adapter,
                    PrintAttributes.Builder()
                        .setMediaSize(mediaSize)
                        .build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }
}
