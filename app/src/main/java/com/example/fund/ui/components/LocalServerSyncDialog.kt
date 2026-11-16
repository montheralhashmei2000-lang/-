package com.example.fund.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fund.ui.viewmodel.SyncViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LocalServerSyncDialog(
    onDismiss: () -> Unit,
    syncViewModel: SyncViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOnline by syncViewModel.isOnline.collectAsStateWithLifecycle()
    val isTransportAvailable by syncViewModel.isTransportAvailable.collectAsStateWithLifecycle()
    val autoSync by syncViewModel.autoSyncOnConnect.collectAsStateWithLifecycle()
    val isSyncing by syncViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by syncViewModel.syncProgress.collectAsStateWithLifecycle()
    val syncStageMsg by syncViewModel.syncStageMessage.collectAsStateWithLifecycle()
    val serverConfig by syncViewModel.serverConfig.collectAsStateWithLifecycle()
    val serverProfiles by syncViewModel.serverProfiles.collectAsStateWithLifecycle()
    val syncQueue by syncViewModel.syncQueue.collectAsStateWithLifecycle()
    val syncHistory by syncViewModel.syncHistory.collectAsStateWithLifecycle()
    val lastSync by syncViewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val vaultBatchCount by syncViewModel.localVaultBatchCount.collectAsStateWithLifecycle()
    val vaultSizeBytes by syncViewModel.localVaultSizeBytes.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: طابور المزامنة, 1: سجل الدفعات, 2: إعداد الخادم
    var customHost by remember(serverConfig.serverHost) { mutableStateOf(serverConfig.serverHost) }
    var customName by remember(serverConfig.serverName) { mutableStateOf(serverConfig.serverName) }
    var customToken by remember(serverConfig.serverHost) { mutableStateOf("") }
    var showTokenText by remember { mutableStateOf(false) }
    var isProbing by remember { mutableStateOf(false) }
    var lastProbeText by remember { mutableStateOf<String?>(null) }
    var lastProbeSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showVaultRestoreConfirm by remember { mutableStateOf(false) }

    val totalRecords = syncViewModel.getTotalLocalRecordsCount()
    val isUsingBuiltInVault = syncViewModel.isBuiltInVaultHost(serverConfig.serverHost)

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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
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
                                        "مركز الخوادم والمزامنة الفورية",
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
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                        }

                        // Telemetry Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ServerMetricPill(
                                label = "حالة العقدة",
                                value = if (isOnline) "متصل ✔" else if (isUsingBuiltInVault) "خزنة جاهزة" else "محلي Offline",
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
                                label = "الاستجابة / الشبكة",
                                value = if (serverConfig.latencyMs > 0) "${serverConfig.latencyMs}ms"
                                else if (isUsingBuiltInVault) "مدمج"
                                else if (isTransportAvailable) "متاحة"
                                else "بلا شبكة",
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
                            containerColor = if (!isOnline) StatusPending.copy(alpha = 0.14f)
                            else if (syncQueue.isNotEmpty()) StatusInfo.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
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
                                        imageVector = if (!isOnline) Icons.Default.CloudOff
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
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "آخر مزامنة: $lastSync",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = isSyncing || isProbing) {
                                if (isProbing && !isSyncing) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp),
                                        color = FundPrimary,
                                        trackColor = FundPrimary.copy(alpha = 0.2f)
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        progress = { syncProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = FundPrimary,
                                        trackColor = FundPrimary.copy(alpha = 0.2f)
                                    )
                                }
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
                                        enabled = !isSyncing && !isProbing,
                                        onCheckedChange = {
                                            scope.launch {
                                                syncViewModel.toggleOnlineSuspending()
                                                Toast.makeText(context, syncViewModel.syncStageMessage.value, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                    Text(
                                        if (isOnline) "متصل بالعقدة" else "وضع غير متصل",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = autoSync,
                                        onCheckedChange = { syncViewModel.setAutoSyncOnConnect(it) }
                                    )
                                    Text(
                                        "مزامنة تلقائية فور الاتصال",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
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
                                        syncViewModel.simulateOfflineFieldOperations()
                                        activeTab = 0
                                        Toast.makeText(
                                            context,
                                            "تم تسجيل 3 عمليات ميدانية في طابور الانتظار (وضع Offline)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("simulate_offline_ops_btn"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("محاكاة +3 عمليات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            syncViewModel.connectAndSyncSmartSuspending()
                                            Toast.makeText(context, syncViewModel.syncStageMessage.value, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    enabled = !isSyncing && !isProbing,
                                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("sync_now_dialog_btn"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (!isOnline) "اتصال ومزامنة (${syncQueue.size})"
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
                            text = { Text("الخوادم والاتصال (${serverProfiles.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
                                            "اضغط على «محاكاة +3 عمليات» أو سجّل قيداً بوضع Offline لاختبار الترحيل الفوري.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            border = BorderStroke(1.dp, StatusPending.copy(alpha = 0.5f)),
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
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            if (syncHistory.isEmpty()) {
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
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Text(
                                            "لا توجد دفعات مزامنة مكتملة بعد",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "ستُدرج هنا سجلات الدفعات المؤكدة بتوقيع SHA-256 فور إتمام المزامنة.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(syncHistory, key = { it.id }) { batch ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        }

                        2 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Built-in Local Vault Status & Recovery Card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = FundPrimary.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, FundPrimary.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(18.dp))
                                            Text(
                                                "خزنة المزامنة المحلية المشفرة (Local Vault)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = FundPrimary
                                            )
                                        }
                                        Text(
                                            "الدفعات المخزنة محلياً: $vaultBatchCount دفعة (${(vaultSizeBytes / 1024.0).let { if (it < 1.0 && vaultSizeBytes > 0) "1" else it.toInt().toString() }} KB) • مشفرة بـ AES-256-GCM",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (vaultBatchCount > 0) {
                                            OutlinedButton(
                                                onClick = { showVaultRestoreConfirm = true },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .testTag("restore_local_vault_btn"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("استعادة آخر نسخة مزامنة من الخزنة المحلية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Text(
                                    "العقد والخوادم المحفوظة (اختر للارتباط والفحص):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                serverProfiles.forEach { profile ->
                                    val selected = serverConfig.serverHost.equals(profile.host, ignoreCase = true)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isProbing && !isSyncing) {
                                                customName = profile.name
                                                customHost = profile.host
                                                customToken = ""
                                                scope.launch {
                                                    isProbing = true
                                                    val probeResult = runCatching {
                                                        syncViewModel.probeServerConnection(
                                                            hostOverride = profile.host,
                                                            nameOverride = profile.name,
                                                            saveAsActiveNode = true
                                                        )
                                                    }
                                                    isProbing = false
                                                    probeResult.onSuccess { res ->
                                                        lastProbeText = res.message
                                                        lastProbeSuccess = res.reachable
                                                        Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                                    }.onFailure { err ->
                                                        lastProbeText = err.message ?: "تعذر تبديل العقدة"
                                                        lastProbeSuccess = false
                                                        Toast.makeText(context, lastProbeText, Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) FundPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(
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
                                            RadioButton(
                                                selected = selected,
                                                onClick = null
                                            )
                                            Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    if (profile.isBuiltInVault) {
                                                        Surface(color = StatusSuccess.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                                                            Text("مدمج", fontSize = 9.sp, color = StatusSuccess, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                        }
                                                    } else if (profile.hasAuthToken) {
                                                        Surface(color = FundGold.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                                            Text("Bearer Token", fontSize = 9.sp, color = FundGoldDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = buildString {
                                                        append(profile.host)
                                                        if (profile.lastLatencyMs != null) append(" • ${profile.lastLatencyMs}ms")
                                                    },
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (profile.lastDiagnostic.isNotBlank()) {
                                                    Text(
                                                        profile.lastDiagnostic,
                                                        fontSize = 10.sp,
                                                        color = if (profile.lastLatencyMs != null) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (!profile.isBuiltInVault) {
                                                IconButton(
                                                    onClick = {
                                                        runCatching { syncViewModel.deleteServerProfile(profile.id) }
                                                            .onSuccess { deleted ->
                                                                if (deleted) Toast.makeText(context, "تم حذف الخادم ${profile.name}", Toast.LENGTH_SHORT).show()
                                                            }
                                                            .onFailure { err ->
                                                                Toast.makeText(context, err.message ?: "غير مصرح بحذف الخادم", Toast.LENGTH_SHORT).show()
                                                            }
                                                    },
                                                    modifier = Modifier.minimumInteractiveComponentSize()
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الخادم", tint = StatusDanger, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                Text(
                                    "إضافة أو تعديل خادم مخصص (HTTPS):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                OutlinedTextField(
                                    value = customName,
                                    onValueChange = { customName = it },
                                    label = { Text("اسم عقدة الخادم") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("server_name_input")
                                )

                                OutlinedTextField(
                                    value = customHost,
                                    onValueChange = { customHost = it },
                                    label = { Text("عنوان الخادم والمنفذ (مثال: 192.168.1.50:8443 أو domain.org)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("server_host_input")
                                )

                                OutlinedTextField(
                                    value = customToken,
                                    onValueChange = { customToken = it },
                                    label = { Text("رمز المصادقة Bearer Token (اختياري للخوادم المحمية)") },
                                    singleLine = true,
                                    visualTransformation = if (showTokenText) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showTokenText = !showTokenText }) {
                                            Icon(
                                                if (showTokenText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "إظهار/إخفاء الرمز"
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("server_token_input")
                                )

                                if (!lastProbeText.isNullOrBlank()) {
                                    Surface(
                                        color = if (lastProbeSuccess == true) StatusSuccess.copy(alpha = 0.12f) else StatusDanger.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (lastProbeSuccess == true) StatusSuccess else StatusDanger),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = lastProbeText.orEmpty(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (lastProbeSuccess == true) StatusSuccess else StatusDanger,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (customHost.isBlank()) {
                                            Toast.makeText(context, "يرجى إدخال عنوان الخادم أولاً", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        scope.launch {
                                            isProbing = true
                                            val probeRes = runCatching {
                                                syncViewModel.probeServerConnection(
                                                    hostOverride = customHost.trim(),
                                                    nameOverride = customName.trim().ifBlank { customHost.trim() },
                                                    saveAsActiveNode = true,
                                                    authTokenOverride = customToken.takeIf { it.isNotBlank() }
                                                )
                                            }
                                            isProbing = false
                                            probeRes.onSuccess { res ->
                                                lastProbeText = res.message
                                                lastProbeSuccess = res.reachable
                                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                            }.onFailure { err ->
                                                lastProbeText = err.message ?: "تعذر حفظ أو فحص الخادم"
                                                lastProbeSuccess = false
                                                Toast.makeText(context, lastProbeText, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = !isProbing && !isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("save_and_ping_server_btn")
                                ) {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isProbing) "جارٍ فحص الاتصال..." else "حفظ وفحص الاتصال بالخادم (Ping Test)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVaultRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showVaultRestoreConfirm = false },
            title = { Text("تأكيد الاستعادة من الخزنة المحلية", fontWeight = FontWeight.Bold, color = FundPrimary) },
            text = { Text("سيتم فك تشفير أحدث دفعة محفوظة في الخزنة المحلية (AES-256-GCM) والتحقق من بصمة SHA-256 ثم استعادة البيانات. هل تريد المتابعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showVaultRestoreConfirm = false
                        val res = runCatching { syncViewModel.restoreLatestFromLocalVault() }
                        res.onSuccess { (_, msg) ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "غير مصرح باستعادة البيانات", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
                ) { Text("استعادة الآن") }
            },
            dismissButton = {
                TextButton(onClick = { showVaultRestoreConfirm = false }) { Text("إلغاء") }
            }
        )
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
