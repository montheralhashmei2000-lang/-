package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.fund.data.FundRepository
import com.example.fund.model.UserRole
import com.example.ui.theme.*

@Composable
fun UsersScreen() {
    val repo = FundRepository.getInstance()
    val users by repo.users.collectAsState()
    val currentUser by repo.currentUser.collectAsState()
    val context = LocalContext.current
    var showAddUserDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("users_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "المستخدمون والصلاحيات (RBAC)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "المستخدم النشط حالياً: ${currentUser.fullName} (${currentUser.role.title})",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
            if (currentUser.role == UserRole.ADMIN) {
                Button(
                    onClick = { showAddUserDialog = true },
                    modifier = Modifier.testTag("add_user_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة مستخدم")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مصفوفة الصلاحيات المعتمدة في الصندوق:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                UserRole.entries.forEach { role ->
                    Text(
                        "• ${role.title}: ${rolePermissionsDescription(role)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(users, key = { it.id }) { u ->
                val isCurrent = u.id == currentUser.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) FundPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = FundPrimary)
                                Text(u.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = (if (u.isActive) StatusSuccess else StatusDanger).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        if (u.isActive) "نشط" else "موقوف",
                                        color = if (u.isActive) StatusSuccess else StatusDanger,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "اسم الدخول: @${u.username} | الدور: ${u.role.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = FundPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                rolePermissionsDescription(u.role),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "آخر دخول: ${u.lastLogin}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isCurrent) {
                                Surface(
                                    color = StatusSuccess.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "الجلسة الحالية",
                                        color = StatusSuccess,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        if (u.isActive) {
                                            repo.switchRole(u.role)
                                            Toast.makeText(context, "تم التبديل إلى حساب: ${u.fullName}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "هذا الحساب موقوف، قم بتفعيله أولاً", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = u.isActive,
                                    modifier = Modifier.testTag("switch_user_${u.username}")
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تبديل الدور", style = MaterialTheme.typography.labelSmall)
                                }
                                if (currentUser.role == UserRole.ADMIN) {
                                    TextButton(
                                        onClick = { repo.toggleUserActive(u.id) },
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            if (u.isActive) "إيقاف الحساب" else "تفعيل الحساب",
                                            color = if (u.isActive) StatusDanger else StatusSuccess,
                                            style = MaterialTheme.typography.labelSmall
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

    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onSave = { username, fullName, role ->
                repo.addUser(username, fullName, role)
                showAddUserDialog = false
                Toast.makeText(context, "تمت إضافة المستخدم بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

private fun rolePermissionsDescription(role: UserRole): String = when (role) {
    UserRole.ADMIN -> "صلاحيات كاملة لإدارة النظام، الاعتماد، الإلغاء، والمستخدمين"
    UserRole.ACCOUNTANT -> "إصدار السندات، تسجيل الاشتراكات، والقيود المحاسبية"
    UserRole.REVIEWER -> "مراجعة وفحص السندات وطلبات المساعدات وسجل التدقيق"
    UserRole.VIEWER -> "استعراض لوحة القيادة والتقارير الإحصائية فقط (قراءة فقط)"
}

@Composable
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, UserRole) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.ACCOUNTANT) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("إضافة موظف / مستخدم جديد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الرباعي والصفة الوظيفية") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("اسم الدخول بالإنجليزية (مثال: cashier2)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Text("الدور الوظيفي والصلاحيات:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text("${role.title} (${rolePermissionsDescription(role)})") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (username.isNotBlank() && fullName.isNotBlank()) {
                                onSave(username, fullName, selectedRole)
                            }
                        }
                    ) {
                        Text("إضافة المستخدم")
                    }
                }
            }
        }
    }
}
