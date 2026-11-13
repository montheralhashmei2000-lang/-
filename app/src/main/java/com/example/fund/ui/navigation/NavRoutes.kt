package com.example.fund.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

enum class BottomHub(
    val title: String,
    val icon: ImageVector
) {
    HOME("الرئيسية", Icons.Default.Dashboard),
    FINANCE("المركز المالي", Icons.Default.AccountBalance),
    MEMBERS("الأعضاء والجهات", Icons.Default.People),
    AIDS("الإعانات والمشاريع", Icons.Default.VolunteerActivism)
}

@Serializable
sealed interface AppRoute {
    @Serializable data object LoginRoute : AppRoute
    @Serializable data object DashboardRoute : AppRoute

    // 3 Main Bottom Hubs (in addition to DashboardRoute)
    @Serializable data object FinanceHubRoute : AppRoute
    @Serializable data object MembersHubRoute : AppRoute
    @Serializable data object AidsHubRoute : AppRoute

    // Member & Donor Modules
    @Serializable data class MembersRoute(
        val statusFilter: String = "الكل",
        val highlightMemberId: String = ""
    ) : AppRoute

    @Serializable data class SubscriptionsRoute(
        val preselectedMemberId: String = "",
        val searchMemberNo: String = ""
    ) : AppRoute

    @Serializable data object DonorsRoute : AppRoute

    // Aid & Project Modules
    @Serializable data class AidsRoute(
        val statusFilter: String = "الكل"
    ) : AppRoute

    @Serializable data object BeneficiariesRoute : AppRoute
    @Serializable data object PeriodicAidsRoute : AppRoute
    @Serializable data object InKindRoute : AppRoute
    @Serializable data object CampaignsRoute : AppRoute

    // Finance & Accounting Modules
    @Serializable data object TreasuryRoute : AppRoute
    @Serializable data class VouchersRoute(
        val typeFilter: String = "الكل",
        val openNewType: String = ""
    ) : AppRoute

    @Serializable data object JournalRoute : AppRoute
    @Serializable data object AccountsRoute : AppRoute
    @Serializable data object BudgetsRoute : AppRoute
    @Serializable data object FinancialReportsRoute : AppRoute
    @Serializable data object ReportsRoute : AppRoute

    // Admin & System Modules
    @Serializable data object SchedulerRoute : AppRoute
    @Serializable data object MessagesRoute : AppRoute
    @Serializable data object FundInfoRoute : AppRoute
    @Serializable data object UsersRoute : AppRoute
    @Serializable data object AuditRoute : AppRoute
    @Serializable data class SettingsRoute(val initialTab: Int = 0) : AppRoute
}

data class SiblingNavItem(
    val shortTitle: String,
    val fullTitle: String,
    val icon: ImageVector,
    val screen: Screen,
    val route: AppRoute
)

data class HubContextGroup(
    val hubTitle: String,
    val hubRoute: AppRoute,
    val items: List<SiblingNavItem>
)

fun Screen.toRoute(): AppRoute = when (this) {
    Screen.LOGIN -> AppRoute.LoginRoute
    Screen.DASHBOARD -> AppRoute.DashboardRoute
    Screen.MEMBERS -> AppRoute.MembersRoute()
    Screen.SUBSCRIPTIONS -> AppRoute.SubscriptionsRoute()
    Screen.AIDS -> AppRoute.AidsRoute()
    Screen.BENEFICIARIES -> AppRoute.BeneficiariesRoute
    Screen.PERIODIC_AIDS -> AppRoute.PeriodicAidsRoute
    Screen.IN_KIND -> AppRoute.InKindRoute
    Screen.TREASURY -> AppRoute.TreasuryRoute
    Screen.VOUCHERS -> AppRoute.VouchersRoute()
    Screen.JOURNAL -> AppRoute.JournalRoute
    Screen.ACCOUNTS -> AppRoute.AccountsRoute
    Screen.BUDGETS -> AppRoute.BudgetsRoute
    Screen.FINANCIAL_REPORTS -> AppRoute.FinancialReportsRoute
    Screen.DONORS -> AppRoute.DonorsRoute
    Screen.CAMPAIGNS -> AppRoute.CampaignsRoute
    Screen.SCHEDULER -> AppRoute.SchedulerRoute
    Screen.MESSAGES -> AppRoute.MessagesRoute
    Screen.REPORTS -> AppRoute.ReportsRoute
    Screen.FUND_INFO -> AppRoute.FundInfoRoute
    Screen.USERS -> AppRoute.UsersRoute
    Screen.AUDIT -> AppRoute.AuditRoute
    Screen.SETTINGS -> AppRoute.SettingsRoute()
}

