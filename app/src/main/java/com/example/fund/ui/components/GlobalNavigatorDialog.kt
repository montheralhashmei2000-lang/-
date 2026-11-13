package com.example.fund.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.UserRole
import com.example.fund.ui.navigation.AppRoute
import com.example.fund.ui.navigation.BottomHub
import com.example.fund.ui.navigation.Screen
import com.example.fund.ui.navigation.toRoute
import com.example.ui.theme.*

private data class QuickJumpResult(
    val title: String,
    val subtitle: String,
    val categoryBadge: String,
    val icon: ImageVector,
    val accentColor: Color,
    val keywords: String,
    val route: AppRoute
)

@Composable
fun GlobalNavigatorDialog(
    onDismiss: () -> Unit,
    onNavigateRoute: (AppRoute) -> Unit,
    onOpenQrCheck: () -> Unit
) {
    val repo = FundRepository.getInstance()
    val members by repo.members.collectAsStateWithLifecycle()
    val vouchers by repo.vouchers.collectAsStateWithLifecycle()
    val aidRequests by repo.aidRequests.collectAsStateWithLifecycle()
    val currentUser by repo.currentUser.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("الكل") }

    val allModules = remember(currentUser.role) {
        buildList {
            // Hubs
            add(QuickJumpResult("لوحة المعلومات الرئيسية", "المؤشرات المالية والرسوم البيانية", "رئيسية", BottomHub.HOME.icon, FundPrimary, "لوحة رئيسية dashboard", AppRoute.DashboardRoute))
            add(QuickJumpResult("المركز المالي والمحاسبي", "السندات والقيود والخزينة والموازنات", "مركز", BottomHub.FINANCE.icon, FundPrimary, "مركز مالي محاسبة", AppRoute.FinanceHubRoute))
            add(QuickJumpResult("الأعضاء والجهات الداعمة", "سجل الأعضاء والاشتراكات والمانحين", "مركز", BottomHub.MEMBERS.icon, StatusSuccess, "اعضاء جهات", AppRoute.MembersHubRoute))
            add(QuickJumpResult("الإعانات والمشاريع التنموية", "الطلبات والكفالات والمستودع والحملات", "مركز", BottomHub.AIDS.icon, StatusPending, "اعانات مشاريع", AppRoute.AidsHubRoute))

            // Functional screens
            Screen.entries.filter { it != Screen.LOGIN && it != Screen.DASHBOARD }.forEach { screen ->
                if (currentUser.role == UserRole.VIEWER && screen == Screen.USERS) return@forEach
                add(
                    QuickJumpResult(
                        title = screen.title,
                        subtitle = "القسم: ${screen.category}",
                        categoryBadge = screen.category,
                        icon = screen.icon,
                        accentColor = FundPrimary,
                        keywords = "${screen.title} ${screen.category} ${screen.name}",
                        route = screen.toRoute()
                    )
                )
            }

            // Settings sub-tabs shortcuts
            add(QuickJumpResult("إعدادات الطباعة والسندات", "الترويسة والتذييل والخطوط والتوقيعات", "إعدادات", Icons.Default.Print, FundGoldDark, "طباعة ترويسة توقيع سند pdf", AppRoute.SettingsRoute(initialTab = 4)))
            add(QuickJumpResult("إدارة المناطق والقرى", "فصل المناطق وإضافة القرى التابعة", "إعدادات", Icons.Default.LocationCity, StatusInfo, "مناطق قرى", AppRoute.SettingsRoute(initialTab = 1)))
            add(QuickJumpResult("المظهر والشعار والوضع الليلي", "تكبير الخط والوضع الليلي وهوية الصندوق", "إعدادات", Icons.Default.Palette, FundPrimary, "مظهر خط داكن ليلي شعار", AppRoute.SettingsRoute(initialTab = 0)))
            add(QuickJumpResult("النسخ الاحتياطي والمزامنة", "تصدير JSON واستعادة البيانات", "إعدادات", Icons.Default.Backup, StatusSuccess, "نسخ احتياطي استعادة json", AppRoute.SettingsRoute(initialTab = 5)))
        }
    }

    val cleanQ = query.trim()

    val matchingModules = remember(cleanQ, activeFilter, allModules) {
        if (activeFilter !in listOf("الكل", "الشاشات")) emptyList()
        else allModules.filter {
            cleanQ.isBlank() ||
                it.title.contains(cleanQ, ignoreCase = true) ||
                it.subtitle.contains(cleanQ, ignoreCase = true) ||
                it.keywords.contains(cleanQ, ignoreCase = true)
        }.take(if (cleanQ.isBlank()) 8 else 14)
    }

    val matchingMembers = remember(cleanQ, activeFilter, members) {
        if (activeFilter !in listOf("الكل", "الأعضاء") || (cleanQ.isBlank() && activeFilter == "الكل")) emptyList()
        else members.filter {
            cleanQ.isBlank() ||
                it.fullName.contains(cleanQ, ignoreCase = true) ||
                it.memberNo.contains(cleanQ, ignoreCase = true) ||
                it.village.contains(cleanQ, ignoreCase = true) ||
                it.region.contains(cleanQ, ignoreCase = true)
        }.take(5).map { m ->
            QuickJumpResult(
                title = "${m.fullName} (${m.memberNo})",
                subtitle = "${m.region} • ${m.village} • ${m.status}",
                categoryBadge = "عضو",
                icon = Icons.Default.Person,
                accentColor = if (m.status == "نشط") StatusSuccess else StatusDanger,
                keywords = m.memberNo,
                route = AppRoute.MembersRoute(highlightMemberId = m.id)
            )
        }
    }

    val matchingVouchers = remember(cleanQ, activeFilter, vouchers) {
        if (activeFilter !in listOf("الكل", "السندات") || (cleanQ.isBlank() && activeFilter == "الكل")) emptyList()
        else vouchers.filter {
            cleanQ.isBlank() ||
                it.voucherNo.contains(cleanQ, ignoreCase = true) ||
                it.partyName.contains(cleanQ, ignoreCase = true) ||
                it.description.contains(cleanQ, ignoreCase = true)
        }.take(5).map { v ->
            QuickJumpResult(
                title = "${v.voucherNo} • ${v.partyName}",
                subtitle = "${v.type.title} • %,d ﷼ • ${v.date}".format(v.amountYer.toLong()),
                categoryBadge = v.type.title,
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                accentColor = if (v.type.name == "RECEIPT") StatusSuccess else StatusDanger,
                keywords = v.voucherNo,
                route = AppRoute.VouchersRoute(typeFilter = if (v.type.name == "RECEIPT") "قبض" else "صرف")
            )
        }
    }

    val matchingAids = remember(cleanQ, activeFilter, aidRequests) {
        if (activeFilter !in listOf("الكل", "الإعانات") || (cleanQ.isBlank() && activeFilter == "الكل")) emptyList()
        else aidRequests.filter {
            cleanQ.isBlank() ||
                it.requestNo.contains(cleanQ, ignoreCase = true) ||
                it.memberName.contains(cleanQ, ignoreCase = true) ||
                it.aidType.title.contains(cleanQ, ignoreCase = true)
        }.take(5).map { a ->
            QuickJumpResult(
                title = "${a.requestNo} • ${a.memberName}",
                subtitle = "${a.aidType.title} • ${a.status.title} • %,d ﷼".format(a.requestedAmountYer.toLong()),
                categoryBadge = a.status.title,
                icon = Icons.Default.VolunteerActivism,
                accentColor = StatusPending,
                keywords = a.requestNo,
                route = AppRoute.AidsRoute(statusFilter = a.status.title)
            )
        }
    }

    val combinedResults = matchingModules + matchingMembers + matchingVouchers + matchingAids

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 640.dp)
                .fillMaxHeight(0.84f)
                .testTag("global_navigator_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(FundPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TravelExplore, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("البحث والتنقل السريع", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FundPrimary)
                            Text("انتقل فوراً لأي شاشة أو عضو أو سند مالي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                onOpenQrCheck()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح QR", tint = FundGoldDark)
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("ابحث عن شاشة، عضو، سند، طلب مساعدة أو إعداد...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FundPrimary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input")
                )

                // Scope filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("الكل", "الشاشات", "الأعضاء", "السندات", "الإعانات").forEach { scope ->
                        FilterChip(
                            selected = activeFilter == scope,
                            onClick = { activeFilter = scope },
                            label = { Text(scope, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FundPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = FundPrimary
                            )
                        )
                    }
                }

                if (combinedResults.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        FundEmptyState(
                            icon = Icons.Default.SearchOff,
                            title = "لا توجد نتائج مطابقة",
                            subtitle = "جرّب البحث باسم شاشة (مثل السندات، الخزينة، الإعدادات) أو رقم عضو أو رقم سند.",
                            actionLabel = "مسح البحث",
                            onAction = {
                                query = ""
                                activeFilter = "الكل"
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(combinedResults, key = { "${it.categoryBadge}_${it.title}_${it.route}" }) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onDismiss()
                                        onNavigateRoute(item.route)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(item.accentColor.copy(alpha = 0.14f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(item.icon, contentDescription = null, tint = item.accentColor, modifier = Modifier.size(20.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                        )
                                    }
                                    Surface(
                                        color = item.accentColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = item.categoryBadge,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = item.accentColor,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
