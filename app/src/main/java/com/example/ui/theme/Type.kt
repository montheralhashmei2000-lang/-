package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Normal),
    Font(R.font.tajawal, FontWeight.Medium),
    Font(R.font.tajawal, FontWeight.Bold),
    Font(R.font.tajawal, FontWeight.ExtraBold)
)

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.Bold),
    Font(R.font.cairo, FontWeight.ExtraBold)
)

val NotoNaskhFontFamily = FontFamily(
    Font(R.font.noto_naskh_arabic, FontWeight.Normal),
    Font(R.font.noto_naskh_arabic, FontWeight.Bold)
)

val IbmPlexSansArabicFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Bold)
)

fun printFontFamilyFor(option: com.example.fund.model.PrintFontOption): FontFamily = when (option) {
    com.example.fund.model.PrintFontOption.AMIRI -> AmiriFontFamily
    com.example.fund.model.PrintFontOption.TAJAWAL -> TajawalFontFamily
    com.example.fund.model.PrintFontOption.CAIRO -> CairoFontFamily
    com.example.fund.model.PrintFontOption.NASKH -> NotoNaskhFontFamily
}

fun appFontFamilyFor(option: com.example.fund.model.AppFontOption): FontFamily = when (option) {
    com.example.fund.model.AppFontOption.SYSTEM -> FontFamily.Default
    com.example.fund.model.AppFontOption.TAJAWAL -> TajawalFontFamily
    com.example.fund.model.AppFontOption.CAIRO -> CairoFontFamily
    com.example.fund.model.AppFontOption.IBM_PLEX_SANS_ARABIC -> IbmPlexSansArabicFontFamily
    com.example.fund.model.AppFontOption.AMIRI -> AmiriFontFamily
}

fun buildAppTypography(option: com.example.fund.model.AppFontOption): Typography {
    val bodyFamily = appFontFamilyFor(option)
    val displayFamily = when (option) {
        com.example.fund.model.AppFontOption.TAJAWAL -> AmiriFontFamily
        else -> bodyFamily
    }
    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontFeatureSettings = "tnum"
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 34.sp,
            fontFeatureSettings = "tnum"
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontFeatureSettings = "tnum"
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            fontFeatureSettings = "tnum"
        ),
        titleLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            fontFeatureSettings = "tnum"
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontFeatureSettings = "tnum"
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            fontFeatureSettings = "tnum"
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            fontFeatureSettings = "tnum"
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFeatureSettings = "tnum"
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontFeatureSettings = "tnum"
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontFeatureSettings = "tnum"
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontFeatureSettings = "tnum"
        )
    )
}

val Typography = buildAppTypography(com.example.fund.model.AppFontOption.TAJAWAL)
