package com.example.fund.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
            currentScreen = currentScreen,
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
    currentScreen: Screen,
    onDismiss: () -> Unit,
    onSelectScreen: (Screen) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredScreens = Screen.entries.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "دليل وحدات وأقسام النظام (${Screen.entries.size} قسماً)",
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = FundPrimary
                        )
                        Text(
                            "انتقل بسرعة إلى أي وحدة مالية أو إدارية أو رقابية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن قسم (مثل: القوائم المالية، اليومية، الكفالات...)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredScreens, key = { it.name }) { screen ->
                        val isSelected = screen == currentScreen
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clickable { onSelectScreen(screen) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) FundPrimary.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) FundPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) FundPrimary else FundPrimary.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        screen.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else FundPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) FundPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = screen.category,
                                    fontSize = 8.sp,
                                    color = FundGoldDark,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
