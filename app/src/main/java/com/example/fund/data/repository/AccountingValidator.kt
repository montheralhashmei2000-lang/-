package com.example.fund.data.repository

import com.example.fund.model.Account
import com.example.fund.model.JournalLine
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

data class JournalValidationResult(
    val isValid: Boolean,
    val totalDebit: Double,
    val totalCredit: Double,
    val errorMessage: String? = null
)

object AccountingValidator {

    const val BALANCE_TOLERANCE = 0.01

    fun roundMoney(amount: Double): Double {
        if (!amount.isFinite()) return amount
        return BigDecimal.valueOf(amount)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun isWithinBalanceTolerance(a: Double, b: Double): Boolean {
        return a.isFinite() && b.isFinite() && abs(a - b) < BALANCE_TOLERANCE
    }

    fun requirePositiveFiniteAmount(value: Double, label: String = "المبلغ") {
        require(value.isFinite() && value > 0.0) {
            "$label يجب أن يكون رقماً مالياً حقيقياً ومحدداً أكبر من صفر."
        }
    }

    fun requireNonNegativeFiniteAmount(value: Double, label: String = "القيمة") {
        require(value.isFinite() && value >= 0.0) {
            "$label يجب أن تكون قيمة رقمية محددة وغير سالبة."
        }
    }

    fun validateJournalLines(
        lines: List<JournalLine>,
        requireBalanced: Boolean = true
    ): JournalValidationResult {
        if (lines.size < 2) {
            val debit = lines.sumOf { if (it.debit.isFinite()) it.debit else 0.0 }
            val credit = lines.sumOf { if (it.credit.isFinite()) it.credit else 0.0 }
            return JournalValidationResult(
                isValid = false,
                totalDebit = debit,
                totalCredit = credit,
                errorMessage = "القيد المحاسبي المزدوج يجب أن يحتوي على طرفين (مدين ودائن) على الأقل."
            )
        }

        for (line in lines) {
            if (line.accountCode.isBlank()) {
                return JournalValidationResult(false, 0.0, 0.0, "رمز الحساب فارغ في أحد سطور القيد.")
            }
            if (!line.debit.isFinite() || !line.credit.isFinite() || line.debit < 0.0 || line.credit < 0.0) {
                return JournalValidationResult(false, 0.0, 0.0, "لا يمكن أن تكون قيم المدين أو الدائن سالبة أو غير منتهية (NaN/Infinity).")
            }
            if (line.debit == 0.0 && line.credit == 0.0) {
                return JournalValidationResult(false, 0.0, 0.0, "يوجد سطر قيد بقيمة صفر في المدين والدائن معاً.")
            }
            if (line.debit > 0.0 && line.credit > 0.0) {
                return JournalValidationResult(false, 0.0, 0.0, "السطر الواحد يجب أن يكون إما مديناً أو دائناً وليس كليهما.")
            }
        }

        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }

        if (!totalDebit.isFinite() || !totalCredit.isFinite()) {
            return JournalValidationResult(false, 0.0, 0.0, "إجمالي المدين أو الدائن تجاوز الحدود الحسابية الصالحة.")
        }

        if (totalDebit <= 0.0 || totalCredit <= 0.0) {
            return JournalValidationResult(
                isValid = false,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                errorMessage = "إجمالي المدين والدائن يجب أن يكون أكبر من صفر."
            )
        }

        if (requireBalanced && abs(totalDebit - totalCredit) >= BALANCE_TOLERANCE) {
            return JournalValidationResult(
                isValid = false,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                errorMessage = "القيد غير متوازن: إجمالي المدين (${totalDebit}) لا يساوي إجمالي الدائن (${totalCredit})."
            )
        }

        return JournalValidationResult(
            isValid = true,
            totalDebit = totalDebit,
            totalCredit = totalCredit
        )
    }

    /**
     * Computes signed balance delta for [account] based on its normal side:
     * - Debit-normal accounts (Asset / Expense): debit - credit
     * - Credit-normal accounts (Liability / Equity / Revenue): credit - debit
     */
    fun computeBalanceDelta(account: Account, line: JournalLine): Double {
        return if (account.isDebitNormal) {
            line.debit - line.credit
        } else {
            line.credit - line.debit
        }
    }
}
