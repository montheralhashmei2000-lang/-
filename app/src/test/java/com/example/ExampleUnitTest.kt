package com.example

import com.example.fund.data.FundRepository
import com.example.fund.model.AccountType
import com.example.fund.model.CurrencyCode
import com.example.fund.model.PaymentMethod
import com.example.fund.model.VoucherType
import com.example.fund.util.TafqeetEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTafqeetArabicEngine() {
        val result = TafqeetEngine.convertToWords(150000L, "ريال يمني")
        assertTrue(result.contains("مائة وخمسون ألف"))
        assertTrue(result.contains("ريال يمني"))
        assertTrue(result.startsWith("فقط"))
        assertTrue(result.endsWith("لا غير"))
    }

    @Test
    fun testDoubleEntryAccountingEngine() {
        val repo = FundRepository.getInstance()
        val initialVouchersCount = repo.vouchers.value.size

        val voucher = repo.issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = "عضو تجريبي",
            amountOriginal = 50000.0,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "1100",
            counterpartAccountCode = "4100",
            description = "تحصيل اشتراك تجريبي لاختبار القيد المزدوج"
        )

        assertEquals(initialVouchersCount + 1, repo.vouchers.value.size)
        assertNotNull(voucher.linkedJournalEntryId)

        // Find linked journal entry and check debit equals credit
        val entry = repo.journalEntries.value.find { it.entryNo == voucher.linkedJournalEntryId }
        assertNotNull(entry)
        val debitSum = entry!!.lines.sumOf { it.debit }
        val creditSum = entry.lines.sumOf { it.credit }
        assertEquals(debitSum, creditSum, 0.001)
    }

    @Test
    fun testMultiCurrencyEngine() {
        val repo = FundRepository.getInstance()
        val convertedYer = repo.convertToYer(100.0, CurrencyCode.USD)
        assertEquals(253000.0, convertedYer, 0.001)

        val convertedSar = repo.convertToYer(100.0, CurrencyCode.SAR)
        assertEquals(66500.0, convertedSar, 0.001)
    }

    @Test
    fun testCampaignAndDonorInteractiveFlows() {
        val repo = FundRepository.getInstance()
        val firstCampaign = repo.campaigns.value.first()
        val beforeCollected = firstCampaign.collectedAmountYer

        repo.donateToCampaign(
            campaignId = firstCampaign.id,
            donorName = "متبرع اختباري",
            amountOriginal = 100.0,
            currency = CurrencyCode.SAR,
            paymentMethod = PaymentMethod.BANK_TRANSFER
        )
        val updatedCampaign = repo.campaigns.value.first { it.id == firstCampaign.id }
        assertEquals(beforeCollected + 66500.0, updatedCampaign.collectedAmountYer, 0.001)

        val addedAccount = repo.addSubAccount("1199", "صندوق فرعي تجريبي", AccountType.ASSET, "1000")
        assertTrue(addedAccount)
    }

    @Test
    fun testLocalServerAndAutoSyncOnNetworkConnect() {
        val repo = FundRepository.getInstance()
        repo.setAutoSyncOnConnect(true)

        // Simulate 3 offline field operations
        repo.simulateOfflineFieldOperations()
        assertFalse(repo.isOnline.value)
        assertTrue(repo.syncQueue.value.size >= 3)

        val historyCountBefore = repo.syncHistory.value.size

        // Reconnect to network -> should auto-sync all queued operations and record a SyncBatchRecord
        repo.toggleOnline()
        assertTrue(repo.isOnline.value)
        assertEquals(0, repo.syncQueue.value.size)
        assertEquals(historyCountBefore + 1, repo.syncHistory.value.size)

        // Test internal treasury transfer
        val cashBefore = repo.accounts.value.first { it.code == "1100" }.balance
        val bankBefore = repo.accounts.value.first { it.code == "1200" }.balance
        val transferOk = repo.transferBetweenTreasuryAccounts("1100", "1200", 100000.0, "تحويل اختباري")
        assertTrue(transferOk)
        assertEquals(cashBefore - 100000.0, repo.accounts.value.first { it.code == "1100" }.balance, 0.001)
        assertEquals(bankBefore + 100000.0, repo.accounts.value.first { it.code == "1200" }.balance, 0.001)
    }

    @Test
    fun testRegionsVillagesSeparationAndSettings() {
        val repo = FundRepository.getInstance()
        val initialRegions = repo.regions.value.size
        assertTrue(repo.addRegion("منطقة الشمال"))
        assertEquals(initialRegions + 1, repo.regions.value.size)

        val newRegion = repo.regions.value.first { it.name == "منطقة الشمال" }
        assertTrue(repo.addVillageToRegion(newRegion.id, "قرية الوادي"))
        assertTrue(repo.addVillageToRegion(newRegion.id, "قرية الجبل"))
        assertEquals(2, repo.regions.value.first { it.id == newRegion.id }.villages.size)

        repo.updateFundLogo("BASE64_SAMPLE_LOGO")
        assertEquals("BASE64_SAMPLE_LOGO", repo.fundInfo.value.customLogoBase64)

        val updatedSettings = repo.appSettings.value.copy(monthlySubscriptionFeeYer = 35000.0, voucherNoPrefix = "REC")
        repo.updateAppSettings(updatedSettings)
        assertEquals(35000.0, repo.appSettings.value.monthlySubscriptionFeeYer, 0.001)
        assertEquals("REC", repo.appSettings.value.voucherNoPrefix)

        repo.resetToDefaultSeedData()
        assertEquals("", repo.fundInfo.value.customLogoBase64)
    }
}
