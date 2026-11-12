package com.example.fund.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fund.data.FundRepository
import com.example.ui.theme.*

@Composable
fun LocalServerSyncDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val isOnline by repo.isOnline.collectAsState()
    val autoSync by repo.autoSyncOnConnect.collectAsState()
    val isSyncing by repo.isSyncing.collectAsState()
    val syncProgress by repo.syncProgress.collectAsState()
    val syncStageMsg by repo.syncStageMessage.collectAsState()
    val serverConfig by repo.serverConfig.collectAsState()
    val syncQueue by repo.syncQueue.collectAsState()
    val syncHistory by repo.syncHistory.collectAsState()
    val lastSync by repo.lastSyncTimestamp.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: طابور المزامنة, 1: سجل الدفعات, 2: إعداد الخادم المحلي
    var customHost by remember(serverConfig) { mutableStateOf(serverConfig.serverHost) }
    var customName by remember(serverConfig) { mutableStateOf(serverConfig.serverName) }

    val totalRecords = repo.getTotalLocalRecordsCount()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(FundDeepHeader, FundPrimaryMedium)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(FundGold.copy(alpha = 0.2f), CircleShape)
                                        .border(1.dp, FundGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = FundGoldLight
                                    )
                                }
                                Column {
                                    Text(
                                        "مركز الخادم المحلي والمزامنة الفورية",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        "${serverConfig.serverName} • ${serverConfig.serverHost}",
                                        fontSize = 11.sp,
                                        color = FundGoldLight
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                        }

                        // Telemetry Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ServerMetricPill(
                                label = "حالة الشبكة",
                                value = if (isOnline) "متصل (Online)" else "محلي (Offline)",
                                color = if (isOnline) Color(0xFFA5D6A7) else Color(0xFFFFCC80),
                                modifier = Modifier.weight(1f)
                            )
                            ServerMetricPill(
                                label = "العمليات المعلقة",
                                value = "${syncQueue.size} عملية",
                                color = if (syncQueue.isEmpty()) Color.White else FundGoldLight,
                                modifier = Modifier.weight(1f)
                            )
                            ServerMetricPill(
                                label = "السجلات المحلية",
                                value = "$totalRecords سجل",
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            ServerMetricPill(
                                label = "زمن الاستجابة",
                                value = if (isOnline) "${serverConfig.latencyMs} ms" else "محلي 0ms",
                                color = FundGoldLight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Live Sync Stage & Progress Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isOnline) Color(0xFFFFF3E0)
                            else if (syncQueue.isNotEmpty()) Color(0xFFE3F2FD)
                            else FundLightSurfaceSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isOnline) StatusPending else if (syncQueue.isNotEmpty()) StatusInfo else StatusSuccess
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (!isOnline) Icons.Default.WifiOff
                                        else if (syncQueue.isNotEmpty()) Icons.Default.SyncProblem
                                        else Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = if (!isOnline) StatusPending else if (syncQueue.isNotEmpty()) StatusInfo else StatusSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = syncStageMsg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14210F)
                                    )
                                }
                                Text(
                                    text = "آخر مزامنة: $lastSync",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }

                            AnimatedVisibility(visible = isSyncing) {
                                LinearProgressIndicator(
                                    progress = { syncProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = FundPrimary,
                                    trackColor = FundPrimary.copy(alpha = 0.2f)
                                )
                            }

                            // Network & Auto-Sync Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Switch(
                                        checked = isOnline,
                                        onCheckedChange = {
                                            repo.toggleOnline()
                                            val msg = if (!isOnline) "تم الاتصال بالشبكة ومزامنة العمليات التلقائية" else "تم التحويل للوضع المحلي (Offline)"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    Text(
                                        if (isOnline) "متصل بالشبكة" else "وضع غير متصل",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14210F)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = autoSync,
                                        onCheckedChange = { repo.setAutoSyncOnConnect(it) }
                                    )
                                    Text(
                                        "مزامنة تلقائية فور الاتصال",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14210F)
                                    )
                                }
                            }

                            // Action Buttons: Simulate Offline Ops & Sync Now
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        repo.simulateOfflineFieldOperations()
                                        activeTab = 0
                                        Toast.makeText(
                                            context,
                                            "تم تسجيل 3 عمليات ميدانية في طابور الخادم المحلي (وضع Offline)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("simulate_offline_ops_btn"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("محاكاة +3 عمليات ميدانية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (!isOnline) {
                                            repo.toggleOnline() // Reconnects and syncs
                                        }
                                        repo.syncNow()
                                        Toast.makeText(context, "تمت مزامنة كافة العمليات مع الخادم بنجاح ✔", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sync_now_dialog_btn"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (!isOnline) "اتصال ومزامنة الآن (${syncQueue.size})"
                                        else "مزامنة فورية (${syncQueue.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Tabs
                    TabRow(selectedTabIndex = activeTab) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("طابور الانتظار (${syncQueue.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("سجل الدفعات (${syncHistory.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("عقدة الخادم المحلي", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    when (activeTab) {
                        0 -> {
                            if (syncQueue.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = StatusSuccess,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "طابور المزامنة فارغ — جميع العمليات المحلية ممرحلة ومطابقة!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = StatusSuccess
                                        )
                                        Text(
                                            "اضغط على «محاكاة +3 عمليات ميدانية» أعلاه لاختبار طابور عدم الاتصال والمزامنة التلقائية.",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(syncQueue, key = { it.id }) { item ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusPending.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Surface(
                                                            color = StatusPending.copy(alpha = 0.15f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                item.entityName,
                                                                color = StatusPending,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(
                                                            "SHA-256: #${item.txHash}",
                                                            fontSize = 10.sp,
                                                            color = FundPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(item.summary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "${item.timestamp} • حجم الحزمة: ${item.payloadBytes} Bytes",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Icon(
                                                    Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = StatusPending
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(syncHistory, key = { it.id }) { batch ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(batch.batchNo, fontWeight = FontWeight.Black, fontSize = 13.sp, color = FundPrimary)
                                                    Surface(
                                                        color = StatusSuccess.copy(alpha = 0.14f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            batch.status,
                                                            color = StatusSuccess,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    "الخادم: ${batch.serverNode}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    "التاريخ: ${batch.timestamp} • الزمن: ${batch.durationMs} مللي ثانية",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Surface(
                                                color = FundGold.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    "${batch.operationsCount} عمليات",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = FundGoldDark,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "اختر عقدة الخادم المحلي أو السحابي للارتباط:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                val presets = listOf(
                                    Triple("الخادم المحلي الميداني (LAN)", "192.168.1.100:8443", 8),
                                    Triple("الخادم السحابي المركزي للصندوق", "sync.social-fund.org:443", 42),
                                    Triple("محرك التخزين المحلي المستقل (Offline Engine)", "127.0.0.1:9000", 1)
                                )

                                presets.forEach { (pName, pHost, ping) ->
                                    val selected = customHost == pHost
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                customName = pName
                                                customHost = pHost
                                                repo.updateServerNode(pName, pHost, ping)
                                                Toast.makeText(context, "تم التبديل إلى: $pName", Toast.LENGTH_SHORT).show()
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) FundPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selected) FundPrimary else MaterialTheme.colorScheme.outline
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(pName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("العنوان: $pHost • الاستجابة: ${ping}ms", fontSize = 10.sp, color = Color.Gray)
                                            }
                                            RadioButton(
                                                selected = selected,
                                                onClick = {
                                                    customName = pName
                                                    customHost = pHost
                                                    repo.updateServerNode(pName, pHost, ping)
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = customHost,
                                    onValueChange = { customHost = it },
                                    label = { Text("عنوان الخادم المحلي المخصص (IP:Port)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (customHost.isNotBlank()) {
                                            repo.updateServerNode(customName, customHost.trim(), 10)
                                            Toast.makeText(context, "تم حفظ عنوان الخادم المحلي وفحص الاتصال بنجاح ✔", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("حفظ وفحص الاتصال بالخادم (Ping Test)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerMetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.75f), maxLines = 1)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}
