package com.example.fund.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Login
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fund.data.SeedData
import com.example.fund.data.repository.AuthResult
import com.example.fund.model.UserRole
import com.example.fund.ui.components.FundLogoImage
import com.example.fund.ui.viewmodel.AuthViewModel
import com.example.ui.theme.*

enum class LoginMode(val title: String) {
    PASSWORD("كلمة المرور"),
    QUICK_PIN("PIN السريع"),
    OTP("التحقق الثنائي"),
    BIOMETRICS("البصمة"),
    SERVER_CONFIG("الخادم المحلي")
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val fundInfo by authViewModel.fundInfo.collectAsStateWithLifecycle()
    val users by authViewModel.users.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val hasCompletedInitialLogin by authViewModel.hasCompletedInitialLogin.collectAsStateWithLifecycle()
    val hasQuickPin by authViewModel.hasQuickPin.collectAsStateWithLifecycle()
    val serverConfig by authViewModel.serverConfig.collectAsStateWithLifecycle()
    val isOnline by authViewModel.isOnline.collectAsStateWithLifecycle()
    val syncQueue by authViewModel.syncQueue.collectAsStateWithLifecycle()
    val authError by authViewModel.authError.collectAsStateWithLifecycle()

    var activeMode by remember { mutableStateOf(LoginMode.PASSWORD) }
    var username by remember(currentUser.username) { mutableStateOf(currentUser.username) }
    var password by remember { mutableStateOf(SeedData.defaultDemoPassword) }
    var showPassword by remember { mutableStateOf(false) }
    var otpDigits by remember { mutableStateOf("849201") }
    var pinDigits by remember { mutableStateOf("") }
    var setupPinDigits by remember { mutableStateOf("") }
    var confirmPinDigits by remember { mutableStateOf("") }
    var customHost by remember(serverConfig) { mutableStateOf(serverConfig.serverHost) }

    val biometricStatus = remember(context) {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
    }
    val biometricHardwareReady = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS

