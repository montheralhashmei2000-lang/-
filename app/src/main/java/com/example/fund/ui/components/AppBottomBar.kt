package com.example.fund.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.theme.*

@Composable
fun AppBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onOpenMore: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.DASHBOARD,
            onClick = { onNavigate(Screen.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
            label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FundPrimary,
                selectedTextColor = FundPrimary,
                indicatorColor = FundGold.copy(alpha = 0.25f)
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.AIDS,
            onClick = { onNavigate(Screen.AIDS) },
            icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = "المساعدات") },
            label = { Text("المساعدات", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.AIDS) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FundPrimary,
                selectedTextColor = FundPrimary,
                indicatorColor = FundGold.copy(alpha = 0.25f)
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.MEMBERS,
            onClick = { onNavigate(Screen.MEMBERS) },
            icon = { Icon(Icons.Default.People, contentDescription = "الأعضاء") },
            label = { Text("الأعضاء", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.MEMBERS) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FundPrimary,
                selectedTextColor = FundPrimary,
                indicatorColor = FundGold.copy(alpha = 0.25f)
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.VOUCHERS,
            onClick = { onNavigate(Screen.VOUCHERS) },
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "السندات") },
            label = { Text("السندات", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.VOUCHERS) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FundPrimary,
                selectedTextColor = FundPrimary,
                indicatorColor = FundGold.copy(alpha = 0.25f)
            )
        )

        NavigationBarItem(
            selected = false,
            onClick = onOpenMore,
            icon = { Icon(Icons.Default.Apps, contentDescription = "المزيد") },
            label = { Text("المزيد", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FundPrimary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
