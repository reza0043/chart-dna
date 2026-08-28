# Workflow ساخت APK (نسخهٔ اصلاح‌شده)

این فایل، نسخهٔ **سالم و اصلاح‌شدهٔ** workflow ساخت APK است. فایل فعال در
`.github/workflows/build-apk.yml` نسخهٔ قدیمی و خراب را دارد (شاخه‌ای را checkout
می‌کرد که حذف شده، به همین دلیل بیلد با خطا می‌افتاد).

## یک‌بار فعال‌سازی (۳۰ ثانیه، فقط از طریق وب‌سایت GitHub)
۱. در همین ریپو به مسیر `.github/workflows/build-apk.yml` بروید → آیکون مداد (Edit this file).
۲. کل محتوای فایل را با محتوای `ais-mittings/.github-workflows/build-apk.yml` (همین پوشه) **جایگزین** کنید.
۳. `Commit changes` را بزنید. تمام — از این پس هر push به `main` خودش APK می‌سازد.

اگر commit با این پیام رد شد: «refusing to allow a GitHub App to create or update workflow
without `workflows` permission» — یعنی این کار فقط باید دستی توسط صاحب ریپو انجام شود (همین مراحل بالا).

## گرفتن فایل APK
- تب **Actions** → جدیدترین run موفق «Build AI Boardroom APK» → پایین صفحه، بخش **Artifacts** →
  روی `AI-Boardroom-debug-APK` کلیک کنید → فایل zip دانلود می‌شود → از zip خارج کنید → `app-debug.apk`
  را روی گوشی بریزید و نصب کنید (نصب از «منبع نامشخص» باید فعال باشد).
- اگر برای دانلود artifact از شما خواست وارد شوید: با همان اکانت GitHub ریپو لاگین کنید.

## نکات
- در اجرای دستی (Run workflow) از هر شاخه‌ای، همان شاخه بیلد می‌شود؛ نیازی به ویرایش مجدد فایل نیست.
- بیلد، APK «debug» با امضای پیش‌فرض می‌سازد که برای نصب مستقیم روی گوشی مناسب است.
  برای نسخهٔ نهایی/فروشگاهی (release + AAB) باید keystore شخصی بسازید (بخش ۳ فایل `ais-mittings/README_BUILD.md`).
