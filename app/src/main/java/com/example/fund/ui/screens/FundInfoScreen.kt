package com.example.fund.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.data.FundRepository
import com.example.ui.theme.*

@Composable
fun FundInfoScreen() {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val fundInfo by repo.fundInfo.collectAsState()

    var name by remember(fundInfo) { mutableStateOf(fundInfo.name) }
    var subtitle by remember(fundInfo) { mutableStateOf(fundInfo.subtitle) }
    var licenseNo by remember(fundInfo) { mutableStateOf(fundInfo.licenseNo) }
    var address by remember(fundInfo) { mutableStateOf(fundInfo.address) }
    var phone by remember(fundInfo) { mutableStateOf(fundInfo.phone) }
    var email by remember(fundInfo) { mutableStateOf(fundInfo.email) }
    var bankKareemi by remember(fundInfo) { mutableStateOf(fundInfo.bankAccountKareemi) }
    var bankCAC by remember(fundInfo) { mutableStateOf(fundInfo.bankAccountCAC) }
    var ceilingStr by remember(fundInfo) { mutableStateOf(fundInfo.maxAidCeilingYer.toLong().toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("بيانات الصندوق والترخيص الرسمي", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FundPrimary)
        Text("هذه البيانات تظهر تلقائياً في ترويسة السندات والبطاقات والتقارير المطبوعة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الصندوق الرسمي") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("الوصف الفرعي للإصدار") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = licenseNo, onValueChange = { licenseNo = it }, label = { Text("رقم ترخيص وزارة الشؤون الاجتماعية") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان والمديرية والمحافظة") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم هاتف الإدارة المالية") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني الرسمي") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bankKareemi, onValueChange = { bankKareemi = it }, label = { Text("بيانات حساب بنك الكريمي الإسلامي") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bankCAC, onValueChange = { bankCAC = it }, label = { Text("بيانات حساب بنك التسليف CAC") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ceilingStr, onValueChange = { ceilingStr = it }, label = { Text("السقف الأعلى الافتراضي للمساعدات الفردية (﷼)") }, modifier = Modifier.fillMaxWidth())

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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حفظ التعديلات الرسمية")
                }
            }
        }
    }
}
