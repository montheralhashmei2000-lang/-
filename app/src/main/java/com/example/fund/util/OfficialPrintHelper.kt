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
                                <div class="title-box">$titleText</div>
                            </div>
                            <div style="text-align:left;">
                                <div style="font-weight:bold; font-size:15px; color:$primaryColor;">رقم السند: ${voucher.voucherNo}</div>
                                <div style="font-size:13px;">التاريخ: ${voucher.date}</div>
                                <div style="font-size:12px; color:#2E7D32;">القيد المحاسبي: ${voucher.linkedJournalEntryId}</div>
                                <div class="amount-box"># %,d # ﷼</div>
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
                            ${fundInfo.name} - ${fundInfo.subtitle} | الرمز الرقمي للتحقق: ALHUBAYLIN|${voucher.voucherNo}|${voucher.amountYer.toLong()}|${voucher.date}
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent().format(voucher.amountYer.toLong())

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
                    <div style="font-size:12px; color:#FDD835; margin-bottom:18px;">بطاقة عضوية رسمية معتمدة - مديرية ردفان</div>
                    <hr style="border-color:rgba(255,255,255,0.2);" />
                    <div style="margin: 14px 0;">
                        <div style="font-size:20px; font-weight:bold;">${member.fullName}</div>
                        <div style="font-size:15px; color:#FDD835; margin-top:6px;">رقم العضوية: ${member.memberNo}</div>
                        <div style="font-size:13px; margin-top:4px;">رقم الهوية: ${member.nationalId} | الهاتف: ${member.phone}</div>
                        <div style="font-size:13px; margin-top:4px;">المهنة والمنطقة: ${member.job} - ${member.villageOrArea}</div>
                    </div>
                    <hr style="border-color:rgba(255,255,255,0.2);" />
                    <div style="font-size:12px; display:flex; justify-content:space-between;">
                        <span>تاريخ الانتساب: ${member.joinDate}</span>
                        <span>إجمالي السداد: %,d ﷼</span>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent().format(member.totalPaidYer.toLong())

        launchAndroidPrintJob(context, html, "MemberCard_${member.memberNo}")
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
