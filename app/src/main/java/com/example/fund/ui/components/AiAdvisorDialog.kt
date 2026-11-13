package com.example.fund.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fund.ai.GeminiFundAdvisor
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiAdvisorDialog(
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Financial Health, 1: Aid Guidelines

    LaunchedEffect(activeTab) {
        isLoading = true
        coroutineScope.launch {
            val repo = com.example.fund.data.FundRepository.getInstance()
            val fi = repo.fundInfo.value
            val st = repo.appSettings.value
            resultText = if (activeTab == 0) {
                GeminiFundAdvisor.analyzeFinancialHealth()
            } else {
                """
                    📋 **ضوابط ومعايير صرف المساعدات (${fi.name})**:
                    
                    1. **سقف المساعدات الفردية الأقصى:**
                       - الحد الأعلى المعتمد للصرف الفردي: حتى ${"%,d".format(fi.maxAidCeilingYer.toLong())} ﷼.
                       - معونة الزواج للأعضاء: 200,000 ﷼.
                       - مساعدات الطوارئ والكوارث: حتى 150,000 ﷼.
                       - المنح التعليمية الجامعية: حتى 150,000 ﷼.
                    
                    2. **شروط الأهلية والاستحقاق:**
                       - مرور 6 أشهر على الأقل من تاريخ انتساب العضو وسداد الاشتراكات.
                       - عدم وجود متأخرات مستحقة تتجاوز ${st.autoSuspendMonths} أشهر.
                       - إرفاق التقارير والمستندات الرسمية المعمدة من الجهات المختصة.
                    
                    3. **آلية اتخاذ القرار:**
                       - تمر المساعدة بلجنة المراجعة والتدقيق المالي قبل مصادقة رئيس مجلس الإدارة وإصدار سند الصرف المحاسبي.
                """.trimIndent()
            }
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = FundGoldDark)
                        Text(
                            text = "مستشار الذكاء الاصطناعي (Gemini Advisor)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = FundPrimary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("تحليل السيولة والمركز المالي", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("معايير أهلية المساعدات", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = FundPrimary)
                            Text("جاري استدعاء محرك الذكاء الاصطناعي وتحليل البيانات...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = resultText,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Re-evaluate button
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            resultText = if (activeTab == 0) GeminiFundAdvisor.analyzeFinancialHealth() else resultText
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة التقييم الفوري للبيانات الحية")
                }
            }
        }
    }
}
