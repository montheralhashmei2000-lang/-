package com.example.fund.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fund.model.AidStatus
import com.example.fund.model.VoucherStatus
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.navigation.AppRoute
import com.example.fund.ui.navigation.Screen
import com.example.fund.ui.navigation.toRoute
import com.example.fund.ui.viewmodel.DashboardViewModel
import com.example.ui.theme.*

data class HubModuleItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val badgeText: String,
    val route: AppRoute,
    val screenKey: Screen
)

@Composable
fun FinanceHubScreen(
    onNavigateRoute: (AppRoute) -> Unit,
    onOpenGlobalSearch: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val accounts by dashboardViewModel.accounts.collectAsStateWithLifecycle()
    val vouchers by dashboardViewModel.vouchers.collectAsStateWithLifecycle()
    val budgets by dashboardViewModel.budgets.collectAsStateWithLifecycle()

    val liquidity = accounts.filter { it.code in listOf("1100", "1200", "1300") }.sumOf { it.balance }.toLong()
    val postedVouchers = vouchers.count { it.status == VoucherStatus.POSTED }

    val modules = listOf(
        HubModuleItem(
            title = "سندات القبض والصرف",
            subtitle = "إصدار السندات الرسمية والطباعة ومشاركة PDF",
            icon = Screen.VOUCHERS.icon,
            accentColor = FundPrimary,
            badgeText = "$postedVouchers سند",
            route = AppRoute.VouchersRoute(),
            screenKey = Screen.VOUCHERS
        ),
        HubModuleItem(
            title = "دفتر القيود اليومية",
            subtitle = "مراقبة التوازن المزدوج والقيود اليدوية والتسويات",
            icon = Screen.JOURNAL.icon,
            accentColor = StatusInfo,
            badgeText = "قيد مزدوج",
            route = AppRoute.JournalRoute,
            screenKey = Screen.JOURNAL
        ),
        HubModuleItem(
            title = "إدارة الخزينة والنقدية",
            subtitle = "أرصدة الصندوق وبنك الكريمي والمحافظ والتحويلات",
            icon = Screen.TREASURY.icon,
            accentColor = StatusSuccess,
            badgeText = "%,d ﷼".format(liquidity),
            route = AppRoute.TreasuryRoute,
            screenKey = Screen.TREASURY
        ),
        HubModuleItem(
            title = "دليل الحسابات والبنوك",
            subtitle = "شجرة الحسابات الخماسية والأرصدة الدفترية",
            icon = Screen.ACCOUNTS.icon,
            accentColor = FundGoldDark,
            badgeText = "${accounts.size} حساب",
            route = AppRoute.AccountsRoute,
            screenKey = Screen.ACCOUNTS
        ),
        HubModuleItem(
            title = "الموازنات الشهرية",
            subtitle = "متابعة المخصصات والإنفاق الفعلي والتنبيهات",
            icon = Screen.BUDGETS.icon,
            accentColor = StatusPending,
            badgeText = "${budgets.size} بند",
            route = AppRoute.BudgetsRoute,
            screenKey = Screen.BUDGETS
        ),
        HubModuleItem(
            title = "القوائم المالية والعملات",
            subtitle = "ميزان المراجعة، قائمة الدخل، المركز المالي والصرف",
            icon = Screen.FINANCIAL_REPORTS.icon,
            accentColor = FundDeepHeader,
            badgeText = "6 قوائم",
            route = AppRoute.FinancialReportsRoute,
            screenKey = Screen.FINANCIAL_REPORTS
        ),
        HubModuleItem(
            title = "التقارير التحليلية",
            subtitle = "إحصائيات شاملة وتقارير الأداء التنفيذية",
            icon = Screen.REPORTS.icon,
            accentColor = Color(0xFF5D4037),
            badgeText = "PDF / CSV",
            route = AppRoute.ReportsRoute,
            screenKey = Screen.REPORTS
        )
    )

    AdaptiveHubScaffold(
        hubTitle = "المركز المالي والمحاسبي",
        hubSubtitle = "إدارة متكاملة للسندات، القيود المزدوجة، الخزينة، الموازنات، والقوائم الختامية",
        summaryMetricLabel = "إجمالي السيولة المتاحة",
        summaryMetricValue = "%,d ﷼".format(liquidity),
        modules = modules,
        extraActionLabel = "سند سريع",
        extraActionIcon = Icons.Default.AddCircleOutline,
        onExtraAction = { onNavigateRoute(AppRoute.VouchersRoute(openNewType = "RECEIPT")) },
        onOpenGlobalSearch = onOpenGlobalSearch,
        onNavigateRoute = onNavigateRoute
    )
}