fun BottomHub.toRoute(): AppRoute = when (this) {
    BottomHub.HOME -> AppRoute.DashboardRoute
    BottomHub.FINANCE -> AppRoute.FinanceHubRoute
    BottomHub.MEMBERS -> AppRoute.MembersHubRoute
    BottomHub.AIDS -> AppRoute.AidsHubRoute
}

fun NavDestination?.isTopLevelHub(): Boolean {
    if (this == null) return true
    return hasRoute<AppRoute.DashboardRoute>() ||
        hasRoute<AppRoute.FinanceHubRoute>() ||
        hasRoute<AppRoute.MembersHubRoute>() ||
        hasRoute<AppRoute.AidsHubRoute>()
}

fun NavDestination?.resolveTitle(): String {
    if (this == null) return Screen.DASHBOARD.title
    return when {
        hasRoute<AppRoute.LoginRoute>() -> Screen.LOGIN.title
        hasRoute<AppRoute.DashboardRoute>() -> Screen.DASHBOARD.title
        hasRoute<AppRoute.FinanceHubRoute>() -> BottomHub.FINANCE.title
        hasRoute<AppRoute.MembersHubRoute>() -> BottomHub.MEMBERS.title
        hasRoute<AppRoute.AidsHubRoute>() -> BottomHub.AIDS.title
        hasRoute<AppRoute.MembersRoute>() -> Screen.MEMBERS.title
        hasRoute<AppRoute.SubscriptionsRoute>() -> Screen.SUBSCRIPTIONS.title
        hasRoute<AppRoute.AidsRoute>() -> Screen.AIDS.title
        hasRoute<AppRoute.BeneficiariesRoute>() -> Screen.BENEFICIARIES.title
        hasRoute<AppRoute.PeriodicAidsRoute>() -> Screen.PERIODIC_AIDS.title
        hasRoute<AppRoute.InKindRoute>() -> Screen.IN_KIND.title
        hasRoute<AppRoute.TreasuryRoute>() -> Screen.TREASURY.title
        hasRoute<AppRoute.VouchersRoute>() -> Screen.VOUCHERS.title
        hasRoute<AppRoute.JournalRoute>() -> Screen.JOURNAL.title
        hasRoute<AppRoute.AccountsRoute>() -> Screen.ACCOUNTS.title
        hasRoute<AppRoute.BudgetsRoute>() -> Screen.BUDGETS.title
        hasRoute<AppRoute.FinancialReportsRoute>() -> Screen.FINANCIAL_REPORTS.title
        hasRoute<AppRoute.DonorsRoute>() -> Screen.DONORS.title
        hasRoute<AppRoute.CampaignsRoute>() -> Screen.CAMPAIGNS.title
        hasRoute<AppRoute.SchedulerRoute>() -> Screen.SCHEDULER.title
        hasRoute<AppRoute.MessagesRoute>() -> Screen.MESSAGES.title
        hasRoute<AppRoute.ReportsRoute>() -> Screen.REPORTS.title
        hasRoute<AppRoute.FundInfoRoute>() -> Screen.FUND_INFO.title
        hasRoute<AppRoute.UsersRoute>() -> Screen.USERS.title
        hasRoute<AppRoute.AuditRoute>() -> Screen.AUDIT.title
        hasRoute<AppRoute.SettingsRoute>() -> Screen.SETTINGS.title
        else -> Screen.DASHBOARD.title
    }
}

