package com.example.fund.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.AidStatus
import com.example.fund.model.UserRole
import com.example.ui.theme.*

@Composable
fun AppTopBar(
    screenTitle: String,
    canNavigateBack: Boolean = false,
    breadcrumbHint: String? = null,
    onNavigateBack: () -> Unit = {},
    onOpenDrawer: () -> Unit,
    onOpenAiAdvisor: () -> Unit,
    onOpenSyncCenter: () -> Unit = {}
) {
    val repo = FundRepository.getInstance()
    val isOnline by repo.isOnline.collectAsStateWithLifecycle()
    val isSyncing by repo.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by repo.syncProgress.collectAsStateWithLifecycle()
    val syncStageMsg by repo.syncStageMessage.collectAsStateWithLifecycle()
    val syncQueue by repo.syncQueue.collectAsStateWithLifecycle()
    val messages by repo.internalMessages.collectAsStateWithLifecycle()
    val aidRequests by repo.aidRequests.collectAsStateWithLifecycle()
    val currentUser by repo.currentUser.collectAsStateWithLifecycle()

    val unreadCount = messages.count { !it.isRead }
    val pendingAidsCount = aidRequests.count { it.status == AidStatus.IN_REVIEW }
    val totalAlerts = unreadCount + pendingAidsCount

    var roleMenuExpanded by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Sleek, Compact Mobile Top Bar with contextual Up/Back or Drawer Menu + Quick Search
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(FundDeepHeader, FundPrimary)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Leading: Contextual Back Button (detail screens) or Drawer Menu (Hubs) + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (canNavigateBack) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("topbar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("topbar_menu_button")
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "القائمة الجانبية",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = canNavigateBack) { onOpenDrawer() }
                    ) {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = breadcrumbHint ?: "${currentUser.fullName} • ${currentUser.role.title}",
                            color = FundGoldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Trailing Actions: Sync Status + Alerts Summary + AI + User Role Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Compact Server & Sync Pill
                    Surface(
                        color = if (isOnline) Color.White.copy(alpha = 0.14f) else StatusPending.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(32.dp)
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
                                maxLines = 1,
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    // Notifications Bell with Badge (opens inline alerts summary without duplicate screen routing)
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = { showAlertsDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "ملخص التنبيهات",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        if (totalAlerts > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-3).dp, y = 3.dp)
                                    .size(16.dp)
                                    .background(StatusDanger, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (totalAlerts > 9) "9+" else "$totalAlerts",
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
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "مستشار الذكاء الاصطناعي",
                            tint = FundGoldLight,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Active Profile & Role Chip (48dp touch target)
                    Box {
                        Surface(
                            color = FundGold.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, FundGold),
                            modifier = Modifier
                                .height(34.dp)
                                .clickable { roleMenuExpanded = true }
                                .testTag("topbar_user_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "حساب المستخدم",
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
                                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = FundPrimary) },
                                text = { Text("مركز الخادم المحلي والمزامنة", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                onClick = {
                                    roleMenuExpanded = false
                                    onOpenSyncCenter()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Slim Animated Sync Progress Bar
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

        // Slim Offline / Pending Sync Strip
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

    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            title = { Text("ملخص التنبيهات الحالية", fontWeight = FontWeight.Bold, color = FundPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• طلبات مساعدة قيد المراجعة: $pendingAidsCount (تُدار من مركز الإعانات والمشاريع)", fontSize = 12.sp)
                    Text("• الرسائل الداخلية غير المقروءة: $unreadCount (تُفتح من القائمة الجانبية)", fontSize = 12.sp)
                    Text("• عمليات بانتظار المزامنة المحلية: ${syncQueue.size}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlertsDialog = false }) { Text("حسناً") }
            }
        )
    }
}
