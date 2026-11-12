package com.example.fund.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.fund.data.FundRepository
import com.example.fund.model.CurrencyCode
import com.example.fund.model.Voucher
import com.example.fund.model.VoucherStatus
import com.example.fund.model.VoucherType
import com.example.fund.util.OfficialPrintHelper
import com.example.fund.util.QrCodeView
import com.example.ui.theme.*

@Composable
fun VoucherPrintDialog(
    voucher: Voucher,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repo = FundRepository.getInstance()
    val fundInfo by repo.fundInfo.collectAsState()

    val isReceipt = voucher.type == VoucherType.RECEIPT
    val titleColor = if (isReceipt) Color(0xFF1B5E20) else Color(0xFFB71C1C)
    val voucherTitle = if (isReceipt) "سـنـد قـبـض" else "سـنـد صـرف"
    val isVoid = voucher.status == VoucherStatus.VOID

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFBFDFB),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                OfficialPrintHelper.printVoucherPdf(context, voucher, fundInfo)
                                Toast.makeText(context, "جاري فتح نافذة الطباعة وحفظ PDF الرسمي...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FundPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "طباعة", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طباعة رسمية / PDF", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareText = """
                                    ${fundInfo.name}
                                    ${voucherTitle} رقم: ${voucher.voucherNo}
                                    التاريخ: ${voucher.date}
                                    المكرم: ${voucher.partyName}
                                    المبلغ: ${"%,d".format(voucher.amountYer.toLong())} ﷼ (${voucher.amountWords})
                                    البيان: ${voucher.description}
                                    القيد المحاسبي: ${voucher.linkedJournalEntryId}
                                """.trimIndent()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة بيانات السند"))
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة", fontSize = 13.sp)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Official Voucher Sheet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .border(2.dp, Color(0xFF1B5E20), RoundedCornerShape(10.dp))
                        .padding(3.dp)
                        .border(1.dp, Color(0xFFF9A825), RoundedCornerShape(8.dp))
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // 1. Triple Official Header (Linked dynamically to FundInfo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Right Side: Government & Fund Details
                            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                                Text("الجمهورية اليمنية", fontFamily = AmiriFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FundDeepHeader)
                                Text(fundInfo.name, fontFamily = AmiriFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FundPrimary)
                                Text("الإدارة المالية - الحسابات", fontSize = 11.sp, color = Color.Gray)
                                Text(fundInfo.licenseNo, fontSize = 10.sp, color = Color.DarkGray)
                                Text(fundInfo.address, fontSize = 10.sp, color = Color.DarkGray)
                            }

                            // Center: Crest Emblem & Title
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                FundLogoImage(
                                    customLogoBase64 = fundInfo.customLogoBase64,
                                    contentDescription = "شعار الصندوق",
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .border(2.dp, titleColor, RoundedCornerShape(6.dp))
                                        .background(titleColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = voucherTitle,
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = titleColor
                                    )
                                }
                            }

                            // Left Side: Voucher Serial, Date, Amount Box
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "رقم السند: ${voucher.voucherNo}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = titleColor
                                )
                                Text("التاريخ: ${voucher.date}", fontSize = 11.sp)
                                Text("القيد الآلي: ${voucher.linkedJournalEntryId}", fontSize = 10.sp, color = FundPrimaryMedium)

                                Spacer(modifier = Modifier.height(6.dp))

                                // Boxed Amount in Gold
                                Box(
                                    modifier = Modifier
                                        .border(1.5.dp, FundGoldDark, RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFF9C4), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "# %,d # ﷼".format(voucher.amountYer.toLong()),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color(0xFF003300)
                                        )
                                        if (voucher.currency != CurrencyCode.YER) {
                                            Text(
                                                text = "${voucher.amountOriginal} ${voucher.currency.name} @ %,.0f".format(voucher.exchangeRate),
                                                fontSize = 9.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 1.dp,
                            color = Color(0xFFD8E4D2)
                        )

                        // 2. Voucher Body Fields
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VoucherFieldRow(
                                label = if (isReceipt) "استلمنا من المكرم:" else "يصرف للمكرم:",
                                value = voucher.partyName,
                                isBold = true
                            )

                            VoucherFieldRow(
                                label = "مبلغ وقدره كتابةً:",
                                value = voucher.amountWords,
                                isHighlight = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    VoucherFieldRow(label = "طريقة الدفع:", value = voucher.paymentMethod.title)
                                }
                                Box(modifier = Modifier.weight(1.2f)) {
                                    VoucherFieldRow(label = "الحساب المالي:", value = voucher.accountName)
                                }
                            }

                            VoucherFieldRow(
                                label = "وذلك مقابل:",
                                value = voucher.description
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Signatures & Verification QR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // QR Verification Badge
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                QrCodeView(
                                    content = "SOCIALFUND|${voucher.voucherNo}|${voucher.amountYer}|${voucher.date}|${voucher.status.name}",
                                    sizeDp = 86.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("رمز التحقق الرقمي", fontSize = 9.sp, color = Color.Gray)
                            }

                            // 4 Signature Boxes
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            ) {
                                SignatureBox(title = "المستلم", name = voucher.receivedOrPaidBy)
                                SignatureBox(title = "المحاسب", name = voucher.accountantName)
                                SignatureBox(title = "المراجع المالي", name = voucher.auditorName)
                                SignatureBox(title = "يعتمد / رئيس الصندوق", name = voucher.managerName)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Footer Note
                        Text(
                            text = "${fundInfo.name} - ${fundInfo.subtitle} | هاتف: ${fundInfo.phone}",
                            textAlign = TextAlign.Center,
                            fontSize = 9.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Void Watermark if Voided
                    if (isVoid) {
                        Box(
                            modifier = Modifier
                                .rotate(-32f)
                                .border(5.dp, Color(0xFFD32F2F).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "سند ملغي - VOID",
                                fontFamily = AmiriFontFamily,
                                color = Color(0xFFD32F2F).copy(alpha = 0.65f),
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoucherFieldRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isHighlight) Color(0xFFF9FBF8) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            fontFamily = if (isHighlight) AmiriFontFamily else TajawalFontFamily,
            fontSize = if (isHighlight) 15.sp else 13.sp,
            fontWeight = if (isBold || isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) Color(0xFF003300) else Color(0xFF14210F),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RowScope.SignatureBox(title: String, name: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .border(1.dp, Color(0xFFD8E4D2), RoundedCornerShape(6.dp))
            .background(Color(0xFFFAFCFA), RoundedCornerShape(6.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FundDeepHeader)
        Spacer(modifier = Modifier.height(28.dp)) // Signature line space
        Text(name, fontSize = 9.sp, color = Color.DarkGray, maxLines = 1)
    }
}
