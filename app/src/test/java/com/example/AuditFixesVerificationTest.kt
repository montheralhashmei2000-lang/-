package com.example

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import com.example.fund.util.OfficialPrintHelper
import com.example.fund.util.generateQrBitmap
import com.example.fund.util.generateQrMatrix
import com.example.fund.util.generateQrSvg
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuditFixesVerificationTest {

    @Test
    fun `standard iso qr code generator round trips through zxing reader`() {
        val payload = "MEMBER|M-1001|01020304|نشط"
        val matrix = generateQrMatrix(payload)
        assertTrue(matrix.size >= 21)
        assertEquals(matrix.size, matrix[0].size)

        val bmp = generateQrBitmap(payload, sizePx = 256, darkColorInt = Color.BLACK, lightColorInt = Color.WHITE)
        val width = bmp.width
        val height = bmp.height
        val pixels = IntArray(width * height)
        bmp.getPixels(pixels, 0, width, 0, 0, width, height)

        val binaryBitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
        val decoded = QRCodeReader().decode(binaryBitmap)
        assertEquals(payload, decoded.text)
        assertTrue(generateQrSvg(payload).contains("<svg"))
    }

    @Test
    fun `budget actualSpent syncs with expense vouchers manual entries and void rollback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        val targetBudget = repo.budgets.value.firstOrNull { it.accountCode == "5100" }
            ?: run {
                repo.addBudget(
                    category = "اختبار المساعدات النقدية",
                    accountCode = "5100",
                    budgetedAmount = 500_000.0,
                    month = "2026"
                )
                repo.budgets.value.first { it.accountCode == "5100" }
            }
        val initialSpent = targetBudget.actualSpent

        val voucher = repo.issueVoucher(
            type = VoucherType.PAYMENT,
            partyName = "مستفيد اختبار الموازنة",
            amountOriginal = 15_000.0,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5100",
            counterpartAccountCode = "1100",
            description = "صرف على بند 5100"
        )
        val afterVoucherSpent = repo.budgets.value.first { it.id == targetBudget.id }.actualSpent
        assertEquals(initialSpent + 15_000.0, afterVoucherSpent, 0.001)

        val manualPosted = repo.createManualJournalEntry(
            lines = listOf(
                JournalLine("5100", "مصروف مساعدات", 5_000.0, 0.0, "قيد يدوي"),
                JournalLine("1100", "الخزينة النقدية", 0.0, 5_000.0, "قيد يدوي")
            ),
            description = "قيد يدوي يمس الموازنة"
        )
        assertTrue(manualPosted)
        assertEquals(initialSpent + 20_000.0, repo.budgets.value.first { it.id == targetBudget.id }.actualSpent, 0.001)

        repo.voidVoucher(voucher.id)
        assertEquals(initialSpent + 5_000.0, repo.budgets.value.first { it.id == targetBudget.id }.actualSpent, 0.001)
    }

    @Test
    fun `unbalanced manual journal entry is always rejected regardless of settings flag`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // Even if someone supplies enforceBalancedJournal = false programmatically
        repo.updateAppSettings(repo.appSettings.value.copy(enforceBalancedJournal = false))
        val posted = repo.createManualJournalEntry(
            lines = listOf(
                JournalLine("1100", "الخزينة", 10_000.0, 0.0),
                JournalLine("4100", "الإيرادات", 0.0, 4_000.0)
            ),
            description = "قيد غير متوازن يجب رفضه دائماً"
        )
        assertFalse(posted)
    }

    @Test
    fun `voidVoucher rolls back member subscription aid donor and campaign side effects`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // 1. Subscription -> voidVoucher rollback + StateFlow accuracy
        val member = repo.members.value.first()
        val paidBefore = member.totalPaidYer
        val dueBefore = member.dueBalanceYer
        val sub = repo.addSubscription(
            memberId = member.id,
            amountOriginal = 10_000.0,
            currency = CurrencyCode.YER,
            monthYear = "2026-12",
            paymentMethod = PaymentMethod.CASH,
            referenceNo = "",
            notes = "اختبار استرداد"
        )
        val memberAfterAdd = repo.members.value.first { it.id == member.id }
        assertEquals(paidBefore + 10_000.0, memberAfterAdd.totalPaidYer, 0.001)
        val subVoucher = repo.vouchers.value.first { it.description.contains(sub.receiptNo) }

        repo.voidVoucher(subVoucher.id)
        val memberAfterVoid = repo.members.value.first { it.id == member.id }
        assertEquals(paidBefore, memberAfterVoid.totalPaidYer, 0.001)
        assertEquals(dueBefore, memberAfterVoid.dueBalanceYer, 0.001)
        assertFalse(repo.subscriptions.value.any { it.id == sub.id })

        // 1b. Subscription rollback on indebted member restores exact dueBalanceYer reduction
        val indebtedMember = Member(
            memberNo = "M-9902",
            fullName = "عضو عليه مستحقات",
            phone = "777111222",
            nationalId = "99029902",
            joinDate = "2026-01-01",
            status = "نشط",
            totalPaidYer = 0.0,
            dueBalanceYer = 25_000.0
        )
        repo.addMember(indebtedMember)
        val subDebt = repo.addSubscription(
            memberId = indebtedMember.id,
            amountOriginal = 10_000.0,
            currency = CurrencyCode.YER,
            monthYear = "2026-12",
            paymentMethod = PaymentMethod.CASH,
            referenceNo = "",
            notes = "سداد جزئي"
        )
        assertEquals(15_000.0, repo.members.value.first { it.id == indebtedMember.id }.dueBalanceYer, 0.001)
        val subDebtVoucher = repo.vouchers.value.first { it.description.contains(subDebt.receiptNo) }
        repo.voidVoucher(subDebtVoucher.id)
        assertEquals(25_000.0, repo.members.value.first { it.id == indebtedMember.id }.dueBalanceYer, 0.001)

        // 2. AidRequest -> voidVoucher restores APPROVED and clears voucherId
        val req = repo.addAidRequest(member.id, AidType.MEDICAL, 20_000.0, "مساعدة طبية")
        repo.updateAidStatus(req.id, AidStatus.APPROVED, 20_000.0)
        repo.updateAidStatus(req.id, AidStatus.DISBURSED, 20_000.0)
        val disbursedReq = repo.aidRequests.value.first { it.id == req.id }
        assertEquals(AidStatus.DISBURSED, disbursedReq.status)
        assertNotNull(disbursedReq.voucherId)

        repo.voidVoucher(disbursedReq.voucherId!!)
        val rolledBackReq = repo.aidRequests.value.first { it.id == req.id }
        assertEquals(AidStatus.APPROVED, rolledBackReq.status)
        assertNull(rolledBackReq.voucherId)

        // 3. Donor pledge payment -> voidVoucher rolls back donor totals
        val donor = repo.donors.value.first { it.pledgeAmountYer > it.pledgeFulfilledYer }
        val donatedBefore = donor.totalDonatedYer
        val fulfilledBefore = donor.pledgeFulfilledYer
        repo.recordDonorPledgePayment(donor.id, 12_000.0, CurrencyCode.YER, PaymentMethod.CASH)
        val donorVoucher = repo.vouchers.value.first {
            it.partyName == donor.name && it.amountYer == 12_000.0 && it.status == VoucherStatus.POSTED
        }
        repo.voidVoucher(donorVoucher.id)
        val donorAfterVoid = repo.donors.value.first { it.id == donor.id }
        assertEquals(donatedBefore, donorAfterVoid.totalDonatedYer, 0.001)
        assertEquals(fulfilledBefore, donorAfterVoid.pledgeFulfilledYer, 0.001)

        // 4. Campaign donation -> voidVoucher rolls back campaign collected amount
        val campaign = repo.campaigns.value.first()
        val collectedBefore = campaign.collectedAmountYer
        repo.donateToCampaign(campaign.id, "متبرع حملة اختبار", 25_000.0, CurrencyCode.YER, PaymentMethod.CASH)
        val campVoucher = repo.vouchers.value.first {
            it.description.contains(campaign.title) && it.amountYer == 25_000.0 && it.status == VoucherStatus.POSTED
        }
        repo.voidVoucher(campVoucher.id)
        val campAfterVoid = repo.campaigns.value.first { it.id == campaign.id }
        assertEquals(collectedBefore, campAfterVoid.collectedAmountYer, 0.001)
    }

    @Test
    fun `autoSuspendOverdueMembers periodic duplicate guard and allowAnonymousDonors enforcement`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // 1. Auto-suspend overdue members
        repo.updateAppSettings(
            repo.appSettings.value.copy(
                monthlySubscriptionFeeYer = 20_000.0,
                autoSuspendMonths = 3,
                allowAnonymousDonors = false
            )
        )
        val overdueMember = Member(
            memberNo = "M-9901",
            fullName = "عضو متأخر للاختبار",
            phone = "777000111",
            nationalId = "99019901",
            joinDate = "2026-01-01",
            status = "نشط",
            dueBalanceYer = 65_000.0
        )
        repo.addMember(overdueMember)
        val suspendedCount = repo.autoSuspendOverdueMembers()
        assertTrue(suspendedCount >= 1)
        assertEquals("معلق", repo.members.value.first { it.id == overdueMember.id }.status)

        // 2. Periodic aid duplicate disbursement blocked for same month
        val aid = repo.periodicAids.value.first { it.status == "نشطة" }
        val targetMonth = "2026-12"
        repo.disburseSinglePeriodicAid(aid.id, targetMonth)
        assertEquals(targetMonth, repo.periodicAids.value.first { it.id == aid.id }.lastDisbursedMonth)
        try {
            repo.disburseSinglePeriodicAid(aid.id, targetMonth)
            fail("Duplicate periodic aid disbursement for same month must fail")
        } catch (_: IllegalStateException) {
            // Expected
        }

        // 3. allowAnonymousDonors = false rejects blank donor name
        val campaign = repo.campaigns.value.first()
        try {
            repo.donateToCampaign(campaign.id, "   ", 10_000.0, CurrencyCode.YER, PaymentMethod.CASH)
            fail("Blank donor name must be rejected when allowAnonymousDonors = false")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `syncHistory starts empty and pdf and shared exports generate non-empty files`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        assertTrue(repo.syncRepository.syncHistory.value.isEmpty())

        val sampleVoucher = repo.vouchers.value.first()
        val a4Pdf = OfficialPrintHelper.createVoucherPdfFile(
            context = context,
            voucher = sampleVoucher,
            fundInfo = repo.fundInfo.value,
            printSettings = repo.printSettings.value.copy(paperSize = PrintPaperSize.A4)
        )
        assertTrue(a4Pdf.exists() && a4Pdf.length() > 200L)
        assertTrue(a4Pdf.readText().startsWith("%PDF-"))

        val thermalPdf = OfficialPrintHelper.createVoucherPdfFile(
            context = context,
            voucher = sampleVoucher,
            fundInfo = repo.fundInfo.value,
            printSettings = repo.printSettings.value.copy(paperSize = PrintPaperSize.THERMAL_80)
        )
        assertTrue(thermalPdf.exists() && thermalPdf.length() > 200L)
        assertTrue(thermalPdf.readText().startsWith("%PDF-"))

        val sharedUri = OfficialPrintHelper.createSharedExportFile(context, "test_export.csv", "\uFEFFcol1,col2\n1,2\n")
        assertTrue(sharedUri.toString().startsWith("content://"))
    }

    @Test
    fun `built in local vault syncs encrypts persists restores and guards server profiles`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // Default server is the working built-in Local Vault
        assertTrue(repo.syncRepository.isBuiltInVaultHost())
        assertEquals("127.0.0.1:9000", repo.serverConfig.value.serverHost)
        assertTrue(repo.syncQueue.value.isEmpty())

        // Probe health of built-in vault succeeds immediately with measured latency
        val probe = repo.syncRepository.probeServerConnection(saveAsActiveNode = true)
        assertTrue(probe.reachable)
        assertTrue(probe.latencyMs >= 1)
        assertTrue(repo.isOnline.value)

        // Add offline operations + custom member, then sync to Local Vault
        val extraMember = Member(
            memberNo = "M-VAULT-1",
            fullName = "عضو مزامنة الخزنة",
            phone = "777555444",
            nationalId = "88776655",
            joinDate = "2026-01-01",
            status = "نشط"
        )
        repo.addMember(extraMember)
        repo.simulateOfflineFieldOperations()
        assertFalse(repo.isOnline.value)
        assertTrue(repo.syncQueue.value.size >= 3)

        val synced = repo.syncRepository.connectAndSyncSmartSuspending()
        assertTrue(synced)
        assertTrue(repo.isOnline.value)
        assertEquals(0, repo.syncQueue.value.size)
        assertEquals(1, repo.syncHistory.value.size)
        assertTrue(repo.localVaultBatchCount.value >= 1)
        assertTrue(repo.localVaultSizeBytes.value > 0L)

        // Reset data to default (extraMember gone), then restore from encrypted Local Vault
        repo.resetToDefaultSeedData()
        assertNull(repo.members.value.find { it.memberNo == "M-VAULT-1" })

        val (restoredOk, restoreMsg) = repo.syncRepository.restoreLatestFromLocalVault()
        assertTrue("Expected vault restore to succeed: $restoreMsg", restoredOk)
        assertNotNull(repo.members.value.find { it.memberNo == "M-VAULT-1" })

        // Custom server profile + Bearer token saved & deleted cleanly; built-in cannot be deleted
        repo.updateServerNode("خادم فرع مخصص", "branch.social-fund.org:8443", authToken = "tok-secret-99")
        assertTrue(repo.syncRepository.hasServerAuthToken("branch.social-fund.org:8443"))
        val customProfile = repo.serverProfiles.value.first { it.host == "branch.social-fund.org:8443" }
        assertTrue(customProfile.hasAuthToken)
        assertTrue(repo.syncRepository.deleteServerProfile(customProfile.id))
        assertFalse(repo.syncRepository.hasServerAuthToken("branch.social-fund.org:8443"))

        val builtInProfile = repo.serverProfiles.value.first { it.isBuiltInVault }
        assertFalse(repo.syncRepository.deleteServerProfile(builtInProfile.id))

        // Invalid host format rejected
        try {
            repo.syncRepository.buildBaseHttpsUrl("invalid host with spaces")
            fail("Malformed host must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        // RBAC: VIEWER cannot update server node
        assertTrue(repo.switchAccountWithPassword("viewer", com.example.fund.data.SeedData.defaultDemoPassword) is com.example.fund.data.repository.AuthResult.Success)
        try {
            repo.updateServerNode("عقدة غير مصرح بها", "evil.example.com:8443")
            fail("VIEWER must not modify sync server config")
        } catch (_: SecurityException) {
            // Expected
        }
    }

    @Test
    fun `decimal tafqeet aid ceiling campaign overdraft member identity and safe region delete guards work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // 1. Decimal Tafqeet preserved in voucher amountWords
        val decimalVoucher = repo.issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = "متبرع اختباري",
            amountOriginal = 1250.75,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "1100",
            counterpartAccountCode = "4200",
            description = "سند بكسر عشري"
        )
        assertTrue(decimalVoucher.amountWords.contains("خمسة وسبعون فلس"))
        assertEquals(1250.75, decimalVoucher.amountYer, 0.001)

        // 2. Aid ceiling enforced in addAidRequest
        val ceiling = repo.fundInfo.value.maxAidCeilingYer
        val memberId = repo.members.value.first().id
        try {
            repo.addAidRequest(memberId, AidType.MEDICAL, ceiling + 10_000.0, "طلب يتجاوز السقف")
            fail("Aid request exceeding maxAidCeilingYer must fail")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        // 3. Campaign overdraft rejected cleanly
        val campaign = repo.campaigns.value.first()
        val remaining = campaign.collectedAmountYer - campaign.spentAmountYer
        try {
            repo.disburseFromCampaign(campaign.id, "مستفيد", remaining + 50_000.0, "صرف يتجاوز المتاح")
            fail("Campaign overdraft must fail")
        } catch (_: IllegalStateException) {
            // Expected
        }

        // 4. Member uniqueness & required nationalId enforced
        val existing = repo.members.value.first()
        try {
            repo.addMember(
                Member(
                    memberNo = existing.memberNo,
                    fullName = "عضو مكرر الرقم",
                    phone = "777888999",
                    nationalId = "unique-id-111",
                    joinDate = "2026-01-01"
                )
            )
            fail("Duplicate memberNo must fail")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        try {
            repo.addMember(
                Member(
                    memberNo = "M-UNIQUE-1",
                    fullName = "عضو بدون هوية",
                    phone = "777888998",
                    nationalId = "   ",
                    joinDate = "2026-01-01"
                )
            )
            fail("Blank nationalId must fail when requireNationalId = true")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        try {
            repo.addMember(
                Member(
                    memberNo = "M-UNIQUE-2",
                    fullName = "عضو مكرر الهوية",
                    phone = "777888997",
                    nationalId = existing.nationalId,
                    joinDate = "2026-01-01"
                )
            )
            fail("Duplicate nationalId must fail")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        // 5. Region with linked members cannot be deleted, unused region can be deleted
        val usedRegion = repo.regions.value.first { r -> repo.members.value.any { it.region == r.name } }
        try {
            repo.deleteRegion(usedRegion.id)
            fail("Deleting region with linked members must fail")
        } catch (_: IllegalStateException) {
            // Expected
        }

        assertTrue(repo.addRegion("منطقة اختبارية فارغة"))
        val emptyRegion = repo.regions.value.first { it.name == "منطقة اختبارية فارغة" }
        repo.deleteRegion(emptyRegion.id)
        assertFalse(repo.regions.value.any { it.id == emptyRegion.id })
    }
}
