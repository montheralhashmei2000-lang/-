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

    @Test
    fun `parent account balances automatically match sum of child accounts after vouchers and subaccount creation`() {
        val repo = FundRepository.getInstance()
        repo.resetToDefaultSeedData()

        val initialParentAssets = repo.accounts.value.first { it.code == "1000" }.balance
        val initialChildAssetsSum = repo.accounts.value.filter { it.parentCode == "1000" }.sumOf { it.balance }
        assertEquals(initialChildAssetsSum, initialParentAssets, 0.001)

        val receipt = repo.issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = "داعم اختباري",
            amountOriginal = 25000.0,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "1100",
            counterpartAccountCode = "4200",
            description = "تحقق من تجميع الحسابات الأبوية"
        )

        val assetsParentAfter = repo.accounts.value.first { it.code == "1000" }.balance
        val assetsLeafSumAfter = repo.accounts.value.filter { it.parentCode == "1000" }.sumOf { it.balance }
        assertEquals(assetsLeafSumAfter, assetsParentAfter, 0.001)
        assertEquals(initialParentAssets + 25000.0, assetsParentAfter, 0.001)

        val revParentAfter = repo.accounts.value.first { it.code == "4000" }.balance
        val revLeafSumAfter = repo.accounts.value.filter { it.parentCode == "4000" }.sumOf { it.balance }
        assertEquals(revLeafSumAfter, revParentAfter, 0.001)

        // Adding a new sub-account with opening balance rolls into parent 1000
        assertTrue(
            repo.addSubAccount(
                code = "1500",
                name = "حساب عهدة اختباري",
                type = com.example.fund.model.AccountType.ASSET,
                parentCode = "1000",
                openingBalance = 15000.0
            )
        )
        val assetsParentWithSub = repo.accounts.value.first { it.code == "1000" }.balance
        val assetsLeafWithSub = repo.accounts.value.filter { it.parentCode == "1000" }.sumOf { it.balance }
        assertEquals(assetsLeafWithSub, assetsParentWithSub, 0.001)
        assertEquals(assetsParentAfter + 15000.0, assetsParentWithSub, 0.001)

        // Voiding receipt keeps parent == leaf sum
        repo.voidVoucher(receipt.id)
        val assetsParentVoided = repo.accounts.value.first { it.code == "1000" }.balance
        val assetsLeafVoided = repo.accounts.value.filter { it.parentCode == "1000" }.sumOf { it.balance }
        assertEquals(assetsLeafVoided, assetsParentVoided, 0.001)
    }

    @Test
    fun `sequential numbering never collides and invalid memberId in addSubscription throws IllegalArgumentException`() {
        val repo = FundRepository.getInstance()
        repo.resetToDefaultSeedData()

        val v1 = repo.issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = "عضو 1",
            amountOriginal = 1000.0,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "1100",
            counterpartAccountCode = "4100",
            description = "سند أول"
        )
        val v2 = repo.issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = "عضو 2",
            amountOriginal = 1000.0,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "1100",
            counterpartAccountCode = "4100",
            description = "سند ثان"
        )
        assertNotEquals(v1.voucherNo, v2.voucherNo)
        assertNotEquals(v1.linkedJournalEntryId, v2.linkedJournalEntryId)

        val existingMemberId = repo.members.value.first().id
        val s1 = repo.addSubscription(existingMemberId, 5000.0, CurrencyCode.YER, "2026-11", PaymentMethod.CASH, "", "")
        val s2 = repo.addSubscription(existingMemberId, 5000.0, CurrencyCode.YER, "2026-12", PaymentMethod.CASH, "", "")
        assertNotEquals(s1.receiptNo, s2.receiptNo)

        val a1 = repo.addAidRequest(existingMemberId, com.example.fund.model.AidType.MEDICAL, 20000.0, "طلب 1")
        val a2 = repo.addAidRequest(existingMemberId, com.example.fund.model.AidType.EMERGENCY, 15000.0, "طلب 2")
        assertNotEquals(a1.requestNo, a2.requestNo)

        try {
            repo.addSubscription("non-existent-member-id", 5000.0, CurrencyCode.YER, "2026-11", PaymentMethod.CASH, "", "")
            fail("Expected IllegalArgumentException for missing memberId")
        } catch (ex: IllegalArgumentException) {
            assertTrue(ex.message?.contains("non-existent-member-id") == true)
        }
    }
}
