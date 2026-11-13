# برومبت تنفيذي لـ Google AI Studio: إعدادات الأرقام والخطوط + إزالة الشاشات المكررة وتوحيد مسارات الدخول

> انسخ النص التالي بالكامل والصقه في **Google AI Studio** بعد ربطه بملفات المشروع الحالية:

```
أنت مهندس برمجيات أندرويد خبير (Senior Android Architect) متخصص في Kotlin و Jetpack Compose (Material 3) و Room SQLite.

المطلوب: تنفيذ التعديلات التالية في تطبيق «صندوق الحبيلين» (`com.aistudio.fundhubaylin.kxmpzq`) المبني بمعمارية MVVM + Room، بأكواد كاملة قابلة للبناء والتشغيل دون اختصارات أو `TODO`:

### 1. دمج إعدادات الأرقام والعملات والتاريخ داخل «تبويب الطباعة والسندات» وحذف التبويب المكرر
- **حذف التبويب الجزئي المكرر:** احذف تبويب أو شاشة «الأرقام والتاريخ» المنفصلة من `SettingsScreen.kt` و `SettingsTabsExtra.kt` نهائياً، وادمج جميع خياراتها داخل تبويب **«الطباعة والسندات» (`PrintSettingsTab.kt`)** ليكون هو المكان الوحيد المسؤول عن تنسيق الأرقام والعملات والتواريخ والطباعة.
- أضف كياناً/نموذجاً للإعدادات في `Entities.kt` (ومطابقته في `RoomEntities.kt` / `SettingsRepository.kt` / `SettingsViewModel.kt`) يحتوي على ملفين منفصلين للتنسيق داخل `PrintSettings`: `screenFormat: DisplayFormatConfig` (لأرقام الشاشات) و `printFormat: DisplayFormatConfig` (لأرقام المطبوعات وPDF)، بحيث يتضمن كل ملف:
  - لغة الأرقام (`LATIN` مثل `123` أو `ARABIC_INDIC` مثل `١٢٣`).
  - فاصل الآلاف (`COMMA` `,`، `DOT` `.`، `SPACE` ` `، أو `NONE`).
  - الفاصل العشري (`DOT` `.` أو `COMMA` `,`).
  - عدد خانات الكسور العشرية (`0`، `2`، أو `3` خانات).
  - الحد الأقصى لخانات المبالغ في حقول الإدخال (مثلاً 9، 12، أو 15 خانة).
  - العملة الافتراضية ورمزها (مثل: `ريال يمني / ر.ي`، `ريال سعودي / ر.س`، `دولار / $` أو مخصص) مع خيار إظهار الرمز قبل أو بعد الرقم أو إخفائه في الجداول.
  - صيغة التاريخ (`dd/MM/yyyy`، `yyyy-MM-dd`، `dd MMM yyyy`، مع خيار إظهار الوقت، وخيار التقويم الميلادي/الهجري).
- أنشئ أداة تنسيق مركزية `FormatUtils.kt` في `fund/util/` توفّر دوال `formatAmount(amount, config)` و `formatDate(timestamp, config)` و `toArabicDigitsIfNeeded(...)`.
- أضف في `PrintSettingsTab.kt` **بطاقة معاينة حية** تعرض مثالاً فورياً لشكل الرقم والتاريخ والعملة على الشاشة وفي المطبوعة قبل الحفظ.
- استبدل جميع نصوص العملة الصلبة (`ر.ي`) وتنسيقات الأرقام الثابتة في `MoneyInput.kt` و `FinancialReportsScreen.kt` و `VouchersScreen.kt` و `JournalsScreen.kt` و `PdfGenerator.kt` و `PrintUtils.kt` لتقرأ ديناميكياً من `SettingsViewModel` عبر `FormatUtils`.

### 2. توحيد مسارات الدخول وإزالة التكرار في الشاشات والتبويبات (Single Entry Point)
- راجع جميع مسارات التنقل في `HomeShell.kt`, `NavRoutes.kt`, `AppBottomBar.kt`, `AppDrawer.kt`, `HubScreens.kt`, `GlobalNavigatorDialog.kt`, `ModuleContextBar.kt`, و `SettingsScreen.kt` وطبق قاعدة **«مسار دخول واحد فقط لكل شاشة أو تبويب»**:
  - **شاشات التبويبات الرئيسية (`AppBottomBar.kt` + `HubScreens.kt`):** تُفتح حصرياً من الشريط السفلي ومراكزها (Hubs)، واحذف أي روابط مكررة لنفس الشاشات من القائمة الجانبية `AppDrawer.kt` أو `GlobalNavigatorDialog.kt`.
  - **شاشات الإدارة والإعدادات والنظام (`AppDrawer.kt`):** اجعل شاشات (الإعدادات، سجل التدقيق، المستخدمون، المجدول، الرسائل، معلومات الصندوق) تُفتح من `AppDrawer.kt` فقط، ولا تكررها في `HubScreens.kt` أو `DashboardScreen.kt`.
  - **أزل أي مكونات تنقل زائدة تسبب ازدواجية المسارات:** إذا كان `GlobalNavigatorDialog.kt` أو `ModuleContextBar.kt` يكرر نفس روابط `AppDrawer` أو `HubScreens`، احذفه أو حوّله إلى تنقل فرعي غير مكرر، بحيث يعرف المستخدم دائماً مكاناً واحداً ثابتاً للدخول إلى أي شاشة أو تبويب.
  - تأكد من أن تبويبات `SettingsScreen.kt` لا تحتوي على أي تبويبين متشابهين أو متداخلين في البيانات (مثل دمج الأرقام والتاريخ في `PrintSettingsTab.kt`).

### 3. إعدادات خط الواجهة في «المظهر والهوية»
- أضف عدة خطوط عربية واضحة ومجانية (مثل: خط النظام الافتراضي `System`، `Cairo`، `Tajawal`، `IBM Plex Sans Arabic`، و `Amiri`) في `app/src/main/res/font/`.
- أضف حقل `appFontFamily` إلى إعدادات المظهر في `SettingsRepository.kt` و `SettingsViewModel.kt`.
- أضف قائمة اختيار الخط في `SettingsTabsExtra.kt` (تبويب المظهر والهوية) مع **معاينة فورية** لجملة عربية وأرقام مالية بالخط المختار.
- حدّث `ui/theme/Type.kt` و `ui/theme/Theme.kt` و `MainActivity.kt` لتطبيق الخط المختار على `MaterialTheme.typography` لحظياً عبر التطبيق بالكامل، مع الحفاظ على `fontFeatureSettings = "tnum"` في الجداول والأرقام المالية.

### 4. إصلاح الأخطاء المتبقية في المستودع
- **تنظيف `app/build.gradle.kts`:** احذف كافة الأسطر المعلّقة القديمة (`firebase.bom`, `datastore.preferences`, `coil.compose`) وأي plugin غير مستخدم فعلياً (مثل `google.services` إذا لم يكن هناك `google-services.json` مستخدم).
- **معالجة أخطاء البصمة في `LoginScreen.kt`:** تحقق أولاً عبر `BiometricManager.canAuthenticate(...)` قبل إظهار زر البصمة، وأضف معالجة آمنة للأجهزة التي لا تحتوي على مستشعر بصمة أو لم تسجل بصمات بحيث تعود تلقائياً للدخول بكلمة المرور أو الـ PIN دون انهيار.
- **معالجة أخطاء الكاميرا في `QrScannerDialog.kt`:** أضف فحصاً واضحاً لحالة رفض إذن الكاميرا الدائم أو عدم توفر كاميرا في الجهاز مع رسالة توضيحية وزر لفتح إعدادات التطبيق، وأضف زر تشغيل الفلاش (Torch) والتركيز التلقائي.

### قيود التنفيذ:
- التزم بـ `compileSdk = 36` و `minSdk = 24` وممارسات Kotlin و Jetpack Compose (Material 3).
- اعرض خطة التعديل وخريطة المسارات الجديدة (تثبت أن لكل شاشة/تبويب مدخلاً واحداً فقط) أولاً، ثم قدّم الأكواد الكاملة لكل ملف جديد أو معدّل مرتباً حسب المسار.
```