package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.data.FundRepository
import com.example.fund.model.Member
import com.example.fund.model.UserRole
import com.example.fund.ui.components.MemberCardDialog
import com.example.ui.theme.*

@Composable
fun MembersScreen(
    onOpenQrCheck: () -> Unit
) {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val members by repo.members.collectAsState()
    val currentUser by repo.currentUser.collectAsState()
    val canEdit = currentUser.role != UserRole.VIEWER

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var selectedMemberForCard by remember { mutableStateOf<Member?>(null) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredMembers = members.filter { m ->
        val matchesSearch = m.fullName.contains(searchQuery, ignoreCase = true) ||
                m.memberNo.contains(searchQuery, ignoreCase = true) ||
                m.phone.contains(searchQuery)
        val matchesFilter = when (selectedFilter) {
            "نشط" -> m.status == "نشط"
            "معلق" -> m.status == "معلق"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "سجل أعضاء الصندوق (${filteredMembers.size})",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FundPrimary
                )
                Text(
                    text = "إجمالي الأعضاء المنتظمين والمتأخرين",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onOpenQrCheck) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح QR", tint = FundGoldDark)
                }

                if (canEdit) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عضو جديد", fontSize = 12.sp)
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث بالاسم، رقم العضوية، أو الهاتف...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Filters Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("الكل", "نشط", "معلق").forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = FundPrimary
                    )
                )
            }
        }

        // Members List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredMembers, key = { it.id }) { member ->
                MemberCardItem(
                    member = member,
                    canEdit = canEdit,
                    onViewCard = { selectedMemberForCard = member },
                    onEditMember = { memberToEdit = member },
                    onToggleStatus = { repo.toggleMemberStatus(member.id) }
                )
            }
        }
    }

    // Official Member ID Card Dialog
    selectedMemberForCard?.let { member ->
        MemberCardDialog(
            member = member,
            onDismiss = { selectedMemberForCard = null }
        )
    }

    // Edit Member Dialog
    memberToEdit?.let { member ->
        MemberFormDialog(
            initialMember = member,
            onDismiss = { memberToEdit = null },
            onSave = { updated ->
                repo.updateMember(updated)
                Toast.makeText(context, "تم تحديث بيانات العضو بنجاح", Toast.LENGTH_SHORT).show()
                memberToEdit = null
            }
        )
    }

    // Add Member Dialog
    if (showAddDialog) {
        MemberFormDialog(
            initialMember = null,
            onDismiss = { showAddDialog = false },
            onSave = { newMember ->
                repo.addMember(newMember)
                Toast.makeText(context, "تم تسجيل العضو الجديد بنجاح", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MemberCardItem(
    member: Member,
    canEdit: Boolean,
    onViewCard: () -> Unit,
    onEditMember: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isActive = member.status == "نشط"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            .size(38.dp)
                            .background(FundPrimary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.fullName.take(1),
                            fontWeight = FontWeight.Bold,
                            color = FundPrimary,
                            fontSize = 16.sp
                        )
                    }

                    Column {
                        Text(member.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "${member.memberNo} | ${member.phone}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (isActive) StatusSuccess else StatusDanger,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = member.status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text("المهنة والمنطقة: ${member.job} - ${member.villageOrArea}", fontSize = 12.sp, color = Color.DarkGray)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "إجمالي السداد: %,d ﷼".format(member.totalPaidYer.toLong()),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FundPrimary
                )
                Text(
                    text = "المستحق: %,d ﷼".format(member.dueBalanceYer.toLong()),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (member.dueBalanceYer > 0) StatusDanger else StatusSuccess
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canEdit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onEditMember) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = FundPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعديل", fontSize = 11.sp, color = FundPrimary)
                        }
                        TextButton(onClick = onToggleStatus) {
                            Text(
                                text = if (isActive) "تعليق" else "تفعيل",
                                fontSize = 11.sp,
                                color = if (isActive) StatusDanger else StatusSuccess
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                OutlinedButton(
                    onClick = onViewCard,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp), tint = FundGoldDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بطاقة العضوية والـ QR", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun MemberFormDialog(
    initialMember: Member?,
    onDismiss: () -> Unit,
    onSave: (Member) -> Unit
) {
    val isEditing = initialMember != null
    var fullName by remember { mutableStateOf(initialMember?.fullName ?: "") }
    var phone by remember { mutableStateOf(initialMember?.phone ?: "") }
    var nationalId by remember { mutableStateOf(initialMember?.nationalId ?: "") }
    var job by remember { mutableStateOf(initialMember?.job ?: "") }
    var village by remember { mutableStateOf(initialMember?.villageOrArea ?: "الحبيلين") }
    var dueStr by remember { mutableStateOf(initialMember?.dueBalanceYer?.toLong()?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) "تعديل بيانات العضو (${initialMember?.memberNo})" else "إضافة عضو جديد للصندوق",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الرباعي للعضو") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nationalId,
                    onValueChange = { nationalId = it },
                    label = { Text("رقم الهوية الوطنية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = job,
                    onValueChange = { job = it },
                    label = { Text("المهنة / الصفة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("المنطقة / القرية بردفان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueStr,
                    onValueChange = { dueStr = it },
                    label = { Text("الرصيد المستحق / المتأخرات (﷼)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        val due = dueStr.toDoubleOrNull() ?: 0.0
                        val member = if (initialMember != null) {
                            initialMember.copy(
                                fullName = fullName,
                                phone = phone,
                                nationalId = nationalId,
                                job = job,
                                villageOrArea = village,
                                dueBalanceYer = due
                            )
                        } else {
                            Member(
                                memberNo = "M-${(1000..9999).random()}",
                                fullName = fullName,
                                phone = phone,
                                nationalId = nationalId,
                                joinDate = "2026-11-08",
                                status = "نشط",
                                totalPaidYer = 0.0,
                                dueBalanceYer = due,
                                job = job,
                                villageOrArea = village
                            )
                        }
                        onSave(member)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text(if (isEditing) "حفظ التعديلات" else "حفظ العضو")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
