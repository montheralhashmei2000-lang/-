package com.example.fund.util

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.fund.model.FundInfo
import com.example.fund.model.Member
import com.example.fund.model.Voucher
import com.example.fund.model.VoucherStatus
import com.example.fund.model.VoucherType

object OfficialPrintHelper {

    fun printVoucherPdf(context: Context, voucher: Voucher, fundInfo: FundInfo) {
        val isReceipt = voucher.type == VoucherType.RECEIPT
        val titleText = if (isReceipt) "سـنـد قـبـض" else "سـنـد صـرف"
        val primaryColor = if (isReceipt) "#1B5E20" else "#B71C1C"
        val isVoid = voucher.status == VoucherStatus.VOID

        val logoHtml = if (fundInfo.customLogoBase64.isNotBlank()) {
            """<img src="data:image/png;base64,${fundInfo.customLogoBase64}" style="width:54px;height:54px;border-radius:50%;object-fit:cover;border:2px solid #F9A825;" />"""
        } else ""

        val watermarkHtml = if (isVoid) {
            """<div style="position:fixed; top:40%; left:20%; transform:rotate(-30deg); border:6px solid rgba(211,47,47,0.45); color:rgba(211,47,47,0.45); font-size:48px; font-weight:900; padding:12px 36px; border-radius:14px; z-index:99;">سند ملغي - VOID</div>"""
        } else ""

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8" />
                <style>
                    body {
                        font-family: 'Tahoma', 'Arial', sans-serif;
                        padding: 20px;
                        color: #14210F;
                        background: #FFFFFF;
                    }
                    .sheet {
                        border: 3px solid #1B5E20;
                        padding: 6px;
                        border-radius: 12px;
                    }
                    .inner-sheet {
                        border: 1.5px solid #F9A825;
                        padding: 24px;
                        border-radius: 8px;
                        position: relative;
                    }
                    .header {
                        display: flex;
                        justify-content: space-between;
                        align-items: flex-start;
                        border-bottom: 2px solid #D8E4D2;
                        padding-bottom: 16px;
                        margin-bottom: 20px;
                    }
                    .title-box {
                        border: 2.5px solid $primaryColor;
                        background: #F5FAF6;
                        color: $primaryColor;
                        padding: 8px 28px;
                        font-size: 24px;
                        font-weight: bold;
                        border-radius: 8px;
                        text-align: center;
                        margin-top: 8px;
                    }
                    .amount-box {
                        border: 2px solid #BF6000;
                        background: #FFF9C4;
                        padding: 8px 16px;
                        border-radius: 8px;
                        font-size: 18px;
                        font-weight: bold;
                        color: #003300;
                        margin-top: 8px;
                        display: inline-block;
                    }
                    .field-row {
                        margin: 12px 0;
                        font-size: 15px;
                        padding: 8px;
                        background: #F9FBF8;
                        border-radius: 6px;
                    }
                    .field-label {
                        font-weight: bold;
                        color: #2E7D32;
                        display: inline-block;
                        width: 150px;
                    }
                    .signatures {
                        display: flex;
                        justify-content: space-between;
                        margin-top: 36px;
                        text-align: center;
                    }
                    .sig-box {
                        border: 1px solid #D8E4D2;
                        border-radius: 6px;
                        padding: 10px;
                        width: 22%;
                        background: #FAFCFA;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 24px;
                        font-size: 11px;
                        color: #666;
                        border-top: 1px dashed #CCC;
                        padding-top: 8px;
                    }
                </style>
            </head>
            <body>
                $watermarkHtml
                <div class="sheet">
                    <div class="inner-sheet">
                        <div class="header">
                            <div>
                                <div style="font-weight:bold; font-size:15px;">الجمهورية اليمنية</div>
                                <div style="font-weight:900; font-size:17px; color:#1B5E20;">${fundInfo.name}</div>
                                <div style="font-size:12px; color:#555;">${fundInfo.licenseNo}</div>
                                <div style="font-size:12px; color:#555;">${fundInfo.address}</div>
                            </div>
                            <div style="text-align:center;">
                                $logoHtml
                                <div class="title-box">$titleText</div>
                            </div>
                            <div style="text-align:left;">
                                <div style="font-weight:bold; font-size:15px; color:$primaryColor;">رقم السند: ${voucher.voucherNo}</div>
                                <div style="font-size:13px;">التاريخ: ${voucher.date}</div>
                                <div style="font-size:12px; color:#2E7D32;">القيد المحاسبي: ${voucher.linkedJournalEntryId}</div>
                                <div class="amount-box"># ${"%,d".format(voucher.amountYer.toLong())} # ﷼</div>
                            </div>
                        </div>

                        <div class="field-row">
                            <span class="field-label">${if (isReceipt) "استلمنا من المكرم:" else "يصرف للمكرم:"}</span>
                            <span style="font-weight:bold;">${voucher.partyName}</span>
                        </div>
                        <div class="field-row">
                            <span class="field-label">مبلغ وقدره كتابةً:</span>
                            <span style="font-weight:bold; color:#003300;">${voucher.amountWords}</span>
                        </div>
                        <div class="field-row">
                            <span class="field-label">طريقة الدفع والحساب:</span>
                            <span>${voucher.paymentMethod.title} - (${voucher.accountName})</span>
                        </div>
                        <div class="field-row">
                            <span class="field-label">وذلك مقابل (البيان):</span>
                            <span>${voucher.description}</span>
                        </div>

                        <div class="signatures">
                            <div class="sig-box">
                                <div style="font-weight:bold; font-size:13px;">المستلم</div>
                                <br/><br/>
                                <div style="font-size:12px;">${voucher.receivedOrPaidBy}</div>
                            </div>
                            <div class="sig-box">
                                <div style="font-weight:bold; font-size:13px;">المحاسب / أمين الصندوق</div>
                                <br/><br/>
                                <div style="font-size:12px;">${voucher.accountantName}</div>
                            </div>
                            <div class="sig-box">
                                <div style="font-weight:bold; font-size:13px;">المراجع المالي</div>
                                <br/><br/>
                                <div style="font-size:12px;">${voucher.auditorName}</div>
                            </div>
                            <div class="sig-box">
                                <div style="font-weight:bold; font-size:13px;">يعتمد / رئيس الصندوق</div>
                                <br/><br/>
                                <div style="font-size:12px;">${voucher.managerName}</div>
                            </div>
                        </div>

                        <div class="footer">
                            ${fundInfo.name} - ${fundInfo.subtitle} | الرمز الرقمي للتحقق: SOCIALFUND|${voucher.voucherNo}|${voucher.amountYer.toLong()}|${voucher.date}
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        launchAndroidPrintJob(context, html, "Voucher_${voucher.voucherNo}")
    }

