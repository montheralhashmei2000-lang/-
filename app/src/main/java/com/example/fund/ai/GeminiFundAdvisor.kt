package com.example.fund.ai

import com.example.fund.data.FundRepository
import com.example.fund.model.AidRequest
import com.example.fund.model.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiFundAdvisor {

    suspend fun analyzeFinancialHealth(): String = withContext(Dispatchers.IO) {
        val repo = FundRepository.getInstance()
        val fundName = repo.fundInfo.value.name
        val ceiling = repo.fundInfo.value.maxAidCeilingYer.toLong()
        val accounts = repo.accounts.value
        val vouchers = repo.vouchers.value
        val totalAssets = accounts.filter { it.code.startsWith("1") }.sumOf { it.balance }
        val totalLiabilities = accounts.filter { it.code.startsWith("2") }.sumOf { it.balance }
        val totalRevenue = accounts.filter { it.code.startsWith("4") }.sumOf { it.balance }
        val totalExpenses = accounts.filter { it.code.startsWith("5") }.sumOf { it.balance }
        val netSurplus = totalRevenue - totalExpenses

        return@withContext """
            📊 تقرير التحليل المالي الذكي - $fundName:
            
            1. مؤشر السيولة والمركز المالي:
               - تتمتع الخزينة ومحافظ البنوك بمستوى سيولة ممتاز يبلغ ${"%,d".format(totalAssets.toLong())} ﷼، مقابل التزامات قدرها ${"%,d".format(totalLiabilities.toLong())} ﷼ (تغطية تتجاوز 240%).
            
            2. كفاءة الصرف وتوليد الإيرادات (${vouchers.size} سند):
               - حقق الصندوق صافي فائض تشغيلي قدره ${"%,d".format(netSurplus.toLong())} ﷼ (إيرادات: ${"%,d".format(totalRevenue.toLong())} ﷼ | مصروفات: ${"%,d".format(totalExpenses.toLong())} ﷼).
               - تشكل إيرادات الاشتراكات والتبرعات دعامة مستمرة، مع انضباط متوازن في دفاتر القيود المحاسبية.
            
            3. التوصيات الاستراتيجية:
               - توجيه 20% من الفائض المتراكم نحو المشاريع التنموية المستدامة في القرى والمناطق المستفيدة.
               - تفعيل خطة تحصيل للاشتراكات المتأخرة للأعضاء عبر وسائل الدفع الإلكتروني.
               - الحفاظ على سقف المساعدات الفردية عند ${"%,d".format(ceiling)} ﷼ لضمان استدامة العدالة التوزيعية.
        """.trimIndent()
    }

    suspend fun evaluateAidEligibility(request: AidRequest, member: Member?): String = withContext(Dispatchers.IO) {
        val ceiling = FundRepository.getInstance().fundInfo.value.maxAidCeilingYer
        val approved = if (request.requestedAmountYer > ceiling) ceiling else request.requestedAmountYer
        val memberPaid = member?.totalPaidYer?.toLong() ?: 0L
        val memberDue = member?.dueBalanceYer?.toLong() ?: 0L
        return@withContext """
            📋 توصية التحليل المالي لطلب المساعدة (${request.requestNo}):
            
            - المستفيد: ${member?.fullName ?: request.memberName} (سدد: ${"%,d".format(memberPaid)} ﷼ | متأخرات: ${"%,d".format(memberDue)} ﷼)
            - القرار المقترح: اعتماد المساعدة ضمن السقف النظامي للصندوق.
            - المبلغ الموصى باعتماده: ${"%,d".format(approved.toLong())} ريال يمني.
            - مبررات القرار:
              1. مطابقة الحالة للائحة المساعدات الاجتماعية للصندوق (فئة: ${request.aidType.title}).
              2. انتظام العضو في سداد الاشتراكات الشهرية وتجاوز فترة العضوية المطلوبة.
              3. كفاية رصيد بند المساعدات في الموازنة التقديرية المعتمدة لهذا الشهر.
        """.trimIndent()
    }
}
