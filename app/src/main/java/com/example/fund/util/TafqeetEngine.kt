package com.example.fund.util

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
        if (amount < 0L) return "سالب " + convertToWords(-amount, currencyName)

        val parts = mutableListOf<String>()

        val billions = amount / 1_000_000_000L
        val millions = (amount % 1_000_000_000L) / 1_000_000L
        val thousands = (amount % 1_000_000L) / 1_000L
        val remainder = amount % 1_000L

        if (billions > 0) {
            parts.add(convertChunk(billions.toInt()) + " مليار")
        }
        if (millions > 0) {
            val chunkStr = when (millions) {
                1L -> "مليون"
                2L -> "مليونان"
                in 3L..10L -> convertChunk(millions.toInt()) + " ملايين"
                else -> convertChunk(millions.toInt()) + " مليون"
            }
            parts.add(chunkStr)
        }
        if (thousands > 0) {
            val chunkStr = when (thousands) {
                1L -> "ألف"
                2L -> "ألفان"
                in 3L..10L -> convertChunk(thousands.toInt()) + " آلاف"
                else -> convertChunk(thousands.toInt()) + " ألف"
            }
            parts.add(chunkStr)
        }
        if (remainder > 0) {
            parts.add(convertChunk(remainder.toInt()))
        }

        val joined = if (parts.isEmpty()) "" else parts.reduce { acc, s -> "$acc و$s" }
        return "فقط $joined $currencyName لا غير"
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

        return if (parts.isEmpty()) "" else parts.reduce { acc, s -> "$acc و$s" }
    }
}
