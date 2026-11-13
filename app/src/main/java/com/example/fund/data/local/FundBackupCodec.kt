package com.example.fund.data.local

import com.example.fund.data.SeedData
import com.example.fund.model.*
import com.example.fund.security.SecurityUtils
import org.json.JSONArray
import org.json.JSONObject

data class ParsedDatabaseSnapshot(
    val fundInfo: FundInfo? = null,
    val regions: List<RegionGroup>? = null,
    val appSettings: AppSettings? = null,
    val printSettings: PrintSettings? = null,
    val members: List<Member>? = null,
    val beneficiaries: List<Beneficiary>? = null,
    val accounts: List<Account>? = null,
    val vouchers: List<Voucher>? = null,
    val journalEntries: List<JournalEntry>? = null,
    val subscriptions: List<Subscription>? = null,
    val aidRequests: List<AidRequest>? = null,
    val budgets: List<Budget>? = null,
    val donors: List<Donor>? = null,
    val campaigns: List<Campaign>? = null,
    val periodicAids: List<PeriodicAid>? = null,
    val inKindItems: List<InKindItem>? = null,
    val inKindMovements: List<InKindMovement>? = null,
    val schedulerEvents: List<SchedulerEvent>? = null,
    val internalMessages: List<InternalMessage>? = null,
    val currencyRates: Map<CurrencyCode, Double>? = null,
    val syncQueue: List<SyncQueueItem>? = null,
    val exportDate: String = ""
)

object FundBackupCodec {

