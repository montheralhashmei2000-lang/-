package com.example

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.example.fund.data.FundRepository
import com.example.fund.data.SeedData
import com.example.fund.data.local.FundBackupCodec
import com.example.fund.data.local.FundDatabase
import com.example.fund.data.local.LegacyMigrationHelper
import com.example.fund.data.repository.AuthResult
import com.example.fund.data.repository.SyncApiService
import com.example.fund.data.repository.SyncBatchAck
import com.example.fund.data.repository.SyncBatchPayload
import com.example.fund.data.repository.SyncHealthResponse
import com.example.fund.data.repository.UserRepository
import com.example.fund.model.*
import com.example.fund.security.EncryptedSessionManager
import com.example.fund.security.SecurityUtils
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityAccountingHardeningTest {

    @Test
    fun `stage1 default logged out state lockout password change and pin user binding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // Default state must never be logged in
        assertFalse(repo.isLoggedIn.value)
        assertFalse(repo.userRepository.hasCompletedInitialLogin.value)

        // Fresh EncryptedSessionManager defaults isLoggedIn to false
        val session = EncryptedSessionManager(context)
        session.clearSession()
        assertFalse(session.isLoggedIn)

        // PBKDF2 hash uses 120,000 iterations & version prefix
        val hash = SecurityUtils.hashPassword("StrongPass#2026")
        assertTrue(hash.startsWith("pbkdf2_sha256_v2$120000:"))
        assertTrue(SecurityUtils.verifyPassword("StrongPass#2026", hash))

        // Lockout after 5 failed attempts
        repeat(UserRepository.MAX_FAILED_ATTEMPTS - 1) {
            val res = repo.authenticateWithPassword("admin", "bad-pw-$it")
            assertTrue(res is AuthResult.Error)
        }
        val locked = repo.authenticateWithPassword("admin", "bad-pw-final")
        assertTrue(locked is AuthResult.Error)
        assertTrue((locked as AuthResult.Error).message.contains("قفل"))
        val blockedEvenWithValid = repo.authenticateWithPassword("admin", SeedData.defaultDemoPassword)
        assertTrue(blockedEvenWithValid is AuthResult.Error)

        // Reset lockout cleanly and authenticate with default password -> mustRequirePasswordChange
        repo.resetToDefaultSeedData()
        val loginRes = repo.authenticateWithPassword("admin", SeedData.defaultDemoPassword)
        assertTrue(loginRes is AuthResult.Success)
        assertTrue(repo.isLoggedIn.value)
        assertTrue(repo.requiresPasswordChange.value)

        // Reject weak or default password on rotation
        assertTrue(repo.changePassword(SeedData.defaultDemoPassword, SeedData.defaultDemoPassword) is AuthResult.Error)
        assertTrue(repo.changePassword(SeedData.defaultDemoPassword, "123") is AuthResult.Error)

        val rotated = repo.changePassword(SeedData.defaultDemoPassword, "Admin@Strong99")
        assertTrue(rotated is AuthResult.Success)
        assertFalse(repo.requiresPasswordChange.value)

        // Bind PIN to current user (admin) and verify username swap is rejected
        assertTrue(repo.userRepository.setQuickPin("5921"))
        val pinValid = repo.userRepository.authenticateWithQuickPin("admin", "5921")
        assertTrue(pinValid is AuthResult.Success)

        val pinSwapAttempt = repo.userRepository.authenticateWithQuickPin("viewer", "5921")
        assertTrue(pinSwapAttempt is AuthResult.Error)

        // Biometric requires verified CryptoObject and matching bound userId
        assertTrue(repo.userRepository.authenticateWithBiometrics("u1", cryptoVerified = false) is AuthResult.Error)
        assertTrue(repo.userRepository.authenticateWithBiometrics("u4", cryptoVerified = true) is AuthResult.Error)
        assertTrue(repo.userRepository.authenticateWithBiometrics("u1", cryptoVerified = true) is AuthResult.Success)
    }

    @Test
    fun `stage1 strict rbac blocks viewer mutations and password required to switch account`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // Invalid password switch must fail and preserve current user
        val failedSwitch = repo.switchAccountWithPassword("viewer", "wrong-password")
        assertTrue(failedSwitch is AuthResult.Error)
        assertEquals(UserRole.ADMIN, repo.currentUser.value.role)

        // Valid password switches to VIEWER
        val viewerSwitch = repo.switchAccountWithPassword("viewer", SeedData.defaultDemoPassword)
        assertTrue(viewerSwitch is AuthResult.Success)
        assertEquals(UserRole.VIEWER, repo.currentUser.value.role)

        // VIEWER cannot issue vouchers or update app settings
        try {
            repo.issueVoucher(
                VoucherType.RECEIPT, "اختبار", 1000.0, CurrencyCode.YER,
                PaymentMethod.CASH, "1100", "4100", "محاولة غير مصرح بها"
            )
            fail("VIEWER must not be allowed to issue vouchers")
        } catch (_: SecurityException) {
            // Expected
        }

        try {
            repo.updateAppSettings(repo.appSettings.value.copy(strictRolePermissions = false))
            fail("VIEWER must not be allowed to weaken RBAC settings")
        } catch (_: SecurityException) {
            // Expected
        }

        // Switch to ACCOUNTANT -> allowed to submit aid request, NOT allowed to approve
        assertTrue(repo.switchAccountWithPassword("accountant", SeedData.defaultDemoPassword) is AuthResult.Success)
        val memberId = repo.members.value.first().id
        val aid = repo.addAidRequest(memberId, AidType.MEDICAL, 50000.0, "فحص صلاحية الاعتماد")
        try {
            repo.updateAidStatus(aid.id, AidStatus.APPROVED, 50000.0)
            fail("ACCOUNTANT must not approve aid requests")
        } catch (_: SecurityException) {
            // Expected
        }
    }

    @Test
    fun `stage2 accounting integrity guards finite amounts negative treasury stock and aid lifecycle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // 1. Reject NaN / Infinity
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -100.0)) {
            try {
                repo.issueVoucher(
                    VoucherType.RECEIPT, "مورد", bad, CurrencyCode.YER,
                    PaymentMethod.CASH, "1100", "4100", "قيمة غير صالحة"
                )
                fail("Should reject amount $bad")
            } catch (_: IllegalArgumentException) {
                // Expected
            }
        }

        // 2. Reject header / missing accounts
        try {
            repo.issueVoucher(
                VoucherType.RECEIPT, "مورد", 1000.0, CurrencyCode.YER,
                PaymentMethod.CASH, "1000", "4100", "حساب أب غير مسموح"
            )
            fail("Should reject parent/header account")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        // 3. Reject negative treasury balance when allowNegativeTreasury = false
        val cashBalance = repo.accounts.value.first { it.code == "1100" }.balance
        try {
            repo.issueVoucher(
                VoucherType.PAYMENT, "مستفيد", cashBalance + 50000.0, CurrencyCode.YER,
                PaymentMethod.CASH, "5100", "1100", "سحب يتجاوز رصيد الصندوق"
            )
            fail("Should reject negative treasury balance")
        } catch (_: IllegalStateException) {
            // Expected
        }

        // 4. Reject stock overdraft without clamping to zero
        val firstItem = repo.inKindItems.value.first()
        val originalQty = firstItem.quantityOnHand
        try {
            repo.addInKindMovement(
                InKindMovement(
                    movementNo = "MOV-ERR-1",
                    type = "صرف وتوزيع",
                    itemId = firstItem.id,
                    itemName = firstItem.name,
                    quantity = originalQty + 10,
                    partyName = "مستفيد",
                    date = "2026-11-13",
                    notes = "كمية أكبر من المتاح"
                )
            )
            fail("Should reject stock overdraw")
        } catch (_: IllegalStateException) {
            // Expected
        }
        assertEquals(originalQty, repo.inKindItems.value.first { it.id == firstItem.id }.quantityOnHand)

        // 5. Aid status: requireReviewerApproval, approved <= requested, and duplicate disburse guard
        val memberId = repo.members.value.first().id
        val aidReq = repo.addAidRequest(memberId, AidType.MEDICAL, 40000.0, "حالة طبية")
        try {
            // Cannot disburse directly from IN_REVIEW when requireReviewerApproval = true
            repo.updateAidStatus(aidReq.id, AidStatus.DISBURSED, 40000.0)
            fail("Should block direct disbursement without approval")
        } catch (_: IllegalStateException) {
            // Expected
        }

        try {
            // Cannot approve more than requestedAmountYer
            repo.updateAidStatus(aidReq.id, AidStatus.APPROVED, 99999.0)
            fail("Should reject approved > requested")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        repo.updateAidStatus(aidReq.id, AidStatus.APPROVED, 35000.0)
        repo.updateAidStatus(aidReq.id, AidStatus.DISBURSED, 35000.0)
        try {
            repo.updateAidStatus(aidReq.id, AidStatus.DISBURSED, 35000.0)
            fail("Should block duplicate disbursement")
        } catch (_: IllegalStateException) {
            // Expected
        }
    }

    @Test
    fun `stage3 backup integrity preserves empty lists safe migration and honest encrypted sync`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // 1. FundBackupCodec rejects tampered SHA-256 and preserves empty arrays []
        val emptyBackup = FundBackupCodec.exportToJson(
            exportDate = "2026-11-13 12:00",
            fundInfo = FundInfo(name = "صندوق فارغ"),
            regions = SeedData.regions,
            appSettings = AppSettings(),
            printSettings = PrintSettings(),
            members = emptyList(),
            beneficiaries = emptyList(),
            accounts = SeedData.accounts,
            vouchers = emptyList(),
            journalEntries = emptyList(),
            subscriptions = emptyList(),
            aidRequests = emptyList(),
            budgets = emptyList(),
            donors = emptyList(),
            campaigns = emptyList(),
            periodicAids = emptyList(),
            inKindItems = emptyList(),
            inKindMovements = emptyList(),
            schedulerEvents = emptyList(),
            internalMessages = emptyList(),
            currencyRates = mapOf(CurrencyCode.YER to 1.0, CurrencyCode.SAR to 665.0, CurrencyCode.USD to 2530.0),
            syncQueue = emptyList(),
            users = SeedData.users,
            auditLogs = emptyList(),
            syncBatches = emptyList(),
            serverConfig = LocalServerConfig()
        )
        val parsedValid = FundBackupCodec.parseFromJson(emptyBackup, FundInfo(), AppSettings(), PrintSettings())
        assertNotNull(parsedValid)
        assertTrue(parsedValid!!.members?.isEmpty() == true)
        assertTrue(parsedValid.vouchers?.isEmpty() == true)
        assertTrue(parsedValid.auditLogs?.isEmpty() == true)

        val tampered = emptyBackup.replace("صندوق فارغ", "صندوق معدل")
        assertNull(FundBackupCodec.parseFromJson(tampered, FundInfo(), AppSettings(), PrintSettings()))

        // 2. Corrupt legacy prefs must NOT be deleted or marked migrated
        val legacyPrefs = context.getSharedPreferences(LegacyMigrationHelper.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        legacyPrefs.edit(commit = true) {
            putBoolean("migrated_to_room_v1", false)
            putString(LegacyMigrationHelper.LEGACY_JSON_KEY, "{broken-json")
        }
        val tempDb = FundDatabase.buildInMemory(context)
        val tempSession = EncryptedSessionManager(context)
        LegacyMigrationHelper.migrateAndSeedIfNeeded(context, tempDb, tempSession)
        assertEquals("{broken-json", legacyPrefs.getString(LegacyMigrationHelper.LEGACY_JSON_KEY, null))
        assertFalse(legacyPrefs.getBoolean("migrated_to_room_v1", false))
        tempDb.close()
        legacyPrefs.edit(commit = true) { clear() }

        // 3. SyncRepository rejects HTTP plaintext URLs
        try {
            repo.syncRepository.buildBaseHttpsUrl("http://192.168.1.100:8080")
            fail("Must reject plaintext http:// URL")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        // 4. SyncRepository preserves queue on HTTP 500 error and clears only on HTTP 200 ACK
        repo.simulateOfflineFieldOperations()
        val queuedCount = repo.syncQueue.value.size
        assertTrue(queuedCount >= 3)

        repo.syncRepository.syncApiOverride = object : SyncApiService {
            override suspend fun checkHealth(): Response<SyncHealthResponse> =
                Response.success(SyncHealthResponse(status = "ok"))

            override suspend fun pushSyncBatch(payload: SyncBatchPayload): Response<SyncBatchAck> =
                Response.error(500, "Server error".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
        try {
            val failedSync = repo.syncRepository.syncNowBlocking()
            assertFalse(failedSync)
            assertEquals(queuedCount, repo.syncQueue.value.size)

            var capturedEnvelope = ""
            repo.syncRepository.syncApiOverride = object : SyncApiService {
                override suspend fun checkHealth(): Response<SyncHealthResponse> =
                    Response.success(SyncHealthResponse(status = "ok"))

                override suspend fun pushSyncBatch(payload: SyncBatchPayload): Response<SyncBatchAck> {
                    capturedEnvelope = payload.encryptedBackupEnvelope
                    return Response.success(SyncBatchAck(accepted = true, batchNo = payload.batchNo, verifiedChecksum = payload.sha256Checksum))
                }
            }
            val succeededSync = repo.syncRepository.syncNowBlocking()
            assertTrue(succeededSync)
            assertEquals(0, repo.syncQueue.value.size)
            assertTrue(capturedEnvelope.startsWith("AES256GCM_V1:"))
            val decrypted = repo.syncRepository.decryptBackupEnvelope(capturedEnvelope)
            assertNotNull(decrypted)
            assertTrue(decrypted!!.contains("integritySha256"))
        } finally {
            repo.syncRepository.syncApiOverride = null
        }
    }
}
