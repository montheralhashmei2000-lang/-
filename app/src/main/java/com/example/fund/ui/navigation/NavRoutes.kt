package com.example.fund.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
    Screen.VOUCHERS -> AppRoute.VOUCHERS_ROUTE_DEFAULT
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

private val AppRoute.Companion.VOUCHERS_ROUTE_DEFAULT: AppRoute.VouchersRoute
    get() = AppRoute.VouchersRoute()

fun BottomHub.toRoute(): AppRoute = when (this) {
    BottomHub.HOME -> AppRoute.DashboardRoute
    BottomHub.FINANCE -> AppRoute.FinanceHubRoute
    BottomHub.MEMBERS -> AppRoute.MembersHubRoute
    BottomHub.AIDS -> AppRoute.AidsHubRoute
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
