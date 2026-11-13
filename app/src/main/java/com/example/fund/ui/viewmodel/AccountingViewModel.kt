package com.example.fund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import kotlinx.coroutines.flow.StateFlow

class AccountingViewModel : ViewModel() {

    private val coordinator = FundRepository.getInstance()
    private val accountingRepo = coordinator.accountingRepository
    private val settingsRepo = coordinator.settingsRepository
    private val userRepo = coordinator.userRepository

    val accounts: StateFlow<List<Account>> = accountingRepo.accounts
    val vouchers: StateFlow<List<Voucher>> = accountingRepo.vouchers
    val journalEntries: StateFlow<List<JournalEntry>> = accountingRepo.journalEntries
    val budgets: StateFlow<List<Budget>> = accountingRepo.budgets
    val currencyRates: StateFlow<Map<CurrencyCode, Double>> = accountingRepo.currencyRates
    val fundInfo: StateFlow<FundInfo> = settingsRepo.fundInfo
    val currentUser: StateFlow<AppUser> = userRepo.currentUser
    val screenFormat: StateFlow<DisplayFormatConfig> = settingsRepo.screenFormat
    val printFormat: StateFlow<DisplayFormatConfig> = settingsRepo.printFormat

    fun issueVoucher(
        type: VoucherType,
        partyName: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        paymentMethod: PaymentMethod,
        accountCode: String,
        counterpartAccountCode: String,
        description: String
    ): Voucher = accountingRepo.issueVoucher(
        type, partyName, amountOriginal, currency, paymentMethod, accountCode, counterpartAccountCode, description
    )

    fun voidVoucher(voucherId: String) = accountingRepo.voidVoucher(voucherId)

    fun createManualJournalEntry(lines: List<JournalLine>, description: String): Boolean =
        accountingRepo.createManualJournalEntry(lines, description)

    fun transferBetweenTreasuryAccounts(
        fromAccountCode: String,
        toAccountCode: String,
        amountYer: Double,
        notes: String
    ): Boolean = accountingRepo.transferBetweenTreasuryAccounts(fromAccountCode, toAccountCode, amountYer, notes)

    fun addSubAccount(
        code: String,
        name: String,
        type: AccountType,
        parentCode: String,
        openingBalance: Double = 0.0
    ): Boolean = accountingRepo.addSubAccount(code, name, type, parentCode, openingBalance)

    fun addBudget(category: String, accountCode: String, budgetedAmount: Double, month: String) =
        accountingRepo.addBudget(category, accountCode, budgetedAmount, month)

    fun updateBudgetAmount(budgetId: String, newAmount: Double) =
        accountingRepo.updateBudgetAmount(budgetId, newAmount)
}
