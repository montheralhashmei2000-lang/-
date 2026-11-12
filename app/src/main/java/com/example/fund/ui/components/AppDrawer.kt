package com.example.fund.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.fund.data.FundRepository
import com.example.fund.model.UserRole
import com.example.fund.ui.navigation.Screen
import com.example.ui.theme.*

@Composable
fun AppDrawer(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val repo = FundRepository.getInstance()
    val currentUser by repo.currentUser.collectAsState()
    val fundInfo by repo.fundInfo.collectAsState()
    val isViewer = currentUser.role == UserRole.VIEWER

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Drawer Header with Shield Crest
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(FundDeepHeader, FundPrimaryMedium)
                        )
                    )
                    .padding(20.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Sections
            val sections = listOf(
                "الرئيسية" to listOf(
                    Screen.DASHBOARD
                ),
                "الأعضاء والاشتراكات" to listOf(
                    Screen.MEMBERS,
                    Screen.SUBSCRIPTIONS
                ),
                "المساعدات والضمان" to listOf(
                    Screen.AIDS,
                    Screen.BENEFICIARIES,
                    Screen.PERIODIC_AIDS,
                    Screen.IN_KIND
                ),
                "المالية والمحاسبة المزدوجة" to listOf(
                    Screen.VOUCHERS,
                    Screen.TREASURY,
                    Screen.FINANCIAL_REPORTS,
                    Screen.JOURNAL,
                    Screen.ACCOUNTS,
                    Screen.BUDGETS
                ),
                "التبرعات والمشاريع" to listOf(
                    Screen.CAMPAIGNS,
                    Screen.DONORS
                ),
                "الإدارة والتقارير" to listOf(
                    Screen.REPORTS,
                    Screen.SCHEDULER,
                    Screen.MESSAGES,
                    Screen.FUND_INFO
                ),
                "إدارة النظام والرقابة" to listOf(
                    Screen.SETTINGS,
                    Screen.USERS,
                    Screen.AUDIT,
                    Screen.LOGIN
                )
            )

            sections.forEach { (sectionTitle, screens) ->
                Text(
                    text = sectionTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FundPrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )

                screens.forEach { screen ->
                    // Permissions check: viewer cannot access users screen
                    val isRestricted = isViewer && (screen == Screen.USERS)

                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 13.sp,
                                fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = currentScreen == screen,
                        onClick = {
                            if (!isRestricted) {
                                onNavigate(screen)
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE8EFE8))
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
