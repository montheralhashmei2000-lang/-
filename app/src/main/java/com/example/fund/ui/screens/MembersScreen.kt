package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fund.model.Member
import com.example.fund.model.UserRole
import com.example.fund.ui.components.FundEmptyState
import com.example.fund.ui.components.MemberCardDialog
import com.example.fund.ui.components.SensitiveActionConfirmDialog
import com.example.fund.ui.viewmodel.MembersViewModel
import com.example.fund.util.FormatUtils
import com.example.fund.util.LocalScreenFormat
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MembersScreen(
    onOpenQrCheck: () -> Unit,
    initialStatusFilter: String = "الكل",
    highlightMemberId: String = "",
    membersViewModel: MembersViewModel = viewModel()
) {
    val context = LocalContext.current
    val members by membersViewModel.members.collectAsStateWithLifecycle()
    val regions by membersViewModel.regions.collectAsStateWithLifecycle()
    val currentUser by membersViewModel.currentUser.collectAsStateWithLifecycle()
    val canEdit = currentUser.role != UserRole.VIEWER
    val screenFormat = LocalScreenFormat.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember(initialStatusFilter) { mutableStateOf(initialStatusFilter) }
    var selectedRegionFilter by remember { mutableStateOf("كل المناطق") }
    var selectedMemberForCard by remember { mutableStateOf<Member?>(null) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var memberToToggleStatus by remember { mutableStateOf<Member?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var handledHighlightId by remember { mutableStateOf("") }

    LaunchedEffect(highlightMemberId, members) {
        if (highlightMemberId.isNotBlank() && highlightMemberId != handledHighlightId) {
            val found = members.find { it.id == highlightMemberId }
            if (found != null) {
                selectedMemberForCard = found
                handledHighlightId = highlightMemberId
            }
        }
    }

    val filteredMembers = members.filter { m ->
        val matchesSearch = m.fullName.contains(searchQuery, ignoreCase = true) ||
                m.memberNo.contains(searchQuery, ignoreCase = true) ||
                m.phone.contains(searchQuery) ||
                m.region.contains(searchQuery, ignoreCase = true) ||
                m.village.contains(searchQuery, ignoreCase = true)
        val matchesStatus = when (selectedStatusFilter) {
            "نشط" -> m.status == "نشط"
            "معلق" -> m.status == "معلق"
            else -> true
        }
        val matchesRegion = selectedRegionFilter == "كل المناطق" || m.region == selectedRegionFilter
        matchesSearch && matchesStatus && matchesRegion
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "سجل أعضاء الصندوق (${FormatUtils.formatCodeOrNumber(filteredMembers.size.toString(), screenFormat)})",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FundPrimary
                )
                Text(
                    text = "موزعين حسب المناطق والقرى التابعة للصندوق",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenQrCheck,
                    modifier = Modifier.testTag("members_qr_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح QR", tint = FundGoldDark)
                }

                if (canEdit) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_member_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عضو جديد", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        if (!canEdit) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        "وضع القراءة فقط: دور (${currentUser.role.title}) يتيح عرض السجلات وبطاقات العضوية دون التعديل.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث بالاسم، رقم العضوية، المنطقة أو القرية...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Status + Region Filters Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf("الكل", "نشط", "معلق").forEach { filter ->
                FilterChip(
                    selected = selectedStatusFilter == filter,
                    onClick = { selectedStatusFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = FundPrimary
                    )
                )
            }
            VerticalDivider(modifier = Modifier.height(24.dp).align(Alignment.CenterVertically))
            FilterChip(
                selected = selectedRegionFilter == "كل المناطق",
                onClick = { selectedRegionFilter = "كل المناطق" },
                label = { Text("كل المناطق", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FundGold.copy(alpha = 0.22f),
                    selectedLabelColor = FundGoldDark
                )
            )
            regions.forEach { reg ->
                FilterChip(
                    selected = selectedRegionFilter == reg.name,
                    onClick = { selectedRegionFilter = reg.name },
                    label = { Text(reg.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundGold.copy(alpha = 0.22f),
                        selectedLabelColor = FundGoldDark
                    )
                )
            }
        }

        // Members List or Empty State
        if (filteredMembers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                FundEmptyState(
                    icon = Icons.Default.PeopleOutline,
                    title = "لا يوجد أعضاء مطابقون",
                    subtitle = "لم يتم العثور على أعضاء مطابقين لعبارة البحث أو فلاتر المنطقة والحالة الحالية.",
                    actionLabel = if (canEdit) "إضافة عضو جديد" else null,
                    onAction = if (canEdit) ({ showAddDialog = true }) else null,
                    secondaryActionLabel = "إعادة ضبط الفلاتر",
                    onSecondaryAction = {
                        searchQuery = ""
                        selectedStatusFilter = "الكل"
                        selectedRegionFilter = "كل المناطق"
                    }
                )
            }
        } else {
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
                        onToggleStatus = { memberToToggleStatus = member }
                    )
                }
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
            existingMemberNos = members.map { it.memberNo }.toSet(),
            membersViewModel = membersViewModel,
            onDismiss = { memberToEdit = null },
            onSave = { updated ->
                membersViewModel.addVillageByRegionName(updated.region, updated.village)
                membersViewModel.updateMember(updated)
                Toast.makeText(context, "تم تحديث بيانات العضو بنجاح", Toast.LENGTH_SHORT).show()
                memberToEdit = null
            }
        )
    }

    // Add Member Dialog
    if (showAddDialog) {
        MemberFormDialog(
            initialMember = null,
            existingMemberNos = members.map { it.memberNo }.toSet(),
            membersViewModel = membersViewModel,
            onDismiss = { showAddDialog = false },
            onSave = { newMember ->
                membersViewModel.addVillageByRegionName(newMember.region, newMember.village)
                membersViewModel.addMember(newMember)
                Toast.makeText(context, "تم تسجيل العضو الجديد بنجاح", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }

    // Confirm Suspend / Activate Member Dialog
    memberToToggleStatus?.let { target ->
        val willSuspend = target.status == "نشط"
        SensitiveActionConfirmDialog(
            title = if (willSuspend) "تأكيد تعليق العضوية" else "تأكيد إعادة تفعيل العضوية",
            message = if (willSuspend) {
                "هل أنت متأكد من تعليق عضوية (${target.fullName} - ${target.memberNo})؟ لن يتمكن من الاستفادة من الاعتمادات الجديدة حتى إعادة التفعيل."
            } else {
                "هل تريد إعادة تفعيل عضوية (${target.fullName} - ${target.memberNo}) ليعود عضواً نشطاً في الصندوق؟"
            },
            confirmText = if (willSuspend) "تأكيد التعليق" else "تفعيل العضوية",
            confirmColor = if (willSuspend) StatusDanger else StatusSuccess,
            icon = if (willSuspend) Icons.Default.PersonOff else Icons.Default.HowToReg,
            onConfirm = {
                membersViewModel.toggleMemberStatus(target.id)
                Toast.makeText(
                    context,
                    if (willSuspend) "تم تعليق العضوية" else "تم إعادة تفعيل العضوية بنجاح",
                    Toast.LENGTH_SHORT
                ).show()
                memberToToggleStatus = null
            },
            onDismiss = { memberToToggleStatus = null }
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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

                    Column(modifier = Modifier.weight(1f)) {
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
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = FundPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                    Text("المنطقة: ${member.region}", fontSize = 10.sp, color = FundPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Surface(color = FundGold.copy(alpha = 0.18f), shape = RoundedCornerShape(6.dp)) {
                    Text("القرية: ${member.village}", fontSize = 10.sp, color = FundGoldDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            if (member.job.isNotBlank()) {
                Text("المهنة / الصفة: ${member.job}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val screenFormat = LocalScreenFormat.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "إجمالي السداد: ${FormatUtils.formatAmount(member.totalPaidYer, screenFormat)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FundPrimary,
                    modifier = Modifier.weight(1f),
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
                Text(
                    text = "المستحق: ${FormatUtils.formatAmount(member.dueBalanceYer, screenFormat)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (member.dueBalanceYer > 0) StatusDanger else StatusSuccess,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

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
    existingMemberNos: Set<String>,
    membersViewModel: MembersViewModel,
    onDismiss: () -> Unit,
    onSave: (Member) -> Unit
) {
    val context = LocalContext.current
    val regions by membersViewModel.regions.collectAsStateWithLifecycle()
    val settings by membersViewModel.appSettings.collectAsStateWithLifecycle()

    val isEditing = initialMember != null
    var fullName by remember { mutableStateOf(initialMember?.fullName ?: "") }
    var phone by remember { mutableStateOf(initialMember?.phone ?: "") }
    var nationalId by remember { mutableStateOf(initialMember?.nationalId ?: "") }
    var job by remember { mutableStateOf(initialMember?.job ?: "") }

    val defaultRegion = initialMember?.region ?: regions.firstOrNull()?.name ?: ""
    val defaultVillage = initialMember?.village ?: regions.firstOrNull()?.villages?.firstOrNull() ?: ""

    var selectedRegion by remember { mutableStateOf(defaultRegion) }
    var selectedVillage by remember { mutableStateOf(defaultVillage) }
    var regionDropdownExpanded by remember { mutableStateOf(false) }
    var villageDropdownExpanded by remember { mutableStateOf(false) }
    var showNewRegionField by remember { mutableStateOf(false) }
    var newRegionInput by remember { mutableStateOf("") }
    var showNewVillageField by remember { mutableStateOf(false) }
    var newVillageInput by remember { mutableStateOf("") }

    var dueStr by remember { mutableStateOf(initialMember?.dueBalanceYer?.toLong()?.toString() ?: "0") }

    val activeRegionObj = regions.find { it.name == selectedRegion }
    val villagesForRegion = activeRegionObj?.villages ?: emptyList()

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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    label = { Text(if (settings.requireNationalId) "رقم الهوية الوطنية (إلزامي)" else "رقم الهوية الوطنية (اختياري)") },
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

                // Separate Region Selector + Quick Add
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المنطقة (تضم عدة قرى):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
                    TextButton(
                        onClick = { showNewRegionField = !showNewRegionField },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(if (showNewRegionField) "إلغاء" else "+ منطقة جديدة", fontSize = 11.sp)
                    }
                }

                if (showNewRegionField) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newRegionInput,
                            onValueChange = { newRegionInput = it },
                            label = { Text("اسم المنطقة الجديدة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val r = newRegionInput.trim()
                                if (r.isNotBlank()) {
                                    membersViewModel.addVillageByRegionName(r, "")
                                    selectedRegion = r
                                    selectedVillage = ""
                                    newRegionInput = ""
                                    showNewRegionField = false
                                    showNewVillageField = true
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) { Text("إضافة", fontSize = 11.sp) }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { regionDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedRegion.ifBlank { "اختر المنطقة..." })
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = regionDropdownExpanded,
                            onDismissRequest = { regionDropdownExpanded = false }
                        ) {
                            regions.forEach { reg ->
                                DropdownMenuItem(
                                    text = { Text("${reg.name} (${reg.villages.size} قرى)") },
                                    onClick = {
                                        selectedRegion = reg.name
                                        selectedVillage = reg.villages.firstOrNull() ?: ""
                                        regionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Separate Village Selector (filtered by Region) + Quick Add
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("القرية التابعة للمنطقة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
                    TextButton(
                        onClick = { showNewVillageField = !showNewVillageField },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(if (showNewVillageField) "اختيار من القائمة" else "+ قرية جديدة", fontSize = 11.sp)
                    }
                }

                if (showNewVillageField || villagesForRegion.isEmpty()) {
                    OutlinedTextField(
                        value = if (showNewVillageField) newVillageInput else selectedVillage,
                        onValueChange = {
                            if (showNewVillageField) newVillageInput = it else selectedVillage = it
                        },
                        label = { Text("اكتب اسم القرية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { villageDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedVillage.ifBlank { "اختر القرية..." })
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = villageDropdownExpanded,
                            onDismissRequest = { villageDropdownExpanded = false }
                        ) {
                            villagesForRegion.forEach { vName ->
                                DropdownMenuItem(
                                    text = { Text(vName) },
                                    onClick = {
                                        selectedVillage = vName
                                        villageDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                val screenFormat = LocalScreenFormat.current
                OutlinedTextField(
                    value = dueStr,
                    onValueChange = { dueStr = it },
                    label = { Text("الرصيد المستحق / المتأخرات (${screenFormat.resolvedCurrencySymbol()})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRegion = selectedRegion.trim()
                    val finalVillage = (if (showNewVillageField) newVillageInput else selectedVillage).trim()
                    if (fullName.isBlank()) {
                        Toast.makeText(context, "يرجى إدخال اسم العضو", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (settings.requireNationalId && nationalId.isBlank()) {
                        Toast.makeText(context, "رقم الهوية الوطنية إلزامي حسب إعدادات النظام", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (finalRegion.isBlank() || finalVillage.isBlank()) {
                        Toast.makeText(context, "يرجى تحديد المنطقة والقرية", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val due = dueStr.toDoubleOrNull() ?: 0.0
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                    val prefix = settings.memberNoPrefix.ifBlank { "M-" }
                    val member = if (initialMember != null) {
                        initialMember.copy(
                            fullName = fullName.trim(),
                            phone = phone.trim(),
                            nationalId = nationalId.trim(),
                            job = job.trim(),
                            region = finalRegion,
                            village = finalVillage,
                            dueBalanceYer = due
                        )
                    } else {
                        val maxExisting = existingMemberNos
                            .mapNotNull { it.substringAfterLast("-").toIntOrNull() }
                            .maxOrNull() ?: 1000
                        var nextSeq = maxOf(maxExisting + 1, 1001)
                        var generatedMemberNo = "$prefix$nextSeq"
                        while (generatedMemberNo in existingMemberNos) {
                            nextSeq++
                            generatedMemberNo = "$prefix$nextSeq"
                        }
                        Member(
                            memberNo = generatedMemberNo,
                            fullName = fullName.trim(),
                            phone = phone.trim(),
                            nationalId = nationalId.trim(),
                            joinDate = today,
                            status = "نشط",
                            totalPaidYer = 0.0,
                            dueBalanceYer = due,
                            job = job.trim(),
                            region = finalRegion,
                            village = finalVillage
                        )
                    }
                    onSave(member)
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
