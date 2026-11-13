package com.example.fund.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.UserRole
import com.example.fund.ui.navigation.AppRoute
import com.example.fund.ui.navigation.Screen
import com.example.fund.ui.navigation.toRoute
import com.example.ui.theme.*

private data class DrawerNavEntry(
    val title: String,
    val icon: ImageVector,
    val screen: Screen,
    val route: AppRoute = screen.toRoute(),
    val adminOnly: Boolean = false,
    val badgeCount: Int = 0
)

@Composable
fun AppDrawer(
    currentScreen: Screen,
    onNavigateRoute: (AppRoute) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val repo = FundRepository.getInstance()
    val currentUser by repo.currentUser.collectAsStateWithLifecycle()
    val fundInfo by repo.fundInfo.collectAsStateWithLifecycle()
    val messages by repo.internalMessages.collectAsStateWithLifecycle()

    val isViewer = currentUser.role == UserRole.VIEWER
    val unreadMessagesCount = messages.count { !it.isRead }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(304.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Drawer Header with Shield Crest & Live User Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(FundDeepHeader, FundPrimaryMedium)
                        )
                    )
                    .statusBarsPadding()
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FundLogoImage(
                            customLogoBase64 = fundInfo.customLogoBase64,
                            contentDescription = "شعار الصندوق",
                            modifier = Modifier.size(54.dp)
                        )
                        Column {
                            Text(
                                fundInfo.name,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                maxLines = 2
                            )
                            Text(
                                "الإدارة والحوكمة وإعدادات النظام",
                                color = FundGoldLight,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(currentUser.fullName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(currentUser.role.title, color = FundGoldLight, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val drawerGroups = listOf(
                "التواصل والجدولة والهوية" to listOf(
                    DrawerNavEntry("المواعيد والجدول", Screen.SCHEDULER.icon, Screen.SCHEDULER),
                    DrawerNavEntry("الرسائل الداخلية", Screen.MESSAGES.icon, Screen.MESSAGES, badgeCount = unreadMessagesCount),
                    DrawerNavEntry("بيانات الصندوق والشعار", Screen.FUND_INFO.icon, Screen.FUND_INFO)
                ),
                "الحوكمة والرقابة والمستخدمون" to listOf(
                    DrawerNavEntry("سجل العمليات الرقابي", Screen.AUDIT.icon, Screen.AUDIT),
                    DrawerNavEntry("إدارة المستخدمين والصلاحيات", Screen.USERS.icon, Screen.USERS, adminOnly = true)
                ),
                "الإعدادات والجلسة" to listOf(
                    DrawerNavEntry("مركز الإعدادات الشامل", Screen.SETTINGS.icon, Screen.SETTINGS, route = AppRoute.SettingsRoute(initialTab = 0)),
                    DrawerNavEntry("تسجيل الخروج / تبديل الحساب", Screen.LOGIN.icon, Screen.LOGIN)
                )
            )

            drawerGroups.forEach { (groupTitle, items) ->
                Text(
                    text = groupTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FundPrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )

                items.forEach { entry ->
                    val isRestricted = isViewer && entry.adminOnly
                    val isSelected = currentScreen == entry.screen

                    NavigationDrawerItem(
                        icon = { Icon(entry.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        label = {
                            Text(
                                text = entry.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            if (!isRestricted) {
                                onNavigateRoute(entry.route)
                                onCloseDrawer()
                            }
                        },
                        badge = {
                            when {
                                isRestricted -> Text("مقيد", fontSize = 10.sp, color = Color.Gray)
                                entry.badgeCount > 0 -> Badge(
                                    containerColor = StatusPending,
                                    contentColor = Color.White
                                ) {
                                    Text("${entry.badgeCount}", fontSize = 10.sp)
                                }
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = FundGold.copy(alpha = 0.2f),
                            selectedIconColor = FundPrimary,
                            selectedTextColor = FundPrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_${entry.screen.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
