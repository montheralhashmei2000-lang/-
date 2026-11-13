package com.example.fund.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    var isLoggedIn by remember(persistedLoggedIn) { mutableStateOf(persistedLoggedIn) }

    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var showSyncCenterDialog by remember { mutableStateOf(false) }
    var showGlobalNavigatorDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }

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
    val contextGroup = currentDestination.resolveContextGroup()
    val isDashboard = currentDestination?.hasRoute<AppRoute.DashboardRoute>() == true
    val canNavigateBack = !isTopLevel && navController.previousBackStackEntry != null

    val currentSettingsTab = remember(navBackStackEntry) {
        if (navBackStackEntry?.destination?.hasRoute<AppRoute.SettingsRoute>() == true) {
            runCatching { navBackStackEntry?.toRoute<AppRoute.SettingsRoute>()?.initialTab ?: 0 }.getOrDefault(0)
        } else 0
    }

    LaunchedEffect(isDashboard) {
        if (!isDashboard) isFabMenuExpanded = false
    }

    BackHandler(enabled = isDashboard && isFabMenuExpanded) {
        isFabMenuExpanded = false
    }

    fun navigateToRoute(route: AppRoute) {
        if (route is AppRoute.LoginRoute) {
            authViewModel.logout()
            isLoggedIn = false
            return
        }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun navigateSibling(route: AppRoute) {
        val currentRouteId = navController.currentBackStackEntry?.destination?.id
        navController.navigate(route) {
            if (currentRouteId != null && !isTopLevel) {
                popUpTo(currentRouteId) { inclusive = true }
            }
            launchSingleTop = true
        }
    }

    fun navigateToHub(hub: BottomHub) {
        val targetRoute = hub.toRoute()
        if (hub == activeHub && !isTopLevel) {
            // Re-selecting active Hub pops detail backstack cleanly to Hub root
            val popped = navController.popBackStack(targetRoute, inclusive = false)
            if (!popped) {
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                }
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
                currentSettingsTab = currentSettingsTab,
                onNavigateRoute = { route -> navigateToRoute(route) },
                onOpenGlobalSearch = { showGlobalNavigatorDialog = true },
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
                        breadcrumbHint = contextGroup?.let { "${it.hubTitle} ◂ $currentTitle" },
                        onNavigateBack = { navController.popBackStack() },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onNavigate = { screen -> navigateToRoute(screen.toRoute()) },
                        onOpenAiAdvisor = { showAiAdvisorDialog = true },
                        onOpenSyncCenter = { showSyncCenterDialog = true },
                        onOpenGlobalSearch = { showGlobalNavigatorDialog = true }
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
                        DashboardQuickFabMenu(
                            expanded = isFabMenuExpanded,
                            onToggle = { isFabMenuExpanded = !isFabMenuExpanded },
                            onNewReceipt = {
                                isFabMenuExpanded = false
                                navigateToRoute(AppRoute.VouchersRoute(typeFilter = "قبض", openNewType = "RECEIPT"))
                            },
                            onNewPayment = {
                                isFabMenuExpanded = false
                                navigateToRoute(AppRoute.VouchersRoute(typeFilter = "صرف", openNewType = "PAYMENT"))
                            },
                            onScanQr = {
                                isFabMenuExpanded = false
                                showQrScannerDialog = true
                            }
                        )
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
                            onOpenQuickScanner = { showQrScannerDialog = true },
                            onOpenGlobalSearch = { showGlobalNavigatorDialog = true }
                        )
                        VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Contextual 1-tap sibling bar inside detail screens
                        AnimatedVisibility(
                            visible = contextGroup != null,
                            enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            contextGroup?.let { group ->
                                ModuleContextBar(
                                    contextGroup = group,
                                    currentScreen = currentScreen,
                                    currentRole = currentUser.role,
                                    onSelectParentHub = { hubRoute ->
                                        val popped = navController.popBackStack(hubRoute, inclusive = false)
                                        if (!popped) navigateToRoute(hubRoute)
                                    },
                                    onSelectSibling = { siblingRoute ->
                                        navigateSibling(siblingRoute)
                                    }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
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
                                            onNavigate = { screen -> navigateToRoute(screen.toRoute()) },
                                            onNavigateRoute = { route -> navigateToRoute(route) },
                                            onOpenQrCheck = { showQrScannerDialog = true },
                                            onOpenAiAdvisor = { showAiAdvisorDialog = true },
                                            onOpenSyncCenter = { showSyncCenterDialog = true }
                                        )
                                    }

                                    composable<AppRoute.FinanceHubRoute> {
                                        FinanceHubScreen(
                                            onNavigateRoute = { route -> navigateToRoute(route) },
                                            onOpenGlobalSearch = { showGlobalNavigatorDialog = true }
                                        )
                                    }

                                    composable<AppRoute.MembersHubRoute> {
                                        MembersHubScreen(
                                            onNavigateRoute = { route -> navigateToRoute(route) },
                                            onOpenQrCheck = { showQrScannerDialog = true },
                                            onOpenGlobalSearch = { showGlobalNavigatorDialog = true }
                                        )
                                    }

                                    composable<AppRoute.AidsHubRoute> {
                                        AidsHubScreen(
                                            onNavigateRoute = { route -> navigateToRoute(route) },
                                            onOpenGlobalSearch = { showGlobalNavigatorDialog = true }
                                        )
                                    }

                                    composable<AppRoute.MembersRoute> { backStackEntry ->
                                        val args = backStackEntry.toRoute<AppRoute.MembersRoute>()
                                        MembersScreen(
                                            onOpenQrCheck = { showQrScannerDialog = true },
                                            initialStatusFilter = args.statusFilter,
                                            highlightMemberId = args.highlightMemberId
                                        )
                                    }

                                    composable<AppRoute.SubscriptionsRoute> { backStackEntry ->
                                        val args = backStackEntry.toRoute<AppRoute.SubscriptionsRoute>()
                                        SubscriptionsScreen(
                                            initialSearch = args.searchMemberNo,
                                            preselectedMemberId = args.preselectedMemberId
                                        )
                                    }

                                    composable<AppRoute.AidsRoute> { backStackEntry ->
                                        val args = backStackEntry.toRoute<AppRoute.AidsRoute>()
                                        AidsScreen(initialStatusFilter = args.statusFilter)
                                    }

                                    composable<AppRoute.BeneficiariesRoute> { BeneficiariesScreen() }
                                    composable<AppRoute.PeriodicAidsRoute> { PeriodicAidsScreen() }
                                    composable<AppRoute.InKindRoute> { InKindScreen() }
                                    composable<AppRoute.TreasuryRoute> {
                                        TreasuryScreen(onNavigate = { screen -> navigateToRoute(screen.toRoute()) })
                                    }

                                    composable<AppRoute.VouchersRoute> { backStackEntry ->
                                        val args = backStackEntry.toRoute<AppRoute.VouchersRoute>()
                                        VouchersScreen(
                                            initialFilter = args.typeFilter,
                                            initialNewVoucherType = args.openNewType
                                        )
                                    }

                                    composable<AppRoute.JournalRoute> { JournalScreen() }
                                    composable<AppRoute.AccountsRoute> { AccountsScreen() }
                                    composable<AppRoute.BudgetsRoute> { BudgetsScreen() }
                                    composable<AppRoute.FinancialReportsRoute> { FinancialReportsScreen() }
                                    composable<AppRoute.CampaignsRoute> { CampaignsScreen() }
                                    composable<AppRoute.DonorsRoute> { DonorsScreen() }
                                    composable<AppRoute.SchedulerRoute> { SchedulerScreen() }
                                    composable<AppRoute.MessagesRoute> { MessagesScreen() }
                                    composable<AppRoute.ReportsRoute> { ReportsScreen() }
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
    }

    if (showGlobalNavigatorDialog) {
        GlobalNavigatorDialog(
            onDismiss = { showGlobalNavigatorDialog = false },
            onNavigateRoute = { route -> navigateToRoute(route) },
            onOpenQrCheck = { showQrScannerDialog = true }
        )
    }

    if (showQrScannerDialog) {
        QrScannerDialog(
            onDismiss = { showQrScannerDialog = false },
            onOpenMemberRecord = { member ->
                showQrScannerDialog = false
                navigateToRoute(AppRoute.MembersRoute(highlightMemberId = member.id))
            },
            onOpenMemberSubscriptions = { member ->
                showQrScannerDialog = false
                navigateToRoute(
                    AppRoute.SubscriptionsRoute(
                        preselectedMemberId = member.id,
                        searchMemberNo = member.memberNo
                    )
                )
            }
        )
    }

    if (showAiAdvisorDialog) {
        AiAdvisorDialog(onDismiss = { showAiAdvisorDialog = false })
    }

    if (showSyncCenterDialog) {
        LocalServerSyncDialog(onDismiss = { showSyncCenterDialog = false })
    }
}

@Composable
private fun DashboardQuickFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onNewReceipt: () -> Unit,
    onNewPayment: () -> Unit,
    onScanQr: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickFabActionChip(
                    label = "سند قبض جديد",
                    icon = Icons.Default.SouthWest,
                    containerColor = StatusSuccess,
                    onClick = onNewReceipt,
                    testTag = "fab_new_receipt"
                )
                QuickFabActionChip(
                    label = "سند صرف جديد",
                    icon = Icons.Default.NorthEast,
                    containerColor = StatusDanger,
                    onClick = onNewPayment,
                    testTag = "fab_new_payment"
                )
                QuickFabActionChip(
                    label = "مسح QR ميداني",
                    icon = Icons.Default.QrCodeScanner,
                    containerColor = FundGoldDark,
                    onClick = onScanQr,
                    testTag = "fab_scan_qr"
                )
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = if (expanded) FundDeepHeader else FundPrimary,
            contentColor = Color.White,
            modifier = Modifier.testTag("quick_fab_menu_btn")
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "إغلاق القائمة السريعة" else "إجراءات سريعة"
            )
        }
    }
}

@Composable
private fun QuickFabActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
