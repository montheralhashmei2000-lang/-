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
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PreparedVoucherPosting(
    val voucher: Voucher,
    val journalEntry: JournalEntry,
    val updatedAccounts: List<Account>,
    val affectedCodes: Set<String>,
    val updatedBudgets: List<Budget> = emptyList(),
    val affectedBudgetIds: Set<String> = emptySet()
)

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
        val dbVouchers = database.accountingDao().observeVouchers().first().map { it.toDomain() }
        val dbJournals = database.accountingDao().observeJournalEntries().first().map { it.toDomain() }
        val dbBudgets = database.accountingDao().observeBudgets().first().map { it.toDomain() }
        val dbRates = database.accountingDao().observeCurrencyRates().first()

        val isDbInitialized = database.settingsDao().getFundInfo() != null || dbAccounts.isNotEmpty()
        if (isDbInitialized) {
            _accounts.value = dbAccounts
            _vouchers.value = dbVouchers
            _journalEntries.value = dbJournals
            _budgets.value = dbBudgets
            if (dbRates.isNotEmpty()) {
                _currencyRates.value = dbRates.associate { it.currencyCode to it.rate }
            }
        }
    }

    fun convertToYer(amountOriginal: Double, currency: CurrencyCode): Double {
        AccountingValidator.requireNonNegativeFiniteAmount(amountOriginal, "المبلغ الأصلي")
        val rate = _currencyRates.value[currency] ?: currency.defaultRate
        AccountingValidator.requirePositiveFiniteAmount(rate, "سعر الصرف")
        val raw = amountOriginal * rate
        require(raw.isFinite()) { "ناتج التحويل بالريال غير صالح." }
        return AccountingValidator.roundMoney(raw)
    }

    fun updateCurrencyRate(currency: CurrencyCode, rate: Double) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تعديل سعر الصرف")
        AccountingValidator.requirePositiveFiniteAmount(rate, "سعر الصرف")
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

    private fun nextVoucherNo(extraOffset: Int = 0): String {
        val customPrefix = settingsRepository.appSettings.value.voucherNoPrefix.ifBlank { "V" }
        val yearPrefix = "$customPrefix-${Calendar.getInstance().get(Calendar.YEAR)}-"
        val maxExisting = _vouchers.value.mapNotNull { v ->
            v.voucherNo.substringAfterLast("-").toIntOrNull()
        }.maxOrNull() ?: 0
        val nextNum = maxOf(maxExisting, _vouchers.value.size) + 1 + extraOffset
        return String.format(Locale.US, "%s%03d", yearPrefix, nextNum)
    }

    private fun nextJournalNo(extraOffset: Int = 0): String {
        val maxExisting = _journalEntries.value.mapNotNull { je ->
            val clean = je.entryNo.removeSuffix("-R")
            clean.substringAfterLast("-").toIntOrNull()
        }.maxOrNull() ?: 0
        val nextNum = maxOf(maxExisting, _journalEntries.value.size) + 1 + extraOffset
        return String.format(Locale.US, "JE-%06d", nextNum)
    }

    /**
     * Validates all accounting invariants, account existence, account types, negative treasury rules,
     * and constructs the [PreparedVoucherPosting] without mutating StateFlows yet.
     */
    fun prepareVoucherPosting(
        type: VoucherType,
        partyName: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        paymentMethod: PaymentMethod,
        accountCode: String,
        counterpartAccountCode: String,
        description: String
    ): PreparedVoucherPosting {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إصدار سند مالي")
        AccountingValidator.requirePositiveFiniteAmount(amountOriginal, "مبلغ السند")
        require(partyName.isNotBlank()) { "اسم الطرف المستلم/المودع مطلوب." }

        val rate = _currencyRates.value[currency] ?: currency.defaultRate
        AccountingValidator.requirePositiveFiniteAmount(rate, "سعر الصرف")
        val amountYer = AccountingValidator.roundMoney(amountOriginal * rate)
        AccountingValidator.requirePositiveFiniteAmount(amountYer, "المبلغ بالريال اليمني")

        require(accountCode.isNotBlank() && counterpartAccountCode.isNotBlank()) {
            "يجب تحديد الحساب المالي والحساب المقابل."
        }
        require(accountCode != counterpartAccountCode) {
            "لا يمكن إصدار سند بين الحساب ونفسه ($accountCode)."
        }

        val currentAccounts = _accounts.value
        val acc = currentAccounts.find { it.code == accountCode }
            ?: throw IllegalArgumentException("الحساب المالي ($accountCode) غير موجود في شجرة الحسابات.")
        val counterpartAcc = currentAccounts.find { it.code == counterpartAccountCode }
            ?: throw IllegalArgumentException("الحساب المقابل ($counterpartAccountCode) غير موجود في شجرة الحسابات.")

        // Disallow posting directly to parent summary accounts
        require(currentAccounts.none { it.parentCode == acc.code }) {
            "لا يمكن الترحيل المباشر إلى الحساب الأب (${acc.code} - ${acc.name})؛ اختر حساباً فرعياً."
        }
        require(currentAccounts.none { it.parentCode == counterpartAcc.code }) {
            "لا يمكن الترحيل المباشر إلى الحساب الأب (${counterpartAcc.code} - ${counterpartAcc.name})؛ اختر حساباً فرعياً."
        }

        // Validate account nature compatibility per voucher type
        if (type == VoucherType.RECEIPT) {
            require(acc.type != AccountType.EXPENSE && counterpartAcc.type != AccountType.EXPENSE) {
                "سند القبض لا يقبل حسابات المصروفات."
            }
        } else {
            require(acc.type != AccountType.REVENUE && counterpartAcc.type != AccountType.REVENUE) {
                "سند الصرف لا يقبل حسابات الإيرادات."
            }
            if (!settingsRepository.appSettings.value.allowNegativeTreasury && counterpartAcc.type == AccountType.ASSET) {
                check(counterpartAcc.balance >= amountYer) {
                    "رصيد حساب الخزينة (${counterpartAcc.name}: ${counterpartAcc.balance} ﷼) لا يكفي لصرف مبلغ $amountYer ﷼."
                }
            }
        }

        val voucherNo = nextVoucherNo()
        val journalNo = nextJournalNo()
        val today = nowStr()
        val words = TafqeetEngine.convertToWords(amountYer, "ريال يمني", "فلس")

        val lines = if (type == VoucherType.RECEIPT) {
            listOf(
                JournalLine(accountCode, acc.name, amountYer, 0.0, "مدين - إيداع بموجب $voucherNo"),
                JournalLine(counterpartAccountCode, counterpartAcc.name, 0.0, amountYer, "دائن - ${counterpartAcc.name}")
            )
        } else {
            listOf(
                JournalLine(accountCode, acc.name, amountYer, 0.0, "مدين - ${acc.name}"),
                JournalLine(counterpartAccountCode, counterpartAcc.name, 0.0, amountYer, "دائن - صرف بموجب $voucherNo")
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
            partyName = partyName.trim(),
            amountOriginal = amountOriginal,
            currency = currency,
            exchangeRate = rate,
            amountYer = amountYer,
            amountWords = words,
            paymentMethod = paymentMethod,
            accountCode = accountCode,
            accountName = acc.name,
            counterpartAccountCode = counterpartAccountCode,
            counterpartAccountName = counterpartAcc.name,
            description = description,
            date = today,
            linkedJournalEntryId = journalNo,
            receivedOrPaidBy = ps.sig1DefaultName.ifBlank { partyName.trim() },
            accountantName = ps.sig2DefaultName.ifBlank { "أمين الصندوق / المحاسب" },
            auditorName = ps.sig3DefaultName.ifBlank { "المراجع المالي" },
            managerName = ps.sig4DefaultName.ifBlank { "رئيس مجلس الإدارة" }
        )

        val updatedAccounts = applyLinesToAccounts(currentAccounts, lines)
        val (updatedBudgets, affectedBudgetIds) = applyLinesToBudgets(_budgets.value, currentAccounts, lines)
        return PreparedVoucherPosting(
            voucher = newVoucher,
            journalEntry = journalEntry,
            updatedAccounts = updatedAccounts,
            affectedCodes = setOf(accountCode, counterpartAccountCode),
            updatedBudgets = updatedBudgets,
            affectedBudgetIds = affectedBudgetIds
        )
    }

    /**
     * Persists [prepared] inside an already-open Room `@Transaction`, verifying unique numbers
     * and account existence in SQLite before inserting.
     */
    suspend fun persistVoucherInTransaction(database: FundDatabase, prepared: PreparedVoucherPosting): PreparedVoucherPosting {
        val dao = database.accountingDao()
        // Resolve unique voucherNo & journalNo atomically inside the SQLite transaction
        var resolvedVoucher = prepared.voucher
        var resolvedJournal = prepared.journalEntry

        var offset = 0
        while (dao.countVouchersByNo(resolvedVoucher.voucherNo) > 0) {
            offset++
            resolvedVoucher = resolvedVoucher.copy(voucherNo = nextVoucherNo(offset))
        }
        var jeOffset = 0
        while (dao.countJournalEntriesByNo(resolvedJournal.entryNo) > 0) {
            jeOffset++
            resolvedJournal = resolvedJournal.copy(entryNo = nextJournalNo(jeOffset))
        }
        if (resolvedJournal.entryNo != resolvedVoucher.linkedJournalEntryId ||
            resolvedVoucher.voucherNo != prepared.voucher.voucherNo
        ) {
            resolvedVoucher = resolvedVoucher.copy(linkedJournalEntryId = resolvedJournal.entryNo)
            resolvedJournal = resolvedJournal.copy(originalVoucherNo = resolvedVoucher.voucherNo)
        }

        val jeCount = _journalEntries.value.size + 1
        val vCount = _vouchers.value.size + 1
        dao.upsertJournalEntry(JournalEntryEntity.fromDomain(resolvedJournal, -jeCount))
        dao.upsertVoucher(VoucherEntity.fromDomain(resolvedVoucher, -vCount))

        val parentCodes = prepared.updatedAccounts
            .filter { it.code in prepared.affectedCodes }
            .mapNotNull { it.parentCode }
            .toSet()
        prepared.updatedAccounts
            .filter { it.code in prepared.affectedCodes || it.code in parentCodes }
            .forEach { acc ->
                dao.upsertAccount(AccountEntity.fromDomain(acc))
            }

        if (prepared.affectedBudgetIds.isNotEmpty()) {
            val changedBudgets = prepared.updatedBudgets.filter { it.id in prepared.affectedBudgetIds }
            if (changedBudgets.isNotEmpty()) {
                dao.upsertBudgets(changedBudgets.map { BudgetEntity.fromDomain(it) })
            }
        }

        return prepared.copy(voucher = resolvedVoucher, journalEntry = resolvedJournal)
    }

    fun commitVoucherInMemory(prepared: PreparedVoucherPosting) {
        _accounts.value = prepared.updatedAccounts
        if (prepared.affectedBudgetIds.isNotEmpty() && prepared.updatedBudgets.isNotEmpty()) {
            _budgets.value = prepared.updatedBudgets
        }
        _journalEntries.value = listOf(prepared.journalEntry) + _journalEntries.value
        _vouchers.value = listOf(prepared.voucher) + _vouchers.value
        userRepository.logAudit(
            "إصدار ${prepared.voucher.type.title}",
            "إصدار ${prepared.voucher.voucherNo} للمكرم ${prepared.voucher.partyName} بمبلغ ${prepared.voucher.amountYer} ﷼ مع القيد ${prepared.journalEntry.entryNo}"
        )
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
        var prepared = prepareVoucherPosting(
            type = type,
            partyName = partyName,
            amountOriginal = amountOriginal,
            currency = currency,
            paymentMethod = paymentMethod,
            accountCode = accountCode,
            counterpartAccountCode = counterpartAccountCode,
            description = description
        )

        db?.let { database ->
            prepared = runBlocking(dbDispatcher) {
                database.withTransaction {
                    persistVoucherInTransaction(database, prepared)
                }
            }
        }

        commitVoucherInMemory(prepared)
        return prepared.voucher
    }

    fun transferBetweenTreasuryAccounts(
        fromAccountCode: String,
        toAccountCode: String,
        amountYer: Double,
        notes: String
    ): Boolean {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تحويل مالي داخلي")
        if (fromAccountCode == toAccountCode || !amountYer.isFinite() || amountYer <= 0.0) return false
        val fromAcc = _accounts.value.find { it.code == fromAccountCode } ?: return false
        val toAcc = _accounts.value.find { it.code == toAccountCode } ?: return false
        if (_accounts.value.any { it.parentCode == fromAcc.code || it.parentCode == toAcc.code }) return false
        if (!settingsRepository.appSettings.value.allowNegativeTreasury && fromAcc.balance < amountYer) return false

        val today = nowStr()
        val desc = notes.ifBlank { "تحويل سيولة داخلي من ${fromAcc.name} إلى ${toAcc.name}" }
        var journalNo = nextJournalNo()
        val lines = listOf(
            JournalLine(toAccountCode, toAcc.name, amountYer, 0.0, "مدين - إيداع تحويل داخلي"),
            JournalLine(fromAccountCode, fromAcc.name, 0.0, amountYer, "دائن - سحب تحويل داخلي")
        )

        if (!AccountingValidator.validateJournalLines(lines, requireBalanced = true).isValid) return false

        var entry = JournalEntry(
            entryNo = journalNo,
            date = today,
            description = desc,
            lines = lines,
            isPosted = true
        )
        val updatedAccounts = applyLinesToAccounts(_accounts.value, lines)

        db?.let { database ->
            val jeCount = _journalEntries.value.size + 1
            entry = runBlocking(dbDispatcher) {
                database.withTransaction {
                    var offset = 0
                    while (database.accountingDao().countJournalEntriesByNo(entry.entryNo) > 0) {
                        offset++
                        journalNo = nextJournalNo(offset)
                        entry = entry.copy(entryNo = journalNo)
                    }
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
                    entry
                }
            }
        }

        _accounts.value = updatedAccounts
        _journalEntries.value = listOf(entry) + _journalEntries.value
        userRepository.logAudit(
            "تحويل مالي داخلي",
            "تحويل مبلغ ${amountYer.toLong()} ﷼ من ${fromAcc.name} إلى ${toAcc.name} بموجب القيد ${entry.entryNo}"
        )
        return true
    }

    fun voidVoucher(
        voucherId: String,
        onTransactionRollback: (suspend (FundDatabase, Voucher) -> Unit)? = null,
        onMemoryRollback: ((Voucher) -> Unit)? = null
    ) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إلغاء سند مالي")
        val voucher = _vouchers.value.find { it.id == voucherId || it.voucherNo == voucherId } ?: return
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
        val (updatedBudgets, affectedBudgetIds) = applyLinesToBudgets(_budgets.value, _accounts.value, lines)

        db?.let { database ->
            val jeCount = _journalEntries.value.size + 1
            runBlocking(dbDispatcher) {
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
                    if (affectedBudgetIds.isNotEmpty()) {
                        val changedBudgets = updatedBudgets.filter { it.id in affectedBudgetIds }
                        if (changedBudgets.isNotEmpty()) {
                            database.accountingDao().upsertBudgets(changedBudgets.map { BudgetEntity.fromDomain(it) })
                        }
                    }
                    onTransactionRollback?.invoke(database, voucher)
                }
            }
        }

        _accounts.value = updatedAccounts
        if (affectedBudgetIds.isNotEmpty()) {
            _budgets.value = updatedBudgets
        }
        _vouchers.value = _vouchers.value.map { if (it.id == voucher.id) updatedVoucher else it }
        _journalEntries.value = listOf(revEntry) + _journalEntries.value
        onMemoryRollback?.invoke(voucher)
        userRepository.logAudit("إلغاء سند", "تم إلغاء السند ${voucher.voucherNo} بنجاح وتوليد قيد عكسي $revJournalNo")
    }

    fun addSubAccount(
        code: String,
        name: String,
        type: AccountType,
        parentCode: String,
        openingBalance: Double = 0.0
    ): Boolean {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إضافة حساب محاسبي فرعي")
        if (code.isBlank() || name.isBlank() || _accounts.value.any { it.code == code }) return false
        if (!openingBalance.isFinite() || openingBalance < 0.0) return false
        if (parentCode.isNotBlank() && _accounts.value.none { it.code == parentCode }) return false

        val isDebitNormal = type == AccountType.ASSET || type == AccountType.EXPENSE
        val newAcc = Account(
            code = code.trim(),
            name = name.trim(),
            type = type,
            isDebitNormal = isDebitNormal,
            balance = openingBalance,
            parentCode = parentCode.ifBlank { null }
        )
        val nextList = recalculateParentBalances((_accounts.value + newAcc).sortedBy { it.code })

        db?.let { database ->
            runBlocking(dbDispatcher) {
                database.withTransaction {
                    database.accountingDao().upsertAccount(AccountEntity.fromDomain(newAcc))
                    nextList.find { it.code == parentCode }?.let { parentAcc ->
                        database.accountingDao().upsertAccount(AccountEntity.fromDomain(parentAcc))
                    }
                }
            }
        }

        _accounts.value = nextList
        userRepository.logAudit("إضافة حساب محاسبي", "إضافة حساب فرعي ($code - $name) تحت ($parentCode)")
        return true
    }

    fun createManualJournalEntry(lines: List<JournalLine>, description: String): Boolean {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إنشاء قيد يومية يدوي")
        if (description.isBlank()) return false
        // Always enforce double-entry balance to preserve Trial Balance integrity
        val validation = AccountingValidator.validateJournalLines(lines, requireBalanced = true)
        if (!validation.isValid) return false

        // All referenced accounts must exist and not be header/parent accounts
        val currentAccounts = _accounts.value
        val accCodes = currentAccounts.map { it.code }.toSet()
        if (lines.any { it.accountCode !in accCodes || currentAccounts.any { child -> child.parentCode == it.accountCode } }) {
            return false
        }

        var entryNo = nextJournalNo()
        var entry = JournalEntry(
            entryNo = entryNo,
            date = nowStr(),
            description = description,
            lines = lines,
            isPosted = true
        )

        val updatedAccounts = applyLinesToAccounts(currentAccounts, lines)
        val (updatedBudgets, affectedBudgetIds) = applyLinesToBudgets(_budgets.value, currentAccounts, lines)

        db?.let { database ->
            val jeCount = _journalEntries.value.size + 1
            entry = runBlocking(dbDispatcher) {
                database.withTransaction {
                    var offset = 0
                    while (database.accountingDao().countJournalEntriesByNo(entry.entryNo) > 0) {
                        offset++
                        entryNo = nextJournalNo(offset)
                        entry = entry.copy(entryNo = entryNo)
                    }
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
                    if (affectedBudgetIds.isNotEmpty()) {
                        val changedBudgets = updatedBudgets.filter { it.id in affectedBudgetIds }
                        if (changedBudgets.isNotEmpty()) {
                            database.accountingDao().upsertBudgets(changedBudgets.map { BudgetEntity.fromDomain(it) })
                        }
                    }
                    entry
                }
            }
        }

        _accounts.value = updatedAccounts
        if (affectedBudgetIds.isNotEmpty()) {
            _budgets.value = updatedBudgets
        }
        _journalEntries.value = listOf(entry) + _journalEntries.value
        userRepository.logAudit("قيد تسوية يدوي", "إنشاء قيد يومية متوازن ${entry.entryNo} بمبلغ ${validation.totalDebit} ﷼")
        return true
    }

    private fun applyLinesToBudgets(
        currentBudgets: List<Budget>,
        accountsList: List<Account>,
        lines: List<JournalLine>
    ): Pair<List<Budget>, Set<String>> {
        if (currentBudgets.isEmpty()) return currentBudgets to emptySet()
        val accMap = accountsList.associateBy { it.code }
        val expenseDeltaByCode = mutableMapOf<String, Double>()
        for (line in lines) {
            val acc = accMap[line.accountCode] ?: continue
            if (acc.type == AccountType.EXPENSE) {
                val delta = line.debit - line.credit
                if (delta != 0.0) {
                    expenseDeltaByCode[acc.code] = (expenseDeltaByCode[acc.code] ?: 0.0) + delta
                }
            }
        }
        if (expenseDeltaByCode.isEmpty()) return currentBudgets to emptySet()
        val affectedIds = mutableSetOf<String>()
        val updated = currentBudgets.map { b ->
            val delta = expenseDeltaByCode[b.accountCode] ?: 0.0
            if (delta != 0.0) {
                affectedIds.add(b.id)
                b.copy(actualSpent = maxOf(0.0, b.actualSpent + delta))
            } else {
                b
            }
        }
        return updated to affectedIds
    }

    private fun applyLinesToAccounts(currentAccounts: List<Account>, lines: List<JournalLine>): List<Account> {
        val deltaByCode = mutableMapOf<String, Double>()
        val accMap = currentAccounts.associateBy { it.code }
        for (line in lines) {
            val acc = accMap[line.accountCode]
                ?: throw IllegalArgumentException("الحساب (${line.accountCode}) غير موجود.")
            val delta = AccountingValidator.computeBalanceDelta(acc, line)
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
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إضافة موازنة")
        AccountingValidator.requireNonNegativeFiniteAmount(budgetedAmount, "المبلغ المقدر للموازنة")
        require(_accounts.value.any { it.code == accountCode }) {
            "رمز الحساب ($accountCode) غير موجود في شجرة الحسابات."
        }
        val b = Budget(
            category = category,
            accountCode = accountCode,
            budgetedAmount = budgetedAmount,
            actualSpent = 0.0,
            month = month
        )
        db?.let { database ->
            runBlocking(dbDispatcher) {
                database.accountingDao().upsertBudget(BudgetEntity.fromDomain(b))
            }
        }
        _budgets.value = _budgets.value + b
        userRepository.logAudit("إضافة موازنة", "إضافة موازنة لبند $category بمبلغ $budgetedAmount ﷼")
    }

    fun updateBudgetAmount(budgetId: String, newAmount: Double) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تعديل الموازنة")
        AccountingValidator.requireNonNegativeFiniteAmount(newAmount, "السقف الجديد للموازنة")
        val target = _budgets.value.find { it.id == budgetId } ?: return
        val updated = target.copy(budgetedAmount = newAmount)
        db?.let { database ->
            runBlocking(dbDispatcher) {
                database.accountingDao().upsertBudget(BudgetEntity.fromDomain(updated))
            }
        }
        _budgets.value = _budgets.value.map { if (it.id == budgetId) updated else it }
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
