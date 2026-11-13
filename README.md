# نظام إدارة الصندوق الاجتماعي التنموي — الإصدار 9.0

تطبيق أندرويد متكامل (Kotlin + Jetpack Compose + Material 3) بواجهة عربية أصيلة (`RTL`) لإدارة الصناديق الاجتماعية والتنموية في المناطق والقرى، مع محاسبة قيد مزدوج حقيقية، وتخزين محلي مستديم في قاعدة بيانات **Room SQLite**، ومصادقة مشفرة، ومعاينة وطباعة رسمية للسندات والتقارير.

---

## المعمارية الهندسية (MVVM + Room + Local-First)

يعتمد المشروع على فصل واضح للمسؤوليات:

- **طبقة الواجهة (`app/src/main/java/com/example/fund/ui/`)**:
  - 24 شاشة مكتملة تغطي لوحة المعلومات، الأعضاء، الاشتراكات، المساعدات، الخزينة، السندات، القيود اليومية، شجرة الحسابات، القوائم المالية، الموازنات، الحملات، الداعمين، المستفيدين، الكفالات، المستودع العيني، المواعيد، الرسائل، التقارير، بيانات الصندوق، المستخدمين، التدقيق، والإعدادات.
  - `ViewModel`s متخصصة (`AuthViewModel`, `DashboardViewModel`, `AccountingViewModel`, `MembersViewModel`, `AidsViewModel`, `SettingsViewModel`, `SyncViewModel`) مع `StateFlow` و `collectAsStateWithLifecycle()`.
- **طبقة المستودعات (`app/src/main/java/com/example/fund/data/repository/`)**:
  - `AccountingRepository`: محرك القيد المزدوج، التحقق الصارم (`AccountingValidator`)، سندات القبض/الصرف، القيود العكسية، التحويلات الداخلية، وتحديث الأرصدة داخل معاملات `Room @Transaction`.
  - `MemberRepository`: إدارة الأعضاء، هيكلية المناطق والقرى المفصولة (`RegionGroup`)، والاشتراكات.
  - `AidRepository`: طلبات المساعدات، الأسر المستفيدة، الكفالات الدورية، المانحين، الحملات، والمستودع العيني.
  - `UserRepository`: إدارة المستخدمين وصلاحيات الأدوار (`RBAC`) والمصادقة وسجل التدقيق (`AuditLog`).
  - `SettingsRepository`: بيانات الصندوق والشعار المخصص، الإعدادات التشغيلية، وإعدادات الطباعة والخطوط (`PrintSettings`).
  - `SyncRepository`: طابور العمل المحلي (`Offline-First`) وواجهة **Retrofit REST** (`SyncApiService`) مع توثيق البصمة الرقمية `SHA-256`.
- **طبقة التخزين المحلي والأمان**:
  - `FundDatabase` (`Room SQLite`): 25 جدولاً مع فهارس ومفاتيح أجنبية و`FundTypeConverters`.
  - `LegacyMigrationHelper`: ترحيل تلقائي لمرة واحدة من `SharedPreferences` القديمة إلى Room عند أول تشغيل.
  - `SecurityUtils`: تجزئة كلمات المرور باستخدام `PBKDF2WithHmacSHA256` مع ملح عشوائي لكل مستخدم (`Salt`) ومقارنة ثابتة الزمن (`MessageDigest.isEqual`).
  - `EncryptedSessionManager`: حفظ بيانات الجلسة والإعدادات الحساسة عبر `EncryptedSharedPreferences` (`AndroidKeyStore AES-256-GCM`).
- **المستشار المالي الذكي (`GeminiFundAdvisor`)**:
  - يتصل بواجهة **Gemini REST API** عبر `Retrofit + Moshi` باستخدام `BuildConfig.GEMINI_API_KEY` (عند تفعيل المفتاح والاتصال بالإنترنت)، مع تحوّل تلقائي سلس إلى محرك التحليل المالي المحلي عند عدم الاتصال أو عدم توفر المفتاح.

---

## الحسابات الافتراضية للتجربة

تُحفظ كلمات المرور بالتجزئة فقط (`PBKDF2`) ولا تُخزن كنص صريح. كلمة المرور الافتراضية للحسابات التجريبية الأربعة هي: `123456`

| اسم المستخدم | الدور الوظيفي |
| --- | --- |
| `admin` | مدير النظام |
| `accountant` | محاسب / أمين الصندوق |
| `reviewer` | المراجع المالي |
| `viewer` | مراقب ميداني |

---

## إعداد مفتاح Gemini API والأسرار

يستخدم المشروع إضافة **Secrets Gradle Plugin** لقراءة المفاتيح من `.env` (أو `.env.example` كقيمة افتراضية).

1. في بيئة **AI Studio**: أضف المتغير `GEMINI_API_KEY` في لوحة **Secrets**.
2. أو محلياً: أنشئ ملف `.env` في جذر المشروع (تأكد أنه غير مرفوع إلى Git) وضع فيه:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key
   ```
3. عند غياب المفتاح أو العمل في الوضع غير المتصل (`Offline`)، يعمل المستشار المالي تلقائياً بالمحرك المحلي دون أخطاء.

---

## أوامر البناء والاختبار

```bash
# فحص وبناء حزمة Debug
gradle assembleDebug

# تشغيل اختبارات الوحدة واختبارات Robolectric (التفقيط، القيد المزدوج، الأمان، الترحيل لـ Room)
gradle :app:testDebugUnitTest
```