fun NavDestination?.resolveScreen(): Screen {
    if (this == null) return Screen.DASHBOARD
    return when {
        hasRoute<AppRoute.LoginRoute>() -> Screen.LOGIN
        hasRoute<AppRoute.DashboardRoute>() -> Screen.DASHBOARD
        hasRoute<AppRoute.MembersRoute>() -> Screen.MEMBERS
        hasRoute<AppRoute.SubscriptionsRoute>() -> Screen.SUBSCRIPTIONS
        hasRoute<AppRoute.AidsRoute>() -> Screen.AIDS
        hasRoute<AppRoute.BeneficiariesRoute>() -> Screen.BENEFICIARIES
        hasRoute<AppRoute.PeriodicAidsRoute>() -> Screen.PERIODIC_AIDS
        hasRoute<AppRoute.InKindRoute>() -> Screen.IN_KIND
        hasRoute<AppRoute.TreasuryRoute>() -> Screen.TREASURY
        hasRoute<AppRoute.VouchersRoute>() -> Screen.VOUCHERS
        hasRoute<AppRoute.JournalRoute>() -> Screen.JOURNAL
        hasRoute<AppRoute.AccountsRoute>() -> Screen.ACCOUNTS
        hasRoute<AppRoute.BudgetsRoute>() -> Screen.BUDGETS
        hasRoute<AppRoute.FinancialReportsRoute>() -> Screen.FINANCIAL_REPORTS
        hasRoute<AppRoute.DonorsRoute>() -> Screen.DONORS
        hasRoute<AppRoute.CampaignsRoute>() -> Screen.CAMPAIGNS
        hasRoute<AppRoute.SchedulerRoute>() -> Screen.SCHEDULER
        hasRoute<AppRoute.MessagesRoute>() -> Screen.MESSAGES
        hasRoute<AppRoute.ReportsRoute>() -> Screen.REPORTS
        hasRoute<AppRoute.FundInfoRoute>() -> Screen.FUND_INFO
        hasRoute<AppRoute.UsersRoute>() -> Screen.USERS
        hasRoute<AppRoute.AuditRoute>() -> Screen.AUDIT
        hasRoute<AppRoute.SettingsRoute>() -> Screen.SETTINGS
        else -> Screen.DASHBOARD
    }
}

fun NavDestination?.resolveBottomHub(): BottomHub {
    if (this == null) return BottomHub.HOME
    return when {
        hasRoute<AppRoute.FinanceHubRoute>() ||
            hasRoute<AppRoute.VouchersRoute>() ||
            hasRoute<AppRoute.JournalRoute>() ||
            hasRoute<AppRoute.AccountsRoute>() ||
            hasRoute<AppRoute.TreasuryRoute>() ||
            hasRoute<AppRoute.BudgetsRoute>() ||
            hasRoute<AppRoute.FinancialReportsRoute>() ||
            hasRoute<AppRoute.ReportsRoute>() -> BottomHub.FINANCE

        hasRoute<AppRoute.MembersHubRoute>() ||
            hasRoute<AppRoute.MembersRoute>() ||
            hasRoute<AppRoute.SubscriptionsRoute>() ||
            hasRoute<AppRoute.DonorsRoute>() -> BottomHub.MEMBERS

        hasRoute<AppRoute.AidsHubRoute>() ||
            hasRoute<AppRoute.AidsRoute>() ||
            hasRoute<AppRoute.BeneficiariesRoute>() ||
            hasRoute<AppRoute.PeriodicAidsRoute>() ||
            hasRoute<AppRoute.InKindRoute>() ||
            hasRoute<AppRoute.CampaignsRoute>() -> BottomHub.AIDS

        else -> BottomHub.HOME
    }
}

private val financeSiblings = HubContextGroup(
    hubTitle = BottomHub.FINANCE.title,
    hubRoute = AppRoute.FinanceHubRoute,
    items = listOf(
        SiblingNavItem("السندات", Screen.VOUCHERS.title, Screen.VOUCHERS.icon, Screen.VOUCHERS, AppRoute.VouchersRoute()),
        SiblingNavItem("القيود", Screen.JOURNAL.title, Screen.JOURNAL.icon, Screen.JOURNAL, AppRoute.JournalRoute),
        SiblingNavItem("الخزينة", Screen.TREASURY.title, Screen.TREASURY.icon, Screen.TREASURY, AppRoute.TreasuryRoute),
        SiblingNavItem("الحسابات", Screen.ACCOUNTS.title, Screen.ACCOUNTS.icon, Screen.ACCOUNTS, AppRoute.AccountsRoute),
        SiblingNavItem("الموازنات", Screen.BUDGETS.title, Screen.BUDGETS.icon, Screen.BUDGETS, AppRoute.BudgetsRoute),
        SiblingNavItem("القوائم المالية", Screen.FINANCIAL_REPORTS.title, Screen.FINANCIAL_REPORTS.icon, Screen.FINANCIAL_REPORTS, AppRoute.FinancialReportsRoute),
        SiblingNavItem("التقارير", Screen.REPORTS.title, Screen.REPORTS.icon, Screen.REPORTS, AppRoute.ReportsRoute)
    )
)

