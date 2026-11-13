package com.example.fund.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.model.CurrencyCode
import com.example.fund.util.TafqeetEngine
import com.example.ui.theme.*

/**
 * Formats integer digits with thousands separators (e.g. 1250000 -> 1,250,000)
 * while preserving exact cursor offset mapping for both integer and decimal inputs.
 */
class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val dotIndex = raw.indexOf('.')
        val intPart = if (dotIndex >= 0) raw.substring(0, dotIndex) else raw
        val fracPart = if (dotIndex >= 0) raw.substring(dotIndex) else ""

        val out = StringBuilder()
        val origToTrans = IntArray(raw.length + 1)
        val transToOrig = mutableListOf<Int>()

        val intLen = intPart.length
        for (i in 0 until intLen) {
            if (i > 0 && (intLen - i) % 3 == 0) {
                out.append(',')
                transToOrig.add(i)
            }
            origToTrans[i] = out.length
            out.append(intPart[i])
            transToOrig.add(i)
        }

        for (i in intLen until raw.length) {
            origToTrans[i] = out.length
            out.append(raw[i])
            transToOrig.add(i)
        }
        origToTrans[raw.length] = out.length
        transToOrig.add(raw.length)

        val transformedString = out.toString()
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, raw.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, transformedString.length)]
            }
        }
        return TransformedText(AnnotatedString(transformedString), mapping)
    }
}

@Composable
fun MoneyInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedCurrency: CurrencyCode,
    onCurrencyChange: (CurrencyCode) -> Unit,
    label: String = "المبلغ المالي",
    modifier: Modifier = Modifier,
    showTafqeetPreview: Boolean = true
) {
    val repo = FundRepository.getInstance()
    val currencyRates by repo.currencyRates.collectAsStateWithLifecycle()
    val rate = currencyRates[selectedCurrency] ?: selectedCurrency.defaultRate
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val amountYer = parsedAmount * rate

    val currencySubUnit = when (selectedCurrency) {
        CurrencyCode.YER -> "فلس"
        CurrencyCode.USD -> "سنت"
        CurrencyCode.SAR -> "هللة"
    }
    val tafqeetText = remember(parsedAmount, selectedCurrency) {
        if (parsedAmount > 0.0) {
            TafqeetEngine.convertToWords(
                amount = parsedAmount,
                currencyName = selectedCurrency.title,
                subUnitName = currencySubUnit
            )
        } else ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Amount Input with automatic thousands formatting and Tabular Numerals (tnum)
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    val normalized = input.replace(",", "")
                    if (normalized.isEmpty() || normalized.matches(Regex("""^\d*\.?\d*$"""))) {
                        onAmountChange(normalized)
                    }
                },
                placeholder = { Text("0") },
                visualTransformation = ThousandSeparatorTransformation(),
                textStyle = LocalTextStyle.current.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FundPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Currency Selector Buttons
            Row(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CurrencyCode.entries.forEach { curr ->
                    val isSelected = curr == selectedCurrency
                    Button(
                        onClick = { onCurrencyChange(curr) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) FundPrimary else Color.Transparent,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = curr.symbol,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Live Tafqeet preview directly under MoneyInput
        if (showTafqeetPreview && tafqeetText.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FundPrimary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spellcheck,
                        contentDescription = null,
                        tint = FundPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = tafqeetText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FundPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Live calculation equivalent preview for foreign currencies
        if (selectedCurrency != CurrencyCode.YER && parsedAmount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FundGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "≈ %,d ﷼".format(amountYer.toLong()),
                        fontWeight = FontWeight.Bold,
                        color = FundPrimary,
                        fontSize = 14.sp,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = "(أصل المبلغ: $amount ${selectedCurrency.name} @ %,.0f)".format(rate),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}
