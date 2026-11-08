package com.example.fund.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.data.FundRepository
import com.example.fund.model.AidStatus
import com.example.fund.model.CurrencyCode
import com.example.fund.model.Voucher
import com.example.fund.model.VoucherType
import com.example.fund.ui.components.*
import com.example.fund.ui.navigation.Screen
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigate: (Screen) -> Unit,
    onOpenQrCheck: () -> Unit,
    onOpenSyncCenter: () -> Unit = {}
) {
    val repo = FundRepository.getInstance()
    val accounts by repo.accounts.collectAsState()
    val members by repo.members.collectAsState()
    val aidRequests by repo.aidRequests.collectAsState()
    val vouchers by repo.vouchers.collectAsState()
    val campaigns by repo.campaigns.collectAsState()
    val rates by repo.currencyRates.collectAsState()
    val isOnline by repo.isOnline.collectAsState()
    val syncQueue by repo.syncQueue.collectAsState()
    val serverConfig by repo.serverConfig.collectAsState()
    val currentUser by repo.currentUser.collectAsState()

    var selectedVoucherForPrint by remember { mutableStateOf<Voucher?>(null) }
    var activeFeedTab by remember { mutableIntStateOf(0) } // 0: أحدث السندات، 1: كبار المساهمين

    // Financial Metrics
    val treasuryCash = accounts.find { it.code == "1100" }?.balance ?: 0.0
    val bankKareemi = accounts.find { it.code == "1200" }?.balance ?: 0.0
    val wallets = accounts.find { it.code == "1300" }?.balance ?: 0.0
    val totalLiquidity = treasuryCash + bankKareemi + wallets

    val usdRate = rates[CurrencyCode.USD] ?: 2530.0
    val sarRate = rates[CurrencyCode.SAR] ?: 665.0
    val equivalentUsd = (totalLiquidity / usdRate).toLong()
    val equivalentSar = (totalLiquidity / sarRate).toLong()

    val totalRevenue = accounts.filter { it.code.startsWith("4") }.sumOf { it.balance }
    val totalExpense = accounts.filter { it.code.startsWith("5") }.sumOf { it.balance }
    val netSurplus = totalRevenue - totalExpense

    val pendingAids = aidRequests.count { it.status == AidStatus.IN_REVIEW }
    val approvedAids = aidRequests.count { it.status == AidStatus.APPROVED }
    val disbursedAids = aidRequests.count { it.status == AidStatus.DISBURSED }
    val activeMembers = members.count { it.status == "نشط" }

    val topMembers = members.sortedByDescending { it.totalPaidYer }.take(5)

    val revenueTrendData = listOf(
        "يونيو" to 1420000.0,
        "يوليو" to 1750000.0,
        "أغسطس" to 1600000.0,
        "سبتمبر" to 1950000.0,
        "أكتوبر" to 2250000.0,
        "نوفمبر" to 2800000.0
    )

    val aidDistributionData = listOf(
        Triple("مرضية وجراحية", 4100000.0, FundPrimaryMedium),
        Triple("كفالات دورية", 2850000.0, FundGoldDark),
        Triple("مساعدات عينية", 1200000.0, StatusInfo),
        Triple("تشغيلية وإدارية", 550000.0, Color.Gray)
    )

    val aidStatusBars = listOf(
        Triple("مصروفة", disbursedAids, StatusSuccess),
        Triple("معتمدة", approvedAids, StatusInfo),
        Triple("قيد المراجعة", pendingAids, StatusPending)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Unified Banking & Liquidity Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(FundDeepHeader, FundPrimary, FundPrimaryMedium)
                        )
                    )
                    .border(1.dp, FundGold.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Top Greeting & Live Server Status Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(FundGold.copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, FundGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = FundGoldLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "مرحباً، ${currentUser.fullName}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentUser.role.title} • ${serverConfig.serverHost}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            color = if (isOnline) StatusSuccess.copy(alpha = 0.3f) else StatusPending.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isOnline) Color(0xFF69F0AE) else FundGold
                            ),
                            modifier = Modifier.clickable { onOpenSyncCenter() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = FundGoldLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isOnline) "متصل ✔" else "طابور: ${syncQueue.size}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Main Net Liquidity Display
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إجمالي السيولة النقدية والبنكية المتاحة",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "%,d ﷼".format(totalLiquidity.toLong()),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color.Black.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "المعادل: $%,d دولار  |  %,d ر.س".format(equivalentUsd, equivalentSar),
                                color = FundGoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // 3 Sub-Account Glass Boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroAccountBox(
                            title = "الخزينة (1100)",
                            amount = treasuryCash,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.TREASURY) }
                        )
                        HeroAccountBox(
                            title = "الكريمي (1200)",
                            amount = bankKareemi,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.TREASURY) }
                        )
                        HeroAccountBox(
                            title = "المحافظ (1300)",
                            amount = wallets,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.TREASURY) }
                        )
                    }
                }
            }
        }

        // 2. Quick Action Circles Bar (5 Mobile Banking Shortcuts)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularQuickAction(
                    label = "سند مالي",
                    icon = Icons.Default.ReceiptLong,
                    containerColor = FundPrimary.copy(alpha = 0.12f),
                    iconColor = FundPrimary,
                    onClick = { onNavigate(Screen.VOUCHERS) }
                )
                CircularQuickAction(
                    label = "اشتراك",
                    icon = Icons.Default.Payments,
                    containerColor = StatusSuccess.copy(alpha = 0.12f),
                    iconColor = StatusSuccess,
                    onClick = { onNavigate(Screen.SUBSCRIPTIONS) }
                )
                CircularQuickAction(
                    label = "فحص QR",
                    icon = Icons.Default.QrCodeScanner,
                    containerColor = FundGold.copy(alpha = 0.25f),
                    iconColor = FundGoldDark,
                    onClick = onOpenQrCheck
                )
                CircularQuickAction(
                    label = "المساعدات",
                    icon = Icons.Default.VolunteerActivism,
                    containerColor = StatusInfo.copy(alpha = 0.12f),
                    iconColor = StatusInfo,
                    onClick = { onNavigate(Screen.AIDS) }
                )
                CircularQuickAction(
                    label = "المزامنة",
                    icon = Icons.Default.CloudSync,
                    containerColor = StatusPending.copy(alpha = 0.12f),
                    iconColor = StatusPending,
                    onClick = onOpenSyncCenter
                )
            }
        }

        // 3. 2x2 Clean Executive KPI Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExecutiveKpiCard(
                    title = "إجمالي الإيرادات",
                    value = "%,d ﷼".format(totalRevenue.toLong()),
                    subtitle = "${vouchers.count { it.type == VoucherType.RECEIPT }} سند قبض",
                    icon = Icons.Default.TrendingUp,
                    color = StatusSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.FINANCIAL_REPORTS) }
                )
                ExecutiveKpiCard(
                    title = "إجمالي المصروفات",
                    value = "%,d ﷼".format(totalExpense.toLong()),
                    subtitle = "الفائض: %,d ﷼".format(netSurplus.toLong()),
                    icon = Icons.Default.TrendingDown,
                    color = StatusDanger,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.FINANCIAL_REPORTS) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExecutiveKpiCard(
                    title = "الأعضاء المسجلون",
                    value = "$activeMembers عضو نشط",
                    subtitle = "من إجمالي ${members.size} عضو",
                    icon = Icons.Default.People,
                    color = FundPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.MEMBERS) }
                )
                ExecutiveKpiCard(
                    title = "طلبات المساعدة",
                    value = "$pendingAids قيد المراجعة",
                    subtitle = "$approvedAids معتمدة • $disbursedAids مصروفة",
                    icon = Icons.Default.AssignmentLate,
                    color = StatusPending,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.AIDS) }
                )
            }
        }

        // 4. Tabbed Recent Vouchers & Top Contributors Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Segmented Header Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FundLightSurfaceSecondary)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SegmentTabButton(
                        title = "أحدث السندات المالية (${vouchers.size})",
                        selected = activeFeedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { activeFeedTab = 0 }
                    )
                    SegmentTabButton(
                        title = "كبار المساهمين (Top 5)",
                        selected = activeFeedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { activeFeedTab = 1 }
                    )
                }

                if (activeFeedTab == 0) {
                    vouchers.take(4).forEach { v ->
                        val isReceipt = v.type == VoucherType.RECEIPT
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(FundLightSurfaceSecondary.copy(alpha = 0.6f))
                                .clickable { selectedVoucherForPrint = v }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isReceipt) StatusSuccess.copy(alpha = 0.14f) else StatusDanger.copy(alpha = 0.14f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isReceipt) Icons.Default.SouthWest else Icons.Default.NorthEast,
                                        contentDescription = null,
                                        tint = if (isReceipt) StatusSuccess else StatusDanger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${v.voucherNo} • ${v.partyName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${v.date} — ${v.description}",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isReceipt) "+" else "-"}%,d ﷼".format(v.amountYer.toLong()),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (isReceipt) StatusSuccess else StatusDanger,
                                    maxLines = 1
                                )
                                Text(
                                    text = "طباعة PDF 🖨",
                                    fontSize = 9.sp,
                                    color = FundPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { onNavigate(Screen.VOUCHERS) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("عرض وإدارة كافة السندات المالية ←", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
                    }
                } else {
                    topMembers.forEachIndexed { index, m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (index % 2 == 0) FundLightSurfaceSecondary.copy(alpha = 0.6f) else Color.Transparent)
                                .clickable { onNavigate(Screen.MEMBERS) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(
                                            if (index == 0) FundGold else FundPrimary.copy(alpha = 0.12f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (index == 0) Color.Black else FundPrimary
                                    )
                                }
                                Column {
                                    Text(
                                        text = m.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${m.memberNo} • ${m.villageOrArea}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Text(
                                text = "%,d ﷼".format(m.totalPaidYer.toLong()),
                                fontWeight = FontWeight.Black,
                                color = FundPrimary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 5. Visual Analytics Section (Cleanly Aligned Charts)
        TrendLineChart(dataPoints = revenueTrendData)

        DonutChart(
            items = aidDistributionData,
            centerText = "إجمالي: %,d ﷼".format(totalExpense.toLong()),
            centerSubtext = "المصروفات"
        )

        ProportionalBarChart(items = aidStatusBars)

        // 6. Active Campaigns & Quick Modules Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickModuleButton("الحملات (${campaigns.size})", Icons.Default.Campaign, Modifier.weight(1f)) { onNavigate(Screen.CAMPAIGNS) }
            QuickModuleButton("القوائم المالية", Icons.Default.Assessment, Modifier.weight(1f)) { onNavigate(Screen.FINANCIAL_REPORTS) }
            QuickModuleButton("دفتر اليومية", Icons.Default.MenuBook, Modifier.weight(1f)) { onNavigate(Screen.JOURNAL) }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    selectedVoucherForPrint?.let { v ->
        VoucherPrintDialog(
            voucher = v,
            onDismiss = { selectedVoucherForPrint = null }
        )
    }
}

@Composable
private fun HeroAccountBox(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "%,d ﷼".format(amount.toLong()),
                color = FundGoldLight,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CircularQuickAction(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun ExecutiveKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SegmentTabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) FundPrimary else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickModuleButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
