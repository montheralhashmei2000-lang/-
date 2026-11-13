# برومبت تنفيذي لـ Google AI Studio: خطة تطوير وإصلاح مشروع «صندوق الحبيلين»

> انسخ النص التالي بالكامل والصقه في **Google AI Studio** بعد ربطه بملفات المشروع:

---

```text
أنت مهندس برمجيات أندرويد خبير (Senior Android Architect) متخصص في Kotlin و Jetpack Compose و Room SQLite والأنظمة المحاسبية ذات القيد المزدوج.

المطلوب: تنفيذ خطة تطوير وإصلاح شاملة لمشروع تطبيق «صندوق الحبيلين» (Hubailin Charitable Fund - `com.aistudio.fundhubaylin.kxmpzq`) المبني بـ Kotlin و Jetpack Compose (Material 3)، مع الحفاظ التام على جميع الشاشات الـ 24 الحالية، والنصوص العربية، والوضع الليلي (`isDarkMode`)، وتكبير الخطوط (`fontScale`)، والبيانات التأسيسية للمناطق في `SeedData.kt` (الحبيلين، الملاح، ردفان، إلخ).

نفّذ التعديلات التالية بدقة وبأكواد كاملة قابلة للبناء والتشغيل دون اختصارات أو `TODO`:

### 1. استبدال `SharedPreferences` بقاعدة بيانات Room (SQLite)
- حوّل جميع الكيانات في `Entities.kt` (`User`, `Member`, `Beneficiary`, `AidRequest`, `Voucher`, `Account`, `JournalEntry`, `Campaign`, `InKindAid`, `Subscription`, `AuditLog`, `FinancialReport`, `PrintSettings`, `PeriodicAid`, `ScheduledJob`, `Message`) إلى جداول Room (`@Entity`) مع فهارس (`@Index`) ومفاتيح أجنبية (`ForeignKey`) مناسبة.
- أنشئ `FundDatabase` وواجهات `@Dao` لكل مجال بحيث تعيد `Flow<List<...>>` للاستعلامات التفاعلية.
- استخدم معالج `KSP` المضاف مسبقاً في `gradle/libs.versions.toml` وفعّل Room في `app/build.gradle.kts`.
- أضف آلية ترحيل تلقائية لمرة واحدة (One-time Migration) عند أول تشغيل: تقرأ بيانات JSON القديمة من `SharedPreferences` (إن وجدت)، وتنقلها إلى Room داخل `@Transaction`، ثم تنظف المفاتيح القديمة.

### 2. الأمان وتشفير كلمات المرور والبيانات الحساسة
- أوقف تخزين كلمات المرور كنص صريح. أنشئ `SecurityUtils.kt` لتجزئة كلمات المرور باستخدام `PBKDF2WithHmacSHA256` مع Salt عشوائي فريد لكل مستخدم، والتحقق منها عند تسجيل الدخول في `LoginScreen.kt`.
- انقل بيانات الجلسة الحالية وإعدادات المستخدم الحساسة إلى `EncryptedSharedPreferences`.

### 3. تشديد المنطق المحاسبي والقيد المزدوج
- في طبقة المستودع المحاسبي، افرض تحققاً صارماً يمنع حفظ أو ترحيل أي `JournalEntry` ما لم يكن إجمالي المدين مساوياً لإجمالي الدائن (`totalDebit == totalCredit`) وأكبر من صفر.
- نفّذ عمليات إنشاء السند (`Voucher`) وتوليد القيد اليومي المقابل (`JournalEntry`) وتسجيل سجل التدقيق (`AuditLog`) داخل Room `@Transaction` واحدة لضمان عدم حدوث أي خلل أو تضارب في الأرصدة عند انقطاع التطبيق.
- راجع `TafqeetEngine.kt` وعالج كافة الحالات الحدية (الصفر، المبالغ السالبة، مطابقة المعدود في الآلاف والملايين باللغة العربية، وتنسيق الريال اليمني).

### 4. إعادة الهيكلة (Refactoring) إلى MVVM وتوحيد الحزم
- قسّم الكلاس المتضخم `FundRepository.kt` (1524 سطراً) إلى مستودعات متخصصة:
  1. `AccountingRepository` (الحسابات، السندات، القيود، الخزينة، الموازنات، التقارير المالية).
  2. `MemberRepository` (الأعضاء، المجموعات المناطقية، الاشتراكات، بطاقات الأعضاء).
  3. `AidRepository` (المستفيدون، طلبات الإعانة، المساعدات الدورية والعينية، الحملات).
  4. `UserRepository` (المستخدمون، الصلاحيات، سجل التدقيق `AuditLog`).
  5. `SettingsRepository` (إعدادات الطباعة، المظهر، المجدول، الرسائل).
- أنشئ `ViewModel` لكل شاشة أو مجموعة شاشات مترابطة باستخدام `StateFlow` بدلاً من تعديل الحالة مباشرة من واجهات Compose.
- انقل جميع الملفات من حزمة `com.example.*` إلى `com.aistudio.fundhubaylin.*` لتتطابق مع `applicationId` في `app/build.gradle.kts`، وتأكد من تحديث `AndroidManifest.xml` وجميع عبارات `package` و`import`.

### 5. المزامنة والنسخ الاحتياطي وتفعيل GeminiFundAdvisor
- اربط `LocalServerSyncDialog.kt` بـ `SyncRepository` يدعم:
  - تصدير واستيراد نسخة احتياطية كاملة (JSON / SQLite) مشفرة أو موثقة.
  - المزامنة عبر الشبكة المحلية أو خادم REST باستخدام `Retrofit` الموجود مسبقاً في المشروع.
- حدّث `GeminiFundAdvisor.kt` ليدعم الاتصال الفعلي بـ Gemini API (بقراءة المفتاح بأمان من `BuildConfig` / `.env`) لتحليل التقارير المالية واكتشاف الشذوذ المحاسبي، مع الإبقاء على محرك القواعد المحلي الحالي كبديل تلقائي (Offline Fallback) عند انقطاع الإنترنت.

### 6. الاختبارات الآلية والتوثيق
- أضف اختبارات وحدة (Unit Tests) في `app/src/test/` تغطي:
  - `TafqeetEngineTest`: التحقق من التفقيط لمختلف الأرقام والحالات الحدية.
  - `AccountingValidationTest`: التحقق من رفض القيود غير المتوازنة وتطابق أرصدة السندات.
  - `SecurityUtilsTest`: التحقق من التجزئة ومطابقة كلمات المرور.
- أنشئ ملف `README.md` شامل في جذر المستودع يوضح: نظرة عامة على النظام، البنية المعمارية الجديدة (MVVM + Room)، خطوات البناء والتشغيل، وإعداد ملف `.env`.

### قيود التنفيذ:
- التزم بأحدث ممارسات Kotlin و Jetpack Compose (Material 3) المتوافقة مع `compileSdk = 36` و `minSdk = 24`.
- لا تحذف أي ميزة قائمة في الشاشات الحالية (مثل مسح وقراءة QR Code، وطباعة السندات الرسمية عبر `OfficialPrintHelper.kt`).
- اعرض خطة التعديل أولاً ثم قدّم الأكواد الكاملة لكل ملف جديد أو معدّل مرتباً حسب المسار.
```
