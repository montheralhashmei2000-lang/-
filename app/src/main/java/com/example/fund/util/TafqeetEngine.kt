package com.example.fund.util

import kotlin.math.abs
import kotlin.math.roundToLong

object TafqeetEngine {

    private val ones = arrayOf(
        "", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة"
    )

    private val teens = arrayOf(
        "عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر",
        "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    )

    private val tens = arrayOf(
        "", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"
    )

    private val hundreds = arrayOf(
        "", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة"
    )

    fun convertToWords(amount: Long, currencyName: String = "ريال يمني"): String {
        if (amount == 0L) return "فقط صفر $currencyName لا غير"
        if (amount < 0L) {
            val positiveWords = if (amount == Long.MIN_VALUE) {
                convertPositiveIntegerToWords(Long.MAX_VALUE)
            } else {
                convertPositiveIntegerToWords(-amount)
            }
            return "فقط سالب $positiveWords $currencyName لا غير"
        }

        val words = convertPositiveIntegerToWords(amount)
        return "فقط $words $currencyName لا غير"
    }

    fun convertToWords(
        amount: Double,
        currencyName: String = "ريال يمني",
        subUnitName: String = "فلس"
    ): String {
        if (amount.isNaN() || amount.isInfinite()) return "فقط صفر $currencyName لا غير"
        val roundedTotalSubUnits = (abs(amount) * 100.0).roundToLong()
        if (roundedTotalSubUnits == 0L) return "فقط صفر $currencyName لا غير"

        val wholePart = roundedTotalSubUnits / 100L
        val fractionalPart = roundedTotalSubUnits % 100L
        val prefix = if (amount < 0.0) "فقط سالب " else "فقط "

        val mainPartStr = if (wholePart > 0L) {
            "${convertPositiveIntegerToWords(wholePart)} $currencyName"
        } else {
            ""
        }

        val subPartStr = if (fractionalPart > 0L) {
            "${convertPositiveIntegerToWords(fractionalPart)} $subUnitName"
        } else {
            ""
        }

        val combined = listOf(mainPartStr, subPartStr).filter { it.isNotBlank() }.joinToString(" و")
        return "$prefix$combined لا غير"
    }

    private fun convertPositiveIntegerToWords(amount: Long): String {
        val parts = mutableListOf<String>()

        val billions = amount / 1_000_000_000L
        val millions = (amount % 1_000_000_000L) / 1_000_000L
        val thousands = (amount % 1_000_000L) / 1_000L
        val remainder = amount % 1_000L

        if (billions > 0L) {
            parts.add(formatScaleGroup(billions, "مليار", "ملياران", "مليارات", "ملياراً"))
        }
        if (millions > 0L) {
            parts.add(formatScaleGroup(millions, "مليون", "مليونان", "ملايين", "مليوناً"))
        }
        if (thousands > 0L) {
            parts.add(formatScaleGroup(thousands, "ألف", "ألفان", "آلاف", "ألفاً"))
        }
        if (remainder > 0L) {
            parts.add(convertChunk(remainder.toInt()))
        }

        return if (parts.isEmpty()) "صفر" else parts.joinToString(" و")
    }

    private fun formatScaleGroup(
        value: Long,
        singular: String,
        dual: String,
        plural3To10: String,
        accusative11To99: String
    ): String {
        val mod100 = (value % 100L).toInt()
        return when {
            value == 1L -> singular
            value == 2L -> dual
            value in 3L..10L -> "${convertChunk(value.toInt())} $plural3To10"
            mod100 in 11..99 -> "${convertBigValue(value)} $accusative11To99"
            else -> "${convertBigValue(value)} $singular"
        }
    }

    private fun convertBigValue(value: Long): String {
        return if (value < 1000L) convertChunk(value.toInt()) else convertPositiveIntegerToWords(value)
    }

    private fun convertChunk(number: Int): String {
        val h = number / 100
        val remainder = number % 100
        val t = remainder / 10
        val o = remainder % 10

        val parts = mutableListOf<String>()

        if (h > 0) {
            parts.add(hundreds[h])
        }

        if (remainder in 10..19) {
            parts.add(teens[remainder - 10])
        } else {
            if (o > 0) {
                parts.add(ones[o])
            }
            if (t > 0) {
                parts.add(tens[t])
            }
        }

        return if (parts.isEmpty()) "" else parts.joinToString(" و")
    }
}
