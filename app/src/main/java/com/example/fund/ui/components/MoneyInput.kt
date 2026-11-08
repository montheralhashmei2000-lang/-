package com.example.fund.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fund.data.FundRepository
import com.example.fund.model.CurrencyCode
import com.example.ui.theme.*

@Composable
fun MoneyInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedCurrency: CurrencyCode,
    onCurrencyChange: (CurrencyCode) -> Unit,
    label: String = "المبلغ المالي",
    modifier: Modifier = Modifier
) {
    val repo = FundRepository.getInstance()
    val currencyRates by repo.currencyRates.collectAsState()
    val rate = currencyRates[selectedCurrency] ?: selectedCurrency.defaultRate
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val amountYer = parsedAmount * rate

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
            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                        onAmountChange(input)
                    }
                },
                placeholder = { Text("0") },
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
                CurrencyCode.values().forEach { curr ->
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

        // Live calculation equivalent preview
        if (selectedCurrency != CurrencyCode.YER && parsedAmount > 0) {
            Surface(
                color = FundLightSurfaceSecondary,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FundGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
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
                        fontSize = 14.sp
                    )
                    Text(
                        text = "(أصل المبلغ: $amount ${selectedCurrency.name} @ %,.0f)".format(rate),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
