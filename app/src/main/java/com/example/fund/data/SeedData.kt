package com.example.fund.data

import com.example.fund.model.*

object SeedData {

    val regions = listOf(
        RegionGroup(
            id = "reg1",
            name = "المنطقة الوسطى",
            villages = listOf("القرية المركزية", "الحي الشرقي", "حي النهضة", "الربوة")
        ),
        RegionGroup(
            id = "reg2",
            name = "المنطقة الشرقية",
            villages = listOf("قرية السلام", "وادي الخير", "الضاحية الشرقية")
        ),
        RegionGroup(
            id = "reg3",
            name = "المنطقة الغربية",
            villages = listOf("قرية الأمل", "المجمع الغربي", "تجمع الروضة")
        )
    )

    val accounts = listOf(
        // Assets 1000
        Account("1000", "الأصول", AccountType.ASSET, true, 19620000.0),
        Account("1100", "الصندوق النقدي الرئيسي", AccountType.ASSET, true, 4850000.0, "1000"),
        Account("1200", "بنك الكريمي الإسلامي", AccountType.ASSET, true, 12400000.0, "1000"),
        Account("1300", "المحافظ الإلكترونية (جوالي/فلوسك)", AccountType.ASSET, true, 1750000.0, "1000"),
        Account("1400", "ذمم الاشتراكات المستحقة", AccountType.ASSET, true, 620000.0, "1000"),

        // Liabilities 2000
        Account("2000", "الالتزامات", AccountType.LIABILITY, false, 800000.0),
        Account("2100", "أمانات وتأمينات مستردة", AccountType.LIABILITY, false, 350000.0, "2000"),
        Account("2200", "مساعدات معتمدة قيد الصرف", AccountType.LIABILITY, false, 450000.0, "2000"),

        // Equity 3000
        Account("3000", "حقوق الملكية وصافي الأصول", AccountType.EQUITY, false, 16820000.0),
        Account("3100", "رأس مال الصندوق والاحتياطي العام", AccountType.EQUITY, false, 15000000.0, "3000"),
        Account("3200", "الفائض/العجز المتراكم", AccountType.EQUITY, false, 1820000.0, "3000"),

        // Revenue 4000
        Account("4000", "الإيرادات", AccountType.REVENUE, false, 10500000.0),
        Account("4100", "إيرادات اشتراكات الأعضاء", AccountType.REVENUE, false, 2800000.0, "4000"),
        Account("4200", "التبرعات والمنح العامة", AccountType.REVENUE, false, 4200000.0, "4000"),
        Account("4300", "إيرادات حملات التبرع المخصصة", AccountType.REVENUE, false, 3500000.0, "4000"),

        // Expenses 5000
        Account("5000", "المصروفات", AccountType.EXPENSE, true, 8700000.0),
        Account("5100", "مصروف المساعدات النقدية للأعضاء", AccountType.EXPENSE, true, 4100000.0, "5000"),
        Account("5200", "مصروف المساعدات الدورية والأسر المستفيدة", AccountType.EXPENSE, true, 2850000.0, "5000"),
        Account("5300", "مصروف المساعدات العينية", AccountType.EXPENSE, true, 1200000.0, "5000"),
        Account("5400", "المصروفات التشغيلية والإدارية", AccountType.EXPENSE, true, 550000.0, "5000")
    )

