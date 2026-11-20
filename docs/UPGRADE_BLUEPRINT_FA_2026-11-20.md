# ARMIN MARKET — نقشه ارتقای ساختار و UX | 2026-11-20

مخزن: gavad54/Armin_market | مبنای main: 79176682f8daedb7a5f9ea8d55ec349f5aea9fc5

## مبنای موجود
Android/Kotlin/Jetpack Compose/Material 3. README main فعلی را پوسته اولیه می‌داند؛ API، دیتابیس، حساب، فروشنده، سفارش و پرداخت واقعی هنوز متصل و اثبات نشده‌اند. docs/PRODUCT_SPEC_FA.md مشخصات رسمی مدل درخواست‌محور است و باید مرجع پذیرش بماند.

## سه تجربه مجزا
خریدار: جست‌وجوی برجسته، دسته‌بندی نیازمحور، ثبت نیاز مرحله‌ای با draft و preview، پیشنهادهای رقابتی، مقایسه قیمت/زمان/شرایط/اعتبار، گفتگو و timeline وضعیت.
فروشنده: onboarding تدریجی، پروفایل و دسته/محدوده، درخواست‌های مرتبط، پیشنهاد استاندارد با تاریخ اعتبار، مدیریت تعامل و داشبورد بازدید/درخواست/نرخ انتخاب.
اپراتور: moderation queue، گزارش تخلف، تکراری‌ها، اعتراض و شواهد، مدیریت دسته/تعرفه/تبلیغ و audit.

## معماری هدف
Android Compose با لایه‌های UI/domain/data، ViewModel/state قابل‌آزمون، repository interfaces و navigation type-safe. Backend API نسخه‌دار/OpenAPI و دامنه‌های Identity، BuyerRequests، SupplierProfiles، Offers، Conversations، Listings، TrustSafety، Moderation، Monetization، Notifications، Admin و Audit. DB رابطه‌ای با migration و ownership؛ فایل‌ها در object storage امن. modular monolith برای MVP. محیط dev/staging/prod، secrets امن، backup/restore و health/metrics.

## UX و اعتماد
RTL فارسی واقعی، autocomplete، فیلترهای پرکاربرد، نتایج مرتبط، وضعیت‌های draft/published/offers received/selected/completed/cancelled/closed، loading/empty/error/offline/success. نشان فروشنده فقط با مدرک، امتیاز توضیح‌پذیر و قابل اعتراض، تبلیغ برچسب‌دار، گزارش/مسدودسازی و رسیدگی انسانی. حفاظت در برابر IDOR، spam، brute force و آپلود مخرب. پرداخت و custody وجه در MVP غیرفعال تا مجوز، بازپرداخت و حل اختلاف تأیید شود.

## درآمد و KPI
آگهی پایه با سقف روشن، ارتقای برچسب‌دار، پروفایل حرفه‌ای، ابزار فروشنده و خدمات سرنخ با تعرفه و رضایت شفاف. KPI: تکمیل درخواست، زمان تا اولین پیشنهاد، پیشنهاد معتبر، نرخ انتخاب، زمان پاسخ، نرخ تبدیل، گزارش تقلب و بازگشت خریدار/فروشنده. درآمد یا فروش تضمین نشود.

## پذیرش و اولویت
P0 inventory Compose، تعیین backend واقعی/ناموجود و baseline. P1 API/DB و auth/ownership/moderation. P2 جریان buyer/seller/operator end-to-end. P3 search/trust/notifications/monetization/analytics. P4 security/accessibility/performance، Android release و UAT. APK debug، release signed نیست.

## Benchmark
Shopify برای discovery؛ الگوهای دوطرفه marketplace برای seller onboarding، buyer search و operator moderation؛ Netguru و LOW/CODE برای seller UX و trust. الگوها اقتباس شوند نه کپی.
