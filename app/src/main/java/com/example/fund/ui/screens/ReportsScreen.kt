package com.example.fund.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.AidStatus
import com.example.fund.model.AidType
import com.example.fund.model.VoucherStatus
import com.example.fund.model.VoucherType
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.util.FormatUtils
import com.example.fund.util.LocalScreenFormat
import com.example.fund.util.OfficialPrintHelper
import com.example.ui.theme.*

private fun csvEscape(value: String): String {
    val clean = value.replace("\"", "\"\"")
    return if (clean.contains(',') || clean.contains('"') || clean.contains('\n')) "\"$clean\"" else clean
}

@Composable
fun ReportsScreen() {
    val repo = FundRepository.getInstance()
    val members by repo.members.collectAsStateWithLifecycle()
    val subscriptions by repo.subscriptions.collectAsStateWithLifecycle()
    val vouchers by repo.vouchers.collectAsStateWithLifecycle()
    val aids by repo.aidRequests.collectAsStateWithLifecycle()
    val regions by repo.regions.collectAsStateWithLifecycle()
    val fundInfo by repo.fundInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val screenFormat = LocalScreenFormat.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var sortAsc by remember { mutableStateOf(false) }

    val postedReceipts = vouchers.filter { it.type == VoucherType.RECEIPT && it.status == VoucherStatus.POSTED }
    val postedPayments = vouchers.filter { it.type == VoucherType.PAYMENT && it.status == VoucherStatus.POSTED }
    val totalRev = postedReceipts.sumOf { it.amountYer }.toLong()
    val totalExp = postedPayments.sumOf { it.amountYer }.toLong()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("reports_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                Text(
                    "مركز التقارير التحليلية والإحصائية",
                    fontFamily = AmiriFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FundPrimary
                )
                Text(
                    "تقارير تفصيلية مع الفرز التفاعلي، التصدير CSV، والطباعة الرسمية PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val csvBuilder = StringBuilder("\uFEFF")
                        csvBuilder.append("تقرير,${csvEscape(fundInfo.name)}\n")
                        when (selectedTab) {
                            0 -> {
                                csvBuilder.append("البند,القيمة\n")
                                csvBuilder.append("إجمالي المقبوضات المعتمدة,$totalRev\n")
                                csvBuilder.append("إجمالي المصروفات المعتمدة,$totalExp\n")
                                csvBuilder.append("صافي الفائض,${totalRev - totalExp}\n")
                                csvBuilder.append("إجمالي الأعضاء,${members.size}\n")
                                csvBuilder.append("طلبات المساعدة,${aids.size}\n")
                            }
                            1 -> {
                                csvBuilder.append("الكود,اسم العضو,المنطقة,القرية,إجمالي المسدد (ريال)\n")
                                members.forEach { m ->
                                    val totalPaid = subscriptions.filter { it.memberId == m.id }.sumOf { it.amountYer }.toLong()
                                    csvBuilder.append(
                                        "${csvEscape(m.memberNo)},${csvEscape(m.fullName)},${csvEscape(m.region)},${csvEscape(m.village)},$totalPaid\n"
                                    )
                                }
                            }
                            2 -> {
                                csvBuilder.append("رقم الطلب,المستفيد,النوع,الحالة,المبلغ المعتمد\n")
                                aids.forEach { a ->
                                    csvBuilder.append(
                                        "${csvEscape(a.requestNo)},${csvEscape(a.memberName)},${csvEscape(a.aidType.title)},${csvEscape(a.status.title)},${a.approvedAmountYer.toLong()}\n"
                                    )
                                }
                            }
                            3 -> {
                                csvBuilder.append("رقم السند,النوع,الطرف,التاريخ,المبلغ بالريال\n")
                                vouchers.forEach { v ->
                                    csvBuilder.append(
                                        "${csvEscape(v.voucherNo)},${csvEscape(v.type.title)},${csvEscape(v.partyName)},${csvEscape(v.date)},${v.amountYer.toLong()}\n"
                                    )
                                }
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "تقرير ${fundInfo.name}")
                            putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
                        }
                        val launched = runCatching {
                            context.startActivity(Intent.createChooser(sendIntent, "تصدير ومشاركة التقرير (CSV)"))
                        }.isSuccess
                        if (!launched) {
                            Toast.makeText(context, "لا يوجد تطبيق متاح لمشاركة ملفات CSV", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تصدير CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        OfficialPrintHelper.printAnalyticalReportPdf(
                            context = context,
                            fundInfo = fundInfo,
                            totalRev = totalRev,
                            totalExp = totalExp,
                            membersCount = members.size,
                            disbursedAidsCount = aids.count { it.status == AidStatus.DISBURSED }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("print_report_pdf_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("طباعة PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = FundPrimary
        ) {
            listOf(
                "ملخص تنفيذي شامل",
                "الأعضاء والاشتراكات (${members.size})",
                "المساعدات (${aids.size})",
                "السندات (${vouchers.size})"
            ).forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportKpiCard("إجمالي المقبوضات", FormatUtils.formatAmount(totalRev.toDouble(), screenFormat), StatusSuccess, Modifier.weight(1f))
            ReportKpiCard("إجمالي المصروفات", FormatUtils.formatAmount(totalExp.toDouble(), screenFormat), StatusDanger, Modifier.weight(1f))
            ReportKpiCard("صافي الفائض", FormatUtils.formatAmount((totalRev - totalExp).toDouble(), screenFormat), FundPrimary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))

        when (selectedTab) {
            0 -> {
                // Executive Overview Tab: Regional coverage + Aid types breakdown
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("التغطية الجغرافية وحصيلة المناطق", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FundPrimary)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                if (regions.isEmpty()) {
                                    Text("لا توجد مناطق مسجلة حالياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    regions.forEach { region ->
                                        val regMembers = members.filter { it.region == region.name }
                                        val regPaid = regMembers.sumOf { it.totalPaidYer }.toLong()
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(region.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("${region.villages.size} قرية • ${regMembers.size} عضو", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                FormatUtils.formatAmount(regPaid.toDouble(), screenFormat, inTable = true),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = StatusSuccess,
                                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("تحليل المساعدات حسب التصنيف", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FundPrimary)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                AidType.entries.forEach { aidType ->
                                    val matching = aids.filter { it.aidType == aidType }
                                    val totalAmt = matching.sumOf {
                                        if (it.approvedAmountYer > 0) it.approvedAmountYer else it.requestedAmountYer
                                    }.toLong()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${aidType.title} (${matching.size} طلب)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            FormatUtils.formatAmount(totalAmt.toDouble(), screenFormat, inTable = true),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FundGoldDark,
                                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    if (memberStats.isEmpty()) {
                        FundEmptyState(
                            icon = Icons.Default.PeopleOutline,
                            title = "لا يوجد أعضاء مسجلون",
                            subtitle = "ستظهر إحصائيات سداد الأعضاء هنا فور إضافة أول عضو."
                        )
                    } else {
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
                                Text("العضو والمنطقة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.4f))
                                Text("الدفعات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("المسدد ${if (sortAsc) "▲" else "▼"}", color = FundGoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                            Text(member.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${member.memberNo} • ${member.villageOrArea}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text("$count دفعة", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            FormatUtils.formatAmount(totalPaid.toDouble(), screenFormat, inTable = true),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
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
            }

            2 -> {
                val sortedAids = if (sortAsc) aids.sortedBy { it.approvedAmountYer } else aids.sortedByDescending { it.approvedAmountYer }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    if (sortedAids.isEmpty()) {
                        FundEmptyState(
                            icon = Icons.Default.VolunteerActivism,
                            title = "لا توجد طلبات مساعدة",
                            subtitle = "ستظهر تقارير المساعدات المنفذة والمعتمدة هنا."
                        )
                    } else {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FundDeepHeader)
                                    .clickable { sortAsc = !sortAsc }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المستفيد ونوع المساعدة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.4f))
                                Text("الحالة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f))
                                Text("المعتمد ${if (sortAsc) "▲" else "▼"}", color = FundGoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
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
                                            Text(aid.memberName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                        val amt = if (aid.approvedAmountYer > 0) aid.approvedAmountYer else aid.requestedAmountYer
                                        Text(
                                            FormatUtils.formatAmount(amt, screenFormat, inTable = true),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    if (sortedVouchers.isEmpty()) {
                        FundEmptyState(
                            icon = Icons.Default.Receipt,
                            title = "لا توجد سندات مسجلة",
                            subtitle = "ستظهر حركات السندات المالية هنا."
                        )
                    } else {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FundDeepHeader)
                                    .clickable { sortAsc = !sortAsc }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("رقم السند والطرف", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                                Text("النوع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                Text("المبلغ ${if (sortAsc) "▲" else "▼"}", color = FundGoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
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
                                            Text("${FormatUtils.formatCodeOrNumber(v.voucherNo, screenFormat)} - ${v.partyName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${FormatUtils.formatDate(v.date, screenFormat)} • ${v.description}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text(
                                            v.type.title,
                                            modifier = Modifier.weight(0.7f),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isReceipt) StatusSuccess else StatusDanger,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            FormatUtils.formatAmount(v.amountYer, screenFormat, inTable = true),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
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
}

@Composable
private fun ReportKpiCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