    val members = listOf(
        Member(
            id = "m1",
            memberNo = "M-1001",
            fullName = "الشيخ فضل عبدالكريم المنصوري",
            phone = "771234501",
            nationalId = "1049281041",
            joinDate = "2020-01-15",
            status = "نشط",
            totalPaidYer = 420000.0,
            dueBalanceYer = 0.0,
            job = "شخصية اجتماعية ومربي فاضل",
            region = "المنطقة الوسطى",
            village = "القرية المركزية",
            notes = "عضو مؤسس منتظم بالسداد"
        ),
        Member(
            id = "m2",
            memberNo = "M-1002",
            fullName = "د. وضاح ناصر السعيدي",
            phone = "772345602",
            nationalId = "1049281042",
            joinDate = "2020-03-10",
            status = "نشط",
            totalPaidYer = 380000.0,
            dueBalanceYer = 20000.0,
            job = "استشاري طب عام وجراحة",
            region = "المنطقة الوسطى",
            village = "حي النهضة",
            notes = "يقدم استشارات طبية مجانية للمستفيدين"
        ),
        Member(
            id = "m3",
            memberNo = "M-1003",
            fullName = "أ. عبدالحميد قاسم المعمري",
            phone = "773456703",
            nationalId = "1049281043",
            joinDate = "2021-02-01",
            status = "نشط",
            totalPaidYer = 310000.0,
            dueBalanceYer = 0.0,
            job = "مدير مدرسة ثانوية",
            region = "المنطقة الشرقية",
            village = "قرية السلام",
            notes = "منسق الأنشطة التعليمية"
        ),
        Member(
            id = "m4",
            memberNo = "M-1004",
            fullName = "م. ياسر صالح البكري",
            phone = "774567804",
            nationalId = "1049281044",
            joinDate = "2021-05-18",
            status = "نشط",
            totalPaidYer = 290000.0,
            dueBalanceYer = 30000.0,
            job = "مهندس شبكات ومياه",
            region = "المنطقة الوسطى",
            village = "الحي الشرقي",
            notes = "مشرف مشاريع الآبار التنموية"
        ),
        Member(
            id = "m5",
            memberNo = "M-1005",
            fullName = "د. مريم أحمد العامري",
            phone = "775678905",
            nationalId = "1049281045",
            joinDate = "2021-08-20",
            status = "نشط",
            totalPaidYer = 340000.0,
            dueBalanceYer = 0.0,
            job = "طبيبة نساء وتوليد",
            region = "المنطقة الغربية",
            village = "قرية الأمل",
            notes = "عضو اللجنة الصحية"
        ),
        Member(
            id = "m6",
            memberNo = "M-1006",
            fullName = "أ. منصور علي ثابت",
            phone = "776789006",
            nationalId = "1049281046",
            joinDate = "2022-01-11",
            status = "معلق",
            totalPaidYer = 180000.0,
            dueBalanceYer = 60000.0,
            job = "تاجر ومستورد مواد بناء",
            region = "المنطقة الشرقية",
            village = "وادي الخير",
            notes = "متأخر 3 أشهر - جارٍ التواصل"
        ),
        Member(
            id = "m7",
            memberNo = "M-1007",
            fullName = "أ. سالم نصر الحمادي",
            phone = "777890107",
            nationalId = "1049281047",
            joinDate = "2022-04-14",
            status = "نشط",
            totalPaidYer = 260000.0,
            dueBalanceYer = 0.0,
            job = "موجه تربوي متقاعد",
            region = "المنطقة الغربية",
            village = "المجمع الغربي",
            notes = "داعم للأنشطة التنظيمية والاجتماعية"
        ),
        Member(
            id = "m8",
            memberNo = "M-1008",
            fullName = "فهد حسين النعماني",
            phone = "778901208",
            nationalId = "1049281048",
            joinDate = "2022-07-01",
            status = "نشط",
            totalPaidYer = 220000.0,
            dueBalanceYer = 20000.0,
            job = "محاسب قانوني",
            region = "المنطقة الوسطى",
            village = "الربوة",
            notes = "عضو لجنة الرقابة والتفتيش"
        )
    )

