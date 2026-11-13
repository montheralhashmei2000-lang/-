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

    // 4 Top-Level Hubs (strict Single Entry Point for all operational modules)
    @Serializable data object DashboardRoute : AppRoute
    @Serializable data object FinanceHubRoute : AppRoute
    @Serializable data object MembersHubRoute : AppRoute
    @Serializable data object AidsHubRoute : AppRoute

    // Admin & System Modules (exclusively in AppDrawer)
    @Serializable data object SchedulerRoute : AppRoute
    @Serializable data object MessagesRoute : AppRoute
    @Serializable data object FundInfoRoute : AppRoute
    @Serializable data object UsersRoute : AppRoute
    @Serializable data object AuditRoute : AppRoute
    @Serializable data class SettingsRoute(val initialTab: Int = 0) : AppRoute
}

val financeHubScreens = listOf(
    Screen.VOUCHERS,
    Screen.JOURNAL,
    Screen.ACCOUNTS,
    Screen.TREASURY,
    Screen.BUDGETS,
    Screen.FINANCIAL_REPORTS,
    Screen.REPORTS
)

val membersHubScreens = listOf(
    Screen.MEMBERS,
    Screen.SUBSCRIPTIONS,
    Screen.DONORS
)

val aidsHubScreens = listOf(
    Screen.AIDS,
    Screen.BENEFICIARIES,
    Screen.PERIODIC_AIDS,
    Screen.IN_KIND,
    Screen.CAMPAIGNS
)

val drawerAdminScreens = listOf(
    Screen.SCHEDULER,
    Screen.MESSAGES,
    Screen.FUND_INFO,
    Screen.AUDIT,
    Screen.USERS,
    Screen.SETTINGS
)

fun BottomHub.screens(): List<Screen> = when (this) {
    BottomHub.HOME -> listOf(Screen.DASHBOARD)
    BottomHub.FINANCE -> financeHubScreens
    BottomHub.MEMBERS -> membersHubScreens
    BottomHub.AIDS -> aidsHubScreens
}

fun Screen.hubTabIndex(): Int = when (this) {
    in financeHubScreens -> financeHubScreens.indexOf(this)
    in membersHubScreens -> membersHubScreens.indexOf(this)
    in aidsHubScreens -> aidsHubScreens.indexOf(this)
    else -> 0
}

fun Screen.toRoute(): AppRoute = when (this) {
    Screen.LOGIN -> AppRoute.LoginRoute
    Screen.DASHBOARD -> AppRoute.DashboardRoute
    in financeHubScreens -> AppRoute.FinanceHubRoute
    in membersHubScreens -> AppRoute.MembersHubRoute
    in aidsHubScreens -> AppRoute.AidsHubRoute
    Screen.SCHEDULER -> AppRoute.SchedulerRoute
    Screen.MESSAGES -> AppRoute.MessagesRoute
    Screen.FUND_INFO -> AppRoute.FundInfoRoute
    Screen.USERS -> AppRoute.UsersRoute
    Screen.AUDIT -> AppRoute.AuditRoute
    Screen.SETTINGS -> AppRoute.SettingsRoute()
    else -> AppRoute.DashboardRoute
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
        hasRoute<AppRoute.SchedulerRoute>() -> Screen.SCHEDULER.title
        hasRoute<AppRoute.MessagesRoute>() -> Screen.MESSAGES.title
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
        hasRoute<AppRoute.FinanceHubRoute>() -> Screen.VOUCHERS
        hasRoute<AppRoute.MembersHubRoute>() -> Screen.MEMBERS
        hasRoute<AppRoute.AidsHubRoute>() -> Screen.AIDS
        hasRoute<AppRoute.SchedulerRoute>() -> Screen.SCHEDULER
        hasRoute<AppRoute.MessagesRoute>() -> Screen.MESSAGES
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
        hasRoute<AppRoute.FinanceHubRoute>() -> BottomHub.FINANCE
        hasRoute<AppRoute.MembersHubRoute>() -> BottomHub.MEMBERS
        hasRoute<AppRoute.AidsHubRoute>() -> BottomHub.AIDS
        else -> BottomHub.HOME
    }
}

fun NavDestination?.resolveBreadcrumbHint(): String? {
    if (this == null || isTopLevelHub()) return null
    val title = resolveTitle()
    val screen = resolveScreen()
    val parentLabel = if (screen in drawerAdminScreens) "الإدارة والنظام" else BottomHub.HOME.title
    return "$parentLabel ◂ $title"
}
