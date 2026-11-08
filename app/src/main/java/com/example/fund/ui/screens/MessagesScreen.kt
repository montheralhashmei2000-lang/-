package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.ui.theme.*

@Composable
fun MessagesScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val messages by repo.internalMessages.collectAsState()

    var showComposeDialog by remember { mutableStateOf(false) }

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
                Text("المراسلات الإدارية الداخلية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FundPrimary)
                Text("التواصل بين المحاسب والمراجع ومجلس الإدارة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = { showComposeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("رسالة جديدة", fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { repo.markMessageRead(msg.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (msg.isRead) MaterialTheme.colorScheme.surface else FundLightSurfaceSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (msg.isRead) MaterialTheme.colorScheme.outline else FundPrimary)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (msg.isRead) MaterialTheme.colorScheme.onSurface else FundPrimary)
                            if (!msg.isRead) {
                                Surface(color = FundGoldDark, shape = RoundedCornerShape(4.dp)) {
                                    Text("جديدة", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }

                        Text("من: ${msg.senderName} (${msg.senderRole}) إلى: ${msg.recipientRole}", fontSize = 11.sp, color = Color.Gray)
                        Text(msg.body, fontSize = 12.sp, lineHeight = 18.sp)
                        Text(msg.timestamp, fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }
    }

    if (showComposeDialog) {
        ComposeMessageDialog(
            onDismiss = { showComposeDialog = false },
            onSend = { subject, body, recipient ->
                repo.sendMessage(subject, body, recipient)
                Toast.makeText(context, "تم إرسال الرسالة بنجاح", Toast.LENGTH_SHORT).show()
                showComposeDialog = false
            }
        )
    }
}

@Composable
private fun ComposeMessageDialog(
    onDismiss: () -> Unit,
    onSend: (String, String, String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("مدير النظام") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إرسال رسالة داخلية جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("موضوع الرسالة") }, modifier = Modifier.fillMaxWidth())
                Text("المستلم المستهدف:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("مدير النظام", "محاسب", "مراجع", "الكل").forEach { role ->
                        FilterChip(selected = recipient == role, onClick = { recipient = role }, label = { Text(role, fontSize = 11.sp) })
                    }
                }
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("نص الرسالة / الملاحظة") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && body.isNotBlank()) onSend(subject, body, recipient)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundPrimary)
            ) {
                Text("إرسال الرسالة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