@Composable
fun MembersHubScreen(
    onNavigateRoute: (AppRoute) -> Unit,
    onOpenQrCheck: () -> Unit,
    onOpenGlobalSearch: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val members by dashboardViewModel.members.collectAsStateWithLifecycle()
    val activeCount = members.count { it.status == "نشط" }
    val pendingCount = members.count { it.status != "نشط" }

    val modules = listOf(
        HubModuleItem(
            title = "سجل الأعضاء والبطاقات",
            subtitle = "إدارة بيانات الأعضاء، المناطق والقرى، وبطاقات QR",
            icon = Screen.MEMBERS.icon,
            accentColor = FundPrimary,
            badgeText = "$activeCount نشط",
            route = AppRoute.MembersRoute(),
            screenKey = Screen.MEMBERS
        ),
        HubModuleItem(
            title = "الاشتراكات الشهرية",
            subtitle = "تحصيل الاشتراكات المتعددة العملات وتوليد الإيصالات",
            icon = Screen.SUBSCRIPTIONS.icon,
            accentColor = StatusSuccess,
            badgeText = "تحصيل آلي",
            route = AppRoute.SubscriptionsRoute(),
            screenKey = Screen.SUBSCRIPTIONS
        ),
        HubModuleItem(
            title = "المانحون والتعهدات",
            subtitle = "سجل كبار الداعمين والجهات المانحة والوفاء بالتعهدات",
            icon = Screen.DONORS.icon,
            accentColor = FundGoldDark,
            badgeText = "الداعمون",
            route = AppRoute.DonorsRoute,
            screenKey = Screen.DONORS
        )
    )

    AdaptiveHubScaffold(
        hubTitle = "إدارة الأعضاء والجهات الداعمة",
        hubSubtitle = "متابعة الاشتراكات، البطاقات التعريفية، والمناطق والقرى",
        summaryMetricLabel = "الأعضاء المسجلون ($pendingCount معلق)",
        summaryMetricValue = "$activeCount / ${members.size} نشط",
        modules = modules,
        extraActionLabel = "فحص بطاقة QR",
        extraActionIcon = Icons.Default.QrCodeScanner,
        onExtraAction = onOpenQrCheck,
        onOpenGlobalSearch = onOpenGlobalSearch,
        onNavigateRoute = onNavigateRoute
    )
}

