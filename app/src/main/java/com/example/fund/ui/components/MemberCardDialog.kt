package com.example.fund.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.fund.data.FundRepository
import com.example.fund.model.Member
import com.example.fund.util.OfficialPrintHelper
import com.example.fund.util.QrCodeView
import com.example.ui.theme.*

@Composable
fun MemberCardDialog(
    member: Member,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val fundInfo by repo.fundInfo.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بطاقة العضوية الرسمية",
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = FundPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The Physical Card Layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(FundDeepHeader, FundPrimaryMedium, Color(0xFF1B5E20))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(2.dp, FundGold, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Card Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_fund_logo),
                                    contentDescription = "شعار الصندوق",
                                    modifier = Modifier.size(36.dp)
                                )
                                Column {
                                    Text(
                                        fundInfo.name,
                                        fontFamily = AmiriFontFamily,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "بطاقة عضوية معتمدة - ردفان",
                                        color = FundGoldLight,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Surface(
                                color = if (member.status == "نشط") FundGold else Color.Gray,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = member.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Middle Content
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Avatar Box
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    .border(2.dp, FundGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "صورة العضو",
                                    tint = Color.White,
                                    modifier = Modifier.size(46.dp)
                                )
                            }

                            // Member Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.fullName,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "رقم العضوية: ${member.memberNo}",
                                    color = FundGoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "المهنة: ${member.job}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "المنطقة: ${member.villageOrArea}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp
                                )
                            }

                            // QR on Card
                            QrCodeView(
                                content = "MEMBER|${member.memberNo}|${member.nationalId}|${member.status}",
                                sizeDp = 64.dp,
                                dotColor = FundDeepHeader
                            )
                        }

                        // Bottom Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "تاريخ الانتساب: ${member.joinDate}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 9.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = FundGoldLight, modifier = Modifier.size(12.dp))
                                Text("إجمالي السداد: %,d ﷼".format(member.totalPaidYer.toLong()), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Print Button
                Button(
                    onClick = {
                        OfficialPrintHelper.printMemberCardPdf(context, member, fundInfo)
                        Toast.makeText(context, "جاري فتح نافذة الطباعة وحفظ بطاقة العضوية PDF...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("طباعة بطاقة العضوية (PDF)")
                }
            }
        }
    }
}
