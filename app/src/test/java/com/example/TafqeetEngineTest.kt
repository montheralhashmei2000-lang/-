package com.example

import com.example.fund.util.TafqeetEngine
import org.junit.Assert.*
import org.junit.Test

class TafqeetEngineTest {

    @Test
    fun `zero and negative numbers format cleanly`() {
        assertEquals("فقط صفر ريال يمني لا غير", TafqeetEngine.convertToWords(0L))
        val neg = TafqeetEngine.convertToWords(-1250L)
        assertTrue(neg.startsWith("فقط سالب"))
        assertTrue(neg.contains("ألف"))
        assertTrue(neg.contains("مائتان وخمسون"))
        assertTrue(neg.endsWith("ريال يمني لا غير"))
    }

    @Test
    fun `singular dual plural and accusative scales for thousands and millions`() {
        assertEquals("فقط ألف ريال يمني لا غير", TafqeetEngine.convertToWords(1000L))
        assertEquals("فقط ألفان ريال يمني لا غير", TafqeetEngine.convertToWords(2000L))
        assertEquals("فقط خمسة آلاف ريال يمني لا غير", TafqeetEngine.convertToWords(5000L))

        val elevenK = TafqeetEngine.convertToWords(11_000L)
        assertTrue(elevenK.contains("أحد عشر ألفاً"))

        val oneMillion = TafqeetEngine.convertToWords(1_000_000L)
        assertEquals("فقط مليون ريال يمني لا غير", oneMillion)

        val twoMillion = TafqeetEngine.convertToWords(2_000_000L)
        assertEquals("فقط مليونان ريال يمني لا غير", twoMillion)

        val fourMillion = TafqeetEngine.convertToWords(4_000_000L)
        assertEquals("فقط أربعة ملايين ريال يمني لا غير", fourMillion)
    }

    @Test
    fun `billions and compound amounts`() {
        assertEquals("فقط مليار ريال يمني لا غير", TafqeetEngine.convertToWords(1_000_000_000L))
        assertEquals("فقط ملياران ريال يمني لا غير", TafqeetEngine.convertToWords(2_000_000_000L))

        val compound = TafqeetEngine.convertToWords(150_000L, "ريال يمني")
        assertTrue(compound.startsWith("فقط"))
        assertTrue(compound.contains("مائة وخمسون ألف"))
        assertTrue(compound.endsWith("لا غير"))
    }

    @Test
    fun `double amounts with fractional sub-units`() {
        val decimalWords = TafqeetEngine.convertToWords(250.75, "ريال سعودي", "هللة")
        assertTrue(decimalWords.contains("مائتان وخمسون ريال سعودي"))
        assertTrue(decimalWords.contains("خمسة وسبعون هللة"))
    }
}
