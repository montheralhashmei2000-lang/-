package com.example.fund.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.fund.ui.navigation.Screen
import com.example.fund.ui.navigation.aidsHubScreens
import com.example.fund.ui.navigation.financeHubScreens
import com.example.fund.ui.navigation.membersHubScreens
import com.example.fund.ui.viewmodel.DashboardViewModel
import com.example.fund.util.FormatUtils
import com.example.fund.util.LocalScreenFormat
import com.example.ui.theme.*

data class SectionTabItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector = screen.icon,
    val badgeCount: Int = 0
)

@Composable
fun FinanceHubScreen(
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    val labels = mapOf(
        Screen.VOUCHERS to "السندات",
        Screen.JOURNAL to "القيود اليومية",
        Screen.ACCOUNTS to "شجرة الحسابات",
        Screen.TREASURY to "الخزينة",
        Screen.BUDGETS to "الموازنات",
        Screen.FINANCIAL_REPORTS to "التقارير المالية",
        Screen.REPORTS to "التقارير التحليلية"
    )
    val tabs = financeHubScreens.map { screen ->
        SectionTabItem(
            screen = screen,
            label = labels[screen] ?: screen.title
        )
    }

    SectionTabScaffold(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onSelectTab = onSelectTab,
        sectionTestTag = "finance_section_tabs"
    )
}

@Composable
fun MembersHubScreen(
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    onOpenQrCheck: () -> Unit
) {
    val labels = mapOf(
        Screen.MEMBERS to "الأعضاء",
        Screen.SUBSCRIPTIONS to "الاشتراكات",
        Screen.DONORS to "المانحون والمساهمون"
    )
    val tabs = membersHubScreens.map { screen ->
        SectionTabItem(
            screen = screen,
            label = labels[screen] ?: screen.title
        )
    }

    SectionTabScaffold(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onSelectTab = onSelectTab,
        onOpenQrCheck = onOpenQrCheck,
        sectionTestTag = "members_section_tabs"
    )
}

@Composable
fun AidsHubScreen(
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val aidRequests by dashboardViewModel.aidRequests.collectAsStateWithLifecycle()
    val inReviewCount = aidRequests.count { it.status == AidStatus.IN_REVIEW }

    val labels = mapOf(
        Screen.AIDS to "طلبات المساعدات",
        Screen.BENEFICIARIES to "الأسر المستفيدة",
        Screen.PERIODIC_AIDS to "المساعدات الدورية",
        Screen.IN_KIND to "مخزن العينية",
        Screen.CAMPAIGNS to "الحملات"
    )
    val tabs = aidsHubScreens.map { screen ->
        SectionTabItem(
            screen = screen,
            label = labels[screen] ?: screen.title,
            badgeCount = if (screen == Screen.AIDS) inReviewCount else 0
        )
    }

    SectionTabScaffold(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onSelectTab = onSelectTab,
        sectionTestTag = "aids_section_tabs"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionTabScaffold(
    tabs: List<SectionTabItem>,
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    sectionTestTag: String,
    onOpenQrCheck: () -> Unit = {}
) {
    val screenFormat = LocalScreenFormat.current
    val safeIndex = selectedTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    val activeScreen = tabs.getOrNull(safeIndex)?.screen ?: Screen.DASHBOARD

    BackHandler(enabled = safeIndex > 0) {
        onSelectTab(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = safeIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = FundPrimary,
            edgePadding = 10.dp,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(safeIndex),
                    height = 3.dp,
                    color = FundPrimary
                )
            },
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(sectionTestTag)
        ) {
            tabs.forEachIndexed { index, item ->
                val selected = index == safeIndex
                Tab(
                    selected = selected,
                    onClick = { onSelectTab(index) },
                    selectedContentColor = FundPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("section_tab_${item.screen.name.lowercase()}"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = item.label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.badgeCount > 0) {
                                Badge(
                                    containerColor = StatusPending,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = FormatUtils.formatCodeOrNumber(item.badgeCount.toString(), screenFormat),
                                        fontSize = 9.sp,
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            RenderEmbeddedScreen(
                screen = activeScreen,
                onOpenQrCheck = onOpenQrCheck
            )
        }
    }
}

@Composable
private fun RenderEmbeddedScreen(
    screen: Screen,
    onOpenQrCheck: () -> Unit
) {
    when (screen) {
        Screen.VOUCHERS -> VouchersScreen()
        Screen.JOURNAL -> JournalScreen()
        Screen.ACCOUNTS -> AccountsScreen()
        Screen.TREASURY -> TreasuryScreen()
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
