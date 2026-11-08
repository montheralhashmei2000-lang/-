package com.example.fund.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.data.FundRepository
import com.example.fund.model.AidStatus
import com.example.fund.model.UserRole
import com.example.fund.ui.navigation.Screen
import com.example.ui.theme.*

@Composable
fun AppTopBar(
    currentScreen: Screen,
    onOpenDrawer: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onOpenAiAdvisor: () -> Unit,
    onOpenSyncCenter: () -> Unit = {}
) {
    val repo = FundRepository.getInstance()
    val isOnline by repo.isOnline.collectAsState()
    val isSyncing by repo.isSyncing.collectAsState()
    val syncProgress by repo.syncProgress.collectAsState()
    val syncStageMsg by repo.syncStageMessage.collectAsState()
    val syncQueue by repo.syncQueue.collectAsState()
    val messages by repo.internalMessages.collectAsState()
    val aidRequests by repo.aidRequests.collectAsState()
    val currentUser by repo.currentUser.collectAsState()

    val unreadCount = messages.count { !it.isRead }
    val pendingAidsCount = aidRequests.count { it.status == AidStatus.IN_REVIEW }
    val totalAlerts = unreadCount + pendingAidsCount

    var roleMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Sleek, Compact Mobile Top Bar (48dp content height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(FundDeepHeader, FundPrimary)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start (Right in RTL): Drawer Icon + Single-line Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "القائمة",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = currentScreen.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // End (Left in RTL): Compact Server/Sync Pill + Alerts + AI + User Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Compact Server & Sync Pill
                    Surface(
                        color = if (isOnline) Color.White.copy(alpha = 0.14f) else StatusPending.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .clickable { onOpenSyncCenter() }
                            .testTag("open_sync_center_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        if (isOnline) Color(0xFF69F0AE) else Color.White,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (!isOnline) "محلي (${syncQueue.size})"
                                else if (syncQueue.isNotEmpty()) "مزامنة (${syncQueue.size})"
                                else "متصل",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // Combined Notifications & Messages Bell
                    Box {
                        IconButton(
                            onClick = {
                                if (pendingAidsCount > 0) onNavigate(Screen.AIDS) else onNavigate(Screen.MESSAGES)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "التنبيهات",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        if (totalAlerts > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-2).dp, y = 2.dp)
                                    .size(15.dp)
                                    .background(StatusDanger, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$totalAlerts",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // AI Advisor Icon
                    IconButton(
                        onClick = onOpenAiAdvisor,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "مستشار الذكاء الاصطناعي",
                            tint = FundGoldLight,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Compact Profile & Role Menu
                    Box {
                        Surface(
                            color = FundGold.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FundGold),
                            modifier = Modifier
                                .height(28.dp)
                                .clickable { roleMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = FundGoldLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = currentUser.role.title.substringBefore(" "),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = roleMenuExpanded,
                            onDismissRequest = { roleMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(currentUser.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("الدور الحالي: ${currentUser.role.title}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                },
                                onClick = { roleMenuExpanded = false }
                            )
                            HorizontalDivider()
                            UserRole.entries.forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "تبديل إلى: ${role.title}",
                                            fontWeight = if (currentUser.role == role) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentUser.role == role) FundPrimary else Color.Unspecified,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        repo.switchRole(role)
                                        roleMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FundPrimary) },
                                text = { Text("الرسائل الداخلية ($unreadCount غير مقروءة)", fontSize = 12.sp) },
                                onClick = {
                                    roleMenuExpanded = false
                                    onNavigate(Screen.MESSAGES)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = FundPrimary) },
                                text = { Text("مركز الخادم المحلي والمزامنة", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                onClick = {
                                    roleMenuExpanded = false
                                    onOpenSyncCenter()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = StatusDanger) },
                                text = { Text("شاشة تسجيل الدخول / تبديل الحساب", fontWeight = FontWeight.Bold, color = StatusDanger, fontSize = 12.sp) },
                                onClick = {
                                    roleMenuExpanded = false
                                    onNavigate(Screen.LOGIN)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Slim Animated Sync Progress Bar (only visible during active sync)
        AnimatedVisibility(visible = isSyncing) {
            Surface(color = Color(0xFF0D47A1), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(syncStageMsg, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${(syncProgress * 100).toInt()}%", color = FundGoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { syncProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = FundGold,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
        }

        // Slim Offline / Pending Sync Strip (only visible when Offline or Sync Queue not empty)
        if (!isOnline || syncQueue.isNotEmpty()) {
            Surface(
                color = if (!isOnline) Color(0xFFE65100) else Color(0xFF0D47A1),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenSyncCenter() }
                    ) {
                        Icon(
                            imageVector = if (!isOnline) Icons.Default.WifiOff else Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (!isOnline) "الوضع المحلي (Offline) • بالانتظار: ${syncQueue.size} عملية"
                            else "متصل • جاهز لمزامنة ${syncQueue.size} عملية محلية",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = {
                            if (!isOnline) repo.toggleOnline() else repo.syncNow()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = if (!isOnline) "اتصال ومزامنة" else "مزامنة الآن",
                            color = FundGoldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
