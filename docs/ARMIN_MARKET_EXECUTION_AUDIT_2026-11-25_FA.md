# ممیزی اجرایی ARMIN MARKET — 2026-11-25

## شواهد مخزن و ساخت

- مخزن: `gavad54/Armin_market`
- PR #2: OPEN / DRAFT / UNMERGED.
- SHA بررسی‌شده: `6b0c66432488667569afc2bd10cf4c59acf3d3a5`.
- Android build workflow: run `45370414494`; job ساخت APK موفق.
- Gradle taskهای `testDebugUnitTest` و `assembleDebug` هر دو موفق.
- CodeQL: PASS.
- Dependency Review: FAIL؛ لاگ و وضعیت مخزن نشان می‌دهد GitHub Dependency Graph فعال نیست. این شکست به‌تنهایی آسیب‌پذیری وابستگی را ثابت نمی‌کند و به معنی عبور از ممیزی وابستگی نیز نیست.
- Artifact: `armin-market-debug-apk`, artifact ID `14908797696`, ZIP size 9,170,917 bytes.
- SHA-256 ZIP: `e350d0d33c4dd1f4bc1afab31060b1d9bada0496f17e9974c9c93232d1257c46`.
- APK داخل ZIP استخراج و نوع فایل به‌عنوان Android APK تأیید شد. APK از نوع debug است، نه release-signed.

## وضعیت قابلیت‌های محصول

بررسی کد نشان می‌دهد این نسخه یک نمونه Android/Kotlin/Jetpack Compose است:
- رابط فارسی و RTL و مسیرهای نمایشی خانه/دسته‌ها/ثبت نیاز/پیشنهادها/تحلیل بازار/حساب کاربری.
- داده کالاها و فروشندگان فعلاً نمونه و hard-coded هستند.
- سبد خرید فقط snapshot محلی روی همان دستگاه نگه می‌دارد؛ پیام برنامه صریحاً اعلام می‌کند سفارش و پرداخت فعال نیست.
- 11 کارت «هوشمندی بازار» صرفاً قرارداد/نمایش نمونه‌اند و به backend یا داده زنده متصل نیستند.
- درخت مخزن هیچ backend server، API service یا database integration را نشان نمی‌دهد.
- فقط دو فایل تست Kotlin برای منطق سبد و codec snapshot در مخزن وجود دارد.

## موانع قطعی

| اولویت | مانع | اقدام لازم | وضعیت |
|---|---|---|---|
| P0 | GitHub Dependency Graph خاموش است | مالک مخزن در Settings → Code security and analysis → Dependency graph آن را فعال و سپس Dependency Review را دوباره اجرا کند | OWNER ACTION |
| P0 | هیچ backend/API/DB واقعی در مخزن نیست | طراحی و پیاده‌سازی backend مستقل، هویت/مجوز، catalog، seller, request/quotation و API contract؛ سپس تست integration/E2E | BLOCKED |
| P0 | داده محصول/فروشنده نمونه است | حذف وابستگی به داده hard-coded فقط پس از ایجاد API و seed کنترل‌شده | BLOCKED |
| P0 | ثبت نیاز/پیشنهاد/سفارش عملیاتی نیست | workflow واقعی ثبت درخواست، پاسخ فروشنده، انتخاب پیشنهاد، وضعیت سفارش، idempotency و audit | BLOCKED |
| P0 | پرداخت و تسویه فعال نیست | sandbox درگاه، webhook signature/replay protection، idempotency و reconciliation؛ بدون مجوز مالک فعال نشود | BLOCKED |
| P1 | APK روی دستگاه واقعی نصب و اجرا نشده | نصب روی گوشی Android، بررسی launch، navigation، RTL، ذخیره سبد پس از restart و crash/log | UNVERIFIED |
| P1 | APK فقط debug است | کلید رسمی release در secret store، build release، امضا و تأیید artifact | BLOCKED |
| P1 | تست UI/E2E و دسترس‌پذیری محدود است | UI tests، network/error states، TalkBack، اندازه فونت، Android versions و offline behavior | BLOCKED |

## تصمیم

**ساخت APK آزمایشی موفق است؛ محصول هنوز فروشگاه واقعی یا نسخه آماده انتشار نیست.** کد فعلی برای نمایش و آزمون اولیه مناسب است، اما هیچ سفارش، فروشنده، پرداخت یا اتصال زنده را نباید فعال یا موجود اعلام کرد. آزمون نصب و اجرای واقعی دستگاه هنوز انجام نشده است.

هیچ تنظیم مخزن، Secret، سرویس تولید، پرداخت یا انتشار تغییر داده نشده است.
