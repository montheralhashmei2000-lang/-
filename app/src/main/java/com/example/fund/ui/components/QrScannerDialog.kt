package com.example.fund.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.Member
import com.example.fund.model.Voucher
import com.example.fund.util.FormatUtils
import com.example.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val repo = FundRepository.getInstance()
    val members by repo.members.collectAsStateWithLifecycle()
    val vouchers by repo.vouchers.collectAsStateWithLifecycle()
    val screenFormat by repo.screenFormat.collectAsStateWithLifecycle()

    val hasCameraFeature = remember(context) {
        runCatching {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        }.getOrDefault(false)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasRequestedCameraPermission by remember { mutableStateOf(false) }
    var showCameraPreview by remember { mutableStateOf(hasCameraPermission && hasCameraFeature) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val grantedNow = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = grantedNow
                if (grantedNow && hasCameraFeature) {
                    showCameraPreview = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permanentlyDenied = remember(hasCameraPermission, hasRequestedCameraPermission, activity) {
        hasRequestedCameraPermission && !hasCameraPermission && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequestedCameraPermission = true
        hasCameraPermission = granted
        showCameraPreview = granted && hasCameraFeature
    }

    var searchQuery by remember { mutableStateOf("M-1001") }
    var matchedMember by remember { mutableStateOf<Member?>(members.find { it.memberNo == "M-1001" }) }
    var matchedVoucher by remember { mutableStateOf<Voucher?>(null) }
    var hasSearched by remember { mutableStateOf(true) }
    var selectedMemberForCard by remember { mutableStateOf<Member?>(null) }

    fun performLookup(rawInput: String) {
        val clean = rawInput.trim()
        // Support structured QR payloads like MEMBER|M-1001|... or VOUCHER|V-2026-001|...
        val tokens = clean.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val candidateKey = when {
            tokens.size >= 2 && (tokens[0].equals("MEMBER", ignoreCase = true) || tokens[0].equals("VOUCHER", ignoreCase = true)) -> tokens[1]
            else -> clean
        }

        hasSearched = true
        matchedMember = members.find {
            it.memberNo.equals(candidateKey, ignoreCase = true) ||
                it.nationalId == candidateKey
        }
        matchedVoucher = vouchers.find {
            it.voucherNo.equals(candidateKey, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("qr_scanner_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = FundGoldDark)
                        Text(
                            text = "التحقق الميداني الذكي (QR)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FundPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Live CameraX Viewfinder Section
                when {
                    !hasCameraFeature -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VideocamOff, contentDescription = null, tint = FundPrimary)
                                Column {
                                    Text("كاميرا المسح غير متوفرة على هذا الجهاز", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        "يمكنك التحقق الميداني الفوري بإدخال رقم العضوية أو رقم السند أو نص رمز QR أدناه.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    hasCameraPermission && showCameraPreview -> {
                        CameraXViewfinderCard(
                            onHideCamera = { showCameraPreview = false }
                        )
                    }

                    else -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (permanentlyDenied) StatusDanger.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (permanentlyDenied) Icons.Default.NoPhotography else Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = if (permanentlyDenied) StatusDanger else FundPrimary
                                        )
                                        Column {
                                            Text(
                                                text = when {
                                                    hasCameraPermission -> "الكاميرا الميدانية جاهزة"
                                                    permanentlyDenied -> "تم رفض إذن الكاميرا نهائياً"
                                                    else -> "تفعيل كاميرا المسح الميداني"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = when {
                                                    hasCameraPermission -> "اضغط لفتح عدسة الكاميرا مع الفلاش والتركيز التلقائي"
                                                    permanentlyDenied -> "فعّل إذن الكاميرا من إعدادات التطبيق لعرض العدسة المباشرة، أو استخدم البحث اليدوي أدناه."
                                                    else -> "يتطلب إذن الكاميرا لعرض العدسة المباشرة"
                                                },
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            when {
                                                hasCameraPermission -> showCameraPreview = true
                                                permanentlyDenied -> {
                                                    runCatching {
                                                        val settingsIntent = Intent(
                                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                            Uri.fromParts("package", context.packageName, null)
                                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(settingsIntent)
                                                    }.onFailure {
                                                        Toast.makeText(context, "تعذر فتح إعدادات التطبيق", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                else -> {
                                                    hasRequestedCameraPermission = true
                                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (permanentlyDenied) StatusDanger else FundPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                hasCameraPermission -> "فتح العدسة"
                                                permanentlyDenied -> "فتح إعدادات التطبيق"
                                                else -> "منح الإذن"
                                            },
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "أدخل رقم العضوية (مثل M-1001) أو رقم السند (مثل V-2026-001) أو نص رمز QR للمطابقة الفورية:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input + Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("رقم العضوية أو السند") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_search_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = { performLookup(searchQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("qr_search_submit")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "فحص")
                    }
                }

                // Quick selector chips for fast field check
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    listOf("M-1001", "M-1002", "M-1006", "V-2026-001").forEach { sample ->
                        AssistChip(
                            onClick = {
                                searchQuery = sample
                                performLookup(sample)
                            },
                            label = { Text(sample, fontSize = 11.sp) }
                        )
                    }
                }

                // Verification Results Card
                if (hasSearched) {
                    when {
                        matchedMember != null -> matchedMember?.let { m ->
                            val isActive = m.status == "نشط"
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else StatusDanger.copy(alpha = 0.14f)
                                ),
                                border = BorderStroke(1.dp, if (isActive) StatusSuccess else StatusDanger),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(m.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Surface(
                                            color = if (isActive) StatusSuccess else StatusDanger,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (isActive) "عضوية سارية ✔" else "عضوية معلقة ⚠",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text("رقم العضوية: ${FormatUtils.formatCodeOrNumber(m.memberNo, screenFormat)} | الهوية: ${FormatUtils.formatCodeOrNumber(m.nationalId, screenFormat)}", fontSize = 12.sp)
                                    Text("المهنة: ${m.job} | المنطقة: ${m.region} | القرية: ${m.village}", fontSize = 12.sp)
                                    Text(
                                        "إجمالي المدفوع: ${FormatUtils.formatAmount(m.totalPaidYer, screenFormat)}",
                                        fontWeight = FontWeight.Bold,
                                        color = FundPrimary,
                                        fontSize = 12.sp,
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )
                                    Text(
                                        "المتأخرات المستحقة: ${FormatUtils.formatAmount(m.dueBalanceYer, screenFormat)}",
                                        color = if (m.dueBalanceYer > 0) StatusDanger else StatusSuccess,
                                        fontSize = 12.sp,
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )
                                    Text(
                                        text = if (isActive && m.dueBalanceYer == 0.0) {
                                            "أهلية الاستفادة: مؤهل بالكامل لجميع المساعدات"
                                        } else {
                                            "أهلية الاستفادة: يلزم تسوية المتأخرات أولاً"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isActive && m.dueBalanceYer == 0.0) StatusSuccess else StatusPending
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )

                                    // Inline Member Card preview without duplicating screen navigation
                                    OutlinedButton(
                                        onClick = { selectedMemberForCard = m },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("qr_open_member_card_inline"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp), tint = FundPrimary)
                                        Spacer(Modifier.width(6.dp))
                                        Text("معاينة وطباعة بطاقة العضو الرسمية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        matchedVoucher != null -> matchedVoucher?.let { v ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, FundGoldDark),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${v.type.title} - ${FormatUtils.formatCodeOrNumber(v.voucherNo, screenFormat)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            v.status.title,
                                            color = if (v.status.name == "POSTED") StatusSuccess else StatusDanger,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("المكرم: ${v.partyName}", fontSize = 12.sp)
                                    Text(
                                        "المبلغ: ${FormatUtils.formatAmount(v.amountYer, screenFormat)}",
                                        fontWeight = FontWeight.Bold,
                                        color = FundPrimary,
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )
                                    Text("البيان: ${v.description}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        else -> {
                            FundEmptyState(
                                icon = Icons.Default.SearchOff,
                                title = "لم يتم العثور على سجل مطابق",
                                subtitle = "تأكد من رقم العضوية أو رقم السند المدخل وأعد الفحص.",
                                actionLabel = "مسح الحقل",
                                onAction = { searchQuery = "" }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedMemberForCard?.let { m ->
        MemberCardDialog(
            member = m,
            onDismiss = { selectedMemberForCard = null }
        )
    }
}

@Composable
private fun CameraXViewfinderCard(
    onHideCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var torchEnabled by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var focusHint by remember { mutableStateOf("وجّه الإطار نحو رمز QR أو المس الشاشة للتركيز") }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            runCatching {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                if (!cameraProvider.hasCamera(selector)) {
                    cameraError = "لا توجد كاميرا خلفية متصلة"
                    return@runCatching
                }
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                cameraControl = camera.cameraControl
                hasFlashUnit = camera.cameraInfo.hasFlashUnit()

                val centerPoint = previewView.meteringPointFactory.createPoint(
                    (previewView.width.coerceAtLeast(1)) / 2f,
                    (previewView.height.coerceAtLeast(1)) / 2f
                )
                val focusAction = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                runCatching { camera.cameraControl.startFocusAndMetering(focusAction) }
            }.onFailure { err ->
                cameraError = err.localizedMessage ?: "الكاميرا غير متوفرة حالياً"
            }
        }
        providerFuture.addListener(listener, executor)

        onDispose {
            runCatching {
                if (providerFuture.isDone) {
                    providerFuture.get().unbindAll()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E1B14))
            .border(1.5.dp, FundGold, RoundedCornerShape(16.dp))
    ) {
        if (cameraError == null) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(cameraControl) {
                        detectTapGestures { offset ->
                            runCatching {
                                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                cameraControl?.startFocusAndMetering(action)
                                focusHint = "تم ضبط التركيز التلقائي عند نقطة اللمس ✔"
                            }.onFailure {
                                focusHint = "التركيز اليدوي غير مدعوم على هذه العدسة"
                            }
                        }
                    }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.VideocamOff, contentDescription = null, tint = FundGoldLight, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "عدسة الكاميرا غير نشطة على هذا الجهاز/المحاكي — يمكنك الفحص المباشر بالرقم أدناه",
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Reticle guide box
        Box(
            modifier = Modifier
                .size(116.dp)
                .align(Alignment.Center)
                .border(2.dp, FundGold.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
        )

        // Overlay top controls: Torch, Auto-Focus, Collapse camera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasFlashUnit && cameraError == null) {
                    IconButton(
                        onClick = {
                            val next = !torchEnabled
                            runCatching {
                                cameraControl?.enableTorch(next)
                                torchEnabled = next
                            }.onFailure {
                                focusHint = "تعذر تشغيل فلاش الكاميرا"
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("qr_torch_toggle")
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (torchEnabled) "إطفاء الفلاش" else "تشغيل الفلاش",
                            tint = if (torchEnabled) FundGoldLight else Color.White
                        )
                    }
                }

                if (cameraError == null) {
                    IconButton(
                        onClick = {
                            runCatching {
                                val point = previewView.meteringPointFactory.createPoint(
                                    (previewView.width.coerceAtLeast(1)) / 2f,
                                    (previewView.height.coerceAtLeast(1)) / 2f
                                )
                                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                cameraControl?.startFocusAndMetering(action)
                                focusHint = "جاري إعادة ضبط التركيز التلقائي..."
                            }.onFailure {
                                focusHint = "التركيز التلقائي غير متاح حالياً"
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("qr_autofocus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "تركيز تلقائي",
                            tint = Color.White
                        )
                    }
                }
            }

            IconButton(
                onClick = onHideCamera,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "طي الكاميرا", tint = Color.White)
            }
        }

        // Bottom hint strip
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Text(
                text = focusHint,
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}
