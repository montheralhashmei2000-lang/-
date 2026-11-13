package com.example.fund.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.ui.components.FundLogoImage
import com.example.fund.ui.components.encodeImageUriToBase64
import com.example.ui.theme.*

@Composable
fun FundInfoScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val fundInfo by repo.fundInfo.collectAsStateWithLifecycle()
    val regions by repo.regions.collectAsStateWithLifecycle()

    var name by remember(fundInfo) { mutableStateOf(fundInfo.name) }
    var subtitle by remember(fundInfo) { mutableStateOf(fundInfo.subtitle) }
    var licenseNo by remember(fundInfo) { mutableStateOf(fundInfo.licenseNo) }
    var address by remember(fundInfo) { mutableStateOf(fundInfo.address) }
    var phone by remember(fundInfo) { mutableStateOf(fundInfo.phone) }
    var email by remember(fundInfo) { mutableStateOf(fundInfo.email) }
    var bankKareemi by remember(fundInfo) { mutableStateOf(fundInfo.bankAccountKareemi) }
    var bankCAC by remember(fundInfo) { mutableStateOf(fundInfo.bankAccountCAC) }
    var ceilingStr by remember(fundInfo) { mutableStateOf(fundInfo.maxAidCeilingYer.toLong().toString()) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = encodeImageUriToBase64(context, uri)
            if (!encoded.isNullOrBlank()) {
                repo.updateFundLogo(encoded)
                Toast.makeText(context, "تم تحديث شعار الصندوق بنجاح في جميع الشاشات والسندات", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "تعذر قراءة الصورة المختارة، يرجى اختيار صورة أخرى", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "بيانات الصندوق والشعار والترخيص الرسمي",
            fontFamily = AmiriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = FundPrimary
        )
        Text(
            "تظهر هذه البيانات والشعار تلقائياً في ترويسة السندات والبطاقات والقوائم المالية المطبوعة",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Live Official Document Letterhead Preview
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(2.dp, FundPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .border(1.dp, FundGold, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الجمهورية اليمنية", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text(name, fontFamily = AmiriFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FundPrimary)
                        Text(licenseNo, fontSize = 10.sp, color = FundGoldDark, fontWeight = FontWeight.Bold)
                        Text(address, fontSize = 10.sp, color = Color.Gray)
                    }

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(FundDeepHeader)
                            .border(2.dp, FundGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        FundLogoImage(
                            customLogoBase64 = fundInfo.customLogoBase64,
                            contentDescription = "شعار الصندوق",
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFD8E4D2))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = FundPrimary, modifier = Modifier.size(14.dp))
                        Text("معاينة حية للترويسة الرسمية", fontSize = 10.sp, color = FundPrimary, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "السقف الفردي: ${"%,d".format(ceilingStr.toLongOrNull() ?: 500000L)} ﷼",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FundGoldDark
                    )
                }
            }
        }

        // Dedicated Fund Logo Change Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, FundGold.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(FundDeepHeader)
                        .border(2.dp, FundGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    FundLogoImage(
                        customLogoBase64 = fundInfo.customLogoBase64,
                        contentDescription = "شعار الصندوق الحالي",
                        modifier = Modifier.size(56.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("شعار الصندوق الرسمي", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FundPrimary)
                    Text(
                        if (fundInfo.customLogoBase64.isBlank()) "يستخدم الشعار الافتراضي — يمكنك اختيار شعار قريتك أو صندوقك من الجهاز." else "تم تعيين شعار مخصص وحفظه في بيانات الصندوق.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("change_fund_logo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تغيير الشعار", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (fundInfo.customLogoBase64.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    repo.updateFundLogo("")
                                    Toast.makeText(context, "تمت استعادة الشعار الافتراضي للصندوق", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("الافتراضي", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Coverage Summary Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "النطاق الجغرافي المسجل (${regions.size} مناطق • ${regions.sumOf { it.villages.size }} قرية)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = FundPrimary
                )
                Text(
                    regions.joinToString(" | ") { "${it.name} (${it.villages.size} قرى)" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الصندوق / القرية / المنطقة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("الوصف الفرعي للإصدار") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = licenseNo, onValueChange = { licenseNo = it }, label = { Text("رقم الترخيص الرسمي") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان والمقر") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم هاتف الإدارة المالية") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني الرسمي") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = bankKareemi, onValueChange = { bankKareemi = it }, label = { Text("بيانات حساب بنك الكريمي الإسلامي") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = bankCAC, onValueChange = { bankCAC = it }, label = { Text("بيانات حساب بنك التسليف CAC") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ceilingStr, onValueChange = { ceilingStr = it }, label = { Text("السقف الأعلى الافتراضي للمساعدات الفردية (﷼)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Button(
                    onClick = {
                        val c = ceilingStr.toDoubleOrNull() ?: fundInfo.maxAidCeilingYer
                        repo.updateFundInfo(
                            fundInfo.copy(
                                name = name,
                                subtitle = subtitle,
                                licenseNo = licenseNo,
                                address = address,
                                phone = phone,
                                email = email,
                                bankAccountKareemi = bankKareemi,
                                bankAccountCAC = bankCAC,
                                maxAidCeilingYer = c
                            )
                        )
                        Toast.makeText(context, "تم حفظ بيانات الصندوق بنجاح وتحديث الترويسات الرسمية", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ التعديلات الرسمية", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
