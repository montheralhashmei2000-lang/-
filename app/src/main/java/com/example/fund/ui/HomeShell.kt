package com.example.fund.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.fund.ui.components.*
import com.example.fund.ui.navigation.*
import com.example.fund.ui.screens.*
import com.example.fund.ui.viewmodel.AuthViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeShell(authViewModel: AuthViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()

    val persistedLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    var isLoggedIn by remember(persistedLoggedIn) { mutableStateOf(persistedLoggedIn) }

    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var showSyncCenterDialog by remember { mutableStateOf(false) }

    var financeTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var membersTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var aidsTabIndex by rememberSaveable { mutableIntStateOf(0) }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { isLoggedIn = true },
            authViewModel = authViewModel
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentTitle = currentDestination.resolveTitle()
    val currentScreen = currentDestination.resolveScreen()
    val activeHub = currentDestination.resolveBottomHub()
    val isTopLevel = currentDestination.isTopLevelHub()
    val isDashboard = currentDestination?.hasRoute<AppRoute.DashboardRoute>() == true
    val canNavigateBack = !isTopLevel && navController.previousBackStackEntry != null

    val activeSectionScreen = when {
        currentDestination?.hasRoute<AppRoute.FinanceHubRoute>() == true ->
            financeHubScreens.getOrElse(financeTabIndex) { Screen.VOUCHERS }
        currentDestination?.hasRoute<AppRoute.MembersHubRoute>() == true ->
            membersHubScreens.getOrElse(membersTabIndex) { Screen.MEMBERS }
        currentDestination?.hasRoute<AppRoute.AidsHubRoute>() == true ->
            aidsHubScreens.getOrElse(aidsTabIndex) { Screen.AIDS }
        else -> currentScreen
    }

    val topBarBreadcrumb = when {
        currentDestination?.hasRoute<AppRoute.FinanceHubRoute>() == true ->
            "${BottomHub.FINANCE.title} ◂ ${activeSectionScreen.title}"
        currentDestination?.hasRoute<AppRoute.MembersHubRoute>() == true ->
            "${BottomHub.MEMBERS.title} ◂ ${activeSectionScreen.title}"
        currentDestination?.hasRoute<AppRoute.AidsHubRoute>() == true ->
            "${BottomHub.AIDS.title} ◂ ${activeSectionScreen.title}"
        else -> currentDestination.resolveBreadcrumbHint()
    }

    fun resetHubTab(hub: BottomHub) {
        when (hub) {
            BottomHub.FINANCE -> financeTabIndex = 0
            BottomHub.MEMBERS -> membersTabIndex = 0
            BottomHub.AIDS -> aidsTabIndex = 0
            BottomHub.HOME -> Unit
        }
    }

    fun navigateToRoute(route: AppRoute) {
        if (route is AppRoute.LoginRoute) {
            authViewModel.logout()
            financeTabIndex = 0
            membersTabIndex = 0
            aidsTabIndex = 0
            isLoggedIn = false
            return
        }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun navigateToHub(hub: BottomHub) {
        val targetRoute = hub.toRoute()
        if (hub == activeHub) {
            if (!isTopLevel) {
                val popped = navController.popBackStack(targetRoute, inclusive = false)
                if (!popped) {
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }
            } else {
                // Active bottom tab reselect returns cleanly to the first tab of that section
                resetHubTab(hub)
            }
            return
        }
        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentScreen = currentScreen,
                onNavigateRoute = { route -> navigateToRoute(route) },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 720.dp

            Scaffold(
                topBar = {
                    AppTopBar(
                        screenTitle = currentTitle,
                        canNavigateBack = canNavigateBack,
                        breadcrumbHint = topBarBreadcrumb,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onOpenAiAdvisor = { showAiAdvisorDialog = true },
                        onOpenSyncCenter = { showSyncCenterDialog = true }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        AppBottomBar(
                            activeHub = activeHub,
                            onSelectHub = { hub -> navigateToHub(hub) }
                        )
                    }
                },
                floatingActionButton = {
                    if (isDashboard) {
                        FloatingActionButton(
                            onClick = { showQrScannerDialog = true },
                            containerColor = FundPrimary,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_scan_qr")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح QR ميداني"
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (isWideScreen) {
                        AppNavigationRail(
                            activeHub = activeHub,
                            onSelectHub = { hub -> navigateToHub(hub) },
                            onOpenQuickScanner = { showQrScannerDialog = true }
                        )
                        VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 1040.dp)
                                .fillMaxSize()
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = AppRoute.DashboardRoute,
                                enterTransition = {
                                    if (targetState.destination.isTopLevelHub() && initialState.destination.isTopLevelHub()) {
                                        fadeIn(tween(190))
                                    } else {
                                        slideIntoContainer(
                                            AnimatedContentTransitionScope.SlideDirection.Start,
                                            tween(240)
                                        ) + fadeIn(tween(200))
                                    }
                                },
                                exitTransition = {
                                    if (targetState.destination.isTopLevelHub() && initialState.destination.isTopLevelHub()) {
                                        fadeOut(tween(180))
                                    } else {
                                        slideOutOfContainer(
                                            AnimatedContentTransitionScope.SlideDirection.Start,
                                            tween(240)
                                        ) + fadeOut(tween(180))
                                    }
                                },
                                popEnterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.End,
                                        tween(240)
                                    ) + fadeIn(tween(200))
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.End,
                                        tween(240)
                                    ) + fadeOut(tween(180))
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable<AppRoute.DashboardRoute> {
                                    DashboardScreen(
                                        onOpenQrCheck = { showQrScannerDialog = true },
                                        onOpenAiAdvisor = { showAiAdvisorDialog = true },
                                        onOpenSyncCenter = { showSyncCenterDialog = true }
                                    )
                                }

                                composable<AppRoute.FinanceHubRoute> {
                                    FinanceHubScreen(
                                        selectedTabIndex = financeTabIndex,
                                        onSelectTab = { index -> financeTabIndex = index }
                                    )
                                }

                                composable<AppRoute.MembersHubRoute> {
                                    MembersHubScreen(
                                        selectedTabIndex = membersTabIndex,
                                        onSelectTab = { index -> membersTabIndex = index },
                                        onOpenQrCheck = { showQrScannerDialog = true }
                                    )
                                }

                                composable<AppRoute.AidsHubRoute> {
                                    AidsHubScreen(
                                        selectedTabIndex = aidsTabIndex,
                                        onSelectTab = { index -> aidsTabIndex = index }
                                    )
                                }

                                composable<AppRoute.SchedulerRoute> { SchedulerScreen() }
                                composable<AppRoute.MessagesRoute> { MessagesScreen() }
                                composable<AppRoute.FundInfoRoute> { FundInfoScreen() }
                                composable<AppRoute.UsersRoute> { UsersScreen() }
                                composable<AppRoute.AuditRoute> { AuditScreen() }

                                composable<AppRoute.SettingsRoute> { backStackEntry ->
                                    val args = backStackEntry.toRoute<AppRoute.SettingsRoute>()
                                    SettingsScreen(initialTab = args.initialTab)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrScannerDialog) {
        QrScannerDialog(
            onDismiss = { showQrScannerDialog = false }
        )
    }

    if (showAiAdvisorDialog) {
        AiAdvisorDialog(onDismiss = { showAiAdvisorDialog = false })
    }

    if (showSyncCenterDialog) {
        LocalServerSyncDialog(onDismiss = { showSyncCenterDialog = false })
    }
}
