package com.example.fund.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.model.*
import com.example.fund.util.FormatUtils
import com.example.ui.theme.*

@Composable
fun DisplayFormatSettingsCard(
    title: String,
    subtitle: String,
    config: DisplayFormatConfig,
    onConfigChange: (DisplayFormatConfig) -> Unit,
    accentColor: Color = FundPrimary,
    testTagPrefix: String = "screen_format"
) {
    val sampleAmount = 1250450.75
    val sampleDate = "2026-11-12 14:35"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Live Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTagPrefix}_preview_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Preview, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                        Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("معاينة المبلغ والعملة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FormatUtils.formatAmount(sampleAmount, config),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = accentColor,
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("داخل الجداول المحاسبية:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FormatUtils.formatAmount(sampleAmount, config, inTable = true),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("رقم السند والقيد:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${FormatUtils.formatCodeOrNumber("V-2026-0042", config)} • ${FormatUtils.formatCodeOrNumber("JE-000015", config)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = accentColor,
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("معاينة التاريخ والوقت:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FormatUtils.formatDate(sampleDate, config),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = FundGoldDark,
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }
            }
        }

        // Numbers & Separators Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Pin, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Text("تنسيق الأرقام والفواصل والمدخلات", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                }

                Text("لغة الأرقام:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DigitLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = config.digitLanguage == lang,
                            onClick = { onConfigChange(config.copy(digitLanguage = lang)) },
                            label = { Text(lang.title, fontSize = 11.sp) }
                        )
                    }
                }

                Text("فاصل الآلاف:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThousandsSeparatorOption.entries.forEach { sep ->
                        FilterChip(
                            selected = config.thousandsSeparator == sep,
                            onClick = { onConfigChange(config.copy(thousandsSeparator = sep)) },
                            label = { Text(sep.title, fontSize = 11.sp) }
                        )
                    }
                }

                Text("الفاصل العشري:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DecimalSeparatorOption.entries.forEach { decSep ->
                        FilterChip(
                            selected = config.decimalSeparator == decSep,
                            onClick = { onConfigChange(config.copy(decimalSeparator = decSep)) },
                            label = { Text(decSep.title, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("خانات الكسور العشرية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0, 2, 3).forEach { dec ->
                                FilterChip(
                                    selected = config.decimalPlaces == dec,
                                    onClick = { onConfigChange(config.copy(decimalPlaces = dec)) },
                                    label = { Text("$dec", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("أقصى خانات للمبلغ:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(9, 12, 15).forEach { maxDigits ->
                                FilterChip(
                                    selected = config.maxAmountDigits == maxDigits,
                                    onClick = { onConfigChange(config.copy(maxAmountDigits = maxDigits)) },
                                    label = { Text("$maxDigits", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Currency Symbol & Placement Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Text("العملة الافتراضية وموضع الرمز", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                }

                Text("العملة والرمز المعتمد للعرض:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DisplayCurrencyOption.entries.forEach { currOpt ->
                        FilterChip(
                            selected = config.currencyOption == currOpt,
                            onClick = { onConfigChange(config.copy(currencyOption = currOpt)) },
                            label = { Text("${currOpt.title} (${currOpt.defaultSymbol})", fontSize = 11.sp) }
                        )
                    }
                }

                if (config.currencyOption == DisplayCurrencyOption.CUSTOM) {
                    OutlinedTextField(
                        value = config.customCurrencySymbol,
                        onValueChange = { onConfigChange(config.copy(customCurrencySymbol = it.take(8))) },
                        label = { Text("رمز العملة المخصص (مثل: ر.ي أو YER)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("موضع رمز العملة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencySymbolPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = config.symbolPosition == pos,
                            onClick = { onConfigChange(config.copy(symbolPosition = pos)) },
                            label = { Text(pos.title, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Date & Calendar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Text("تنسيق التاريخ ونظام التقويم", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                }

                Text("صيغة التاريخ:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DateFormatPattern.entries.forEach { pat ->
                        FilterChip(
                            selected = config.datePattern == pat,
                            onClick = { onConfigChange(config.copy(datePattern = pat)) },
                            label = { Text(pat.title, fontSize = 11.sp) }
                        )
                    }
                }

                Text("نظام التقويم:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalendarSystem.entries.forEach { calSys ->
                        FilterChip(
                            selected = config.calendarSystem == calSys,
                            onClick = { onConfigChange(config.copy(calendarSystem = calSys)) },
                            label = { Text(calSys.title, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إظهار الوقت بجانب التاريخ (HH:mm)", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = config.showTime,
                        onCheckedChange = { onConfigChange(config.copy(showTime = it)) }
                    )
                }
            }
        }
    }
}
