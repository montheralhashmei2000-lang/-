package com.example.fund.ai

import com.example.BuildConfig
import com.example.fund.data.FundRepository
import com.example.fund.model.AidRequest
import com.example.fund.model.Member
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class GeminiPart(val text: String)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiRequest(val contents: List<GeminiContent>)
data class GeminiCandidate(val content: GeminiContent?)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiRestService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

object GeminiFundAdvisor {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val service: GeminiRestService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiRestService::class.java)
    }

    private fun resolveApiKey(): String {
        return runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("").trim()
    }

    private fun hasUsableApiKey(key: String): Boolean {
        return key.isNotBlank() &&
            !key.equals("MY_GEMINI_API_KEY", ignoreCase = true) &&
            !key.equals("null", ignoreCase = true)
    }

    suspend fun analyzeFinancialHealth(): String = withContext(Dispatchers.IO) {
        val repo = FundRepository.getInstance()
        val fundName = repo.fundInfo.value.name
        val ceiling = repo.fundInfo.value.maxAidCeilingYer.toLong()
        val accounts = repo.accounts.value
        val vouchers = repo.vouchers.value
        val journalEntries = repo.journalEntries.value
        val inKindItems = repo.inKindItems.value
        val budgets = repo.budgets.value
        val isOnline = repo.isOnline.value

        val totalAssets = accounts.filter { it.code.startsWith("1") && it.parentCode != null }.sumOf { it.balance }
            .takeIf { it > 0 } ?: accounts.filter { it.code.startsWith("1") }.sumOf { it.balance }
        val totalLiabilities = accounts.filter { it.code.startsWith("2") && it.parentCode != null }.sumOf { it.balance }
            .takeIf { it > 0 } ?: accounts.filter { it.code.startsWith("2") }.sumOf { it.balance }
        val totalRevenue = accounts.filter { it.code.startsWith("4") && it.parentCode != null }.sumOf { it.balance }
            .takeIf { it > 0 } ?: accounts.filter { it.code.startsWith("4") }.sumOf { it.balance }
        val totalExpenses = accounts.filter { it.code.startsWith("5") && it.parentCode != null }.sumOf { it.balance }
            .takeIf { it > 0 } ?: accounts.filter { it.code.startsWith("5") }.sumOf { it.balance }
        val netSurplus = totalRevenue - totalExpenses

        val totalDebits = journalEntries.flatMap { it.lines }.sumOf { it.debit }
        val totalCredits = journalEntries.flatMap { it.lines }.sumOf { it.credit }
        val isBalanced = abs(totalDebits - totalCredits) < 0.01
        val lowStockCount = inKindItems.count { it.quantityOnHand <= it.reorderLevel }
        val overBudgetCount = budgets.count { b ->
            b.budgetedAmount > 0 && ((b.actualSpent / b.budgetedAmount) * 100.0) >= repo.appSettings.value.budgetAlertPercent
        }

        val apiKey = resolveApiKey()
        if (isOnline && hasUsableApiKey(apiKey)) {
            val prompt = """
                أنت محلل مالي ومحاسبي خبير لمؤسسة تكافل اجتماعي عربية اسمها "$fundName".
                قدّم تحليلاً مالياً تنفيذياً موجزاً ومنظماً باللغة العربية (بدون كشف أي بيانات شخصية)، اعتماداً على المؤشرات المجمّعة التالية بالريال اليمني:
                - إجمالي الأصول والسيولة: ${totalAssets.toLong()}
                - إجمالي الالتزامات: ${totalLiabilities.toLong()}
                - إجمالي الإيرادات: ${totalRevenue.toLong()}
                - إجمالي المصروفات: ${totalExpenses.toLong()}
                - صافي الفائض التشغيلي: ${netSurplus.toLong()}
                - عدد السندات: ${vouchers.size} | عدد القيود المزدوجة: ${journalEntries.size} | توازن دفتر اليومية: $isBalanced
                - سقف المساعدة الفردية: $ceiling
                - أصناف مستودع منخفضة المخزون: $lowStockCount | بنود موازنة تجاوزت حد التنبيه: $overBudgetCount
                أعطِ 3 نقاط: 1) صحة السيولة والتوازن المحاسبي، 2) كشف المخاطر أو الشذوذ المالي، 3) توصيات عملية قابلة للتنفيذ.
            """.trimIndent()

            val cloudResult = runCatching {
                val response = service.generateContent(
                    apiKey = apiKey,
                    request = GeminiRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))))
                )
                if (response.isSuccessful) {
                    response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                } else null
            }.getOrNull()

            if (!cloudResult.isNullOrBlank()) {
                return@withContext "✨ تحليل Gemini AI السحابي ($fundName):\n\n$cloudResult"
            }
        }

        return@withContext buildOfflineFinancialReport(
            fundName = fundName,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netSurplus = netSurplus,
            vouchersCount = vouchers.size,
            isBalanced = isBalanced,
            lowStockCount = lowStockCount,
            overBudgetCount = overBudgetCount,
            ceiling = ceiling,
            isOfflineMode = !isOnline || !hasUsableApiKey(apiKey)
        )
    }

    suspend fun evaluateAidEligibility(request: AidRequest, member: Member?): String = withContext(Dispatchers.IO) {
        val repo = FundRepository.getInstance()
        val ceiling = repo.fundInfo.value.maxAidCeilingYer
        val approved = if (request.requestedAmountYer > ceiling) ceiling else request.requestedAmountYer
        val memberPaid = member?.totalPaidYer?.toLong() ?: 0L
        val memberDue = member?.dueBalanceYer?.toLong() ?: 0L
        val isOnline = repo.isOnline.value
        val apiKey = resolveApiKey()

        if (isOnline && hasUsableApiKey(apiKey)) {
            val anonymizedPrompt = """
                بصفتك مستشاراً مالياً لصندوق تكافل اجتماعي، قيّم استحقاق طلب المساعدة رقم ${request.requestNo} (بيانات مجهولة الهوية):
                - فئة المساعدة: ${request.aidType.title}
                - المبلغ المطلوب: ${request.requestedAmountYer.toLong()} ريال يمني
                - السقف النظامي للصندوق: ${ceiling.toLong()} ريال يمني
                - إجمالي اشتراكات العضو المسددة: $memberPaid ريال | المتأخرات: $memberDue ريال
                - حالة العضوية: ${member?.status ?: "مستفيد خارجي"}
                قدّم توصية مهنية مختصرة بالعربية تشمل القرار والمبلغ الموصى به ومبرراته.
            """.trimIndent()

            val cloudResult = runCatching {
                val response = service.generateContent(
                    apiKey = apiKey,
                    request = GeminiRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart(anonymizedPrompt)))))
                )
                if (response.isSuccessful) {
                    response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                } else null
            }.getOrNull()

            if (!cloudResult.isNullOrBlank()) {
                return@withContext "✨ توصية Gemini AI السحابية للطلب (${request.requestNo}):\n\n$cloudResult"
            }
        }

        val screenFormat = repo.screenFormat.value
        val eligibilityNote = if (memberDue > 0L) {
            "توصية بسداد المتأخرات (${com.example.fund.util.FormatUtils.formatAmount(memberDue.toDouble(), screenFormat)}) أو خصمها بالتقسيط الميسّر وفق اللائحة."
        } else {
            "العضو منتظم تماماً في سداد الاشتراكات الشهرية ولا توجد عليه متأخرات."
        }

        val reqNoFormatted = com.example.fund.util.FormatUtils.formatCodeOrNumber(request.requestNo, screenFormat)
        return@withContext """
            📋 توصية التحليل المالي المحلي لطلب المساعدة ($reqNoFormatted):

            - المستفيد: ${member?.fullName ?: request.memberName} (سدد: ${com.example.fund.util.FormatUtils.formatAmount(memberPaid.toDouble(), screenFormat)} | متأخرات: ${com.example.fund.util.FormatUtils.formatAmount(memberDue.toDouble(), screenFormat)})
            - القرار المقترح: اعتماد المساعدة ضمن السقف النظامي للصندوق.
            - المبلغ الموصى باعتماده: ${com.example.fund.util.FormatUtils.formatAmount(approved, screenFormat)}.
            - مبررات القرار:
              1. مطابقة الحالة للائحة المساعدات الاجتماعية للصندوق (فئة: ${request.aidType.title}).
              2. $eligibilityNote
              3. كفاية رصيد بند المساعدات في الموازنة التقديرية المعتمدة لهذا العام.
        """.trimIndent()
    }

    private fun buildOfflineFinancialReport(
        fundName: String,
        totalAssets: Double,
        totalLiabilities: Double,
        totalRevenue: Double,
        totalExpenses: Double,
        netSurplus: Double,
        vouchersCount: Int,
        isBalanced: Boolean,
        lowStockCount: Int,
        overBudgetCount: Int,
        ceiling: Long,
        isOfflineMode: Boolean
    ): String {
        val screenFormat = com.example.fund.data.FundRepository.getInstance().screenFormat.value
        val coverage = if (totalLiabilities > 0) ((totalAssets / totalLiabilities) * 100).toInt() else 100
        val modeTag = if (isOfflineMode) "⚡ محرك التحليل المحلي (Offline Fallback)" else "📊 تقرير التحليل المالي الذكي"
        return """
            $modeTag - $fundName:

            1. مؤشر السيولة والتوازن المحاسبي:
               - مستوى السيولة والأصول: ${com.example.fund.util.FormatUtils.formatAmount(totalAssets, screenFormat)}، مقابل التزامات قدرها ${com.example.fund.util.FormatUtils.formatAmount(totalLiabilities, screenFormat)} (نسبة التغطية: $coverage%).
               - حالة دفتر القيود المزدوجة: ${if (isBalanced) "متوازن بالكامل ✔" else "يتطلب مراجعة فرق التوازن ⚠"}.

            2. كفاءة الصرف والتنبيهات (${com.example.fund.util.FormatUtils.formatCodeOrNumber(vouchersCount.toString(), screenFormat)} سند):
               - صافي الفائض التشغيلي: ${com.example.fund.util.FormatUtils.formatAmount(netSurplus, screenFormat)} (إيرادات: ${com.example.fund.util.FormatUtils.formatAmount(totalRevenue, screenFormat)} | مصروفات: ${com.example.fund.util.FormatUtils.formatAmount(totalExpenses, screenFormat)}).
               - تنبيهات المخزون والموازنة: $lowStockCount أصناف قاربت حد إعادة الطلب | $overBudgetCount بنود موازنة تجاوزت حد التنبيه.

            3. التوصيات الاستراتيجية:
               - توجيه 20% من الفائض المتراكم نحو المشاريع التنموية المستدامة في القرى والمناطق المستفيدة.
               - تفعيل خطة تحصيل للاشتراكات المتأخرة للأعضاء عبر وسائل الدفع الإلكتروني.
               - الحفاظ على سقف المساعدات الفردية عند ${com.example.fund.util.FormatUtils.formatAmount(ceiling.toDouble(), screenFormat)} لضمان استدامة العدالة التوزيعية.
        """.trimIndent()
    }
}
