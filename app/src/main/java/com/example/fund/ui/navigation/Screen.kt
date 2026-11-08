package com.example.fund.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val title: String,
    val icon: ImageVector,
    val category: String = "الرئيسية"
) {
    LOGIN("تسجيل الدخول", Icons.Default.Lock, "النظام"),
    DASHBOARD("لوحة المعلومات", Icons.Default.Dashboard, "الرئيسية"),
    MEMBERS("إدارة الأعضاء", Icons.Default.People, "الأعضاء"),
    SUBSCRIPTIONS("الاشتراكات الشهرية", Icons.Default.Payment, "الأعضاء"),
    AIDS("طلبات المساعدة", Icons.Default.VolunteerActivism, "المساعدات"),
    BENEFICIARIES("الأسر المستفيدة", Icons.Default.FamilyRestroom, "المساعدات"),
    PERIODIC_AIDS("المساعدات الدورية والكفالات", Icons.Default.EventRepeat, "المساعدات"),
    IN_KIND("المساعدات العينية والمستودع", Icons.Default.Inventory2, "المساعدات"),
    TREASURY("إدارة الخزينة والنقدية", Icons.Default.AccountBalanceWallet, "المالية"),
    VOUCHERS("سندات القبض والصرف", Icons.Default.ReceiptLong, "المالية"),
    JOURNAL("دفتر القيود اليومية", Icons.Default.MenuBook, "المالية"),
    ACCOUNTS("دليل الحسابات والبنوك", Icons.Default.AccountTree, "المالية"),
    BUDGETS("الموازنات الشهرية", Icons.Default.PieChart, "المالية"),
    FINANCIAL_REPORTS("القوائم المالية والعملات", Icons.Default.Assessment, "المالية"),
    DONORS("المانحون والتعهدات", Icons.Default.Handshake, "التبرعات"),
    CAMPAIGNS("حملات التبرعات", Icons.Default.Campaign, "التبرعات"),
    SCHEDULER("المواعيد والجدول", Icons.Default.CalendarMonth, "الإدارة"),
    MESSAGES("الرسائل الداخلية", Icons.Default.Email, "الإدارة"),
    REPORTS("التقارير والإحصائيات", Icons.Default.TableChart, "التقارير"),
    FUND_INFO("بيانات الصندوق", Icons.Default.Info, "الإدارة"),
    USERS("إدارة المستخدمين", Icons.Default.ManageAccounts, "النظام"),
    AUDIT("سجل العمليات الرقابي", Icons.Default.History, "النظام"),
    SETTINGS("مركز الإعدادات", Icons.Default.Settings, "النظام")
}
