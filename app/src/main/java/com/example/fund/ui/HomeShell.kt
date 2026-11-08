package com.example.fund.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.ui.components.*
import com.example.fund.ui.navigation.Screen
import com.example.fund.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeShell() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var isLoggedIn by remember { mutableStateOf(true) }

    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var showSyncCenterDialog by remember { mutableStateOf(false) }
    var showMoreSectionsSheet by remember { mutableStateOf(false) }

    // System Back Handler
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    if (!isLoggedIn || currentScreen == Screen.LOGIN) {
        LoginScreen(
            onLoginSuccess = {
                isLoggedIn = true
                currentScreen = Screen.DASHBOARD
            }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentScreen = currentScreen,
                onNavigate = { screen ->
                    if (screen == Screen.LOGIN) {
                        isLoggedIn = false
                    }
                    currentScreen = screen
                },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    currentScreen = currentScreen,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onNavigate = { screen ->
                        if (screen == Screen.LOGIN) {
                            isLoggedIn = false
                        }
                        currentScreen = screen
                    },
                    onOpenAiAdvisor = { showAiAdvisorDialog = true },
                    onOpenSyncCenter = { showSyncCenterDialog = true }
                )
            },
            bottomBar = {
                AppBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it },
                    onOpenMore = { showMoreSectionsSheet = true }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        onNavigate = { screen ->
                            if (screen == Screen.LOGIN) isLoggedIn = false
                            currentScreen = screen
                        },
                        onOpenQrCheck = { showQrScannerDialog = true },
                        onOpenSyncCenter = { showSyncCenterDialog = true }
                    )
                    Screen.MEMBERS -> MembersScreen(
                        onOpenQrCheck = { showQrScannerDialog = true }
                    )
                    Screen.SUBSCRIPTIONS -> SubscriptionsScreen()
                    Screen.AIDS -> AidsScreen()
                    Screen.TREASURY -> TreasuryScreen(onNavigate = { currentScreen = it })
                    Screen.VOUCHERS -> VouchersScreen()
                    Screen.FINANCIAL_REPORTS -> FinancialReportsScreen()
                    Screen.JOURNAL -> JournalScreen()
                    Screen.ACCOUNTS -> AccountsScreen()
                    Screen.BUDGETS -> BudgetsScreen()
                    Screen.CAMPAIGNS -> CampaignsScreen()
                    Screen.DONORS -> DonorsScreen()
                    Screen.BENEFICIARIES -> BeneficiariesScreen()
                    Screen.PERIODIC_AIDS -> PeriodicAidsScreen()
                    Screen.IN_KIND -> InKindScreen()
                    Screen.SCHEDULER -> SchedulerScreen()
                    Screen.MESSAGES -> MessagesScreen()
                    Screen.REPORTS -> ReportsScreen()
                    Screen.FUND_INFO -> FundInfoScreen()
                    Screen.USERS -> UsersScreen()
                    Screen.AUDIT -> AuditScreen()
                    Screen.SETTINGS -> SettingsScreen()
                    Screen.LOGIN -> LoginScreen(onLoginSuccess = { currentScreen = Screen.DASHBOARD })
                }
            }
        }
    }

    // Global Dialogs
    if (showQrScannerDialog) {
        QrScannerDialog(onDismiss = { showQrScannerDialog = false })
    }

    if (showAiAdvisorDialog) {
        AiAdvisorDialog(onDismiss = { showAiAdvisorDialog = false })
    }

    if (showSyncCenterDialog) {
        LocalServerSyncDialog(onDismiss = { showSyncCenterDialog = false })
    }

    if (showMoreSectionsSheet) {
        MoreSectionsModal(
            onDismiss = { showMoreSectionsSheet = false },
            onSelectScreen = { screen ->
                showMoreSectionsSheet = false
                if (screen == Screen.LOGIN) isLoggedIn = false
                currentScreen = screen
            }
        )
    }
}

@Composable
private fun MoreSectionsModal(
    onDismiss: () -> Unit,
    onSelectScreen: (Screen) -> Unit
) {
    val allScreens = Screen.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("جميع أقسام ووظائف الصندوق (22 قسماً)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FundDeepHeader)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                items(allScreens) { screen ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectScreen(screen) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(screen.icon, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
