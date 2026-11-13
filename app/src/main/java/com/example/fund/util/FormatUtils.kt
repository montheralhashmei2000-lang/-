package com.example.fund.util

import android.icu.util.IslamicCalendar
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.fund.model.CalendarSystem
import com.example.fund.model.DateFormatPattern
import com.example.fund.model.DecimalSeparatorOption
import com.example.fund.model.DigitLanguage
import com.example.fund.model.DisplayFormatConfig
import com.example.fund.model.ThousandsSeparatorOption
import com.example.fund.model.CurrencySymbolPosition
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

val LocalScreenFormat = staticCompositionLocalOf { DisplayFormatConfig() }
val LocalPrintFormat = staticCompositionLocalOf { DisplayFormatConfig() }

object FormatUtils {

    private val gregorianMonths = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private val hijriMonths = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun toArabicDigitsIfNeeded(input: String, digitLanguage: DigitLanguage): String {
        if (digitLanguage == DigitLanguage.LATIN) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(('٠'.code + (ch.code - '0'.code)).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toLatinDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Formats alphanumeric codes (such as voucherNo, entryNo, memberNo, accountCode)
     * according to the chosen DigitLanguage without allowing Android device locale
     * to force Arabic-Indic digits when LATIN is configured.
     */
    fun formatCodeOrNumber(rawText: String, config: DisplayFormatConfig): String {
        val latinNormalized = toLatinDigits(rawText)
        return when (config.digitLanguage) {
            DigitLanguage.LATIN -> latinNormalized
            DigitLanguage.ARABIC_INDIC -> toArabicDigitsIfNeeded(latinNormalized, DigitLanguage.ARABIC_INDIC)
        }
    }

    fun formatNumber(
        value: Double,
        config: DisplayFormatConfig,
        decimalsOverride: Int? = null
    ): String {
        val safeValue = if (value.isFinite()) value else 0.0
        val decimals = (decimalsOverride ?: config.decimalPlaces).coerceIn(0, 4)
        val isNegative = safeValue < 0.0
        val absBig = BigDecimal.valueOf(abs(safeValue)).setScale(decimals, RoundingMode.HALF_UP)
        val plain = absBig.toPlainString()
        val dotIdx = plain.indexOf('.')
        val rawInt = if (dotIdx >= 0) plain.substring(0, dotIdx) else plain
        val rawFrac = if (dotIdx >= 0 && decimals > 0) plain.substring(dotIdx + 1) else ""

        val sep = config.thousandsSeparator.charStr
        val groupedInt = if (sep.isEmpty() || rawInt.length <= 3) {
            rawInt
        } else {
            val sb = StringBuilder()
            val len = rawInt.length
            for (i in 0 until len) {
                if (i > 0 && (len - i) % 3 == 0) {
                    sb.append(sep)
                }
                sb.append(rawInt[i])
            }
            sb.toString()
        }

        val signPrefix = if (isNegative && absBig.compareTo(BigDecimal.ZERO) > 0) "-" else ""
        val combined = if (decimals > 0 && rawFrac.isNotEmpty()) {
            "$signPrefix$groupedInt${config.decimalSeparator.charStr}$rawFrac"
        } else {
            "$signPrefix$groupedInt"
        }

        return toArabicDigitsIfNeeded(combined, config.digitLanguage)
    }

    fun formatAmount(
        amount: Double,
        config: DisplayFormatConfig,
        includeSymbol: Boolean = true,
        overrideSymbol: String? = null,
        inTable: Boolean = false,
        decimalsOverride: Int? = null
    ): String {
        val formattedNum = formatNumber(amount, config, decimalsOverride)
        if (!includeSymbol || (inTable && config.symbolPosition == CurrencySymbolPosition.HIDDEN_IN_TABLES)) {
            return formattedNum
        }
        val sym = overrideSymbol ?: config.resolvedCurrencySymbol()
        return when (config.symbolPosition) {
            CurrencySymbolPosition.BEFORE -> "$sym $formattedNum"
            CurrencySymbolPosition.AFTER,
            CurrencySymbolPosition.HIDDEN_IN_TABLES -> "$formattedNum $sym"
        }
    }

    fun formatDate(timestampMillis: Long, config: DisplayFormatConfig): String {
        val date = Date(timestampMillis)
        return formatParsedDate(date, hasExplicitTime = true, fallbackText = "", config = config)
    }

    fun formatDate(rawDateOrTimestamp: String, config: DisplayFormatConfig): String {
        val trimmed = toLatinDigits(rawDateOrTimestamp).trim()
        if (trimmed.isEmpty()) return ""

        // Numeric epoch millis
        trimmed.toLongOrNull()?.let { millis ->
            if (millis > 100_000_000_000L) {
                return formatDate(millis, config)
            }
        }

        val candidates = listOf(
            "yyyy-MM-dd HH:mm:ss" to true,
            "yyyy-MM-dd HH:mm" to true,
            "dd/MM/yyyy HH:mm" to true,
            "yyyy-MM-dd" to false,
            "dd/MM/yyyy" to false,
            "yyyy-MM" to false
        )
        for ((pattern, hasTime) in candidates) {
            try {
                if (pattern == "yyyy-MM" && trimmed.length == 7) {
                    val sdf = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).apply { isLenient = false }
                    val parsed = sdf.parse(trimmed) ?: continue
                    val cal = Calendar.getInstance().apply { time = parsed }
                    val m = cal.get(Calendar.MONTH)
                    val y = cal.get(Calendar.YEAR)
                    val out = when (config.calendarSystem) {
                        CalendarSystem.HIJRI -> {
                            val hijri = runCatching {
                                IslamicCalendar().apply { time = parsed }
                            }.getOrNull()
                            if (hijri != null) {
                                "${hijriMonths[hijri.get(IslamicCalendar.MONTH).coerceIn(0, 11)]} ${hijri.get(IslamicCalendar.YEAR)} هـ"
                            } else {
                                "${gregorianMonths[m.coerceIn(0, 11)]} $y"
                            }
                        }
                        CalendarSystem.GREGORIAN -> "${gregorianMonths[m.coerceIn(0, 11)]} $y"
                    }
                    return toArabicDigitsIfNeeded(out, config.digitLanguage)
                } else if (trimmed.length >= pattern.length || (pattern.contains(" ") && trimmed.contains(" "))) {
                    val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }
                    val parsed = sdf.parse(trimmed) ?: continue
                    return formatParsedDate(parsed, hasExplicitTime = hasTime, fallbackText = trimmed, config = config)
                }
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return toArabicDigitsIfNeeded(trimmed, config.digitLanguage)
    }

    private fun formatParsedDate(
        date: Date,
        hasExplicitTime: Boolean,
        fallbackText: String,
        config: DisplayFormatConfig
    ): String {
        val gCal = Calendar.getInstance().apply { time = date }
        val hour = gCal.get(Calendar.HOUR_OF_DAY)
        val minute = gCal.get(Calendar.MINUTE)

        val datePart = when (config.calendarSystem) {
            CalendarSystem.GREGORIAN -> {
                val day = gCal.get(Calendar.DAY_OF_MONTH)
                val month = gCal.get(Calendar.MONTH) + 1
                val year = gCal.get(Calendar.YEAR)
                formatDateComponents(day, month, year, gregorianMonths, config.datePattern, suffix = "")
            }
            CalendarSystem.HIJRI -> {
                val hijri = runCatching {
                    IslamicCalendar().apply { time = date }
                }.getOrNull()
                if (hijri != null) {
                    val day = hijri.get(IslamicCalendar.DAY_OF_MONTH)
                    val month = hijri.get(IslamicCalendar.MONTH) + 1
                    val year = hijri.get(IslamicCalendar.YEAR)
                    formatDateComponents(day, month, year, hijriMonths, config.datePattern, suffix = " هـ")
                } else if (fallbackText.isNotBlank()) {
                    fallbackText
                } else {
                    val day = gCal.get(Calendar.DAY_OF_MONTH)
                    val month = gCal.get(Calendar.MONTH) + 1
                    val year = gCal.get(Calendar.YEAR)
                    formatDateComponents(day, month, year, gregorianMonths, config.datePattern, suffix = "")
                }
            }
        }

        val withTime = if (config.showTime) {
            val hh = if (hasExplicitTime) hour else 9
            val mm = if (hasExplicitTime) minute else 0
            String.format(Locale.ENGLISH, "%s %02d:%02d", datePart, hh, mm)
        } else {
            datePart
        }

        return toArabicDigitsIfNeeded(withTime, config.digitLanguage)
    }

    private fun formatDateComponents(
        day: Int,
        month1Based: Int,
        year: Int,
        monthNames: Array<String>,
        pattern: DateFormatPattern,
        suffix: String
    ): String {
        val dd = String.format(Locale.ENGLISH, "%02d", day)
        val mm = String.format(Locale.ENGLISH, "%02d", month1Based)
        val yyyy = String.format(Locale.ENGLISH, "%04d", year)
        return when (pattern) {
            DateFormatPattern.DMY_SLASH -> "$dd/$mm/$yyyy$suffix"
            DateFormatPattern.YMD_DASH -> "$yyyy-$mm-$dd$suffix"
            DateFormatPattern.DMY_MONTH_NAME -> {
                val monthName = monthNames[(month1Based - 1).coerceIn(0, 11)]
                "$dd $monthName $yyyy$suffix"
            }
        }
    }

    fun displayFormatToJson(config: DisplayFormatConfig): JSONObject = JSONObject().apply {
        put("digitLanguage", config.digitLanguage.name)
        put("thousandsSeparator", config.thousandsSeparator.name)
        put("decimalSeparator", config.decimalSeparator.name)
        put("decimalPlaces", config.decimalPlaces)
        put("maxAmountDigits", config.maxAmountDigits)
        put("currencyOption", config.currencyOption.name)
        put("customCurrencySymbol", config.customCurrencySymbol)
        put("symbolPosition", config.symbolPosition.name)
        put("datePattern", config.datePattern.name)
        put("showTime", config.showTime)
        put("calendarSystem", config.calendarSystem.name)
    }

    fun displayFormatFromJson(obj: JSONObject?, fallback: DisplayFormatConfig = DisplayFormatConfig()): DisplayFormatConfig {
        if (obj == null) return fallback
        return DisplayFormatConfig(
            digitLanguage = runCatching { DigitLanguage.valueOf(obj.optString("digitLanguage")) }.getOrDefault(fallback.digitLanguage),
            thousandsSeparator = runCatching { ThousandsSeparatorOption.valueOf(obj.optString("thousandsSeparator")) }.getOrDefault(fallback.thousandsSeparator),
            decimalSeparator = runCatching { DecimalSeparatorOption.valueOf(obj.optString("decimalSeparator")) }.getOrDefault(fallback.decimalSeparator),
            decimalPlaces = obj.optInt("decimalPlaces", fallback.decimalPlaces).coerceIn(0, 3),
            maxAmountDigits = obj.optInt("maxAmountDigits", fallback.maxAmountDigits).coerceIn(6, 18),
            currencyOption = runCatching { com.example.fund.model.DisplayCurrencyOption.valueOf(obj.optString("currencyOption")) }.getOrDefault(fallback.currencyOption),
            customCurrencySymbol = obj.optString("customCurrencySymbol", fallback.customCurrencySymbol),
            symbolPosition = runCatching { CurrencySymbolPosition.valueOf(obj.optString("symbolPosition")) }.getOrDefault(fallback.symbolPosition),
            datePattern = runCatching { DateFormatPattern.valueOf(obj.optString("datePattern")) }.getOrDefault(fallback.datePattern),
            showTime = obj.optBoolean("showTime", fallback.showTime),
            calendarSystem = runCatching { CalendarSystem.valueOf(obj.optString("calendarSystem")) }.getOrDefault(fallback.calendarSystem)
        )
    }
}