    fun exportToJson(
        exportDate: String,
        fundInfo: FundInfo,
        regions: List<RegionGroup>,
        appSettings: AppSettings,
        printSettings: PrintSettings,
        members: List<Member>,
        beneficiaries: List<Beneficiary>,
        accounts: List<Account>,
        vouchers: List<Voucher>,
        journalEntries: List<JournalEntry>,
        subscriptions: List<Subscription>,
        aidRequests: List<AidRequest>,
        budgets: List<Budget>,
        donors: List<Donor>,
        campaigns: List<Campaign>,
        periodicAids: List<PeriodicAid>,
        inKindItems: List<InKindItem>,
        inKindMovements: List<InKindMovement>,
        schedulerEvents: List<SchedulerEvent>,
        internalMessages: List<InternalMessage>,
        currencyRates: Map<CurrencyCode, Double>,
        syncQueue: List<SyncQueueItem>
    ): String {
        val root = JSONObject()
        root.put("version", "9.0")
        root.put("exportDate", exportDate)

        // FundInfo
        root.put("fundInfo", JSONObject().apply {
            put("name", fundInfo.name)
            put("subtitle", fundInfo.subtitle)
            put("licenseNo", fundInfo.licenseNo)
            put("address", fundInfo.address)
            put("phone", fundInfo.phone)
            put("email", fundInfo.email)
            put("bankAccountKareemi", fundInfo.bankAccountKareemi)
            put("bankAccountCAC", fundInfo.bankAccountCAC)
            put("maxAidCeilingYer", fundInfo.maxAidCeilingYer)
            put("fiscalYear", fundInfo.fiscalYear)
            put("customLogoBase64", fundInfo.customLogoBase64)
        })

        // Regions & Villages
        root.put("regions", JSONArray().apply {
            regions.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("villages", JSONArray(r.villages))
                })
            }
        })

        // AppSettings
        root.put("appSettings", JSONObject().apply {
            put("monthlySubscriptionFeeYer", appSettings.monthlySubscriptionFeeYer)
            put("autoSuspendMonths", appSettings.autoSuspendMonths)
            put("requireNationalId", appSettings.requireNationalId)
            put("memberNoPrefix", appSettings.memberNoPrefix)
            put("requireReviewerApproval", appSettings.requireReviewerApproval)
            put("periodicAidDisburseDay", appSettings.periodicAidDisburseDay)
            put("lowStockThreshold", appSettings.lowStockThreshold)
            put("defaultWarehouseName", appSettings.defaultWarehouseName)
            put("voucherNoPrefix", appSettings.voucherNoPrefix)
            put("defaultTreasuryAccountCode", appSettings.defaultTreasuryAccountCode)
            put("allowNegativeTreasury", appSettings.allowNegativeTreasury)
            put("enforceBalancedJournal", appSettings.enforceBalancedJournal)
            put("budgetAlertPercent", appSettings.budgetAlertPercent)
            put("autoPrintVoucher", appSettings.autoPrintVoucher)
            put("allowAnonymousDonors", appSettings.allowAnonymousDonors)
            put("defaultEventLocation", appSettings.defaultEventLocation)
            put("strictRolePermissions", appSettings.strictRolePermissions)
            put("enableAuditTrail", appSettings.enableAuditTrail)
        })

        // PrintSettings
        root.put("printSettings", JSONObject().apply {
            put("showCountryHeader", printSettings.showCountryHeader)
            put("countryHeaderText", printSettings.countryHeaderText)
            put("showDepartmentHeader", printSettings.showDepartmentHeader)
            put("departmentHeaderText", printSettings.departmentHeaderText)
            put("showLogoInPrint", printSettings.showLogoInPrint)
            put("showLicenseAndAddress", printSettings.showLicenseAndAddress)
            put("receiptVoucherTitle", printSettings.receiptVoucherTitle)
            put("paymentVoucherTitle", printSettings.paymentVoucherTitle)
            put("journalVoucherTitle", printSettings.journalVoucherTitle)
            put("financialStatementSubtitle", printSettings.financialStatementSubtitle)
            put("analyticalReportSubtitle", printSettings.analyticalReportSubtitle)
            put("receiptPartyLabel", printSettings.receiptPartyLabel)
            put("paymentPartyLabel", printSettings.paymentPartyLabel)
            put("amountWordsLabel", printSettings.amountWordsLabel)
            put("paymentMethodLabel", printSettings.paymentMethodLabel)
            put("accountLabel", printSettings.accountLabel)
            put("descriptionLabel", printSettings.descriptionLabel)
            put("fontFamily", printSettings.fontFamily.name)
            put("headerFontSizeSp", printSettings.headerFontSizeSp)
            put("bodyFontSizeSp", printSettings.bodyFontSizeSp)
            put("signatureFontSizeSp", printSettings.signatureFontSizeSp)
            put("borderStyle", printSettings.borderStyle.name)
            put("paperSize", printSettings.paperSize.name)
            put("accentColorHex", printSettings.accentColorHex)
            put("showSignaturesSection", printSettings.showSignaturesSection)
            put("sig1Enabled", printSettings.sig1Enabled)
            put("sig1Title", printSettings.sig1Title)
            put("sig1DefaultName", printSettings.sig1DefaultName)
            put("sig2Enabled", printSettings.sig2Enabled)
            put("sig2Title", printSettings.sig2Title)
            put("sig2DefaultName", printSettings.sig2DefaultName)
            put("sig3Enabled", printSettings.sig3Enabled)
            put("sig3Title", printSettings.sig3Title)
            put("sig3DefaultName", printSettings.sig3DefaultName)
            put("sig4Enabled", printSettings.sig4Enabled)
            put("sig4Title", printSettings.sig4Title)
            put("sig4DefaultName", printSettings.sig4DefaultName)
            put("showSignatureLine", printSettings.showSignatureLine)
            put("showOfficialStampBox", printSettings.showOfficialStampBox)
            put("officialStampLabel", printSettings.officialStampLabel)
            put("showQrVerification", printSettings.showQrVerification)
            put("qrCaptionText", printSettings.qrCaptionText)
            put("showJournalReference", printSettings.showJournalReference)
            put("showCurrencyDetails", printSettings.showCurrencyDetails)
            put("showFundContactInFooter", printSettings.showFundContactInFooter)
            put("footerNote", printSettings.footerNote)
            put("footerSubNote", printSettings.footerSubNote)
        })

        // Members
        root.put("members", JSONArray().apply {
            members.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("memberNo", m.memberNo)
                    put("fullName", m.fullName)
                    put("phone", m.phone)
                    put("nationalId", m.nationalId)
                    put("joinDate", m.joinDate)
                    put("status", m.status)
                    put("totalPaidYer", m.totalPaidYer)
                    put("dueBalanceYer", m.dueBalanceYer)
                    put("job", m.job)
                    put("region", m.region)
                    put("village", m.village)
                    put("notes", m.notes)
                })
            }
        })

        // Beneficiaries
        root.put("beneficiaries", JSONArray().apply {
            beneficiaries.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("familyHeadName", b.familyHeadName)
                    put("nationalId", b.nationalId)
                    put("phone", b.phone)
                    put("membersCount", b.membersCount)
                    put("monthlyIncomeYer", b.monthlyIncomeYer)
                    put("povertyTier", b.povertyTier.name)
                    put("region", b.region)
                    put("village", b.village)
                    put("address", b.address)
                    put("aidHistoryCount", b.aidHistoryCount)
                })
            }
        })

        // Accounts
        root.put("accounts", JSONArray().apply {
            accounts.forEach { a ->
                put(JSONObject().apply {
                    put("code", a.code)
                    put("name", a.name)
                    put("type", a.type.name)
                    put("isDebitNormal", a.isDebitNormal)
                    put("balance", a.balance)
                    put("parentCode", a.parentCode ?: "")
                })
            }
        })

        // Vouchers
        root.put("vouchers", JSONArray().apply {
            vouchers.forEach { v ->
                put(JSONObject().apply {
                    put("id", v.id)
                    put("voucherNo", v.voucherNo)
                    put("type", v.type.name)
                    put("status", v.status.name)
                    put("partyName", v.partyName)
                    put("amountOriginal", v.amountOriginal)
                    put("currency", v.currency.name)
                    put("exchangeRate", v.exchangeRate)
                    put("amountYer", v.amountYer)
                    put("amountWords", v.amountWords)
                    put("paymentMethod", v.paymentMethod.name)
                    put("accountCode", v.accountCode)
                    put("accountName", v.accountName)
                    put("counterpartAccountCode", v.counterpartAccountCode)
                    put("counterpartAccountName", v.counterpartAccountName)
                    put("description", v.description)
                    put("date", v.date)
                    put("linkedJournalEntryId", v.linkedJournalEntryId)
                    put("receivedOrPaidBy", v.receivedOrPaidBy)
                    put("accountantName", v.accountantName)
                    put("auditorName", v.auditorName)
                    put("managerName", v.managerName)
                })
            }
        })

        // Journal Entries
        root.put("journalEntries", JSONArray().apply {
            journalEntries.forEach { je ->
                put(JSONObject().apply {
                    put("id", je.id)
                    put("entryNo", je.entryNo)
                    put("date", je.date)
                    put("description", je.description)
                    put("isPosted", je.isPosted)
                    put("isReversed", je.isReversed)
                    put("originalVoucherNo", je.originalVoucherNo ?: "")
                    put("lines", JSONArray().apply {
                        je.lines.forEach { l ->
                            put(JSONObject().apply {
                                put("accountCode", l.accountCode)
                                put("accountName", l.accountName)
                                put("debit", l.debit)
                                put("credit", l.credit)
                                put("description", l.description)
                            })
                        }
                    })
                })
            }
        })

        // Subscriptions
        root.put("subscriptions", JSONArray().apply {
            subscriptions.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("receiptNo", s.receiptNo)
                    put("memberId", s.memberId)
                    put("memberName", s.memberName)
                    put("amountOriginal", s.amountOriginal)
                    put("currency", s.currency.name)
                    put("exchangeRate", s.exchangeRate)
                    put("amountYer", s.amountYer)
                    put("monthYear", s.monthYear)
                    put("paymentMethod", s.paymentMethod.name)
                    put("referenceNo", s.referenceNo)
                    put("date", s.date)
                    put("notes", s.notes)
                })
            }
        })

        // AidRequests
        root.put("aidRequests", JSONArray().apply {
            aidRequests.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("requestNo", r.requestNo)
                    put("memberId", r.memberId)
                    put("memberName", r.memberName)
                    put("aidType", r.aidType.name)
                    put("requestedAmountYer", r.requestedAmountYer)
                    put("approvedAmountYer", r.approvedAmountYer)
                    put("status", r.status.name)
                    put("requestDate", r.requestDate)
                    put("decisionDate", r.decisionDate)
                    put("notes", r.notes)
                    put("voucherId", r.voucherId ?: "")
                })
            }
        })

        // Budgets
        root.put("budgets", JSONArray().apply {
            budgets.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("category", b.category)
                    put("accountCode", b.accountCode)
                    put("budgetedAmount", b.budgetedAmount)
                    put("actualSpent", b.actualSpent)
                    put("month", b.month)
                })
            }
        })

        // Donors
        root.put("donors", JSONArray().apply {
            donors.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id)
                    put("name", d.name)
                    put("phone", d.phone)
                    put("type", d.type)
                    put("totalDonatedYer", d.totalDonatedYer)
                    put("pledgeAmountYer", d.pledgeAmountYer)
                    put("pledgeFulfilledYer", d.pledgeFulfilledYer)
                    put("lastDonationDate", d.lastDonationDate)
                })
            }
        })

        // Campaigns
        root.put("campaigns", JSONArray().apply {
            campaigns.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("goalAmountYer", c.goalAmountYer)
                    put("collectedAmountYer", c.collectedAmountYer)
                    put("spentAmountYer", c.spentAmountYer)
                    put("status", c.status)
                    put("description", c.description)
                    put("targetCategory", c.targetCategory)
                })
            }
        })

        // PeriodicAids
        root.put("periodicAids", JSONArray().apply {
            periodicAids.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("beneficiaryId", p.beneficiaryId)
                    put("beneficiaryName", p.beneficiaryName)
                    put("monthlyAmountYer", p.monthlyAmountYer)
                    put("sponsorshipType", p.sponsorshipType)
                    put("status", p.status)
                    put("lastDisbursedMonth", p.lastDisbursedMonth)
                })
            }
        })

        // InKindItems
        root.put("inKindItems", JSONArray().apply {
            inKindItems.forEach { itItem ->
                put(JSONObject().apply {
                    put("id", itItem.id)
                    put("code", itItem.code)
                    put("name", itItem.name)
                    put("category", itItem.category)
                    put("unit", itItem.unit)
                    put("quantityOnHand", itItem.quantityOnHand)
                    put("reorderLevel", itItem.reorderLevel)
                    put("unitCostYer", itItem.unitCostYer)
                    put("location", itItem.location)
                })
            }
        })

        // InKindMovements
        root.put("inKindMovements", JSONArray().apply {
            inKindMovements.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("movementNo", m.movementNo)
                    put("type", m.type)
                    put("itemId", m.itemId)
                    put("itemName", m.itemName)
                    put("quantity", m.quantity)
                    put("partyName", m.partyName)
                    put("date", m.date)
                    put("notes", m.notes)
                })
            }
        })

        // SchedulerEvents
        root.put("schedulerEvents", JSONArray().apply {
            schedulerEvents.forEach { ev ->
                put(JSONObject().apply {
                    put("id", ev.id)
                    put("title", ev.title)
                    put("date", ev.date)
                    put("time", ev.time)
                    put("location", ev.location)
                    put("type", ev.type)
                    put("colorHex", ev.colorHex)
                    put("notes", ev.notes)
                })
            }
        })

        // InternalMessages
        root.put("internalMessages", JSONArray().apply {
            internalMessages.forEach { msg ->
                put(JSONObject().apply {
                    put("id", msg.id)
                    put("senderName", msg.senderName)
                    put("senderRole", msg.senderRole)
                    put("recipientRole", msg.recipientRole)
                    put("subject", msg.subject)
                    put("body", msg.body)
                    put("timestamp", msg.timestamp)
                    put("isRead", msg.isRead)
                })
            }
        })

        // CurrencyRates
        root.put("currencyRates", JSONObject().apply {
            currencyRates.forEach { (code, rate) -> put(code.name, rate) }
        })

        // SyncQueue
        root.put("syncQueue", JSONArray().apply {
            syncQueue.forEach { sq ->
                put(JSONObject().apply {
                    put("id", sq.id)
                    put("timestamp", sq.timestamp)
                    put("entityName", sq.entityName)
                    put("action", sq.action)
                    put("summary", sq.summary)
                    put("txHash", sq.txHash)
                    put("payloadBytes", sq.payloadBytes)
                })
            }
        })

        val coreJson = root.toString()
        root.put("integritySha256", SecurityUtils.sha256Hex(coreJson))
        return root.toString(2)
    }

    fun parseFromJson(
        json: String,
        fallbackFundInfo: FundInfo = FundInfo(),
        fallbackAppSettings: AppSettings = AppSettings(),
        fallbackPrintSettings: PrintSettings = PrintSettings()
    ): ParsedDatabaseSnapshot? {
        return try {
            val root = JSONObject(json)

            val fundInfo = root.optJSONObject("fundInfo")?.let { fi ->
                FundInfo(
                    name = fi.optString("name", fallbackFundInfo.name),
                    subtitle = fi.optString("subtitle", fallbackFundInfo.subtitle),
                    licenseNo = fi.optString("licenseNo", fallbackFundInfo.licenseNo),
                    address = fi.optString("address", fallbackFundInfo.address),
                    phone = fi.optString("phone", fallbackFundInfo.phone),
                    email = fi.optString("email", fallbackFundInfo.email),
                    bankAccountKareemi = fi.optString("bankAccountKareemi", fallbackFundInfo.bankAccountKareemi),
                    bankAccountCAC = fi.optString("bankAccountCAC", fallbackFundInfo.bankAccountCAC),
                    maxAidCeilingYer = fi.optDouble("maxAidCeilingYer", fallbackFundInfo.maxAidCeilingYer),
                    fiscalYear = fi.optString("fiscalYear", fallbackFundInfo.fiscalYear),
                    customLogoBase64 = fi.optString("customLogoBase64", fallbackFundInfo.customLogoBase64)
                )
            }

            val regions = root.optJSONArray("regions")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val vArr = o.optJSONArray("villages")
                    val villages = if (vArr != null) {
                        List(vArr.length()) { j -> vArr.optString(j) }.filter { it.isNotBlank() }
                    } else emptyList()
                    RegionGroup(id = o.optString("id"), name = o.optString("name"), villages = villages)
                }.takeIf { it.isNotEmpty() }
            }

            val appSettings = root.optJSONObject("appSettings")?.let { st ->
                val cur = fallbackAppSettings
                AppSettings(
                    monthlySubscriptionFeeYer = st.optDouble("monthlySubscriptionFeeYer", cur.monthlySubscriptionFeeYer),
                    autoSuspendMonths = st.optInt("autoSuspendMonths", cur.autoSuspendMonths),
                    requireNationalId = st.optBoolean("requireNationalId", cur.requireNationalId),
                    memberNoPrefix = st.optString("memberNoPrefix", cur.memberNoPrefix),
                    requireReviewerApproval = st.optBoolean("requireReviewerApproval", cur.requireReviewerApproval),
                    periodicAidDisburseDay = st.optInt("periodicAidDisburseDay", cur.periodicAidDisburseDay),
                    lowStockThreshold = st.optInt("lowStockThreshold", cur.lowStockThreshold),
                    defaultWarehouseName = st.optString("defaultWarehouseName", cur.defaultWarehouseName),
                    voucherNoPrefix = st.optString("voucherNoPrefix", cur.voucherNoPrefix),
                    defaultTreasuryAccountCode = st.optString("defaultTreasuryAccountCode", cur.defaultTreasuryAccountCode),
                    allowNegativeTreasury = st.optBoolean("allowNegativeTreasury", cur.allowNegativeTreasury),
                    enforceBalancedJournal = st.optBoolean("enforceBalancedJournal", cur.enforceBalancedJournal),
                    budgetAlertPercent = st.optInt("budgetAlertPercent", cur.budgetAlertPercent),
                    autoPrintVoucher = st.optBoolean("autoPrintVoucher", cur.autoPrintVoucher),
                    allowAnonymousDonors = st.optBoolean("allowAnonymousDonors", cur.allowAnonymousDonors),
                    defaultEventLocation = st.optString("defaultEventLocation", cur.defaultEventLocation),
                    strictRolePermissions = st.optBoolean("strictRolePermissions", cur.strictRolePermissions),
                    enableAuditTrail = st.optBoolean("enableAuditTrail", cur.enableAuditTrail)
                )
            }

            val printSettings = root.optJSONObject("printSettings")?.let { psObj ->
                val cur = fallbackPrintSettings
                PrintSettings(
                    showCountryHeader = psObj.optBoolean("showCountryHeader", cur.showCountryHeader),
                    countryHeaderText = psObj.optString("countryHeaderText", cur.countryHeaderText),
                    showDepartmentHeader = psObj.optBoolean("showDepartmentHeader", cur.showDepartmentHeader),
                    departmentHeaderText = psObj.optString("departmentHeaderText", cur.departmentHeaderText),
                    showLogoInPrint = psObj.optBoolean("showLogoInPrint", cur.showLogoInPrint),
                    showLicenseAndAddress = psObj.optBoolean("showLicenseAndAddress", cur.showLicenseAndAddress),
                    receiptVoucherTitle = psObj.optString("receiptVoucherTitle", cur.receiptVoucherTitle),
                    paymentVoucherTitle = psObj.optString("paymentVoucherTitle", cur.paymentVoucherTitle),
                    journalVoucherTitle = psObj.optString("journalVoucherTitle", cur.journalVoucherTitle),
                    financialStatementSubtitle = psObj.optString("financialStatementSubtitle", cur.financialStatementSubtitle),
                    analyticalReportSubtitle = psObj.optString("analyticalReportSubtitle", cur.analyticalReportSubtitle),
                    receiptPartyLabel = psObj.optString("receiptPartyLabel", cur.receiptPartyLabel),
                    paymentPartyLabel = psObj.optString("paymentPartyLabel", cur.paymentPartyLabel),
                    amountWordsLabel = psObj.optString("amountWordsLabel", cur.amountWordsLabel),
                    paymentMethodLabel = psObj.optString("paymentMethodLabel", cur.paymentMethodLabel),
                    accountLabel = psObj.optString("accountLabel", cur.accountLabel),
                    descriptionLabel = psObj.optString("descriptionLabel", cur.descriptionLabel),
                    fontFamily = runCatching { PrintFontOption.valueOf(psObj.optString("fontFamily")) }.getOrDefault(cur.fontFamily),
                    headerFontSizeSp = psObj.optInt("headerFontSizeSp", cur.headerFontSizeSp),
                    bodyFontSizeSp = psObj.optInt("bodyFontSizeSp", cur.bodyFontSizeSp),
                    signatureFontSizeSp = psObj.optInt("signatureFontSizeSp", cur.signatureFontSizeSp),
                    borderStyle = runCatching { PrintBorderStyle.valueOf(psObj.optString("borderStyle")) }.getOrDefault(cur.borderStyle),
                    paperSize = runCatching { PrintPaperSize.valueOf(psObj.optString("paperSize")) }.getOrDefault(cur.paperSize),
                    accentColorHex = psObj.optString("accentColorHex", cur.accentColorHex),
                    showSignaturesSection = psObj.optBoolean("showSignaturesSection", cur.showSignaturesSection),
                    sig1Enabled = psObj.optBoolean("sig1Enabled", cur.sig1Enabled),
                    sig1Title = psObj.optString("sig1Title", cur.sig1Title),
                    sig1DefaultName = psObj.optString("sig1DefaultName", cur.sig1DefaultName),
                    sig2Enabled = psObj.optBoolean("sig2Enabled", cur.sig2Enabled),
                    sig2Title = psObj.optString("sig2Title", cur.sig2Title),
                    sig2DefaultName = psObj.optString("sig2DefaultName", cur.sig2DefaultName),
                    sig3Enabled = psObj.optBoolean("sig3Enabled", cur.sig3Enabled),
                    sig3Title = psObj.optString("sig3Title", cur.sig3Title),
                    sig3DefaultName = psObj.optString("sig3DefaultName", cur.sig3DefaultName),
                    sig4Enabled = psObj.optBoolean("sig4Enabled", cur.sig4Enabled),
                    sig4Title = psObj.optString("sig4Title", cur.sig4Title),
                    sig4DefaultName = psObj.optString("sig4DefaultName", cur.sig4DefaultName),
                    showSignatureLine = psObj.optBoolean("showSignatureLine", cur.showSignatureLine),
                    showOfficialStampBox = psObj.optBoolean("showOfficialStampBox", cur.showOfficialStampBox),
                    officialStampLabel = psObj.optString("officialStampLabel", cur.officialStampLabel),
                    showQrVerification = psObj.optBoolean("showQrVerification", cur.showQrVerification),
                    qrCaptionText = psObj.optString("qrCaptionText", cur.qrCaptionText),
                    showJournalReference = psObj.optBoolean("showJournalReference", cur.showJournalReference),
                    showCurrencyDetails = psObj.optBoolean("showCurrencyDetails", cur.showCurrencyDetails),
                    showFundContactInFooter = psObj.optBoolean("showFundContactInFooter", cur.showFundContactInFooter),
                    footerNote = psObj.optString("footerNote", cur.footerNote),
                    footerSubNote = psObj.optString("footerSubNote", cur.footerSubNote)
                )
            }

            val members = root.optJSONArray("members")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val legacy = o.optString("villageOrArea")
                    val regionVal = o.optString("region").ifBlank { legacy.substringBefore(" - ", "المنطقة الوسطى").trim() }
                    val villageVal = o.optString("village").ifBlank {
                        if (legacy.contains(" - ")) legacy.substringAfter(" - ").trim() else "القرية المركزية"
                    }
                    Member(
                        id = o.optString("id"),
                        memberNo = o.optString("memberNo"),
                        fullName = o.optString("fullName"),
                        phone = o.optString("phone"),
                        nationalId = o.optString("nationalId"),
                        joinDate = o.optString("joinDate"),
                        status = o.optString("status", "نشط"),
                        totalPaidYer = o.optDouble("totalPaidYer", 0.0),
                        dueBalanceYer = o.optDouble("dueBalanceYer", 0.0),
                        job = o.optString("job"),
                        region = regionVal,
                        village = villageVal,
                        notes = o.optString("notes")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val beneficiaries = root.optJSONArray("beneficiaries")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Beneficiary(
                        id = o.optString("id"),
                        familyHeadName = o.optString("familyHeadName"),
                        nationalId = o.optString("nationalId"),
                        phone = o.optString("phone"),
                        membersCount = o.optInt("membersCount", 1),
                        monthlyIncomeYer = o.optDouble("monthlyIncomeYer", 0.0),
                        povertyTier = runCatching { PovertyTier.valueOf(o.optString("povertyTier")) }.getOrDefault(PovertyTier.MEDIUM),
                        region = o.optString("region", "المنطقة الوسطى"),
                        village = o.optString("village", "القرية المركزية"),
                        address = o.optString("address"),
                        aidHistoryCount = o.optInt("aidHistoryCount", 0)
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val accounts = root.optJSONArray("accounts")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val pCode = o.optString("parentCode")
                    Account(
                        code = o.optString("code"),
                        name = o.optString("name"),
                        type = runCatching { AccountType.valueOf(o.optString("type")) }.getOrDefault(AccountType.ASSET),
                        isDebitNormal = o.optBoolean("isDebitNormal", true),
                        balance = o.optDouble("balance", 0.0),
                        parentCode = if (pCode.isNullOrEmpty()) null else pCode
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val vouchers = root.optJSONArray("vouchers")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Voucher(
                        id = o.optString("id"),
                        voucherNo = o.optString("voucherNo"),
                        type = runCatching { VoucherType.valueOf(o.optString("type")) }.getOrDefault(VoucherType.RECEIPT),
                        status = runCatching { VoucherStatus.valueOf(o.optString("status")) }.getOrDefault(VoucherStatus.POSTED),
                        partyName = o.optString("partyName"),
                        amountOriginal = o.optDouble("amountOriginal", 0.0),
                        currency = runCatching { CurrencyCode.valueOf(o.optString("currency")) }.getOrDefault(CurrencyCode.YER),
                        exchangeRate = o.optDouble("exchangeRate", 1.0),
                        amountYer = o.optDouble("amountYer", 0.0),
                        amountWords = o.optString("amountWords"),
                        paymentMethod = runCatching { PaymentMethod.valueOf(o.optString("paymentMethod")) }.getOrDefault(PaymentMethod.CASH),
                        accountCode = o.optString("accountCode"),
                        accountName = o.optString("accountName"),
                        counterpartAccountCode = o.optString("counterpartAccountCode"),
                        counterpartAccountName = o.optString("counterpartAccountName"),
                        description = o.optString("description"),
                        date = o.optString("date"),
                        linkedJournalEntryId = o.optString("linkedJournalEntryId"),
                        receivedOrPaidBy = o.optString("receivedOrPaidBy"),
                        accountantName = o.optString("accountantName", "أمين الصندوق / المحاسب"),
                        auditorName = o.optString("auditorName", "المراجع المالي"),
                        managerName = o.optString("managerName", "رئيس مجلس الإدارة")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val journalEntries = root.optJSONArray("journalEntries")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val lArr = o.optJSONArray("lines")
                    val lines = if (lArr != null) {
                        List(lArr.length()) { j ->
                            val lo = lArr.getJSONObject(j)
                            JournalLine(
                                accountCode = lo.optString("accountCode"),
                                accountName = lo.optString("accountName"),
                                debit = lo.optDouble("debit", 0.0),
                                credit = lo.optDouble("credit", 0.0),
                                description = lo.optString("description")
                            )
                        }
                    } else emptyList()
                    val ov = o.optString("originalVoucherNo")
                    JournalEntry(
                        id = o.optString("id"),
                        entryNo = o.optString("entryNo"),
                        date = o.optString("date"),
                        description = o.optString("description"),
                        lines = lines,
                        isPosted = o.optBoolean("isPosted", true),
                        isReversed = o.optBoolean("isReversed", false),
                        originalVoucherNo = if (ov.isNullOrBlank()) null else ov
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val subscriptions = root.optJSONArray("subscriptions")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Subscription(
                        id = o.optString("id"),
                        receiptNo = o.optString("receiptNo"),
                        memberId = o.optString("memberId"),
                        memberName = o.optString("memberName"),
                        amountOriginal = o.optDouble("amountOriginal", 0.0),
                        currency = runCatching { CurrencyCode.valueOf(o.optString("currency")) }.getOrDefault(CurrencyCode.YER),
                        exchangeRate = o.optDouble("exchangeRate", 1.0),
                        amountYer = o.optDouble("amountYer", 0.0),
                        monthYear = o.optString("monthYear"),
                        paymentMethod = runCatching { PaymentMethod.valueOf(o.optString("paymentMethod")) }.getOrDefault(PaymentMethod.CASH),
                        referenceNo = o.optString("referenceNo"),
                        date = o.optString("date"),
                        notes = o.optString("notes")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val aidRequests = root.optJSONArray("aidRequests")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val vId = o.optString("voucherId")
                    AidRequest(
                        id = o.optString("id"),
                        requestNo = o.optString("requestNo"),
                        memberId = o.optString("memberId"),
                        memberName = o.optString("memberName"),
                        aidType = runCatching { AidType.valueOf(o.optString("aidType")) }.getOrDefault(AidType.EMERGENCY),
                        requestedAmountYer = o.optDouble("requestedAmountYer", 0.0),
                        approvedAmountYer = o.optDouble("approvedAmountYer", 0.0),
                        status = runCatching { AidStatus.valueOf(o.optString("status")) }.getOrDefault(AidStatus.IN_REVIEW),
                        requestDate = o.optString("requestDate"),
                        decisionDate = o.optString("decisionDate"),
                        notes = o.optString("notes"),
                        voucherId = if (vId.isNullOrBlank()) null else vId
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val budgets = root.optJSONArray("budgets")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Budget(
                        id = o.optString("id"),
                        category = o.optString("category"),
                        accountCode = o.optString("accountCode"),
                        budgetedAmount = o.optDouble("budgetedAmount", 0.0),
                        actualSpent = o.optDouble("actualSpent", 0.0),
                        month = o.optString("month")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val donors = root.optJSONArray("donors")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Donor(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        phone = o.optString("phone"),
                        type = o.optString("type"),
                        totalDonatedYer = o.optDouble("totalDonatedYer", 0.0),
                        pledgeAmountYer = o.optDouble("pledgeAmountYer", 0.0),
                        pledgeFulfilledYer = o.optDouble("pledgeFulfilledYer", 0.0),
                        lastDonationDate = o.optString("lastDonationDate")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val campaigns = root.optJSONArray("campaigns")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Campaign(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        goalAmountYer = o.optDouble("goalAmountYer", 0.0),
                        collectedAmountYer = o.optDouble("collectedAmountYer", 0.0),
                        spentAmountYer = o.optDouble("spentAmountYer", 0.0),
                        status = o.optString("status", "نشطة"),
                        description = o.optString("description"),
                        targetCategory = o.optString("targetCategory")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val periodicAids = root.optJSONArray("periodicAids")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    PeriodicAid(
                        id = o.optString("id"),
                        beneficiaryId = o.optString("beneficiaryId"),
                        beneficiaryName = o.optString("beneficiaryName"),
                        monthlyAmountYer = o.optDouble("monthlyAmountYer", 0.0),
                        sponsorshipType = o.optString("sponsorshipType"),
                        status = o.optString("status"),
                        lastDisbursedMonth = o.optString("lastDisbursedMonth")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val inKindItems = root.optJSONArray("inKindItems")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    InKindItem(
                        id = o.optString("id"),
                        code = o.optString("code"),
                        name = o.optString("name"),
                        category = o.optString("category"),
                        unit = o.optString("unit"),
                        quantityOnHand = o.optInt("quantityOnHand", 0),
                        reorderLevel = o.optInt("reorderLevel", 10),
                        unitCostYer = o.optDouble("unitCostYer", 0.0),
                        location = o.optString("location")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val inKindMovements = root.optJSONArray("inKindMovements")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    InKindMovement(
                        id = o.optString("id"),
                        movementNo = o.optString("movementNo"),
                        type = o.optString("type"),
                        itemId = o.optString("itemId"),
                        itemName = o.optString("itemName"),
                        quantity = o.optInt("quantity", 0),
                        partyName = o.optString("partyName"),
                        date = o.optString("date"),
                        notes = o.optString("notes")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val schedulerEvents = root.optJSONArray("schedulerEvents")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    SchedulerEvent(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        date = o.optString("date"),
                        time = o.optString("time"),
                        location = o.optString("location"),
                        type = o.optString("type"),
                        colorHex = o.optString("colorHex", "#2E7D32"),
                        notes = o.optString("notes")
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val internalMessages = root.optJSONArray("internalMessages")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    InternalMessage(
                        id = o.optString("id"),
                        senderName = o.optString("senderName"),
                        senderRole = o.optString("senderRole"),
                        recipientRole = o.optString("recipientRole"),
                        subject = o.optString("subject"),
                        body = o.optString("body"),
                        timestamp = o.optString("timestamp"),
                        isRead = o.optBoolean("isRead", false)
                    )
                }.takeIf { it.isNotEmpty() }
            }

            val currencyRates = root.optJSONObject("currencyRates")?.let { crObj ->
                val map = SeedData.currencyRates.toMutableMap()
                CurrencyCode.entries.forEach { code ->
                    if (crObj.has(code.name)) {
                        map[code] = crObj.optDouble(code.name, code.defaultRate)
                    }
                }
                map
            }

            val syncQueue = root.optJSONArray("syncQueue")?.let { arr ->
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    SyncQueueItem(
                        id = o.optString("id"),
                        timestamp = o.optString("timestamp"),
                        entityName = o.optString("entityName"),
                        action = o.optString("action"),
                        summary = o.optString("summary"),
                        txHash = o.optString("txHash", "A9F401C2"),
                        payloadBytes = o.optInt("payloadBytes", 512)
                    )
                }
            }

            ParsedDatabaseSnapshot(
                fundInfo = fundInfo,
                regions = regions,
                appSettings = appSettings,
                printSettings = printSettings,
                members = members,
                beneficiaries = beneficiaries,
                accounts = accounts,
                vouchers = vouchers,
                journalEntries = journalEntries,
                subscriptions = subscriptions,
                aidRequests = aidRequests,
                budgets = budgets,
                donors = donors,
                campaigns = campaigns,
                periodicAids = periodicAids,
                inKindItems = inKindItems,
                inKindMovements = inKindMovements,
                schedulerEvents = schedulerEvents,
                internalMessages = internalMessages,
                currencyRates = currencyRates,
                syncQueue = syncQueue,
                exportDate = root.optString("exportDate")
            )
        } catch (_: Exception) {
            null
        }
    }
}