    val subscriptions = listOf(
        Subscription(
            id = "sub1",
            receiptNo = "SUB-2026-001",
            memberId = "m1",
            memberName = "الشيخ فضل عبدالكريم المنصوري",
            amountOriginal = 20000.0,
            currency = CurrencyCode.YER,
            exchangeRate = 1.0,
            amountYer = 20000.0,
            monthYear = "2026-11",
            paymentMethod = PaymentMethod.CASH,
            referenceNo = "REC-1049",
            date = "2026-11-01",
            notes = "سداد اشتراك نوفمبر 2026 نقداً"
        ),
        Subscription(
            id = "sub2",
            receiptNo = "SUB-2026-002",
            memberId = "m2",
            memberName = "د. وضاح ناصر السعيدي",
            amountOriginal = 100.0,
            currency = CurrencyCode.SAR,
            exchangeRate = 665.0,
            amountYer = 66500.0,
            monthYear = "2026-11",
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            referenceNo = "KRM-994820",
            date = "2026-11-02",
            notes = "سداد اشتراك نوفمبر ودفعة مقدمة عبر الكريمي"
        ),
        Subscription(
            id = "sub3",
            receiptNo = "SUB-2026-003",
            memberId = "m5",
            memberName = "د. مريم أحمد العامري",
            amountOriginal = 20000.0,
            currency = CurrencyCode.YER,
            exchangeRate = 1.0,
            amountYer = 20000.0,
            monthYear = "2026-11",
            paymentMethod = PaymentMethod.ELECTRONIC_WALLET,
            referenceNo = "JAW-55019",
            date = "2026-11-03",
            notes = "عبر محفظة جوالي"
        )
    )

    val aidRequests = listOf(
        AidRequest(
            id = "aid1",
            requestNo = "REQ-2026-011",
            memberId = "m6",
            memberName = "أ. منصور علي ثابت",
            aidType = AidType.MEDICAL,
            requestedAmountYer = 250000.0,
            approvedAmountYer = 250000.0,
            status = AidStatus.DISBURSED,
            requestDate = "2026-10-15",
            decisionDate = "2026-10-18",
            notes = "مساهمة في عملية جراحية بالقلب - مستشفى التخصصي العام",
            voucherId = "v_pay_01"
        ),
        AidRequest(
            id = "aid2",
            requestNo = "REQ-2026-012",
            memberId = "m4",
            memberName = "م. ياسر صالح البكري",
            aidType = AidType.MARRIAGE,
            requestedAmountYer = 200000.0,
            approvedAmountYer = 200000.0,
            status = AidStatus.APPROVED,
            requestDate = "2026-11-01",
            decisionDate = "2026-11-04",
            notes = "مساعدة زواج العضو وفق اللائحة المعتمدة",
            voucherId = null
        ),
        AidRequest(
            id = "aid3",
            requestNo = "REQ-2026-013",
            memberId = "m3",
            memberName = "أ. عبدالحميد قاسم المعمري",
            aidType = AidType.EDUCATION,
            requestedAmountYer = 150000.0,
            approvedAmountYer = 0.0,
            status = AidStatus.IN_REVIEW,
            requestDate = "2026-11-06",
            decisionDate = "",
            notes = "رسوم دراسية جامعية لابن العضو المتفوق كلية الطب",
            voucherId = null
        ),
        AidRequest(
            id = "aid4",
            requestNo = "REQ-2026-014",
            memberId = "m8",
            memberName = "فهد حسين النعماني",
            aidType = AidType.EMERGENCY,
            requestedAmountYer = 100000.0,
            approvedAmountYer = 100000.0,
            status = AidStatus.APPROVED,
            requestDate = "2026-11-05",
            decisionDate = "2026-11-07",
            notes = "أضرار سيول جارفة على مزرعة العضو",
            voucherId = null
        )
    )

