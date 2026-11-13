package com.example

import com.example.fund.data.FundRepository
import com.example.fund.data.repository.AccountingValidator
import com.example.fund.model.CurrencyCode
import com.example.fund.model.JournalLine
import com.example.fund.model.PaymentMethod
import com.example.fund.model.VoucherType
import org.junit.Assert.*
import org.junit.Test

class AccountingValidationTest {

    @Test
    fun `validator rejects empty single-line zero-sum negative and unbalanced entries`() {
        assertFalse(AccountingValidator.validateJournalLines(emptyList()).isValid)
        assertFalse(
            AccountingValidator.validateJournalLines(
                listOf(JournalLine("1100", "الصندوق", 1000.0, 0.0))
            ).isValid
        )
        assertFalse(
            AccountingValidator.validateJournalLines(
                listOf(
                    JournalLine("1100", "الصندوق", 0.0, 0.0),
                    JournalLine("4100", "الإيرادات", 0.0, 0.0)
                )
            ).isValid
        )
        assertFalse(
            AccountingValidator.validateJournalLines(
                listOf(
                    JournalLine("1100", "الصندوق", -500.0, 0.0),
                    JournalLine("4100", "الإيرادات", 0.0, -500.0)
                )
            ).isValid
        )
        assertFalse(
            AccountingValidator.validateJournalLines(
                listOf(
                    JournalLine("1100", "الصندوق", 1000.0, 0.0),
                    JournalLine("4100", "الإيرادات", 0.0, 950.0)
                )
            ).isValid
        )
    }

    @Test
    fun `validator accepts balanced double entry and multi-line compound entries`() {
        val result = AccountingValidator.validateJournalLines(
            listOf(
                JournalLine("1100", "الصندوق", 5000.0, 0.0),
                JournalLine("1200", "بنك الكريمي", 5000.0, 0.0),
                JournalLine("4100", "الإيرادات", 0.0, 10000.0)
            )
        )
        assertTrue(result.isValid)
        assertEquals(10000.0, result.totalDebit, 0.001)
        assertEquals(10000.0, result.totalCredit, 0.001)
    }

    @Test
    fun `repository rejects unbalanced manual entry and correctly updates debit and credit normal balances`() {
        val repo = FundRepository.getInstance()
        repo.resetToDefaultSeedData()

        val unbalancedSaved = repo.createManualJournalEntry(
            lines = listOf(
                JournalLine("1100", "الصندوق", 1000.0, 0.0),
                JournalLine("4100", "الإيرادات", 0.0, 500.0)
            ),
            description = "قيد غير متوازن"
        )
        assertFalse(unbalancedSaved)

        val cashBefore = repo.accounts.value.first { it.code == "1100" }.balance
        val expenseBefore = repo.accounts.value.first { it.code == "5100" }.balance

        val payVoucher = repo.issueVoucher(
            type = VoucherType.PAYMENT,
            partyName = "مستفيد اختباري",
            amountOriginal = 10000.0,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5100",
            counterpartAccountCode = "1100",
            description = "صرف اختباري للتحقق من الرصيد الطبيعي للمصروف والأصل"
        )

        val cashAfter = repo.accounts.value.first { it.code == "1100" }.balance
        val expenseAfter = repo.accounts.value.first { it.code == "5100" }.balance

        // Payment debits Expense (+10,000) and credits Cash (-10,000)
        assertEquals(expenseBefore + 10000.0, expenseAfter, 0.001)
        assertEquals(cashBefore - 10000.0, cashAfter, 0.001)

        // Voiding the payment reverses both balances back cleanly
        repo.voidVoucher(payVoucher.id)
        assertEquals(cashBefore, repo.accounts.value.first { it.code == "1100" }.balance, 0.001)
        assertEquals(expenseBefore, repo.accounts.value.first { it.code == "5100" }.balance, 0.001)
    }
}
