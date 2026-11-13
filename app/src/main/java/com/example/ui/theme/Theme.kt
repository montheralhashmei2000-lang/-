package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.fund.model.AppFontOption
import com.example.fund.model.DisplayFormatConfig
import com.example.fund.util.LocalPrintFormat
import com.example.fund.util.LocalScreenFormat

private val DarkColorScheme = darkColorScheme(
    primary = FundLightGreen,
    onPrimary = Color.Black,
    primaryContainer = FundDeepHeader,
    onPrimaryContainer = FundGoldLight,
    secondary = FundGold,
    onSecondary = Color.Black,
    secondaryContainer = FundDarkSurfaceSecondary,
    onSecondaryContainer = FundGoldLight,
    tertiary = FundGoldLight,
    background = FundDarkBg,
    onBackground = FundDarkTextPrimary,
    surface = FundDarkSurface,
    onSurface = FundDarkTextPrimary,
    surfaceVariant = FundDarkSurfaceSecondary,
    onSurfaceVariant = FundDarkTextSecondary,
    outline = FundDarkBorder,
    error = StatusDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = FundPrimary,
    onPrimary = Color.White,
    primaryContainer = FundPrimaryMedium,
    onPrimaryContainer = Color.White,
    secondary = FundGold,
    onSecondary = Color.Black,
    secondaryContainer = FundLightSurfaceSecondary,
    onSecondaryContainer = FundGoldDark,
    tertiary = FundGoldDark,
    background = FundLightBg,
    onBackground = FundLightTextPrimary,
    surface = FundLightSurface,
    onSurface = FundLightTextPrimary,
    surfaceVariant = FundLightSurfaceSecondary,
    onSurfaceVariant = FundLightTextSecondary,
    outline = FundLightBorder,
    error = StatusDanger,
    onError = Color.White
)

@Composable
fun FundAppTheme(
    darkTheme: Boolean = false,
    fontScale: Float = 1.0f,
    appFont: AppFontOption = AppFontOption.TAJAWAL,
    screenFormat: DisplayFormatConfig = DisplayFormatConfig(),
    printFormat: DisplayFormatConfig = DisplayFormatConfig(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val dynamicTypography = remember(appFont) { buildAppTypography(appFont) }
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontScale
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides scaledDensity,
        LocalScreenFormat provides screenFormat,
        LocalPrintFormat provides printFormat
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = dynamicTypography,
            content = content
        )
    }
}
