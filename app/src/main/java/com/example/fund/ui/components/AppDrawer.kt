package com.example.fund.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val adminOnly: Boolean = false
)

@Composable
fun AppDrawer(
    currentScreen: Screen,
    onNavigateRoute: (AppRoute) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val repo = FundRepository.getInstance()
    val currentUser by repo.currentUser.collectAsState()
    val fundInfo by repo.fundInfo.collectAsState()
    val isViewer = currentUser.role == UserRole.VIEWER

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
                                "نظام الإدارة المالية والرقابية",
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

            Spacer(modifier = Modifier.height(8.dp))

            val drawerGroups = listOf(
                "التواصل والجدولة والإدارة" to listOf(
                    DrawerNavEntry("المواعيد والجدول", Screen.SCHEDULER.icon, Screen.SCHEDULER),
                    DrawerNavEntry("الرسائل الداخلية", Screen.MESSAGES.icon, Screen.MESSAGES),
                    DrawerNavEntry("بيانات الصندوق والشعار", Screen.FUND_INFO.icon, Screen.FUND_INFO),
                    DrawerNavEntry("التقارير والإحصائيات", Screen.REPORTS.icon, Screen.REPORTS)
                ),
                "الحوكمة والرقابة والمستخدمين" to listOf(
                    DrawerNavEntry("سجل العمليات الرقابي", Screen.AUDIT.icon, Screen.AUDIT),
                    DrawerNavEntry("إدارة المستخدمين والصلاحيات", Screen.USERS.icon, Screen.USERS, adminOnly = true)
                ),
                "الطباعة والإعدادات والجلسة" to listOf(
                    DrawerNavEntry(
                        title = "إعدادات الطباعة والسندات",
                        icon = Icons.Default.Print,
                        screen = Screen.SETTINGS,
                        route = AppRoute.SettingsRoute(initialTab = 4)
                    ),
                    DrawerNavEntry("مركز الإعدادات الشامل", Screen.SETTINGS.icon, Screen.SETTINGS, route = AppRoute.SettingsRoute(initialTab = 0)),
                    DrawerNavEntry("تسجيل الخروج / تبديل الحساب", Screen.LOGIN.icon, Screen.LOGIN)
                ),
                "اختصارات المراكز التشغيلية" to listOf(
                    DrawerNavEntry("لوحة المعلومات الرئيسية", Screen.DASHBOARD.icon, Screen.DASHBOARD),
                    DrawerNavEntry("سندات القبض والصرف", Screen.VOUCHERS.icon, Screen.VOUCHERS),
                    DrawerNavEntry("إدارة الأعضاء والقرى", Screen.MEMBERS.icon, Screen.MEMBERS),
                    DrawerNavEntry("طلبات المساعدة", Screen.AIDS.icon, Screen.AIDS)
                )
            )

            drawerGroups.forEach { (groupTitle, items) ->
                Text(
                    text = groupTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FundPrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )

                items.forEach { entry ->
                    val isRestricted = isViewer && entry.adminOnly
                    val isSelected = currentScreen == entry.screen && entry.title != "إعدادات الطباعة والسندات"

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
                            if (isRestricted) {
                                Text("مقيد", fontSize = 10.sp, color = Color.Gray)
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = FundGold.copy(alpha = 0.2f),
                            selectedIconColor = FundPrimary,
                            selectedTextColor = FundPrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