private val membersSiblings = HubContextGroup(
    hubTitle = BottomHub.MEMBERS.title,
    hubRoute = AppRoute.MembersHubRoute,
    items = listOf(
        SiblingNavItem("الأعضاء", Screen.MEMBERS.title, Screen.MEMBERS.icon, Screen.MEMBERS, AppRoute.MembersRoute()),
        SiblingNavItem("الاشتراكات", Screen.SUBSCRIPTIONS.title, Screen.SUBSCRIPTIONS.icon, Screen.SUBSCRIPTIONS, AppRoute.SubscriptionsRoute()),
        SiblingNavItem("المانحون", Screen.DONORS.title, Screen.DONORS.icon, Screen.DONORS, AppRoute.DonorsRoute)
    )
)

private val aidsSiblings = HubContextGroup(
    hubTitle = BottomHub.AIDS.title,
    hubRoute = AppRoute.AidsHubRoute,
    items = listOf(
        SiblingNavItem("الطلبات", Screen.AIDS.title, Screen.AIDS.icon, Screen.AIDS, AppRoute.AidsRoute()),
        SiblingNavItem("الأسر المستفيدة", Screen.BENEFICIARIES.title, Screen.BENEFICIARIES.icon, Screen.BENEFICIARIES, AppRoute.BeneficiariesRoute),
        SiblingNavItem("الكفالات", Screen.PERIODIC_AIDS.title, Screen.PERIODIC_AIDS.icon, Screen.PERIODIC_AIDS, AppRoute.PeriodicAidsRoute),
        SiblingNavItem("العينية", Screen.IN_KIND.title, Screen.IN_KIND.icon, Screen.IN_KIND, AppRoute.InKindRoute),
        SiblingNavItem("الحملات", Screen.CAMPAIGNS.title, Screen.CAMPAIGNS.icon, Screen.CAMPAIGNS, AppRoute.CampaignsRoute)
    )
)

private val adminSiblings = HubContextGroup(
    hubTitle = "الإدارة والرقابة",
    hubRoute = AppRoute.DashboardRoute,
    items = listOf(
        SiblingNavItem("الصندوق", Screen.FUND_INFO.title, Screen.FUND_INFO.icon, Screen.FUND_INFO, AppRoute.FundInfoRoute),
        SiblingNavItem("المواعيد", Screen.SCHEDULER.title, Screen.SCHEDULER.icon, Screen.SCHEDULER, AppRoute.SchedulerRoute),
        SiblingNavItem("الرسائل", Screen.MESSAGES.title, Screen.MESSAGES.icon, Screen.MESSAGES, AppRoute.MessagesRoute),
        SiblingNavItem("الرقابة", Screen.AUDIT.title, Screen.AUDIT.icon, Screen.AUDIT, AppRoute.AuditRoute),
        SiblingNavItem("المستخدمون", Screen.USERS.title, Screen.USERS.icon, Screen.USERS, AppRoute.UsersRoute),
        SiblingNavItem("الإعدادات", Screen.SETTINGS.title, Screen.SETTINGS.icon, Screen.SETTINGS, AppRoute.SettingsRoute())
    )
)

fun NavDestination?.resolveContextGroup(): HubContextGroup? {
    if (this == null || isTopLevelHub()) return null
    return when {
        hasRoute<AppRoute.VouchersRoute>() ||
            hasRoute<AppRoute.JournalRoute>() ||
            hasRoute<AppRoute.AccountsRoute>() ||
            hasRoute<AppRoute.TreasuryRoute>() ||
            hasRoute<AppRoute.BudgetsRoute>() ||
            hasRoute<AppRoute.FinancialReportsRoute>() ||
            hasRoute<AppRoute.ReportsRoute>() -> financeSiblings

        hasRoute<AppRoute.MembersRoute>() ||
            hasRoute<AppRoute.SubscriptionsRoute>() ||
            hasRoute<AppRoute.DonorsRoute>() -> membersSiblings

        hasRoute<AppRoute.AidsRoute>() ||
            hasRoute<AppRoute.BeneficiariesRoute>() ||
            hasRoute<AppRoute.PeriodicAidsRoute>() ||
            hasRoute<AppRoute.InKindRoute>() ||
            hasRoute<AppRoute.CampaignsRoute>() -> aidsSiblings

        hasRoute<AppRoute.FundInfoRoute>() ||
            hasRoute<AppRoute.SchedulerRoute>() ||
            hasRoute<AppRoute.MessagesRoute>() ||
            hasRoute<AppRoute.AuditRoute>() ||
            hasRoute<AppRoute.UsersRoute>() ||
            hasRoute<AppRoute.SettingsRoute>() -> adminSiblings

        else -> null
    }
}
