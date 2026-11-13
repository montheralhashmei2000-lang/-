package com.example.fund.data.repository

import androidx.room.withTransaction
import com.example.fund.data.SeedData
import com.example.fund.data.local.*
import com.example.fund.model.*
import com.example.fund.util.TafqeetEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AccountingRepository(
    private val dbScope: CoroutineScope,
    private val dbDispatcher: CoroutineDispatcher,
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository
) {
    private val _accounts = MutableStateFlow(SeedData.accounts)
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _vouchers = MutableStateFlow(SeedData.vouchers)
    val vouchers: StateFlow<List<Voucher>> = _vouchers.asStateFlow()

    private val _journalEntries = MutableStateFlow(SeedData.journalEntries)
    val journalEntries: StateFlow<List<JournalEntry>> = _journalEntries.asStateFlow()

    private val _budgets = MutableStateFlow(SeedData.budgets)
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private val _currencyRates = MutableStateFlow(SeedData.currencyRates.toMap())
    val currencyRates: StateFlow<Map<CurrencyCode, Double>> = _currencyRates.asStateFlow()

    private var db: FundDatabase? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private fun nowStr() = dateFormat.format(Date())

    suspend fun attachAndHydrate(database: FundDatabase) {
        db = database
        val dbAccounts = database.accountingDao().observeAccounts().first().map { it.toDomain() }
        if (dbAccounts.isNotEmpty()) _accounts.value = dbAccounts

        val dbVouchers = database.accountingDao().observeVouchers().first().map { it.toDomain() }
        if (dbVouchers.isNotEmpty()) _vouchers.value = dbVouchers

        val dbJournals = database.accountingDao().observeJournalEntries().first().map { it.toDomain() }
        if (dbJournals.isNotEmpty()) _journalEntries.value = dbJournals

        val dbBudgets = database.accountingDao().observeBudgets().first().map { it.toDomain() }
        if (dbBudgets.isNotEmpty()) _budgets.value = dbBudgets

        val dbRates = database.accountingDao().observeCurrencyRates().first()
        if (dbRates.isNotEmpty()) {
            _currencyRates.value = dbRates.associate { it.currencyCode to it.rate }
        }
    }

    fun convertToYer(amountOriginal: Double, currency: CurrencyCode): Double {
        val rate = _currencyRates.value[currency] ?: currency.defaultRate
        return amountOriginal * rate
    }

    fun updateCurrencyRate(currency: CurrencyCode, rate: Double) {
        if (rate <= 0.0) return
        val current = _currencyRates.value.toMutableMap()
        current[currency] = rate
        _currencyRates.value = current
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.accountingDao().upsertCurrencyRate(CurrencyRateEntity(currency, rate))
            }
        }
        userRepository.logAudit("تعديل سعر الصرف", "تم تعديل سعر صرف ${currency.title} إلى $rate ﷼")
    }

    private fun nextVoucherNo(): String {
        val customPrefix = settingsRepository.appSettings.value.voucherNoPrefix.ifBlank { "V" }
        val yearPrefix = "$customPrefix-${Calendar.getInstance().get(Calendar.YEAR)}-"
        val maxExisting = _vouchers.value.mapNotNull { v ->
            v.voucherNo.substringAfterLast("-").toIntOrNull()
        }.maxOrNull() ?: 0
        val nextNum = maxOf(maxExisting, _vouchers.value.size) + 1
        return "$yearPrefix%03d".format(nextNum)
    }

    private fun nextJournalNo(): String {
        val maxExisting = _journalEntries.value.mapNotNull { je ->
            val clean = je.entryNo.removeSuffix("-R")
            clean.substringAfterLast("-").toIntOrNull()
        }.maxOrNull() ?: 0
        val nextNum = maxOf(maxExisting, _journalEntries.value.size) + 1
        return "JE-%06d".format(nextNum)
    }

    fun issueVoucher(
        type: VoucherType,
        partyName: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        paymentMethod: PaymentMethod,
        accountCode: String,
        counterpartAccountCode: String,
        description: String
    ): Voucher {
        require(amountOriginal > 0.0) { "مبلغ السند يجب أن يكون أكبر من صفر." }
        val rate = _currencyRates.value[currency] ?: currency.defaultRate
        val amountYer = amountOriginal * rate
        val words = TafqeetEngine.convertToWords(amountYer.toLong(), "ريال يمني")
        val voucherNo = nextVoucherNo()
        val journalNo = nextJournalNo()
        val today = nowStr()

        val acc = _accounts.value.find { it.code == accountCode }
        val counterpartAcc = _accounts.value.find { it.code == counterpartAccountCode }
        val accName = acc?.name ?: "الحساب المالي"
        val counterpartName = counterpartAcc?.name ?: "الحساب المقابل"

        val lines = if (type == VoucherType.RECEIPT) {
            listOf(
                JournalLine(accountCode, accName, amountYer, 0.0, "مدين - إيداع بموجب $voucherNo"),
                JournalLine(counterpartAccountCode, counterpartName, 0.0, amountYer, "دائن - $counterpartName")
            )
        } else {
            listOf(
                JournalLine(accountCode, accName, amountYer, 0.0, "مدين - $accName"),
                JournalLine(counterpartAccountCode, counterpartName, 0.0, amountYer, "دائن - صرف بموجب $voucherNo")
            )
        }

        val validation = AccountingValidator.validateJournalLines(lines, requireBalanced = true)
        require(validation.isValid) { validation.errorMessage ?: "فشل التحقق من توازن القيد المزدوج." }

        val journalEntry = JournalEntry(
            entryNo = journalNo,
            date = today,
            description = "إثبات $voucherNo: $description",
            lines = lines,
            isPosted = true,
            originalVoucherNo = voucherNo
        )

        val ps = settingsRepository.printSettings.value
        val newVoucher = Voucher(
            voucherNo = voucherNo,
            type = type,
            status = VoucherStatus.POSTED,
            partyName = partyName,
            amountOriginal = amountOriginal,
            currency = currency,
            exchangeRate = rate,
            amountYer = amountYer,
            amountWords = words,
            paymentMethod = paymentMethod,
            accountCode = accountCode,
            accountName = accName,
            counterpartAccountCode = counterpartAccountCode,
            counterpartAccountName = counterpartName,
            description = description,
            date = today,
            linkedJournalEntryId = journalNo,
            receivedOrPaidBy = ps.sig1DefaultName.ifBlank { partyName },
            accountantName = ps.sig2DefaultName.ifBlank { "أمين الصندوق / المحاسب" },
            auditorName = ps.sig3DefaultName.ifBlank { "المراجع المالي" },
            managerName = ps.sig4DefaultName.ifBlank { "رئيس مجلس الإدارة" }
        )

        // Update state atomically in memory
        val updatedAccounts = applyLinesToAccounts(_accounts.value, lines)
        _accounts.value = updatedAccounts
        _journalEntries.value = listOf(journalEntry) + _journalEntries.value
        _vouchers.value = listOf(newVoucher) + _vouchers.value

        // Persist inside Room @Transaction
        db?.let { database ->
            val jeCount = _journalEntries.value.size
            val vCount = _vouchers.value.size
            dbScope.launch(dbDispatcher) {
                database.withTransaction {
                    database.accountingDao().upsertJournalEntry(JournalEntryEntity.fromDomain(journalEntry, -jeCount))
                    database.accountingDao().upsertVoucher(VoucherEntity.fromDomain(newVoucher, -vCount))
                    val affectedCodes = lines.map { it.accountCode }.toSet()
                    val parentCodes = updatedAccounts
                        .filter { it.code in affectedCodes }
                        .mapNotNull { it.parentCode }
                        .toSet()
                    updatedAccounts
                        .filter { it.code in affectedCodes || it.code in parentCodes }
                        .forEach { acc ->
                            database.accountingDao().upsertAccount(AccountEntity.fromDomain(acc))
                        }
                }
            }
        }

        userRepository.logAudit("إصدار ${type.title}", "إصدار $voucherNo للمكرم $partyName بمبلغ $amountYer ﷼ مع القيد $journalNo")
        return newVoucher
    }

    fun transferBetweenTreasuryAccounts(
        fromAccountCode: String,
        toAccountCode: String,
        amountYer: Double,
        notes: String
    ): Boolean {
        if (fromAccountCode == toAccountCode || amountYer <= 0.0) return false
        val fromAcc = _accounts.value.find { it.code == fromAccountCode } ?: return false
        val toAcc = _accounts.value.find { it.code == toAccountCode } ?: return false
        if (!settingsRepository.appSettings.value.allowNegativeTreasury && fromAcc.balance < amountYer) return false

        val journalNo = nextJournalNo()
        val today = nowStr()
        val desc = notes.ifBlank { "تحويل سيولة داخلي من ${fromAcc.name} إلى ${toAcc.name}" }
        val lines = listOf(
            JournalLine(toAccountCode, toAcc.name, amountYer, 0.0, "مدين - إيداع تحويل داخلي"),
            JournalLine(fromAccountCode, fromAcc.name, 0.0, amountYer, "دائن - سحب تحويل داخلي")
        )

        if (!AccountingValidator.validateJournalLines(lines, requireBalanced = true).isValid) return false

        val entry = JournalEntry(
            entryNo = journalNo,
            date = today,
            description = desc,
            lines = lines,
            isPosted = true
        )
        val updatedAccounts = applyLinesToAccounts(_accounts.value, lines)
        _accounts.value = updatedAccounts
        _journalEntries.value = listOf(entry) + _journalEntries.value

        db?.let { database ->
            val jeCount = _journalEntries.value.size
            dbScope.launch(dbDispatcher) {
                database.withTransaction {
                    database.accountingDao().upsertJournalEntry(JournalEntryEntity.fromDomain(entry, -jeCount))
                    val affectedCodes = setOf(fromAccountCode, toAccountCode)
                    val parentCodes = updatedAccounts
                        .filter { it.code in affectedCodes }
                        .mapNotNull { it.parentCode }
                        .toSet()
                    updatedAccounts
                        .filter { it.code in affectedCodes || it.code in parentCodes }
                        .forEach { acc ->
                            database.accountingDao().upsertAccount(AccountEntity.fromDomain(acc))
                        }
                }
            }
        }

        userRepository.logAudit(
            "تحويل مالي داخلي",
            "تحويل مبلغ ${amountYer.toLong()} ﷼ من ${fromAcc.name} إلى ${toAcc.name} بموجب القيد $journalNo"
        )
        return true
    }

    fun voidVoucher(voucherId: String) {
        val voucher = _vouchers.value.find { it.id == voucherId } ?: return
        if (voucher.status == VoucherStatus.VOID) return

        val updatedVoucher = voucher.copy(status = VoucherStatus.VOID)
        val revJournalNo = "${voucher.linkedJournalEntryId}-R"
        val lines = if (voucher.type == VoucherType.RECEIPT) {
            listOf(
                JournalLine(voucher.accountCode, voucher.accountName, 0.0, voucher.amountYer, "دائن - عكس إيداع السند الملغي ${voucher.voucherNo}"),
                JournalLine(voucher.counterpartAccountCode, voucher.counterpartAccountName, voucher.amountYer, 0.0, "مدين - عكس الإيراد للسند الملغي")
            )
        } else {
            listOf(
                JournalLine(voucher.accountCode, voucher.accountName, 0.0, voucher.amountYer, "دائن - عكس المصروف للسند الملغي ${voucher.voucherNo}"),
                JournalLine(voucher.counterpartAccountCode, voucher.counterpartAccountName, voucher.amountYer, 0.0, "مدين - استرداد النقدية للسند الملغي")
            )
        }

        if (!AccountingValidator.validateJournalLines(lines, requireBalanced = true).isValid) return

        val revEntry = JournalEntry(
            entryNo = revJournalNo,
            date = nowStr(),
            description = "قيد عكسي آلي لإلغاء السند ${voucher.voucherNo} (${voucher.description})",
            lines = lines,
            isPosted = true,
            isReversed = true,
            originalVoucherNo = voucher.voucherNo
        )

        val updatedAccounts = applyLinesToAccounts(_accounts.value, lines)
        _accounts.value = updatedAccounts
        _vouchers.value = _vouchers.value.map { if (it.id == voucherId) updatedVoucher else it }
        _journalEntries.value = listOf(revEntry) + _journalEntries.value

        db?.let { database ->
            val jeCount = _journalEntries.value.size
            dbScope.launch(dbDispatcher) {
                database.withTransaction {
                    database.accountingDao().upsertVoucher(VoucherEntity.fromDomain(updatedVoucher))
                    database.accountingDao().upsertJournalEntry(JournalEntryEntity.fromDomain(revEntry, -jeCount))
                    val affectedCodes = lines.map { it.accountCode }.toSet()
                    val parentCodes = updatedAccounts
                        .filter { it.code in affectedCodes }
                        .mapNotNull { it.parentCode }
                        .toSet()
                    updatedAccounts
                        .filter { it.code in affectedCodes || it.code in parentCodes }
                        .forEach { acc ->
                            database.accountingDao().upsertAccount(AccountEntity.fromDomain(acc))
                        }
                }
            }
        }

        userRepository.logAudit("إلغاء سند", "تم إلغاء السند ${voucher.voucherNo} بنجاح وتوليد قيد عكسي $revJournalNo")
    }

    fun addSubAccount(
        code: String,
        name: String,
        type: AccountType,
        parentCode: String,
        openingBalance: Double = 0.0
    ): Boolean {
        if (code.isBlank() || name.isBlank() || _accounts.value.any { it.code == code }) return false
        val isDebitNormal = type == AccountType.ASSET || type == AccountType.EXPENSE
        val newAcc = Account(
            code = code,
            name = name,
            type = type,
            isDebitNormal = isDebitNormal,
            balance = openingBalance,
            parentCode = parentCode
        )
        val nextList = recalculateParentBalances((_accounts.value + newAcc).sortedBy { it.code })
        _accounts.value = nextList
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.withTransaction {
                    database.accountingDao().upsertAccount(AccountEntity.fromDomain(newAcc))
                    nextList.find { it.code == parentCode }?.let { parentAcc ->
                        database.accountingDao().upsertAccount(AccountEntity.fromDomain(parentAcc))
                    }
                }
            }
        }
        userRepository.logAudit("إضافة حساب محاسبي", "إضافة حساب فرعي ($code - $name) تحت ($parentCode)")
        return true
    }

    fun createManualJournalEntry(lines: List<JournalLine>, description: String): Boolean {
        if (description.isBlank()) return false
        val enforceBalanced = settingsRepository.appSettings.value.enforceBalancedJournal
        // Strict double-entry validation: never allow unbalanced or zero-sum journal entry
        val validation = AccountingValidator.validateJournalLines(lines, requireBalanced = enforceBalanced)
        if (!validation.isValid) return false

        val entryNo = nextJournalNo()
        val entry = JournalEntry(
            entryNo = entryNo,
            date = nowStr(),
            description = description,
            lines = lines,
            isPosted = true
        )

        val updatedAccounts = applyLinesToAccounts(_accounts.value, lines)
        _accounts.value = updatedAccounts
        _journalEntries.value = listOf(entry) + _journalEntries.value

        db?.let { database ->
            val jeCount = _journalEntries.value.size
            dbScope.launch(dbDispatcher) {
                database.withTransaction {
                    database.accountingDao().upsertJournalEntry(JournalEntryEntity.fromDomain(entry, -jeCount))
                    val affectedCodes = lines.map { it.accountCode }.toSet()
                    val parentCodes = updatedAccounts
                        .filter { it.code in affectedCodes }
                        .mapNotNull { it.parentCode }
                        .toSet()
                    updatedAccounts
                        .filter { it.code in affectedCodes || it.code in parentCodes }
                        .forEach { acc ->
                            database.accountingDao().upsertAccount(AccountEntity.fromDomain(acc))
                        }
                }
            }
        }

        userRepository.logAudit("قيد تسوية يدوي", "إنشاء قيد يومية متوازن $entryNo بمبلغ ${validation.totalDebit} ﷼")
        return true
    }

    private fun applyLinesToAccounts(currentAccounts: List<Account>, lines: List<JournalLine>): List<Account> {
        val deltaByCode = mutableMapOf<String, Double>()
        val accMap = currentAccounts.associateBy { it.code }
        for (line in lines) {
            val acc = accMap[line.accountCode]
            val delta = if (acc != null) {
                AccountingValidator.computeBalanceDelta(acc, line)
            } else {
                line.debit - line.credit
            }
            deltaByCode[line.accountCode] = (deltaByCode[line.accountCode] ?: 0.0) + delta
        }
        val withLeafUpdates = currentAccounts.map { acc ->
            val delta = deltaByCode[acc.code] ?: 0.0
            if (delta != 0.0) acc.copy(balance = acc.balance + delta) else acc
        }
        return recalculateParentBalances(withLeafUpdates)
    }

    private fun recalculateParentBalances(accountsList: List<Account>): List<Account> {
        val childrenSumByParent = accountsList
            .mapNotNull { acc -> acc.parentCode?.let { parent -> parent to acc.balance } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, balances) -> balances.sum() }
        return accountsList.map { acc ->
            if (acc.parentCode == null && childrenSumByParent.containsKey(acc.code)) {
                acc.copy(balance = childrenSumByParent.getValue(acc.code))
            } else {
                acc
            }
        }
    }

    fun addBudget(category: String, accountCode: String, budgetedAmount: Double, month: String) {
        val b = Budget(
            category = category,
            accountCode = accountCode,
            budgetedAmount = budgetedAmount,
            actualSpent = 0.0,
            month = month
        )
        _budgets.value = _budgets.value + b
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.accountingDao().upsertBudget(BudgetEntity.fromDomain(b))
            }
        }
        userRepository.logAudit("إضافة موازنة", "إضافة موازنة لبند $category بمبلغ $budgetedAmount ﷼")
    }

    fun updateBudgetAmount(budgetId: String, newAmount: Double) {
        var updated: Budget? = null
        _budgets.value = _budgets.value.map {
            if (it.id == budgetId) it.copy(budgetedAmount = newAmount).also { b -> updated = b } else it
        }
        updated?.let { b ->
            db?.let { database ->
                dbScope.launch(dbDispatcher) {
                    database.accountingDao().upsertBudget(BudgetEntity.fromDomain(b))
                }
            }
        }
        userRepository.logAudit("تحديث موازنة", "تعديل السقف التقديري لبند الموازنة إلى $newAmount ﷼")
    }

    fun applySnapshot(
        accounts: List<Account>?,
        vouchers: List<Voucher>?,
        journalEntries: List<JournalEntry>?,
        budgets: List<Budget>?,
        currencyRates: Map<CurrencyCode, Double>?
    ) {
        accounts?.let { _accounts.value = it }
        vouchers?.let { _vouchers.value = it }
        journalEntries?.let { _journalEntries.value = it }
        budgets?.let { _budgets.value = it }
        currencyRates?.let { _currencyRates.value = it }
    }

    fun resetToSeed() {
        _accounts.value = SeedData.accounts
        _vouchers.value = SeedData.vouchers
        _journalEntries.value = SeedData.journalEntries
        _budgets.value = SeedData.budgets
        _currencyRates.value = SeedData.currencyRates.toMap()
    }
}