    val vouchers = listOf(
        Voucher(
            id = "v_rec_01",
            voucherNo = "V-2026-001",
            type = VoucherType.RECEIPT,
            status = VoucherStatus.POSTED,
            partyName = "د. وضاح ناصر السعيدي",
            amountOriginal = 100.0,
            currency = CurrencyCode.SAR,
            exchangeRate = 665.0,
            amountYer = 66500.0,
            amountWords = "فقط ستة وستون ألف وخمسمائة ريال يمني لا غير",
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            accountCode = "1200",
            accountName = "بنك الكريمي الإسلامي",
            counterpartAccountCode = "4100",
            counterpartAccountName = "إيرادات اشتراكات الأعضاء",
            description = "تحصيل اشتراك نوفمبر 2026 ودفعة مقدمة - حوالة الكريمي #994820",
            date = "2026-11-02",
            linkedJournalEntryId = "JE-000001",
            receivedOrPaidBy = "د. وضاح السعيدي"
        ),
        Voucher(
            id = "v_rec_02",
            voucherNo = "V-2026-002",
            type = VoucherType.RECEIPT,
            status = VoucherStatus.POSTED,
            partyName = "فاعل خير - مغترب بالخارج",
            amountOriginal = 200.0,
            currency = CurrencyCode.USD,
            exchangeRate = 2530.0,
            amountYer = 506000.0,
            amountWords = "فقط خمسمائة وستة آلاف ريال يمني لا غير",
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            accountCode = "1200",
            accountName = "بنك الكريمي الإسلامي",
            counterpartAccountCode = "4200",
            counterpartAccountName = "التبرعات والمنح العامة",
            description = "تبرع عام لدعم الأسر المحتاجة في المناطق والقرى",
            date = "2026-11-03",
            linkedJournalEntryId = "JE-000002",
            receivedOrPaidBy = "الكريمي اكسبرس"
        ),
        Voucher(
            id = "v_pay_01",
            voucherNo = "V-2026-003",
            type = VoucherType.PAYMENT,
            status = VoucherStatus.POSTED,
            partyName = "أ. منصور علي ثابت",
            amountOriginal = 250000.0,
            currency = CurrencyCode.YER,
            exchangeRate = 1.0,
            amountYer = 250000.0,
            amountWords = "فقط مائتان وخمسون ألف ريال يمني لا غير",
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5100",
            accountName = "مصروف المساعدات النقدية للأعضاء",
            counterpartAccountCode = "1100",
            counterpartAccountName = "الصندوق النقدي الرئيسي",
            description = "صرف مساعدة علاجية جراحية معتمدة بموجب الطلب REQ-2026-011",
            date = "2026-10-18",
            linkedJournalEntryId = "JE-000003",
            receivedOrPaidBy = "منصور علي ثابت"
        ),
        Voucher(
            id = "v_pay_02",
            voucherNo = "V-2026-004",
            type = VoucherType.PAYMENT,
            status = VoucherStatus.VOID,
            partyName = "مؤسسة الرواد للطباعة",
            amountOriginal = 45000.0,
            currency = CurrencyCode.YER,
            exchangeRate = 1.0,
            amountYer = 45000.0,
            amountWords = "فقط خمسة وأربعون ألف ريال يمني لا غير",
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5400",
            accountName = "المصروفات التشغيلية والإدارية",
            counterpartAccountCode = "1100",
            counterpartAccountName = "الصندوق النقدي الرئيسي",
            description = "طباعة دفاتر سندات (تم إلغاء السند لوجود خطأ فني بالقيد)",
            date = "2026-10-25",
            linkedJournalEntryId = "JE-000004",
            receivedOrPaidBy = "ملغي"
        )
    )