@Composable
fun AidsHubScreen(
    onNavigateRoute: (AppRoute) -> Unit,
    onOpenGlobalSearch: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val aidRequests by dashboardViewModel.aidRequests.collectAsStateWithLifecycle()
    val campaigns by dashboardViewModel.campaigns.collectAsStateWithLifecycle()

    val inReview = aidRequests.count { it.status == AidStatus.IN_REVIEW }
    val disbursed = aidRequests.count { it.status == AidStatus.DISBURSED }

    val modules = listOf(
        HubModuleItem(
            title = "طلبات المساعدة المالية",
            subtitle = "استلام ومراجعة واعتماد وصرف المساعدات الطارئة والمرضية",
            icon = Screen.AIDS.icon,
            accentColor = StatusPending,
            badgeText = "$inReview للمراجعة",
            route = AppRoute.AidsRoute(),
            screenKey = Screen.AIDS
        ),
        HubModuleItem(
            title = "الأسر المستفيدة",
            subtitle = "قاعدة بيانات الأسر ودرجات الاحتياج والتغطية الميدانية",
            icon = Screen.BENEFICIARIES.icon,
            accentColor = FundPrimary,
            badgeText = "الضمان الاجتماعي",
            route = AppRoute.BeneficiariesRoute,
            screenKey = Screen.BENEFICIARIES
        ),
        HubModuleItem(
            title = "المساعدات الدورية والكفالات",
            subtitle = "جداول الكفالات الشهرية والمخصصات المستمرة للأيتام",
            icon = Screen.PERIODIC_AIDS.icon,
            accentColor = StatusInfo,
            badgeText = "كفالات دورية",
            route = AppRoute.PeriodicAidsRoute,
            screenKey = Screen.PERIODIC_AIDS
        ),
        HubModuleItem(
            title = "المساعدات العينية والمستودع",
            subtitle = "متابعة المخزون الغذائي والدوائي وحدود إعادة الطلب",
            icon = Screen.IN_KIND.icon,
            accentColor = FundGoldDark,
            badgeText = "المخزون",
            route = AppRoute.InKindRoute,
            screenKey = Screen.IN_KIND
        ),
        HubModuleItem(
            title = "حملات التبرعات والمشاريع",
            subtitle = "إدارة الحملات التنموية ونسب التمويل المحققة",
            icon = Screen.CAMPAIGNS.icon,
            accentColor = StatusSuccess,
            badgeText = "${campaigns.size} حملة",
            route = AppRoute.CampaignsRoute,
            screenKey = Screen.CAMPAIGNS
        )
    )

    AdaptiveHubScaffold(
        hubTitle = "بوابة الإعانات والمشاريع التنموية",
        hubSubtitle = "دورة اعتماد وصرف المساعدات النقدية والعينية والكفالات والحملات",
        summaryMetricLabel = "طلبات بانتظار الاعتماد ($disbursed مصروفة)",
        summaryMetricValue = "$inReview طلب قيد المراجعة",
        modules = modules,
        extraActionLabel = "مراجعة المعلقة",
        extraActionIcon = Icons.Default.AssignmentLate,
        onExtraAction = { onNavigateRoute(AppRoute.AidsRoute(statusFilter = "قيد المراجعة")) },
        onOpenGlobalSearch = onOpenGlobalSearch,
        onNavigateRoute = onNavigateRoute
    )
}

