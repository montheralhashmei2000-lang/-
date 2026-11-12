package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.fund.data.FundRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("الصندوق التنموي", appName)
  }

  @Test
  fun `regions villages logo settings and json backup restore round trip`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = FundRepository.getInstance()
    repo.initStorage(context)
    repo.resetToDefaultSeedData()

    assertTrue(repo.addRegion("منطقة الشمال"))
    val newRegion = repo.regions.value.first { it.name == "منطقة الشمال" }
    assertTrue(repo.addVillageToRegion(newRegion.id, "قرية الوادي"))
    assertTrue(repo.addVillageToRegion(newRegion.id, "قرية الجبل"))
    assertEquals(2, repo.regions.value.first { it.id == newRegion.id }.villages.size)

    repo.updateFundLogo("BASE64_SAMPLE_LOGO")
    assertEquals("BASE64_SAMPLE_LOGO", repo.fundInfo.value.customLogoBase64)

    val updatedSettings = repo.appSettings.value.copy(
      monthlySubscriptionFeeYer = 35000.0,
      voucherNoPrefix = "REC"
    )
    repo.updateAppSettings(updatedSettings)

    val updatedPrintSettings = repo.printSettings.value.copy(
      countryHeaderText = "إدارة التنمية المجتمعية",
      receiptVoucherTitle = "إيصال قبض مخصص",
      footerNote = "تذييل اختباري مخصص",
      fontFamily = com.example.fund.model.PrintFontOption.CAIRO,
      sig2Title = "أمين الصندوق العام",
      sig2DefaultName = "سالم عبدالله"
    )
    repo.updatePrintSettings(updatedPrintSettings)

    val exportedJson = repo.exportDatabaseJson()
    assertTrue(exportedJson.contains("منطقة الشمال"))
    assertTrue(exportedJson.contains("BASE64_SAMPLE_LOGO"))
    assertTrue(exportedJson.contains("إيصال قبض مخصص"))

    repo.resetToDefaultSeedData()
    assertEquals("", repo.fundInfo.value.customLogoBase64)
    assertEquals(com.example.fund.model.PrintFontOption.AMIRI, repo.printSettings.value.fontFamily)

    assertTrue(repo.importDatabaseFromJson(exportedJson))
    assertEquals("BASE64_SAMPLE_LOGO", repo.fundInfo.value.customLogoBase64)
    assertEquals(35000.0, repo.appSettings.value.monthlySubscriptionFeeYer, 0.001)
    assertEquals("REC", repo.appSettings.value.voucherNoPrefix)
    assertEquals("إيصال قبض مخصص", repo.printSettings.value.receiptVoucherTitle)
    assertEquals("تذييل اختباري مخصص", repo.printSettings.value.footerNote)
    assertEquals(com.example.fund.model.PrintFontOption.CAIRO, repo.printSettings.value.fontFamily)
    assertEquals("سالم عبدالله", repo.printSettings.value.sig2DefaultName)
    assertTrue(repo.regions.value.any { it.name == "منطقة الشمال" && it.villages.contains("قرية الجبل") })
    assertTrue(com.example.fund.util.generateQrSvg("TEST").contains("<svg"))
  }
}
