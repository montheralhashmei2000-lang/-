package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import com.example.fund.model.Campaign
import com.example.fund.model.CurrencyCode
import com.example.fund.model.PaymentMethod
import com.example.fund.model.UserRole
import com.example.fund.ui.components.MoneyInput
import com.example.ui.theme.*

@Composable
fun CampaignsScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val campaigns by repo.campaigns.collectAsState()
    val currentUser by repo.currentUser.collectAsState()
    val canManage = currentUser.role != UserRole.VIEWER

    var showNewCampaignDialog by remember { mutableStateOf(false) }
    var campaignToDonate by remember { mutableStateOf<Campaign?>(null) }
    var campaignToDisburse by remember { mutableStateOf<Campaign?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "حملات التبرعات والمشاريع الخيرية",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FundPrimary
                )
                Text("مشاريع التنمية والإغاثة العاجلة بردفان", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (canManage) {
                Button(
                    onClick = { showNewCampaignDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حملة جديدة", fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(campaigns, key = { it.id }) { c ->
                val ratio = (c.collectedAmountYer / c.goalAmountYer.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                val remaining = (c.collectedAmountYer - c.spentAmountYer).coerceAtLeast(0.0)
                val isCompleted = c.status == "مكتملة" || ratio >= 1.0f

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                color = if (isCompleted) StatusSuccess else StatusInfo,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isCompleted) "مكتملة الهدف ✔" else "حملة نشطة",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(c.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (isCompleted) StatusSuccess else FundGold,
                            trackColor = Color(0xFFEEEEEE)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المحصل: ${"%,d".format(c.collectedAmountYer.toLong())} ﷼ (${(ratio * 100).toInt()}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FundPrimary)
                            Text("المصروف: %,d ﷼".format(c.spentAmountYer.toLong()), fontSize = 11.sp, color = StatusDanger)
                            Text("المتبقي: %,d ﷼".format(remaining.toLong()), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FundGoldDark)
                        }

                        Text("المبلغ المستهدف الكلي: %,d ﷼".format(c.goalAmountYer.toLong()), fontSize = 11.sp, color = Color.Gray)

                        if (canManage) {
                            HorizontalDivider(color = Color(0xFFEEEEEE))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { campaignToDisburse = c },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("صرف من الحملة", color = StatusDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { campaignToDonate = c },
                                    colors = ButtonDefaults.buttonColors(containerColor = FundGold),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسجيل تبرع للحملة", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Donate Modal
    campaignToDonate?.let { campaign ->
        DonateToCampaignDialog(
            campaign = campaign,
            onDismiss = { campaignToDonate = null },
            onDonate = { donorName, amount, currency, method ->
                repo.donateToCampaign(campaign.id, donorName, amount, currency, method)
                Toast.makeText(context, "تم تسجيل التبرع للحملة وإصدار سند القبض والقيد المحاسبي", Toast.LENGTH_SHORT).show()
                campaignToDonate = null
            }
        )
    }

    // Disburse Modal
    campaignToDisburse?.let { campaign ->
        DisburseFromCampaignDialog(
            campaign = campaign,
            onDismiss = { campaignToDisburse = null },
            onDisburse = { recipient, amountYer, desc ->
                repo.disburseFromCampaign(campaign.id, recipient, amountYer, desc)
                Toast.makeText(context, "تم الصرف من الحملة وإصدار سند الصرف والقيد المحاسبي", Toast.LENGTH_SHORT).show()
                campaignToDisburse = null
            }
        )
    }

    if (showNewCampaignDialog) {
        NewCampaignDialog(
            onDismiss = { showNewCampaignDialog = false },
            onAdd = { newC ->
                repo.addCampaign(newC)
                Toast.makeText(context, "تم إطلاق الحملة بنجاح", Toast.LENGTH_SHORT).show()
                showNewCampaignDialog = false
            }
        )
    }
}

@Composable
private fun DonateToCampaignDialog(
    campaign: Campaign,
    onDismiss: () -> Unit,
    onDonate: (String, Double, CurrencyCode, PaymentMethod) -> Unit
) {
    var donorName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("100000") }
    var currency by remember { mutableStateOf(CurrencyCode.YER) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل تبرع لحملة: ${campaign.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = donorName,
                    onValueChange = { donorName = it },
                    label = { Text("اسم المتبرع / فاعل خير") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                MoneyInput(
                    amount = amountStr,
                    onAmountChange = { amountStr = it },
                    selectedCurrency = currency,
                    onCurrencyChange = { currency = it },
                    label = "مبلغ التبرع"
                )

                Text("طريقة الإيداع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaymentMethod.values().forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.title, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onDonate(donorName, amt, currency, method)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("إيداع وإصدار سند قبض")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun DisburseFromCampaignDialog(
    campaign: Campaign,
    onDismiss: () -> Unit,
    onDisburse: (String, Double, String) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("150000") }
    var desc by remember { mutableStateOf("دفعة تنفيذ ميداني للحملة") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("صرف مالي من حملة: ${campaign.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("يصرف للمكرم / المقاول / المستفيد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ المصروف (ريال يمني)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("البيان التفصيلي للصرف") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onDisburse(recipient, amt, desc)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
            ) {
                Text("تأكيد الصرف وإصدار السند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun NewCampaignDialog(
    onDismiss: () -> Unit,
    onAdd: (Campaign) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var goalStr by remember { mutableStateOf("3000000") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إطلاق حملة تبرعات جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الحملة") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = goalStr,
                    onValueChange = { goalStr = it },
                    label = { Text("المبلغ المستهدف (ريال يمني)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("وصف الحملة والفئة المستهدفة") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goal = goalStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && goal > 0) {
                        val c = Campaign(
                            title = title,
                            goalAmountYer = goal,
                            collectedAmountYer = 0.0,
                            spentAmountYer = 0.0,
                            description = desc
                        )
                        onAdd(c)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("حفظ وإطلاق")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