    val journalEntries = listOf(
        JournalEntry(
            id = "je1",
            entryNo = "JE-000001",
            date = "2026-11-02",
            description = "إثبات تحصيل اشتراك بالريال السعودي معادل 66,500 ريال يمني بموجب السند V-2026-001",
            lines = listOf(
                JournalLine("1200", "بنك الكريمي الإسلامي", 66500.0, 0.0, "مدين - إيداع حوالة بنكية"),
                JournalLine("4100", "إيرادات اشتراكات الأعضاء", 0.0, 66500.0, "دائن - إيراد اشتراك د. وضاح")
            ),
            isPosted = true,
            originalVoucherNo = "V-2026-001"
        ),
        JournalEntry(
            id = "je2",
            entryNo = "JE-000002",
            date = "2026-11-03",
            description = "إثبات تبرع عام بمبلغ 200 دولار أمريكي معادل 506,000 ريال يمني بموجب السند V-2026-002",
            lines = listOf(
                JournalLine("1200", "بنك الكريمي الإسلامي", 506000.0, 0.0, "مدين - إيداع تبرع خارجي"),
                JournalLine("4200", "التبرعات والمنح العامة", 0.0, 506000.0, "دائن - إيراد تبرعات عامة")
            ),
            isPosted = true,
            originalVoucherNo = "V-2026-002"
        ),
        JournalEntry(
            id = "je3",
            entryNo = "JE-000003",
            date = "2026-10-18",
            description = "إثبات صرف مساعدة علاجية للأخ منصور علي بموجب السند V-2026-003",
            lines = listOf(
                JournalLine("5100", "مصروف المساعدات النقدية للأعضاء", 250000.0, 0.0, "مدين - مصروف مساعدة علاجية"),
                JournalLine("1100", "الصندوق النقدي الرئيسي", 0.0, 250000.0, "دائن - صرف نقدي من الخزينة")
            ),
            isPosted = true,
            originalVoucherNo = "V-2026-003"
        ),
        JournalEntry(
            id = "je4_rev",
            entryNo = "JE-000004-R",
            date = "2026-10-25",
            description = "قيد عكسي آلي لإلغاء السند V-2026-004 لمصروفات الطباعة",
            lines = listOf(
                JournalLine("1100", "الصندوق النقدي الرئيسي", 45000.0, 0.0, "مدين - إعادة المبلغ للخزينة"),
                JournalLine("5400", "المصروفات التشغيلية والإدارية", 0.0, 45000.0, "دائن - عكس بند المصروف الملغي")
            ),
            isPosted = true,
            isReversed = true,
            originalVoucherNo = "V-2026-004"
        )
    )

    val budgets = listOf(
        Budget("b1", "المساعدات النقدية للأعضاء", "5100", 5000000.0, 4100000.0, "2026"),
        Budget("b2", "كفالات الأسر والمساعدات الدورية", "5200", 3500000.0, 2850000.0, "2026"),
        Budget("b3", "المساعدات العينية والإغاثية", "5300", 1500000.0, 1200000.0, "2026"),
        Budget("b4", "المصروفات التشغيلية والإدارية", "5400", 800000.0, 550000.0, "2026")
    )

    val donors = listOf(
        Donor("d1", "مجموعة المغتربين الخيرية", "009665012345", "مغتربين", 3500000.0, 5000000.0, 3500000.0, "2026-10-20"),
        Donor("d2", "رجل الأعمال حزام عبد ربه السالمي", "771992019", "فردي", 1800000.0, 2000000.0, 1800000.0, "2026-11-01"),
        Donor("d3", "مؤسسة الأمل التنموية الاجتماعية", "02251092", "مؤسسي", 2500000.0, 3000000.0, 2500000.0, "2026-09-15")
    )

    val campaigns = listOf(
        Campaign(
            id = "c1",
            title = "حملة علاج مرضى الغسيل الكلوي",
            goalAmountYer = 4000000.0,
            collectedAmountYer = 3150000.0,
            spentAmountYer = 2400000.0,
            status = "نشطة",
            description = "توفير جلسات الغسيل ومستلزمات الفلاتر بالمستشفى العام",
            targetCategory = "طبي عاجل"
        ),
        Campaign(
            id = "c2",
            title = "مشروع دفء الشتاء وكسوة الأيتام 2026",
            goalAmountYer = 2500000.0,
            collectedAmountYer = 2100000.0,
            spentAmountYer = 1800000.0,
            status = "نشطة",
            description = "توزيع البطانيات والملابس الشتوية لأسر الأيتام الأشد فقراً",
            targetCategory = "إغاثة موسمية"
        ),
        Campaign(
            id = "c3",
            title = "مشروع تأهيل شبكة المياه بالطاقة الشمسية",
            goalAmountYer = 6000000.0,
            collectedAmountYer = 6000000.0,
            spentAmountYer = 5850000.0,
            status = "مكتملة",
            description = "تمديد أنابيب بطول 2 كم وتركيب مضخة غاطسة تعمل بالطاقة الشمسية للقرى المستفيدة",
            targetCategory = "مشاريع تنموية"
        )
    )