    fun launchRealBiometricPrompt() {
        if (!hasCompletedInitialLogin) {
            Toast.makeText(
                context,
                "يرجى تسجيل الدخول بكلمة المرور لمرة واحدة أولاً لتفعيل البصمة الحيوية.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (activity == null || !biometricHardwareReady) {
            val msg = when (biometricStatus) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "لم يتم تسجيل بصمة أو رمز قفل حيوي في إعدادات الجهاز."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    "مستشعر البصمة غير متاح على هذا الجهاز؛ استخدم رمز PIN السريع أو كلمة المرور."
                else -> "المصادقة الحيوية غير مهيأة على هذا الجهاز."
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    when (val res = authViewModel.loginWithBiometrics(username)) {
                        is AuthResult.Success -> {
                            Toast.makeText(
                                context,
                                "تم التحقق الحيوي بنجاح: ${res.user.fullName} ✔",
                                Toast.LENGTH_SHORT
                            ).show()
                            onLoginSuccess()
                        }
                        is AuthResult.Error -> {
                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        Toast.makeText(context, "تعذر التحقق الحيوي: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "لم تتم مطابقة البصمة، حاول مرة أخرى", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("المصادقة الحيوية للصندوق")
            .setSubtitle("فتح جلسة الموظف (@$username)")
            .setNegativeButtonText("استخدام كلمة المرور")
            .build()

        prompt.authenticate(promptInfo)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(FundDeepHeader, FundPrimaryMedium, FundLightBg)
                )
            )
            .testTag("login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Official Emblem & Identity
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(FundPrimary.copy(alpha = 0.08f), CircleShape)
                        .border(2.dp, FundGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    FundLogoImage(
                        customLogoBase64 = fundInfo.customLogoBase64,
                        contentDescription = "شعار الصندوق",
                        modifier = Modifier.size(62.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = fundInfo.name,
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = FundPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "بوابة الإدارة المالية والرقابة الميدانية (الإصدار 9.0)",
                    fontSize = 11.sp,
                    color = FundGoldDark,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Active Local Server & Connection Status Strip
                Surface(
                    color = if (isOnline) FundPrimary.copy(alpha = 0.08f) else StatusPending.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isOnline) FundPrimary.copy(alpha = 0.35f) else StatusPending
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeMode = LoginMode.SERVER_CONFIG }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.Dns else Icons.Default.Storage,
                                contentDescription = null,
                                tint = if (isOnline) StatusSuccess else StatusPending,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = serverConfig.serverName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${serverConfig.serverHost} • ${if (isOnline) "متصل (${serverConfig.latencyMs}ms)" else "وضع محلي Offline (${syncQueue.size} معلق)"}",
                                    fontSize = 10.sp,
                                    color = if (isOnline) StatusSuccess else StatusPending,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            color = FundGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "تغيير الخادم",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FundGoldDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector Tabs
                ScrollableTabRow(
                    selectedTabIndex = activeMode.ordinal,
                    edgePadding = 0.dp,
                    containerColor = FundLightSurfaceSecondary,
                    contentColor = FundPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LoginMode.entries.forEach { mode ->
                        Tab(
                            selected = activeMode == mode,
                            onClick = {
                                authViewModel.clearError()
                                activeMode = mode
                            },
                            text = { Text(mode.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (activeMode) {
                    LoginMode.PASSWORD -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "اختر حساب الموظف للدخول السريع أو أدخل بيانات الاعتماد:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                users.take(4).forEach { staff ->
                                    val isSelected = username.equals(staff.username, ignoreCase = true)
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                username = staff.username
                                                authViewModel.clearError()
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) FundPrimary.copy(alpha = 0.14f) else FundLightSurfaceSecondary
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) FundPrimary else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = when (staff.role) {
                                                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                                    UserRole.ACCOUNTANT -> Icons.Default.Calculate
                                                    UserRole.REVIEWER -> Icons.AutoMirrored.Filled.FactCheck
                                                    UserRole.VIEWER -> Icons.Default.Visibility
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected) FundPrimary else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = staff.role.title,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) FundPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "@${staff.username}",
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it
                                    authViewModel.clearError()
                                },
                                label = { Text("اسم المستخدم الوظيفي") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_username_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    authViewModel.clearError()
                                },
                                label = { Text("كلمة المرور المشفرة (PBKDF2)") },
                                supportingText = {
                                    Text(
                                        "كلمة المرور الافتراضية للحسابات التجريبية: ${SeedData.defaultDemoPassword}",
                                        fontSize = 10.sp
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "إظهار كلمة المرور"
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                isError = authError != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (authError != null) {
                                AuthErrorBanner(authError ?: "")
                            }

                            // Network Connection Mode Toggle during Login
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FundLightSurfaceSecondary, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (isOnline) StatusSuccess else StatusPending,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        if (isOnline) "الدخول في وضع الاتصال المباشر بالخادم" else "الدخول في وضع العمل المحلي (Offline-First)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Switch(
                                    checked = isOnline,
                                    onCheckedChange = { authViewModel.toggleOnline() }
                                )
                            }

                            Button(
                                onClick = {
                                    when (val res = authViewModel.loginWithPassword(username, password)) {
                                        is AuthResult.Success -> {
                                            Toast.makeText(
                                                context,
                                                "مرحباً بك: ${res.user.fullName} (${res.user.role.title})",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onLoginSuccess()
                                        }
                                        is AuthResult.Error -> {
                                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("login_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("تسجيل الدخول إلى النظام", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    LoginMode.QUICK_PIN -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when {
                                !hasCompletedInitialLogin -> {
                                    Surface(
                                        color = StatusPending.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, StatusPending.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Pin, contentDescription = null, tint = StatusPending, modifier = Modifier.size(32.dp))
                                            Text(
                                                "رمز PIN السريع غير مفعل بعد",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                "لأغراض الأمان المالي، يجب تسجيل الدخول بكلمة المرور لمرة واحدة أولاً لربط الجهاز وتفعيل الدخول برمز PIN أو البصمة.",
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            OutlinedButton(
                                                onClick = { activeMode = LoginMode.PASSWORD },
                                                modifier = Modifier.heightIn(min = 48.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("الانتقال إلى تسجيل الدخول بكلمة المرور", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                !hasQuickPin -> {
                                    Text(
                                        "تعيين رمز PIN السريع للحساب (@$username)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = FundPrimary
                                    )
                                    Text(
                                        "أنشئ رمز دخول سريع من 4 إلى 6 أرقام لفتح التطبيق لاحقاً بسرعة وأمان:",
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = setupPinDigits,
                                        onValueChange = { if (it.length <= 6) setupPinDigits = it.filter { c -> c.isDigit() } },
                                        label = { Text("رمز PIN الجديد (4–6 أرقام)") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("setup_pin_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = confirmPinDigits,
                                        onValueChange = { if (it.length <= 6) confirmPinDigits = it.filter { c -> c.isDigit() } },
                                        label = { Text("تأكيد رمز PIN") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    if (authError != null) {
                                        AuthErrorBanner(authError ?: "")
                                    }
                                    Button(
                                        onClick = {
                                            if (setupPinDigits != confirmPinDigits) {
                                                Toast.makeText(context, "رمزا PIN غير متطابقين", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            if (authViewModel.setQuickPin(setupPinDigits)) {
                                                Toast.makeText(context, "تم حفظ وتشفير رمز PIN السريع بنجاح ✔", Toast.LENGTH_SHORT).show()
                                                pinDigits = setupPinDigits
                                                setupPinDigits = ""
                                                confirmPinDigits = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("save_pin_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("حفظ وتفعيل رمز PIN السريع", fontWeight = FontWeight.Bold)
                                    }
                                }

                                else -> {
                                    Text(
                                        "أدخل رمز PIN السريع للحساب (@$username):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(6) { idx ->
                                            val filled = idx < pinDigits.length
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .size(38.dp)
                                                    .background(
                                                        if (filled) FundPrimary.copy(alpha = 0.14f) else FundLightSurfaceSecondary,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .border(
                                                        1.5.dp,
                                                        if (filled) FundPrimary else MaterialTheme.colorScheme.outline,
                                                        RoundedCornerShape(10.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (filled) "●" else "",
                                                    color = FundPrimary,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = pinDigits,
                                        onValueChange = {
                                            if (it.length <= 6) {
                                                pinDigits = it.filter { c -> c.isDigit() }
                                                authViewModel.clearError()
                                            }
                                        },
                                        label = { Text("رمز PIN السريع (4–6 أرقام)") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_pin_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    if (authError != null) {
                                        AuthErrorBanner(authError ?: "")
                                    }

                                    Button(
                                        onClick = {
                                            when (val res = authViewModel.loginWithQuickPin(username, pinDigits)) {
                                                is AuthResult.Success -> {
                                                    Toast.makeText(
                                                        context,
                                                        "مرحباً بك: ${res.user.fullName}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    onLoginSuccess()
                                                }
                                                is AuthResult.Error -> {
                                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("login_pin_submit"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("دخول سريع برمز PIN", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    LoginMode.OTP -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "أدخل رمز التحقق الرقمي المكون من 6 خانات المرسل إلى رقم الهاتف المعتمد أو مولد الرموز المحلي (TOTP):",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val padded = otpDigits.padEnd(6, '•').take(6)
                                padded.forEach { ch ->
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(FundLightSurfaceSecondary, RoundedCornerShape(10.dp))
                                            .border(1.5.dp, FundPrimary, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ch.toString(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = FundPrimary,
                                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = otpDigits,
                                onValueChange = { if (it.length <= 6) otpDigits = it.filter { c -> c.isDigit() } },
                                label = { Text("تعديل رمز التحقق (OTP)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        otpDigits = (100000..999999).random().toString()
                                        Toast.makeText(context, "تم توليد رمز تحقق جديد: $otpDigits", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("توليد رمز جديد", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        when (val res = authViewModel.loginWithOtp(username, otpDigits)) {
                                            is AuthResult.Success -> onLoginSuccess()
                                            is AuthResult.Error -> Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .heightIn(min = 48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تأكيد الرمز والدخول", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    LoginMode.BIOMETRICS -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(
                                        if (hasCompletedInitialLogin && biometricHardwareReady) {
                                            FundPrimary.copy(alpha = 0.1f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        CircleShape
                                    )
                                    .border(
                                        2.5.dp,
                                        if (hasCompletedInitialLogin && biometricHardwareReady) FundGold else Color.Gray,
                                        CircleShape
                                    )
                                    .clickable { launchRealBiometricPrompt() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = "البصمة الحيوية",
                                    tint = if (hasCompletedInitialLogin && biometricHardwareReady) FundPrimary else Color.Gray,
                                    modifier = Modifier.size(56.dp)
                                )
                            }

                            val statusHint = when {
                                !hasCompletedInitialLogin ->
                                    "يتطلب الدخول أولاً بكلمة المرور لمرة واحدة لتفعيل المصادقة الحيوية على هذا الحساب."
                                !biometricHardwareReady ->
                                    "لا تتوفر بصمة مسجلة أو مستشعر حيوي نشط في نظام الجهاز الحالي. يمكنك استخدام كلمة المرور أو PIN السريع."
                                else ->
                                    "اضغط لتشغيل نافذة البصمة الحيوية الرسمية للنظام (BiometricPrompt) وفتح حساب @$username."
                            }

                            Text(
                                text = statusHint,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { launchRealBiometricPrompt() },
                                enabled = hasCompletedInitialLogin && biometricHardwareReady,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("login_biometric_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("تشغيل التحقق بالبصمة الحيوية", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    LoginMode.SERVER_CONFIG -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "إعدادات الارتباط بالخادم المحلي والمزامنة:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = FundPrimary
                            )

                            val nodes = listOf(
                                Triple("الخادم المحلي الميداني (LAN)", "192.168.1.100:8443", 8),
                                Triple("الخادم السحابي المركزي للصندوق", "sync.social-fund.org:443", 42),
                                Triple("محرك التخزين المحلي المستقل (Offline-First)", "127.0.0.1:9000", 1)
                            )

                            nodes.forEach { (nTitle, nHost, ping) ->
                                val selected = serverConfig.serverHost == nHost
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            customHost = nHost
                                            authViewModel.updateServerNode(nTitle, nHost, ping)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) FundPrimary.copy(alpha = 0.1f) else FundLightSurfaceSecondary
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
                                        Column {
                                            Text(nTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("العنوان: $nHost • الاستجابة: ${ping}ms", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        RadioButton(
                                            selected = selected,
                                            onClick = {
                                                customHost = nHost
                                                authViewModel.updateServerNode(nTitle, nHost, ping)
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customHost,
                                onValueChange = { customHost = it },
                                label = { Text("عنوان IP والمنفذ للخادم المحلي") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    authViewModel.updateServerNode(serverConfig.serverName, customHost.trim(), 9)
                                    Toast.makeText(context, "تم حفظ إعدادات الخادم المحلي وفحص الاتصال بنجاح ✔", Toast.LENGTH_SHORT).show()
                                    activeMode = LoginMode.PASSWORD
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("حفظ الخادم والعودة لتسجيل الدخول")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthErrorBanner(message: String) {
    Surface(
        color = StatusDanger.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = StatusDanger,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(10.dp)
        )
    }
}