    fun printMemberCardPdf(context: Context, member: Member, fundInfo: FundInfo) {
        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8" />
                <style>
                    body { font-family: 'Tahoma', sans-serif; padding: 30px; }
                    .card {
                        width: 480px;
                        border: 3px solid #F9A825;
                        border-radius: 18px;
                        background: linear-gradient(135deg, #003300, #1B5E20);
                        color: white;
                        padding: 24px;
                    }
                    .badge {
                        background: #F9A825;
                        color: black;
                        padding: 4px 12px;
                        border-radius: 12px;
                        font-weight: bold;
                        font-size: 13px;
                        float: left;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <span class="badge">${member.status}</span>
                    <div style="font-size:18px; font-weight:bold;">${fundInfo.name}</div>
                    <div style="font-size:12px; color:#FDD835; margin-bottom:18px;">بطاقة عضوية رسمية معتمدة</div>
                    <hr style="border-color:rgba(255,255,255,0.2);" />
                    <div style="margin: 14px 0;">
                        <div style="font-size:20px; font-weight:bold;">${member.fullName}</div>
                        <div style="font-size:15px; color:#FDD835; margin-top:6px;">رقم العضوية: ${member.memberNo}</div>
                        <div style="font-size:13px; margin-top:4px;">رقم الهوية: ${member.nationalId} | الهاتف: ${member.phone}</div>
                        <div style="font-size:13px; margin-top:4px;">المهنة: ${member.job} | المنطقة: ${member.region} | القرية: ${member.village}</div>
                    </div>
                    <hr style="border-color:rgba(255,255,255,0.2);" />
                    <div style="font-size:12px; display:flex; justify-content:space-between;">
                        <span>تاريخ الانتساب: ${member.joinDate}</span>
                        <span>إجمالي السداد: ${"%,d".format(member.totalPaidYer.toLong())} ﷼</span>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        launchAndroidPrintJob(context, html, "MemberCard_${member.memberNo}")
    }

    fun printJournalEntryPdf(
        context: Context,
        entry: com.example.fund.model.JournalEntry,
        fundInfo: FundInfo
    ) {
        val totalDebit = entry.lines.sumOf { it.debit }.toLong()
        val totalCredit = entry.lines.sumOf { it.credit }.toLong()
        val rowsHtml = entry.lines.joinToString("\n") { line ->
            val debStr = if (line.debit > 0) "%,d ﷼".format(line.debit.toLong()) else "-"
            val credStr = if (line.credit > 0) "%,d ﷼".format(line.credit.toLong()) else "-"
            """
            <tr>
                <td><b>${line.accountCode}</b></td>
                <td>${line.accountName}</td>
                <td style="color:#1B5E20; font-weight:bold;">$debStr</td>
                <td style="color:#BF6000; font-weight:bold;">$credStr</td>
                <td>${line.description}</td>
            </tr>
            """.trimIndent()
        }

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8" />
                <style>
                    body { font-family: 'Tahoma', sans-serif; padding: 24px; color: #14210F; }
                    .sheet { border: 2.5px solid #1B5E20; border-radius: 12px; padding: 20px; }
                    .header { display: flex; justify-content: space-between; border-bottom: 2px solid #D8E4D2; padding-bottom: 12px; margin-bottom: 16px; }
                    .badge { border: 2px solid #1B5E20; background: #F2F6F1; color: #1B5E20; padding: 6px 20px; border-radius: 8px; font-weight: bold; font-size: 18px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 13px; }
                    th { background: #1B5E20; color: white; padding: 9px; border: 1px solid #1B5E20; text-align: right; }
                    td { padding: 8px; border: 1px solid #D8E4D2; text-align: right; }
                    .total-row { background: #FFF9C4; font-weight: bold; }
                    .sigs { display: flex; justify-content: space-between; margin-top: 32px; text-align: center; font-size: 12px; }
                    .sig { width: 30%; border: 1px solid #D8E4D2; border-radius: 6px; padding: 10px; background: #FAFCFA; }
                </style>
            </head>
            <body>
                <div class="sheet">
                    <div class="header">
                        <div>
                            <div style="font-weight:bold; font-size:16px; color:#1B5E20;">${fundInfo.name}</div>
                            <div style="font-size:12px; color:#555;">${fundInfo.licenseNo} | ${fundInfo.address}</div>
                        </div>
                        <div class="badge">سند قيد يومية مزدوج</div>
                        <div style="text-align:left; font-size:13px;">
                            <div><b>رقم القيد:</b> ${entry.entryNo}</div>
                            <div><b>التاريخ:</b> ${entry.date}</div>
                            <div><b>السند المرتبط:</b> ${entry.originalVoucherNo ?: "قيد يدوي"}</div>
                        </div>
                    </div>
                    <div style="margin-bottom:10px; font-size:14px;"><b>البيان المحاسبي:</b> ${entry.description}</div>
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
                    <div class="sigs">
                        <div class="sig"><b>إعداد المحاسب المالي</b><br/><br/>أمين الصندوق / المحاسب</div>
                        <div class="sig"><b>مراجعة وتدقيق</b><br/><br/>المراجع المالي</div>
                        <div class="sig"><b>اعتماد رئيس الصندوق</b><br/><br/>رئيس مجلس الإدارة</div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        launchAndroidPrintJob(context, html, "JournalEntry_${entry.entryNo}")
    }

    fun printFinancialStatementPdf(
        context: Context,
        statementTitle: String,
        accounts: List<com.example.fund.model.Account>,
        fundInfo: FundInfo
    ) {
        val rowsHtml = accounts.joinToString("\n") { acc ->
            """
            <tr>
                <td><b>${acc.code}</b></td>
                <td>${acc.name}</td>
                <td>${acc.type.title}</td>
                <td style="font-weight:bold; color:#1B5E20;">${"%,d".format(acc.balance.toLong())} ﷼</td>
            </tr>
            """.trimIndent()
        }
        val totalAssets = accounts.filter { it.parentCode != null && it.code.startsWith("1") }.sumOf { it.balance }.toLong()
        val totalRev = accounts.filter { it.parentCode != null && it.code.startsWith("4") }.sumOf { it.balance }.toLong()
        val totalExp = accounts.filter { it.parentCode != null && it.code.startsWith("5") }.sumOf { it.balance }.toLong()

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8" />
                <style>
                    body { font-family: 'Tahoma', sans-serif; padding: 24px; color: #14210F; }
                    .header { display: flex; justify-content: space-between; border-bottom: 3px solid #1B5E20; padding-bottom: 12px; margin-bottom: 16px; }
                    .kpi-row { display: flex; justify-content: space-between; margin-bottom: 16px; gap: 10px; }
                    .kpi { flex: 1; border: 1px solid #D8E4D2; background: #F5FAF6; padding: 10px; border-radius: 8px; text-align: center; }
                    table { width: 100%; border-collapse: collapse; font-size: 12px; }
                    th { background: #003300; color: white; padding: 8px; border: 1px solid #003300; text-align: right; }
                    td { padding: 7px; border: 1px solid #D8E4D2; text-align: right; }
                    .sigs { display: flex; justify-content: space-between; margin-top: 28px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div>
                        <div style="font-weight:bold; font-size:17px; color:#1B5E20;">${fundInfo.name}</div>
                        <div style="font-size:12px;">${fundInfo.licenseNo} - ${fundInfo.address}</div>
                    </div>
                    <div style="text-align:center;">
                        <div style="font-size:18px; font-weight:bold; color:#003300;">$statementTitle</div>
                        <div style="font-size:11px; color:#666;">مستخرج رسمي معتمد من النظام المحاسبي (الإصدار 9.0)</div>
                    </div>
                    <div style="text-align:left; font-size:12px;">
                        <div>الهاتف: ${fundInfo.phone}</div>
                        <div>العملة الأساسية: ريال يمني (YER)</div>
                    </div>
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
                <div class="sigs">
                    <div><b>المحاسب المالي</b><br/><br/>التوقيع: ........................</div>
                    <div><b>المراجع الداخلي</b><br/><br/>التوقيع: ........................</div>
                    <div><b>رئيس مجلس الإدارة</b><br/><br/>الختم والتوقيع: ........................</div>
                </div>
            </body>
            </html>
        """.trimIndent()

        launchAndroidPrintJob(context, html, "FinancialStatement_${System.currentTimeMillis()}")
    }

    private fun launchAndroidPrintJob(context: Context, htmlContent: String, jobName: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val adapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    adapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }
}
