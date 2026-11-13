package com.example.fund.data.local

import androidx.room.TypeConverter
import com.example.fund.model.*
import org.json.JSONArray
import org.json.JSONObject

class FundTypeConverters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        val arr = JSONArray()
        value?.forEach { arr.put(it) }
        return arr.toString()
    }

    @TypeConverter
    fun toStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromJournalLines(lines: List<JournalLine>?): String {
        val arr = JSONArray()
        lines?.forEach { line ->
            arr.put(JSONObject().apply {
                put("accountCode", line.accountCode)
                put("accountName", line.accountName)
                put("debit", line.debit)
                put("credit", line.credit)
                put("description", line.description)
            })
        }
        return arr.toString()
    }

    @TypeConverter
    fun toJournalLines(raw: String?): List<JournalLine> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                JournalLine(
                    accountCode = obj.optString("accountCode"),
                    accountName = obj.optString("accountName"),
                    debit = obj.optDouble("debit", 0.0),
                    credit = obj.optDouble("credit", 0.0),
                    description = obj.optString("description")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter fun fromUserRole(v: UserRole): String = v.name
    @TypeConverter fun toUserRole(s: String): UserRole = runCatching { UserRole.valueOf(s) }.getOrDefault(UserRole.VIEWER)

    @TypeConverter fun fromAidType(v: AidType): String = v.name
    @TypeConverter fun toAidType(s: String): AidType = runCatching { AidType.valueOf(s) }.getOrDefault(AidType.EMERGENCY)

    @TypeConverter fun fromAidStatus(v: AidStatus): String = v.name
    @TypeConverter fun toAidStatus(s: String): AidStatus = runCatching { AidStatus.valueOf(s) }.getOrDefault(AidStatus.IN_REVIEW)

    @TypeConverter fun fromVoucherType(v: VoucherType): String = v.name
    @TypeConverter fun toVoucherType(s: String): VoucherType = runCatching { VoucherType.valueOf(s) }.getOrDefault(VoucherType.RECEIPT)

    @TypeConverter fun fromVoucherStatus(v: VoucherStatus): String = v.name
    @TypeConverter fun toVoucherStatus(s: String): VoucherStatus = runCatching { VoucherStatus.valueOf(s) }.getOrDefault(VoucherStatus.POSTED)

    @TypeConverter fun fromPaymentMethod(v: PaymentMethod): String = v.name
    @TypeConverter fun toPaymentMethod(s: String): PaymentMethod = runCatching { PaymentMethod.valueOf(s) }.getOrDefault(PaymentMethod.CASH)

    @TypeConverter fun fromCurrencyCode(v: CurrencyCode): String = v.name
    @TypeConverter fun toCurrencyCode(s: String): CurrencyCode = runCatching { CurrencyCode.valueOf(s) }.getOrDefault(CurrencyCode.YER)

    @TypeConverter fun fromAccountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(s: String): AccountType = runCatching { AccountType.valueOf(s) }.getOrDefault(AccountType.ASSET)

    @TypeConverter fun fromPovertyTier(v: PovertyTier): String = v.name
    @TypeConverter fun toPovertyTier(s: String): PovertyTier = runCatching { PovertyTier.valueOf(s) }.getOrDefault(PovertyTier.MEDIUM)

    @TypeConverter fun fromPrintFont(v: PrintFontOption): String = v.name
    @TypeConverter fun toPrintFont(s: String): PrintFontOption = runCatching { PrintFontOption.valueOf(s) }.getOrDefault(PrintFontOption.AMIRI)

    @TypeConverter fun fromPrintBorder(v: PrintBorderStyle): String = v.name
    @TypeConverter fun toPrintBorder(s: String): PrintBorderStyle = runCatching { PrintBorderStyle.valueOf(s) }.getOrDefault(PrintBorderStyle.DOUBLE_CLASSIC)

    @TypeConverter fun fromPrintPaper(v: PrintPaperSize): String = v.name
    @TypeConverter fun toPrintPaper(s: String): PrintPaperSize = runCatching { PrintPaperSize.valueOf(s) }.getOrDefault(PrintPaperSize.A4)
}
