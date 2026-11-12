package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.fund.data.FundRepository
import com.example.fund.model.InKindItem
import com.example.fund.model.InKindMovement
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InKindScreen() {
    val repo = FundRepository.getInstance()
    val items by repo.inKindItems.collectAsState()
    val movements by repo.inKindMovements.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: المخزون، 1: سجل الحركات
    var showMoveDialog by remember { mutableStateOf<Pair<InKindItem, String>?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    val totalValueYer = items.sumOf { (it.quantityOnHand * it.unitCostYer).toLong() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("in_kind_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "المستودع والمساعدات العينية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "إجمالي القيمة التقديرية للمخزون: ${"%,d ﷼".format(totalValueYer)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showAddItemDialog = true },
                    modifier = Modifier.testTag("add_in_kind_item_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("صنف جديد", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = {
                        items.firstOrNull()?.let { showMoveDialog = it to "توريد" }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    modifier = Modifier.testTag("supply_in_kind_button")
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("توريد", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("الأصناف والمخزون (${items.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("سجل الحركات (${movements.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val isLowStock = item.quantityOnHand <= item.reorderLevel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "الكود: ${item.code} | الفئة: ${item.category} | قيمة الوحدة: ${"%,d ﷼".format(item.unitCostYer.toLong())}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = if (isLowStock) StatusDanger.copy(alpha = 0.14f) else StatusSuccess.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "${item.quantityOnHand} ${item.unit}",
                                        color = if (isLowStock) StatusDanger else StatusSuccess,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            if (isLowStock) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "تنبيه: الرصيد وصل أو أقل من الحد الأدنى (${item.reorderLevel} ${item.unit})",
                                    color = StatusDanger,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showMoveDialog = item to "توريد" },
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Text("توريد للمخزن (+)", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = { showMoveDialog = item to "صرف وتوزيع" },
                                    enabled = item.quantityOnHand > 0,
                                    colors = ButtonDefaults.buttonColors(containerColor = FundGold, contentColor = Color(0xFF14210F)),
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Text("صرف لمستفيد (-)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(movements, key = { it.id }) { mov ->
                    InKindMovementCard(mov)
                }
            }
        }
    }

    showMoveDialog?.let { (initialItem, typeStr) ->
        InKindMovementDialog(
            items = items,
            initialItem = initialItem,
            typeStr = typeStr,
            onDismiss = { showMoveDialog = null },
            onSave = { selected, qty, party, notes ->
                if (typeStr != "توريد" && qty > selected.quantityOnHand) {
                    Toast.makeText(context, "الكمية المطلوبة أكبر من الرصيد المتاح بالمستودع", Toast.LENGTH_SHORT).show()
                } else {
                    val movNo = "MOV-%03d".format(movements.size + 1)
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                    repo.addInKindMovement(
                        InKindMovement(
                            movementNo = movNo,
                            type = typeStr,
                            itemId = selected.id,
                            itemName = selected.name,
                            quantity = qty,
                            partyName = party,
                            date = today,
                            notes = notes
                        )
                    )
                    Toast.makeText(context, "تم تسجيل حركة المستودع وتحديث الرصيد بنجاح", Toast.LENGTH_SHORT).show()
                    showMoveDialog = null
                }
            }
        )
    }

    if (showAddItemDialog) {
        AddInKindItemDialog(
            nextIndex = items.size + 1,
            onDismiss = { showAddItemDialog = false },
            onSave = { newItem ->
                repo.addInKindItem(newItem)
                showAddItemDialog = false
                Toast.makeText(context, "تمت إضافة الصنف العيني الجديد إلى المستودع", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun InKindMovementCard(mov: InKindMovement) {
    val isSupply = mov.type == "توريد"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = (if (isSupply) StatusSuccess else StatusInfo).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            mov.type,
                            color = if (isSupply) StatusSuccess else StatusInfo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(mov.itemName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${if (isSupply) "المورد / المتبرع" else "المستفيد"}: ${mov.partyName} | التاريخ: ${mov.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (mov.notes.isNotBlank()) {
                    Text(
                        "البيان: ${mov.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "${if (isSupply) "+" else "-"}${mov.quantity}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSupply) StatusSuccess else StatusDanger
            )
        }
    }
}

@Composable
private fun InKindMovementDialog(
    items: List<InKindItem>,
    initialItem: InKindItem,
    typeStr: String,
    onDismiss: () -> Unit,
    onSave: (InKindItem, Int, String, String) -> Unit
) {
    val isSupply = typeStr == "توريد"
    var selectedItem by remember { mutableStateOf(initialItem) }
    var itemDropdownExpanded by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("5") }
    var party by remember {
        mutableStateOf(
            if (isSupply) "فاعل خير - توريد مباشر" else "أسرة مستفيدة"
        )
    }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "$typeStr مستودعي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))

                Text("الصنف العيني:", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { itemDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${selectedItem.name} (المتوفر: ${selectedItem.quantityOnHand} ${selectedItem.unit})", fontWeight = FontWeight.Bold)
                            Text("▼")
                        }
                    }
                    DropdownMenu(
                        expanded = itemDropdownExpanded,
                        onDismissRequest = { itemDropdownExpanded = false }
                    ) {
                        items.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text("${candidate.name} (${candidate.quantityOnHand} ${candidate.unit})") },
                                onClick = {
                                    selectedItem = candidate
                                    itemDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                    label = { Text("الكمية (${selectedItem.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = party,
                    onValueChange = { party = it },
                    label = { Text(if (isSupply) "المتبرع / المورد" else "اسم المستفيد") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات ورقم إذن الصرف/التوريد") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val q = qtyText.toIntOrNull() ?: 0
                            if (q > 0 && party.isNotBlank()) onSave(selectedItem, q, party, notes)
                        }
                    ) {
                        Text("حفظ الحركة")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddInKindItemDialog(
    nextIndex: Int,
    onDismiss: () -> Unit,
    onSave: (InKindItem) -> Unit
) {
    val appSettings = FundRepository.getInstance().appSettings.collectAsState().value
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("مواد غذائية") }
    var unit by remember { mutableStateOf("كرتون") }
    var qtyText by remember { mutableStateOf("10") }
    var unitValText by remember { mutableStateOf("25000") }
    var thresholdText by remember(appSettings.lowStockThreshold) { mutableStateOf(appSettings.lowStockThreshold.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("تعريف صنف عيني جديد بالمستودع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف (مثال: بطانيات شتوية / أرز بسمتي)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("الفئة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("الوحدة (سلة/كيس/قطعة)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                        label = { Text("الرصيد الافتتاحي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it.filter { c -> c.isDigit() } },
                        label = { Text("حد التنبيه الأدنى") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = unitValText,
                    onValueChange = { unitValText = it.filter { c -> c.isDigit() } },
                    label = { Text("القيمة التقديرية للوحدة (ريال يمني)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val q = qtyText.toIntOrNull() ?: 0
                            val v = unitValText.toDoubleOrNull() ?: 0.0
                            val t = thresholdText.toIntOrNull() ?: 5
                            if (name.isNotBlank() && v > 0) {
                                onSave(
                                    InKindItem(
                                        code = "IK-%03d".format(nextIndex),
                                        name = name.trim(),
                                        category = category.trim(),
                                        unit = unit.trim(),
                                        quantityOnHand = q,
                                        reorderLevel = t,
                                        unitCostYer = v,
                                        location = appSettings.defaultWarehouseName
                                    )
                                )
                            }
                        }
                    ) {
                        Text("إضافة الصنف")
                    }
                }
            }
        }
    }
}