@Composable
private fun AdaptiveHubScaffold(
    hubTitle: String,
    hubSubtitle: String,
    summaryMetricLabel: String,
    summaryMetricValue: String,
    modules: List<HubModuleItem>,
    extraActionLabel: String? = null,
    extraActionIcon: ImageVector? = null,
    onExtraAction: (() -> Unit)? = null,
    onOpenGlobalSearch: () -> Unit = {},
    onNavigateRoute: (AppRoute) -> Unit
) {
    var localFilter by remember { mutableStateOf("") }
    val filteredModules = remember(modules, localFilter) {
        if (localFilter.isBlank()) modules
        else modules.filter {
            it.title.contains(localFilter, ignoreCase = true) ||
                it.subtitle.contains(localFilter, ignoreCase = true)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp
        var selectedScreenKey by remember(hubTitle) { mutableStateOf(modules.first().screenKey) }
        val selectedDetailModule = modules.find { it.screenKey == selectedScreenKey } ?: modules.first()

        if (isWideScreen) {
            // Canonical Tablet / Landscape List-Detail Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Master List Pane
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HubBannerCard(
                        hubTitle = hubTitle,
                        hubSubtitle = hubSubtitle,
                        summaryMetricLabel = summaryMetricLabel,
                        summaryMetricValue = summaryMetricValue,
                        extraActionLabel = extraActionLabel,
                        extraActionIcon = extraActionIcon,
                        onExtraAction = onExtraAction
                    )

                    OutlinedTextField(
                        value = localFilter,
                        onValueChange = { localFilter = it },
                        placeholder = { Text("تصفية شاشات القسم...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (localFilter.isNotEmpty()) {
                                IconButton(onClick = { localFilter = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (filteredModules.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                            FundEmptyState(
                                icon = Icons.Default.SearchOff,
                                title = "لا توجد شاشة مطابقة",
                                subtitle = "امسح عبارة التصفية لعرض جميع الشاشات.",
                                actionLabel = "إظهار الكل",
                                onAction = { localFilter = "" }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredModules, key = { it.screenKey.name }) { item ->
                                val isSelected = selectedDetailModule.screenKey == item.screenKey
                                HubModuleRowCard(
                                    item = item,
                                    selected = isSelected,
                                    onClick = { selectedScreenKey = item.screenKey }
                                )
                            }
                        }
                    }
                }

                // Detail Content Pane with Expand Full-Screen Option
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    selectedDetailModule.icon,
                                    contentDescription = null,
                                    tint = selectedDetailModule.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    selectedDetailModule.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            TextButton(
                                onClick = { onNavigateRoute(selectedDetailModule.route) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("فتح مستقل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Box(modifier = Modifier.weight(1f)) {
                            RenderEmbeddedScreen(
                                screen = selectedDetailModule.screenKey,
                                onNavigateRoute = onNavigateRoute,
                                onOpenQrCheck = onExtraAction ?: {}
                            )
                        }
                    }
                }
            }
        } else {
            // Compact Phone Layout: Summary Banner + Quick Search + Rich 2-Column Module Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HubBannerCard(
                    hubTitle = hubTitle,
                    hubSubtitle = hubSubtitle,
                    summaryMetricLabel = summaryMetricLabel,
                    summaryMetricValue = summaryMetricValue,
                    extraActionLabel = extraActionLabel,
                    extraActionIcon = extraActionIcon,
                    onExtraAction = onExtraAction
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = localFilter,
                        onValueChange = { localFilter = it },
                        placeholder = { Text("ابحث داخل $hubTitle...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (localFilter.isNotEmpty()) {
                                IconButton(onClick = { localFilter = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    FilledTonalIconButton(
                        onClick = onOpenGlobalSearch,
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.TravelExplore,
                            contentDescription = "البحث الشامل في كل الشاشات",
                            tint = FundPrimary
                        )
                    }
                }

                if (filteredModules.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        FundEmptyState(
                            icon = Icons.Default.SearchOff,
                            title = "لا توجد شاشة مطابقة",
                            subtitle = "جرّب مسح التصفية أو استخدام البحث الشامل للوصول لأي سجل.",
                            actionLabel = "البحث الشامل",
                            onAction = onOpenGlobalSearch,
                            secondaryActionLabel = "إظهار الكل",
                            onSecondaryAction = { localFilter = "" }
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredModules, key = { it.screenKey.name }) { item ->
                            HubGridCard(
                                item = item,
                                onClick = { onNavigateRoute(item.route) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubBannerCard(
    hubTitle: String,
    hubSubtitle: String,
    summaryMetricLabel: String,
    summaryMetricValue: String,
    extraActionLabel: String? = null,
    extraActionIcon: ImageVector? = null,
    onExtraAction: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(FundDeepHeader, FundPrimary)))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = hubTitle,
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = hubSubtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(summaryMetricLabel, fontSize = 10.sp, color = FundGoldLight)
                        Text(
                            text = summaryMetricValue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                        )
                    }
                    if (extraActionLabel != null && onExtraAction != null) {
                        FilledTonalButton(
                            onClick = onExtraAction,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = FundGold,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 42.dp)
                        ) {
                            if (extraActionIcon != null) {
                                Icon(extraActionIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(extraActionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubGridCard(
    item: HubModuleItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .clickable { onClick() }
            .testTag("hub_card_${item.screenKey.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(item.accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(
                    color = item.accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.accentColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        maxLines = 1,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HubModuleRowCard(
    item: HubModuleItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) item.accentColor.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) item.accentColor else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(item.accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.accentColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(item.badgeText, fontSize = 10.sp, color = item.accentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RenderEmbeddedScreen(
    screen: Screen,
    onNavigateRoute: (AppRoute) -> Unit,
    onOpenQrCheck: () -> Unit
) {
    when (screen) {
        Screen.VOUCHERS -> VouchersScreen()
        Screen.JOURNAL -> JournalScreen()
        Screen.TREASURY -> TreasuryScreen(onNavigate = { onNavigateRoute(it.toRoute()) })
        Screen.ACCOUNTS -> AccountsScreen()
        Screen.BUDGETS -> BudgetsScreen()
        Screen.FINANCIAL_REPORTS -> FinancialReportsScreen()
        Screen.REPORTS -> ReportsScreen()
        Screen.MEMBERS -> MembersScreen(onOpenQrCheck = onOpenQrCheck)
        Screen.SUBSCRIPTIONS -> SubscriptionsScreen()
        Screen.DONORS -> DonorsScreen()
        Screen.AIDS -> AidsScreen()
        Screen.BENEFICIARIES -> BeneficiariesScreen()
        Screen.PERIODIC_AIDS -> PeriodicAidsScreen()
        Screen.IN_KIND -> InKindScreen()
        Screen.CAMPAIGNS -> CampaignsScreen()
        else -> VouchersScreen()
    }
}
