package com.example.fund.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.AidStatus
import com.example.fund.ui.navigation.BottomHub
import com.example.ui.theme.*

@Composable
fun AppBottomBar(
    activeHub: BottomHub,
    onSelectHub: (BottomHub) -> Unit
) {
    val repo = FundRepository.getInstance()
    val aidRequests by repo.aidRequests.collectAsStateWithLifecycle()
    val syncQueue by repo.syncQueue.collectAsStateWithLifecycle()
    val pendingAidsCount = aidRequests.count { it.status == AidStatus.IN_REVIEW }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        BottomHub.entries.forEach { hub ->
            val selected = activeHub == hub
            val badgeCount = when (hub) {
                BottomHub.AIDS -> pendingAidsCount
                BottomHub.FINANCE -> syncQueue.size
                else -> 0
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onSelectHub(hub) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge(
                                    containerColor = if (hub == BottomHub.AIDS) StatusPending else StatusDanger,
                                    contentColor = Color.White
                                ) {
                                    Text(if (badgeCount > 9) "9+" else "$badgeCount", fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = hub.icon,
                            contentDescription = hub.title
                        )
                    }
                },
                label = {
                    Text(
                        text = hub.title,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FundPrimary,
                    selectedTextColor = FundPrimary,
                    indicatorColor = FundGold.copy(alpha = 0.25f)
                ),
                modifier = Modifier.testTag("bottom_hub_${hub.name.lowercase()}")
            )
        }
    }
}

@Composable
fun AppNavigationRail(
    activeHub: BottomHub,
    onSelectHub: (BottomHub) -> Unit,
    onOpenQuickScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repo = FundRepository.getInstance()
    val aidRequests by repo.aidRequests.collectAsStateWithLifecycle()
    val syncQueue by repo.syncQueue.collectAsStateWithLifecycle()
    val pendingAidsCount = aidRequests.count { it.status == AidStatus.IN_REVIEW }

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.weight(0.15f))
        BottomHub.entries.forEach { hub ->
            val selected = activeHub == hub
            val badgeCount = when (hub) {
                BottomHub.AIDS -> pendingAidsCount
                BottomHub.FINANCE -> syncQueue.size
                else -> 0
            }

            NavigationRailItem(
                selected = selected,
                onClick = { onSelectHub(hub) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge(
                                    containerColor = if (hub == BottomHub.AIDS) StatusPending else StatusDanger,
                                    contentColor = Color.White
                                ) {
                                    Text(if (badgeCount > 9) "9+" else "$badgeCount", fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        Icon(hub.icon, contentDescription = hub.title)
                    }
                },
                label = {
                    Text(
                        text = hub.title,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = FundPrimary,
                    selectedTextColor = FundPrimary,
                    indicatorColor = FundGold.copy(alpha = 0.25f)
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        FloatingActionButton(
            onClick = onOpenQuickScanner,
            containerColor = FundPrimary,
            contentColor = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "مسح QR"
            )
        }
    }
}