    val beneficiaries = listOf(
        Beneficiary("ben1", "أم محمد صالح السالمي", "104928301", "770119283", 6, 45000.0, PovertyTier.EXTREME, "المنطقة الوسطى", "القرية المركزية", "أرملة تعول 5 أيتام", 4),
        Beneficiary("ben2", "أحمد سالم مقبل الناصري", "104928302", "771228391", 8, 55000.0, PovertyTier.EXTREME, "المنطقة الشرقية", "قرية السلام", "عاجز عن العمل ولديه مريض مزمن", 5),
        Beneficiary("ben3", "فاطمة ناصر عبد ربه", "104928303", "772339402", 4, 60000.0, PovertyTier.HIGH, "المنطقة الوسطى", "الحي الشرقي", "أسرة شهيد بدون معيل دائم", 3),
        Beneficiary("ben4", "محسن قاسم الصالحي", "104928304", "773440513", 7, 75000.0, PovertyTier.MEDIUM, "المنطقة الغربية", "قرية الأمل", "دخل موسمي غير منتظم", 2)
    )

    val periodicAids = listOf(
        PeriodicAid("pa1", "ben1", "أم محمد صالح السالمي (كفالة أيتام)", 70000.0, "كفالة أيتام", "نشطة", "2026-10"),
        PeriodicAid("pa2", "ben2", "أحمد سالم مقبل الناصري (إعانة أسرة)", 60000.0, "إعانة عجز دائم", "نشطة", "2026-10"),
        PeriodicAid("pa3", "ben3", "فاطمة ناصر عبد ربه (كفالة أسرة شهيد)", 50000.0, "كفالة أسرة شهيد", "نشطة", "2026-10")
    )

    val inKindItems = listOf(
        InKindItem("ik1", "IK-101", "سلة غذائية متكاملة (دقيق، رز، سكر، زيت)", "إغاثة غذائية", "سلة", 45, 15, 38000.0),
        InKindItem("ik2", "IK-102", "بطانية شتوية فاخرة مزدوجة", "كسوة ودفء", "حبة", 85, 20, 16000.0),
        InKindItem("ik3", "IK-103", "حقيبة مدرسية متكاملة بالدفاتر والأقلام", "تعليم", "حقيبة", 120, 30, 9500.0),
        InKindItem("ik4", "IK-104", "أنسولين بشري وشرائط فحص السكر", "أدوية ومستلزمات", "طقم", 12, 15, 14000.0) // Low stock alert!
    )

    val inKindMovements = listOf(
        InKindMovement("ikm1", "MOV-2026-001", "توريد", "ik1", "سلة غذائية متكاملة", 50, "منحة مؤسسة الأمل الخيرية", "2026-10-10", "توريد لمستودع الصندوق"),
        InKindMovement("ikm2", "MOV-2026-002", "صرف وتوزيع", "ik1", "سلة غذائية متكاملة", 5, "الأسر الأشد فقراً - المنطقة الوسطى", "2026-10-22", "بموجب كشف التوزيع الميداني")
    )

    val schedulerEvents = listOf(
        SchedulerEvent("ev1", "اجتماع مجلس الإدارة الدوري لمناقشة موازنة 2027", "2026-11-15", "04:30 م", "مقر الصندوق - قاعة الاجتماعات", "مجلس الإدارة", "#1B5E20"),
        SchedulerEvent("ev2", "تدشين حملة توزيع السلال الغذائية للأيتام", "2026-11-20", "09:00 ص", "المستودع المركزي للصندوق", "ميداني", "#F9A825"),
        SchedulerEvent("ev3", "جلسة التسوية المالية الشهرية مع بنك الكريمي", "2026-11-28", "11:00 ص", "الإدارة المالية", "مالي", "#0D47A1")
    )

