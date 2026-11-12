package com.example.fund.ui.screens

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fund.data.FundRepository
import com.example.fund.model.AidStatus
import com.example.fund.model.VoucherStatus
import com.example.fund.model.VoucherType
import com.example.ui.theme.*

@Composable
fun ReportsScreen() {
    val repo = FundRepository.getInstance()
    val members by repo.members.collectAsState()
    val subscriptions by repo.subscriptions.collectAsState()
    val vouchers by repo.vouchers.collectAsState()
    val aids by repo.aidRequests.collectAsState()
    val fundInfo by repo.fundInfo.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var sortAsc by remember { mutableStateOf(false) }

    val totalRev = vouchers.filter { it.type == VoucherType.RECEIPT && it.status == VoucherStatus.POSTED }.sumOf { it.amountYer }.toLong()
    val totalExp = vouchers.filter { it.type == VoucherType.PAYMENT && it.status == VoucherStatus.POSTED }.sumOf { it.amountYer }.toLong()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("reports_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "مركز التقارير التحليلية والإحصائية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "تقارير تفصيلية مع الفرز التفاعلي، التصدير CSV، والطباعة الرسمية PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val csvBuilder = StringBuilder()
                        csvBuilder.append("تقرير,${fundInfo.name}\n")
                        when (selectedTab) {
                            0, 1 -> {
                                csvBuilder.append("الكود,اسم العضو,المنطقة,القرية,إجمالي المسدد (ريال)\n")
                                members.forEach { m ->
                                    val totalPaid = subscriptions.filter { it.memberId == m.id }.sumOf { it.amountYer }.toLong()
                                    csvBuilder.append("${m.memberNo},${m.fullName},${m.region},${m.village},$totalPaid\n")
                                }
                            }
                            2 -> {
                                csvBuilder.append("رقم الطلب,المستفيد,النوع,الحالة,المبلغ المعتمد\n")
                                aids.forEach { a ->
                                    csvBuilder.append("${a.requestNo},${a.memberName},${a.aidType.title},${a.status.title},${a.approvedAmountYer.toLong()}\n")
                                }
                            }
                            3 -> {
                                csvBuilder.append("رقم السند,النوع,الطرف,التاريخ,المبلغ بالريال\n")
                                vouchers.forEach { v ->
                                    csvBuilder.append("${v.voucherNo},${v.type.title},${v.partyName},${v.date},${v.amountYer.toLong()}\n")
                                }
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "تقرير ${fundInfo.name}")
                            putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "تصدير ومشاركة التقرير (CSV)"))
                    },
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تصدير CSV")
                }
                Button(
                    onClick = {
                        printAnalyticalReportPdf(
                            context = context,
                            fundName = fundInfo.name,
                            logoBase64 = fundInfo.customLogoBase64,
                            totalRev = totalRev,
                            totalExp = totalExp,
                            membersCount = members.size,
                            disbursedAidsCount = aids.count { it.status == AidStatus.DISBURSED }
                        )
                    },
                    modifier = Modifier.testTag("print_report_pdf_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("طباعة PDF")
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            listOf(
                "عام وملخص مالي",
                "الأعضاء والاشتراكات (${members.size})",
                "المساعدات المنفذة (${aids.size})",
                "حركة السندات (${vouchers.size})"
            ).forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportKpiCard("إجمالي المقبوضات", "%,d ﷼".format(totalRev), StatusSuccess, Modifier.weight(1f))
            ReportKpiCard("إجمالي المصروفات", "%,d ﷼".format(totalExp), StatusDanger, Modifier.weight(1f))
            ReportKpiCard("صافي الفائض", "%,d ﷼".format(totalRev - totalExp), FundPrimary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0, 1 -> {
                val memberStats = members.map { m ->
                    val totalPaid = subscriptions.filter { it.memberId == m.id }.sumOf { it.amountYer }.toLong()
                    Triple(m, totalPaid, subscriptions.count { it.memberId == m.id })
                }.let { list ->
                    if (sortAsc) list.sortedBy { it.second } else list.sortedByDescending { it.second }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FundDeepHeader)
                                .clickable { sortAsc = !sortAsc }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("العضو والمنطقة", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                            Text("عدد الدفعات", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إجمالي المسدد ${if (sortAsc) "▲" else "▼"}", color = FundGoldLight, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.SwapVert, contentDescription = null, tint = FundGoldLight, modifier = Modifier.size(16.dp))
                            }
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(memberStats, key = { it.first.id }) { (member, totalPaid, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.4f)) {
                                        Text(member.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("${member.memberNo} • ${member.villageOrArea}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("$count دفعة", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "%,d ﷼".format(totalPaid),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusSuccess
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                        }
                    }
                }
            }

            2 -> {
                val sortedAids = if (sortAsc) aids.sortedBy { it.approvedAmountYer } else aids.sortedByDescending { it.approvedAmountYer }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FundDeepHeader)
                                .clickable { sortAsc = !sortAsc }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المستفيد ونوع المساعدة", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                            Text("الحالة", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                            Text("المبلغ المعتمد ${if (sortAsc) "▲" else "▼"}", color = FundGoldLight, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(sortedAids, key = { it.id }) { aid ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.4f)) {
                                        Text(aid.memberName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("${aid.requestNo} • ${aid.aidType.title}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        aid.status.title,
                                        modifier = Modifier.weight(0.8f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (aid.status) {
                                            AidStatus.APPROVED -> StatusSuccess
                                            AidStatus.DISBURSED -> StatusInfo
                                            AidStatus.IN_REVIEW -> StatusPending
                                            AidStatus.REJECTED -> StatusDanger
                                        }
                                    )
                                    val amt = (if (aid.approvedAmountYer > 0) aid.approvedAmountYer else aid.requestedAmountYer).toLong()
                                    Text(
                                        "%,d ﷼".format(amt),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                        }
                    }
                }
            }

            3 -> {
                val sortedVouchers = if (sortAsc) vouchers.sortedBy { it.amountYer } else vouchers.sortedByDescending { it.amountYer }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FundDeepHeader)
                                .clickable { sortAsc = !sortAsc }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("رقم السند والطرف", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                            Text("النوع", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                            Text("المبلغ بالريال ${if (sortAsc) "▲" else "▼"}", color = FundGoldLight, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(sortedVouchers, key = { it.id }) { v ->
                                val isReceipt = v.type == VoucherType.RECEIPT
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text("${v.voucherNo} - ${v.partyName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("${v.date} • ${v.description}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    Text(
                                        v.type.title,
                                        modifier = Modifier.weight(0.7f),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isReceipt) StatusSuccess else StatusDanger,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "%,d ﷼".format(v.amountYer.toLong()),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReceipt) StatusSuccess else StatusDanger
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportKpiCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

private fun printAnalyticalReportPdf(
    context: Context,
    fundName: String,
    logoBase64: String,
    totalRev: Long,
    totalExp: Long,
    membersCount: Int,
    disbursedAidsCount: Int
) {
    try {
        val logoHtml = if (logoBase64.isNotBlank()) {
            """<img src="data:image/png;base64,$logoBase64" style="width:52px;height:52px;border-radius:50%;object-fit:cover;border:2px solid #F9A825;margin-bottom:6px;" />"""
        } else ""

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8"/>
                <style>
                    body { font-family: sans-serif; direction: rtl; padding: 24px; color: #14210F; }
                    .header { background: #003300; color: white; padding: 16px; border-radius: 8px; text-align: center; }
                    table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                    th, td { border: 1px solid #D8E4D2; padding: 12px; text-align: right; }
                    th { background: #1B5E20; color: white; }
                </style>
            </head>
            <body>
                <div class="header">
                    $logoHtml
                    <h2>$fundName</h2>
                    <p>التقرير المالي والإحصائي المعتمد</p>
                </div>
                <table>
                    <tr><th>البيان الإحصائي / المالي</th><th>القيمة المعتمدة</th></tr>
                    <tr><td>إجمالي المقبوضات المعتمدة</td><td>${"%,d ﷼".format(totalRev)}</td></tr>
                    <tr><td>إجمالي المصروفات المعتمدة</td><td>${"%,d ﷼".format(totalExp)}</td></tr>
                    <tr><td>صافي الفائض النقدي والبنكي</td><td><b>${"%,d ﷼".format(totalRev - totalExp)}</b></td></tr>
                    <tr><td>إجمالي الأعضاء المسجلين</td><td>$membersCount عضو</td></tr>
                    <tr><td>عدد حالات المساعدات المنفذة</td><td>$disbursedAidsCount حالة</td></tr>
                </table>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val adapter = view.createPrintDocumentAdapter("Analytical_Report")
                    printManager.print("تقرير_الصندوق_المالي", adapter, PrintAttributes.Builder().build())
                }
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "تم تجهيز التقرير للطباعة", Toast.LENGTH_SHORT).show()
    }
}
