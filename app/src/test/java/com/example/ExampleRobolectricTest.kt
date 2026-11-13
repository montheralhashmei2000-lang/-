package com.example

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.example.fund.data.FundRepository
import com.example.fund.data.SeedData
import com.example.fund.data.local.FundDatabase
import com.example.fund.data.local.LegacyMigrationHelper
import com.example.fund.data.repository.AuthResult
import com.example.fund.model.AppFontOption
import com.example.fund.model.CalendarSystem
import com.example.fund.model.CurrencySymbolPosition
import com.example.fund.model.DateFormatPattern
import com.example.fund.model.DecimalSeparatorOption
import com.example.fund.model.DigitLanguage
import com.example.fund.model.DisplayCurrencyOption
import com.example.fund.model.DisplayFormatConfig
import com.example.fund.model.PrintFontOption
import com.example.fund.model.ThousandsSeparatorOption
import com.example.fund.security.EncryptedSessionManager
import com.example.fund.util.FormatUtils
import com.example.fund.util.generateQrSvg
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fund.ui.components.ThousandSeparatorTransformation
import com.example.fund.ui.screens.PeriodicAidsScreen
import com.example.fund.util.LocalScreenFormat
import com.example.fund.ui.navigation.AppRoute
import com.example.fund.ui.navigation.BottomHub
import com.example.fund.ui.navigation.Screen
import com.example.fund.ui.navigation.aidsHubScreens
import com.example.fund.ui.navigation.drawerAdminScreens
import com.example.fund.ui.navigation.financeHubScreens
import com.example.fund.ui.navigation.hubTabIndex
import com.example.fund.ui.navigation.isTopLevelHub
import com.example.fund.ui.navigation.membersHubScreens
import com.example.fund.ui.navigation.resolveBottomHub
import com.example.fund.ui.navigation.resolveBreadcrumbHint
import com.example.fund.ui.navigation.resolveTitle
import com.example.fund.ui.navigation.screens
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
    fun `type-safe navigation mapping resolves bottom hubs and screens with single entry point`() {
        assertEquals(AppRoute.DashboardRoute, BottomHub.HOME.toRoute())
        assertEquals(AppRoute.FinanceHubRoute, BottomHub.FINANCE.toRoute())
        assertEquals(AppRoute.MembersHubRoute, BottomHub.MEMBERS.toRoute())
        assertEquals(AppRoute.AidsHubRoute, BottomHub.AIDS.toRoute())

        // Operational modules map strictly to their parent hub and fixed tab index
        assertEquals(AppRoute.FinanceHubRoute, Screen.VOUCHERS.toRoute())
        assertEquals(0, Screen.VOUCHERS.hubTabIndex())
        assertEquals(1, Screen.JOURNAL.hubTabIndex())
        assertEquals(2, Screen.ACCOUNTS.hubTabIndex())
        assertEquals(AppRoute.MembersHubRoute, Screen.SUBSCRIPTIONS.toRoute())
        assertEquals(1, Screen.SUBSCRIPTIONS.hubTabIndex())
        assertEquals(AppRoute.AidsHubRoute, Screen.CAMPAIGNS.toRoute())
        assertEquals(4, Screen.CAMPAIGNS.hubTabIndex())
        assertTrue(Screen.SETTINGS.toRoute() is AppRoute.SettingsRoute)

        assertEquals(financeHubScreens, BottomHub.FINANCE.screens())
        assertEquals(membersHubScreens, BottomHub.MEMBERS.screens())
        assertEquals(aidsHubScreens, BottomHub.AIDS.screens())

        assertEquals(23, Screen.entries.size)
        Screen.entries.forEach { screen ->
            assertNotNull(screen.toRoute())
        }
    }

    @Test
    fun `single entry point registries partition all screens without overlap and resolve breadcrumbs`() {
        val hubAssigned = financeHubScreens + membersHubScreens + aidsHubScreens + drawerAdminScreens
        val allAccounted = hubAssigned + listOf(Screen.DASHBOARD, Screen.LOGIN)
        assertEquals(allAccounted.size, allAccounted.toSet().size)
        assertEquals(Screen.entries.toSet(), allAccounted.toSet())

        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = AppRoute.DashboardRoute) {
                composable<AppRoute.DashboardRoute> {}
                composable<AppRoute.FinanceHubRoute> {}
                composable<AppRoute.MembersHubRoute> {}
                composable<AppRoute.AidsHubRoute> {}
                composable<AppRoute.SchedulerRoute> {}
                composable<AppRoute.MessagesRoute> {}
                composable<AppRoute.FundInfoRoute> {}
                composable<AppRoute.UsersRoute> {}
                composable<AppRoute.AuditRoute> {}
                composable<AppRoute.SettingsRoute> {}
            }
        }
        composeTestRule.waitForIdle()

        // Start destination is Dashboard hub (top-level -> no breadcrumb hint)
        assertTrue(navController.currentDestination.isTopLevelHub())
        assertNull(navController.currentDestination.resolveBreadcrumbHint())
        assertEquals(BottomHub.HOME, navController.currentDestination.resolveBottomHub())

        // Finance hub -> top-level permanent tab container
        composeTestRule.runOnUiThread { navController.navigate(AppRoute.FinanceHubRoute) }
        composeTestRule.waitForIdle()
        assertTrue(navController.currentDestination.isTopLevelHub())
        assertEquals(BottomHub.FINANCE, navController.currentDestination.resolveBottomHub())
        assertEquals(BottomHub.FINANCE.title, navController.currentDestination.resolveTitle())

        // Members hub -> top-level permanent tab container
        composeTestRule.runOnUiThread { navController.navigate(AppRoute.MembersHubRoute) }
        composeTestRule.waitForIdle()
        assertTrue(navController.currentDestination.isTopLevelHub())
        assertEquals(BottomHub.MEMBERS, navController.currentDestination.resolveBottomHub())

        // Aids hub -> top-level permanent tab container
        composeTestRule.runOnUiThread { navController.navigate(AppRoute.AidsHubRoute) }
        composeTestRule.waitForIdle()
        assertTrue(navController.currentDestination.isTopLevelHub())
        assertEquals(BottomHub.AIDS, navController.currentDestination.resolveBottomHub())

        // Settings -> Drawer Admin route breadcrumb
        composeTestRule.runOnUiThread { navController.navigate(AppRoute.SettingsRoute(initialTab = 4)) }
        composeTestRule.waitForIdle()
        assertFalse(navController.currentDestination.isTopLevelHub())
        assertEquals(BottomHub.HOME, navController.currentDestination.resolveBottomHub())
        assertTrue(navController.currentDestination.resolveBreadcrumbHint()?.startsWith("الإدارة والنظام") == true)
    }

    @Test
    fun `formatUtils formats numbers currencies dates codes and hijri calendar per display config`() {
        val arabicConfig = DisplayFormatConfig(
            digitLanguage = DigitLanguage.ARABIC_INDIC,
            thousandsSeparator = ThousandsSeparatorOption.DOT,
            decimalSeparator = DecimalSeparatorOption.COMMA,
            decimalPlaces = 2,
            currencyOption = DisplayCurrencyOption.CUSTOM,
            customCurrencySymbol = "د.أ",
            symbolPosition = CurrencySymbolPosition.BEFORE,
            datePattern = DateFormatPattern.DMY_SLASH,
            showTime = false,
            calendarSystem = CalendarSystem.GREGORIAN
        )
        assertEquals("١.٢٥٠.٠٠٠,٥٠", FormatUtils.formatNumber(1250000.5, arabicConfig))
        assertEquals("د.أ ١.٢٥٠.٠٠٠,٥٠", FormatUtils.formatAmount(1250000.5, arabicConfig))
        assertEquals("٠٨/١١/٢٠٢٦", FormatUtils.formatDate("2026-11-08", arabicConfig))
        assertEquals("V-٢٠٢٦-٠٠٤٢", FormatUtils.formatCodeOrNumber("V-2026-0042", arabicConfig))
        assertEquals("JE-٠٠٠٠١٥", FormatUtils.formatCodeOrNumber("JE-000015", arabicConfig))

        val latinConfig = DisplayFormatConfig(digitLanguage = DigitLanguage.LATIN)
        assertEquals("V-2026-0042", FormatUtils.formatCodeOrNumber("V-٢٠٢٦-٠٠٤٢", latinConfig))
        assertEquals("JE-000015", FormatUtils.formatCodeOrNumber("JE-٠٠٠٠١٥", latinConfig))

        val tableHideConfig = DisplayFormatConfig(
            currencyOption = DisplayCurrencyOption.YER_SIGN,
            symbolPosition = CurrencySymbolPosition.HIDDEN_IN_TABLES,
            datePattern = DateFormatPattern.DMY_MONTH_NAME,
            showTime = true,
            calendarSystem = CalendarSystem.HIJRI
        )
        assertEquals("50,000", FormatUtils.formatAmount(50000.0, tableHideConfig, inTable = true))
        assertEquals("50,000 ﷼", FormatUtils.formatAmount(50000.0, tableHideConfig, inTable = false))
        val hijriFormatted = FormatUtils.formatDate("2026-11-08 14:30", tableHideConfig)
        assertTrue(hijriFormatted.contains("هـ"))
        assertTrue(hijriFormatted.contains("14:30"))
    }

    @Test
    fun `app font option and independent screen vs print formats persist and round trip backup`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        val session1 = EncryptedSessionManager(context)
        session1.appFontFamily = AppFontOption.CAIRO.name
        assertEquals(AppFontOption.CAIRO.name, EncryptedSessionManager(context).appFontFamily)

        repo.setAppFontPreference(AppFontOption.IBM_PLEX_SANS_ARABIC)
        assertEquals(AppFontOption.IBM_PLEX_SANS_ARABIC, repo.appFontFamily.value)
        assertEquals(AppFontOption.IBM_PLEX_SANS_ARABIC.name, repo.getSessionManagerOrNull()?.appFontFamily)

        val customScreen = DisplayFormatConfig(
            digitLanguage = DigitLanguage.ARABIC_INDIC,
            thousandsSeparator = ThousandsSeparatorOption.SPACE,
            decimalPlaces = 2,
            currencyOption = DisplayCurrencyOption.SAR,
            calendarSystem = CalendarSystem.HIJRI
        )
        val customPrint = DisplayFormatConfig(
            digitLanguage = DigitLanguage.LATIN,
            thousandsSeparator = ThousandsSeparatorOption.COMMA,
            decimalPlaces = 0,
            currencyOption = DisplayCurrencyOption.USD,
            symbolPosition = CurrencySymbolPosition.BEFORE,
            datePattern = DateFormatPattern.DMY_SLASH
        )
        repo.settingsRepository.updateScreenFormat(customScreen)
        repo.settingsRepository.updatePrintFormat(customPrint)

        assertEquals(DigitLanguage.ARABIC_INDIC, repo.screenFormat.value.digitLanguage)
        assertEquals(DisplayCurrencyOption.SAR, repo.screenFormat.value.currencyOption)
        assertEquals(DigitLanguage.ARABIC_INDIC, repo.printSettings.value.screenFormat.digitLanguage)
        assertEquals(DigitLanguage.LATIN, repo.printFormat.value.digitLanguage)
        assertEquals(DisplayCurrencyOption.USD, repo.printFormat.value.currencyOption)
        assertEquals(DisplayCurrencyOption.USD, repo.printSettings.value.printFormat.currencyOption)

        val backupJson = repo.exportDatabaseJson()
        repo.resetToDefaultSeedData()
        assertEquals(DisplayCurrencyOption.YER, repo.screenFormat.value.currencyOption)
        assertEquals(DisplayCurrencyOption.YER, repo.printFormat.value.currencyOption)

        assertTrue(repo.importDatabaseFromJson(backupJson))
        assertEquals(DigitLanguage.ARABIC_INDIC, repo.screenFormat.value.digitLanguage)
        assertEquals(ThousandsSeparatorOption.SPACE, repo.screenFormat.value.thousandsSeparator)
        assertEquals(CalendarSystem.HIJRI, repo.screenFormat.value.calendarSystem)
        assertEquals(CalendarSystem.HIJRI, repo.printSettings.value.screenFormat.calendarSystem)
        assertEquals(DisplayCurrencyOption.USD, repo.printFormat.value.currencyOption)
        assertEquals(CurrencySymbolPosition.BEFORE, repo.printFormat.value.symbolPosition)
        assertEquals(DateFormatPattern.DMY_SLASH, repo.printFormat.value.datePattern)
    }

    @Test
    fun `thousand separator transformation supports custom separators and arabic indic digits`() {
        val tx = ThousandSeparatorTransformation(
            thousandsSep = ".",
            decimalSep = ",",
            digitLanguage = DigitLanguage.ARABIC_INDIC
        )
        val result = tx.filter(AnnotatedString("1250000.50"))
        assertEquals("١.٢٥٠.٠٠٠,٥٠", result.text.text)
        assertEquals(12, result.offsetMapping.originalToTransformed(10))
        assertEquals(10, result.offsetMapping.transformedToOriginal(12))
    }

    @Test
    fun `periodic aids screen displays header and registered sponsorships list on compact viewport`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FundRepository.getInstance()
        repo.initStorage(context)
        repo.resetToDefaultSeedData()

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(baseDensity.density, fontScale = 1.25f),
                LocalScreenFormat provides repo.screenFormat.value
            ) {
                Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                    PeriodicAidsScreen()
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("periodic_aids_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("المساعدات والكفالات الدورية الشهرية").assertIsDisplayed()
        composeTestRule.onNodeWithText("قائمة الكفالات الشهرية المسجلة", substring = true).assertIsDisplayed()
        val firstBeneficiary = repo.periodicAids.value.first().beneficiaryName
        composeTestRule.onNodeWithText(firstBeneficiary).assertIsDisplayed()
    }
}