    val internalMessages = listOf(
        InternalMessage("msg1", "أ. منصور المحاسب", "محاسب", "مدير النظام", "جاهزية القوائم المالية لشهر أكتوبر 2026", "تمت مطابقة ميزان المراجعة وكشف حركة بنك الكريمي بنجاح، ومستعدون للمصادقة.", "2026-11-06 10:30 ص", false),
        InternalMessage("msg2", "د. وضاح السعيدي", "مراجع", "محاسب", "مراجعة تقرير المساعدات الطبية", "نأمل إرفاق الفواتير الأصلية لطلب المساعدة الجراحية الخاصة بالمستشفى التخصصي.", "2026-11-05 02:15 م", true),
        InternalMessage("msg3", "م. ياسر البكري", "عضو هيئة إدارية", "الكل", "اكتمال المرحلة الأولى من مشروع المياه", "بحمد الله وتوفيقه تم الانتهاء من ضخ التجربة في بئر المنطقة الشرقية ونتائج العذوبة ممتازة.", "2026-11-04 06:40 م", true)
    )

    val auditLogs = listOf(
        AuditLog("al1", "2026-11-07 18:22", "مدير النظام (admin)", "مدير النظام", "دخول ناجح", "تسجيل دخول للنظام من لوحة التحكم المركزية"),
        AuditLog("al2", "2026-11-03 14:10", "المحاسب (accountant)", "محاسب", "إصدار سند قبض", "إصدار سند قبض V-2026-002 بمبلغ 200 USD وتوليد قيد JE-000002"),
        AuditLog("al3", "2026-10-25 11:35", "المراجع (reviewer)", "مراجع", "إلغاء سند", "إلغاء السند V-2026-004 وتوليد قيد عكسي تلقائي JE-000004-R"),
        AuditLog("al4", "2026-10-18 09:45", "المحاسب (accountant)", "محاسب", "صرف مساعدة", "صرف مساعدة جراحية للأخ منصور علي ثابت سند V-2026-003")
    )

    val defaultDemoPassword: String = "123456"

    val users by lazy {
        listOf(
            AppUser(
                id = "u1",
                username = "admin",
                fullName = "المدير العام - عبدالحافظ المعمري",
                role = UserRole.ADMIN,
                isActive = true,
                lastLogin = "2026-11-07 18:22",
                passwordHash = com.example.fund.security.SecurityUtils.hashPassword(defaultDemoPassword)
            ),
            AppUser(
                id = "u2",
                username = "accountant",
                fullName = "أمين الصندوق والمحاسب - صالح نصر",
                role = UserRole.ACCOUNTANT,
                isActive = true,
                lastLogin = "2026-11-07 16:10",
                passwordHash = com.example.fund.security.SecurityUtils.hashPassword(defaultDemoPassword)
            ),
            AppUser(
                id = "u3",
                username = "reviewer",
                fullName = "المراجع القانوني - فهد النعماني",
                role = UserRole.REVIEWER,
                isActive = true,
                lastLogin = "2026-11-06 12:00",
                passwordHash = com.example.fund.security.SecurityUtils.hashPassword(defaultDemoPassword)
            ),
            AppUser(
                id = "u4",
                username = "viewer",
                fullName = "المراقب الميداني - قاسم الناصري",
                role = UserRole.VIEWER,
                isActive = true,
                lastLogin = "2026-11-05 09:30",
                passwordHash = com.example.fund.security.SecurityUtils.hashPassword(defaultDemoPassword)
            )
        )
    }

    val currencyRates = mutableMapOf(
        CurrencyCode.YER to 1.0,
        CurrencyCode.USD to 2530.0,
        CurrencyCode.SAR to 665.0
    )
}
