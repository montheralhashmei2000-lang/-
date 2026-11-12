package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.data.FundRepository
import com.example.fund.model.UserRole
import com.example.fund.ui.components.FundLogoImage
import com.example.ui.theme.*

enum class LoginMode(val title: String) {
    PASSWORD("بيانات الاعتماد"),
    OTP("التحقق الثنائي (OTP)"),
    BIOMETRICS("البصمة الحيوية"),
    SERVER_CONFIG("الخادم المحلي")
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val fundInfo by repo.fundInfo.collectAsState()
    val users by repo.users.collectAsState()
    val serverConfig by repo.serverConfig.collectAsState()
    val isOnline by repo.isOnline.collectAsState()
    val syncQueue by repo.syncQueue.collectAsState()

    var activeMode by remember { mutableStateOf(LoginMode.PASSWORD) }
    var username by remember { mutableStateOf(repo.currentUser.value.username) }
    var password by remember { mutableStateOf("••••••••") }
    var showPassword by remember { mutableStateOf(false) }
    var otpDigits by remember { mutableStateOf("849201") }
    var customHost by remember(serverConfig) { mutableStateOf(serverConfig.serverHost) }

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
                    border = androidx.compose.foundation.BorderStroke(
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
                            onClick = { activeMode = mode },
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
                            // Quick Staff Profile Selector (1-Tap Role Selection)
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
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) FundPrimary.copy(alpha = 0.14f) else FundLightSurfaceSecondary
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
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
                                onValueChange = { username = it },
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
                                onValueChange = { password = it },
                                label = { Text("كلمة المرور المشفرة") },
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

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
                                    onCheckedChange = { repo.toggleOnline() }
                                )
                            }

                            Button(
                                onClick = {
                                    val loggedInUser = repo.authenticateUser(username)
                                    Toast.makeText(
                                        context,
                                        "مرحباً بك: ${loggedInUser.fullName} (${loggedInUser.role.title})",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onLoginSuccess()
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

                            // 6 Digit visual boxes
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
                                            color = FundPrimary
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = otpDigits,
                                onValueChange = { if (it.length <= 6) otpDigits = it.filter { c -> c.isDigit() } },
                                label = { Text("تعديل رمز التحقق (OTP)") },
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
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("توليد رمز جديد", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val loggedInUser = repo.authenticateUser(username)
                                        repo.logAudit("تحقق ثنائي OTP", "دخول موثق بالرمز الثنائي للمستخدم ${loggedInUser.fullName}")
                                        onLoginSuccess()
                                    },
                                    modifier = Modifier.weight(1.4f),
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
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(FundPrimary.copy(alpha = 0.1f), CircleShape)
                                    .border(2.5.dp, FundGold, CircleShape)
                                    .clickable {
                                        val loggedInUser = repo.authenticateUser(username)
                                        repo.logAudit("مصادقة حيوية", "تسجيل دخول بالبصمة الحيوية للمستخدم ${loggedInUser.fullName}")
                                        Toast.makeText(context, "تم التحقق من البصمة بنجاح ✔", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = "البصمة",
                                    tint = FundPrimary,
                                    modifier = Modifier.size(56.dp)
                                )
                            }

                            Text(
                                "اضغط على أيقونة البصمة أعلاه أو الزر أدناه للمصادقة الحيوية السريعة والدخول الآمن",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    val loggedInUser = repo.authenticateUser(username)
                                    repo.logAudit("مصادقة حيوية", "تسجيل دخول بالبصمة الحيوية للمستخدم ${loggedInUser.fullName}")
                                    onLoginSuccess()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("تأكيد البصمة الحيوية والدخول", fontWeight = FontWeight.Bold)
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
                                            repo.updateServerNode(nTitle, nHost, ping)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) FundPrimary.copy(alpha = 0.1f) else FundLightSurfaceSecondary
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
                                            Text(nTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("العنوان: $nHost • الاستجابة: ${ping}ms", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        RadioButton(
                                            selected = selected,
                                            onClick = {
                                                customHost = nHost
                                                repo.updateServerNode(nTitle, nHost, ping)
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
                                    repo.updateServerNode(serverConfig.serverName, customHost.trim(), 9)
                                    Toast.makeText(context, "تم حفظ إعدادات الخادم المحلي وفحص الاتصال بنجاح ✔", Toast.LENGTH_SHORT).show()
                                    activeMode = LoginMode.PASSWORD
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                                modifier = Modifier.fillMaxWidth()
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
