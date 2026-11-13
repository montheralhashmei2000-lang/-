package com.example

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.example.fund.data.FundRepository
import com.example.fund.data.SeedData
import com.example.fund.data.local.FundDatabase
import com.example.fund.data.local.LegacyMigrationHelper
import com.example.fund.data.repository.AuthResult
import com.example.fund.model.PrintFontOption
import com.example.fund.security.EncryptedSessionManager
import com.example.fund.util.generateQrSvg
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fund.ui.components.ThousandSeparatorTransformation
import com.example.fund.ui.navigation.AppRoute
import com.example.fund.ui.navigation.BottomHub
import com.example.fund.ui.navigation.Screen
import com.example.fund.ui.navigation.isTopLevelHub
import com.example.fund.ui.navigation.resolveBottomHub
import com.example.fund.ui.navigation.resolveContextGroup
import com.example.fund.ui.navigation.resolveTitle
import com.example.fund.ui.navigation.toRoute
import com.example.fund.util.OfficialPrintHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
            fontFamily = PrintFontOption.CAIRO,
            sig2Title = "أمين الصندوق العام",
            sig2DefaultName = "سالم عبدالله"
        )
        repo.updatePrintSettings(updatedPrintSettings)

        val exportedJson = repo.exportDatabaseJson()
        assertTrue(exportedJson.contains("منطقة الشمال"))
        assertTrue(exportedJson.contains("BASE64_SAMPLE_LOGO"))
        assertTrue(exportedJson.contains("إيصال قبض مخصص"))
        assertTrue(exportedJson.contains("integritySha256"))

        repo.resetToDefaultSeedData()
        assertEquals("", repo.fundInfo.value.customLogoBase64)
        assertEquals(PrintFontOption.AMIRI, repo.printSettings.value.fontFamily)

        assertTrue(repo.importDatabaseFromJson(exportedJson))
        assertEquals("BASE64_SAMPLE_LOGO", repo.fundInfo.value.customLogoBase64)
        assertEquals(35000.0, repo.appSettings.value.monthlySubscriptionFeeYer, 0.001)
        assertEquals("REC", repo.appSettings.value.voucherNoPrefix)
        assertEquals("إيصال قبض مخصص", repo.printSettings.value.receiptVoucherTitle)
        assertEquals("تذييل اختباري مخصص", repo.printSettings.value.footerNote)
        assertEquals(PrintFontOption.CAIRO, repo.printSettings.value.fontFamily)
        assertEquals("سالم عبدالله", repo.printSettings.value.sig2DefaultName)
        assertTrue(repo.regions.value.any { it.name == "منطقة الشمال" && it.villages.contains("قرية الجبل") })
        assertTrue(generateQrSvg("TEST").contains("<svg"))
    }

    @Test
    fun `one-time migration moves legacy shared preferences json into Room and removes legacy key`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacyPrefs = context.getSharedPreferences(LegacyMigrationHelper.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val seedJson = FundRepository.getInstance().exportDatabaseJson()
            .replace("الصندوق الاجتماعي التنموي", "صندوق اختبار الترحيل")

        legacyPrefs.edit(commit = true) {
            putString(LegacyMigrationHelper.LEGACY_JSON_KEY, seedJson)
            putBoolean("is_dark_mode", true)
            putFloat("font_scale", 1.15f)
        }

        val inMemoryDb = FundDatabase.buildInMemory(context)
        val session = EncryptedSessionManager(context)
        LegacyMigrationHelper.migrateAndSeedIfNeeded(context, inMemoryDb, session)

        // Legacy JSON key must be deleted after migration
        assertNull(legacyPrefs.getString(LegacyMigrationHelper.LEGACY_JSON_KEY, null))
        assertTrue(legacyPrefs.getBoolean("migrated_to_room_v1", false))

        // Migrated preferences & database rows must live in EncryptedSessionManager & Room
        assertTrue(session.isDarkMode)
        assertEquals(1.15f, session.fontScale, 0.001f)
        assertEquals("صندوق اختبار الترحيل", inMemoryDb.settingsDao().getFundInfo()?.name)
        assertTrue(inMemoryDb.accountingDao().observeAccounts().first().isNotEmpty())
        inMemoryDb.close()
    }

    @Test
    fun `password authentication verifies pbkdf2 hash and rejects wrong passwords`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)

        val validLogin = repo.authenticateWithPassword("admin", SeedData.defaultDemoPassword)
        assertTrue(validLogin is AuthResult.Success)

        val invalidLogin = repo.authenticateWithPassword("admin", "wrong-password-123")
        assertTrue(invalidLogin is AuthResult.Error)
    }

    @Test
    fun `quick pin setup and authentication enforce initial password login and verify hash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        // Complete initial password login first
        val pwLogin = repo.authenticateWithPassword("admin", SeedData.defaultDemoPassword)
        assertTrue(pwLogin is AuthResult.Success)

        assertFalse(repo.userRepository.setQuickPin("12"))
        assertFalse(repo.userRepository.setQuickPin("abcd"))
        assertTrue(repo.userRepository.setQuickPin("4829"))

        val validPin = repo.userRepository.authenticateWithQuickPin("admin", "4829")
        assertTrue(validPin is AuthResult.Success)

        val badPin = repo.userRepository.authenticateWithQuickPin("admin", "0000")
        assertTrue(badPin is AuthResult.Error)
    }

    @Test
    fun `encrypted session persists initial login flag and quick pin hash across instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session1 = EncryptedSessionManager(context)
        session1.clearQuickPin()
        session1.hasCompletedInitialLogin = false

        assertFalse(session1.hasCompletedInitialLogin)
        assertNull(session1.quickPinHash)

        session1.saveActiveSession("u1", "admin", loggedIn = true)
        session1.quickPinHash = "PBKDF2_HASH_SAMPLE"

        val session2 = EncryptedSessionManager(context)
        assertTrue(session2.hasCompletedInitialLogin)
        assertEquals("admin", session2.lastAuthenticatedUsername)
        assertEquals("PBKDF2_HASH_SAMPLE", session2.quickPinHash)
    }

    @Test
    fun `thousand separator transformation formats integers and decimals with valid offset mapping`() {
        val tx = ThousandSeparatorTransformation()
        val formattedInt = tx.filter(AnnotatedString("1250000"))
        assertEquals("1,250,000", formattedInt.text.text)
        assertEquals(9, formattedInt.offsetMapping.originalToTransformed(7))
        assertEquals(7, formattedInt.offsetMapping.transformedToOriginal(9))

        val formattedDec = tx.filter(AnnotatedString("25000.50"))
        assertEquals("25,000.50", formattedDec.text.text)
        assertEquals(9, formattedDec.offsetMapping.originalToTransformed(8))
    }

    @Test
    fun `type-safe navigation mapping resolves bottom hubs and screens accurately`() {
        assertEquals(AppRoute.DashboardRoute, BottomHub.HOME.toRoute())
        assertEquals(AppRoute.FinanceHubRoute, BottomHub.FINANCE.toRoute())
        assertEquals(AppRoute.MembersHubRoute, BottomHub.MEMBERS.toRoute())
        assertEquals(AppRoute.AidsHubRoute, BottomHub.AIDS.toRoute())
        assertTrue(Screen.VOUCHERS.toRoute() is AppRoute.VouchersRoute)
        assertTrue(Screen.SETTINGS.toRoute() is AppRoute.SettingsRoute)
        assertEquals(23, Screen.entries.size)
        Screen.entries.forEach { screen ->
            assertNotNull(screen.toRoute())
        }
    }

    @Test
    fun `navigation destinations resolve top-level hubs and sibling context groups`() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoute.DashboardRoute) {
                composable<AppRoute.DashboardRoute> {}
                composable<AppRoute.FinanceHubRoute> {}
                composable<AppRoute.MembersHubRoute> {}
                composable<AppRoute.AidsHubRoute> {}
                composable<AppRoute.VouchersRoute> {}
                composable<AppRoute.MembersRoute> {}
                composable<AppRoute.AidsRoute> {}
                composable<AppRoute.SettingsRoute> {}
            }
        }
        composeTestRule.waitForIdle()

        // Start destination is Dashboard hub
        assertTrue(navController.currentDestination.isTopLevelHub())
        assertNull(navController.currentDestination.resolveContextGroup())
        assertEquals(BottomHub.HOME, navController.currentDestination.resolveBottomHub())

        // Finance hub -> no sibling bar; Vouchers -> Finance context group
        composeTestRule.runOnUiThread { navController.navigate(AppRoute.FinanceHubRoute) }
        composeTestRule.waitForIdle()
        assertTrue(navController.currentDestination.isTopLevelHub())
        assertNull(navController.currentDestination.resolveContextGroup())

        composeTestRule.runOnUiThread { navController.navigate(AppRoute.VouchersRoute()) }
        composeTestRule.waitForIdle()
        assertFalse(navController.currentDestination.isTopLevelHub())
        assertEquals(BottomHub.FINANCE, navController.currentDestination.resolveBottomHub())
        assertEquals(Screen.VOUCHERS.title, navController.currentDestination.resolveTitle())
        val financeGroup = navController.currentDestination.resolveContextGroup()
        assertNotNull(financeGroup)
        assertEquals(AppRoute.FinanceHubRoute, financeGroup?.hubRoute)
        assertTrue(financeGroup!!.items.any { it.screen == Screen.TREASURY })

        // Members child -> Members context group
        composeTestRule.runOnUiThread { navController.navigate(AppRoute.MembersRoute()) }
        composeTestRule.waitForIdle()
        assertFalse(navController.currentDestination.isTopLevelHub())
        assertEquals(BottomHub.MEMBERS, navController.currentDestination.resolveBottomHub())
        assertEquals(AppRoute.MembersHubRoute, navController.currentDestination.resolveContextGroup()?.hubRoute)

        // Aids child -> Aids context group
        composeTestRule.runOnUiThread { navController.navigate(AppRoute.AidsRoute()) }
        composeTestRule.waitForIdle()
        assertEquals(BottomHub.AIDS, navController.currentDestination.resolveBottomHub())
        assertEquals(AppRoute.AidsHubRoute, navController.currentDestination.resolveContextGroup()?.hubRoute)

        // Settings -> Admin context group
        composeTestRule.runOnUiThread { navController.navigate(AppRoute.SettingsRoute(initialTab = 4)) }
        composeTestRule.waitForIdle()
        assertEquals(BottomHub.HOME, navController.currentDestination.resolveBottomHub())
        assertEquals("الإدارة والرقابة", navController.currentDestination.resolveContextGroup()?.hubTitle)
    }
}
