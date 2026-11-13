# برومبت تنفيذي لـ Google AI Studio: إعدادات الأرقام والعملات والتاريخ + خطوط الواجهة + إصلاح الأخطاء المتبقية

> انسخ النص التالي بالكامل والصقه في **Google AI Studio** بعد ربطه بملفات المشروع الحالية:

```
أنت مهندس برمجيات أندرويد خبير (Senior Android Architect) متخصص في Kotlin و Jetpack Compose (Material 3) و Room SQLite.

المطلوب: إضافة نظام متكامل لتخصيص الأرقام والعملات والتواريخ وخطوط الواجهة، وإصلاح الأخطاء المتبقية في تطبيق «صندوق الحبيلين» (`com.aistudio.fundhubaylin.kxmpzq`) المبني بمعمارية MVVM + Room، مع الحفاظ التام على جميع الشاشات والبيانات الحالية.

نفّذ التعديلات التالية بأكواد كاملة قابلة للبناء والتشغيل دون اختصارات أو `TODO`:

### 1. إعدادات الأرقام والعملات والتاريخ (للشاشات والمطبوعات بشكل مستقل)
- أضف كياناً/نموذجاً للإعدادات في `Entities.kt` (ومطابقته في `RoomEntities.kt` / `SettingsRepository.kt` / `SettingsViewModel.kt`) يحتوي على ملفين منفصلين للتنسيق: `screenFormat: DisplayFormatConfig` (للشاشات) و `printFormat: DisplayFormatConfig` (للمطبوعات وPDF)، بحيث يتضمن كل ملف:
  - لغة الأرقام (`LATIN` مثل `123` أو `ARABIC_INDIC` مثل `١٢٣`).
  - فاصل الآلاف (`COMMA` `,`، `DOT` `.`، `SPACE` ` `، أو `NONE`).
  - الفاصل العشري (`DOT` `.` أو `COMMA` `,`).
  - عدد خانات الكسور العشرية (`0`، `2`، أو `3` خانات).
  - الحد الأقصى لخانات المبالغ في حقول الإدخال (مثلاً 9، 12، أو 15 خانة).
  - العملة الافتراضية ورمزها (مثل: `ريال يمني / ر.ي`، `ريال سعودي / ر.س`، `دولار / $` أو مخصص) مع خيار إظهار الرمز قبل أو بعد الرقم أو إخفائه في الجداول.
  - صيغة التاريخ (`dd/MM/yyyy`، `yyyy-MM-dd`، `dd MMM yyyy`، مع خيار إظهار الوقت، وخيار التقويم الميلادي/الهجري).
- أنشئ أداة تنسيق مركزية `FormatUtils.kt` في `fund/util/` توفّر دوال:
  - `formatAmount(amount, config)` و `formatDate(timestamp, config)` و `toArabicDigitsIfNeeded(...)`.
- أضف تبويباً أو قسماً واضحاً في `SettingsScreen.kt` (و `PrintSettingsTab.kt` للمطبوعات) يتيح ضبط كل هذه الخيارات مع **بطاقة معاينة حية** تعرض مثالاً فورياً لشكل الرقم والتاريخ والعملة على الشاشة وفي المطبوعة قبل الحفظ.
- استبدل جميع نصوص العملة الصلبة (`ر.ي`) وتنسيقات الأرقام الثابتة في `MoneyInput.kt` و `FinancialReportsScreen.kt` و `VouchersScreen.kt` و `JournalsScreen.kt` و `PdfGenerator.kt` و `PrintUtils.kt` لتقرأ ديناميكياً من `SettingsViewModel` عبر `FormatUtils`.

### 2. إعدادات خط الواجهة في «المظهر والهوية»
- أضف عدة خطوط عربية واضحة ومجانية (مثل: خط النظام الافتراضي `System`، `Cairo`، `Tajawal`، `IBM Plex Sans Arabic`، و `Amiri`) في `app/src/main/res/font/` (أو عبر `Downloadable Fonts` / ملفات `.ttf` مدمجة).
- أضف حقل `appFontFamily` إلى إعدادات المظهر في `SettingsRepository.kt` و `SettingsViewModel.kt`.
- أضف قائمة اختيار الخط في `SettingsTabsExtra.kt` (قسم المظهر والهوية) مع **معاينة فورية** لجملة عربية وأرقام مالية بالخط المختار.
- حدّث `ui/theme/Type.kt` و `ui/theme/Theme.kt` و `MainActivity.kt` لتطبيق الخط المختار على `MaterialTheme.typography` لحظياً عبر التطبيق بالكامل، مع الحفاظ على `fontFeatureSettings = "tnum"` في الجداول والأرقام المالية.

### 3. إصلاح الأخطاء المتبقية في المستودع
- **تنظيف `app/build.gradle.kts`:** احذف كافة الأسطر المعلّقة القديمة (`firebase.bom`, `datastore.preferences`, `coil.compose`) وأي plugin غير مستخدم فعلياً (مثل `google.services` إذا لم يكن هناك `google-services.json` مستخدم).
- **معالجة أخطاء البصمة في `LoginScreen.kt`:** تحقق أولاً عبر `BiometricManager.canAuthenticate(...)` قبل إظهار زر البصمة، وأضف معالجة آمنة للأجهزة التي لا تحتوي على مستشعر بصمة أو لم تسجل بصمات بحيث تعود تلقائياً للدخول بكلمة المرور أو الـ PIN دون انهيار.
- **معالجة أخطاء الكاميرا في `QrScannerDialog.kt`:** أضف فحصاً واضحاً لحالة رفض إذن الكاميرا الدائم أو عدم توفر كاميرا في الجهاز مع رسالة توضيحية وزر لفتح إعدادات التطبيق، وأضف زر تشغيل الفلاش (Torch) والتركيز التلقائي.

### قيود التنفيذ:
- التزم بـ `compileSdk = 36` و `minSdk = 24` وممارسات Kotlin و Jetpack Compose (Material 3).
- اعرض خطة التعديل أولاً ثم قدّم الأكواد الكاملة لكل ملف جديد أو معدّل مرتباً حسب المسار.
```
