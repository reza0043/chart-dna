# گرفتن فایل APK — دو راه

اپلیکیشن شما **همین حالا یک‌بار با موفقیت بیلد شده** و فایل APK آن روی GitHub آمادهٔ دانلود است.

## راه ۱ — دانلود APK آماده (۱ دقیقه، بدون هیچ تغییری)

این فایل APK دقیقاً از همان سورسی ساخته شده که هم‌اکنون روی شاخهٔ `main` است (کامیت `e9500a1`، نسخهٔ ۱.۱).

1. با اکانت GitHub خودتان وارد این صفحه شوید (چون ریپو خصوصی است، باید لاگین باشید):
   <https://github.com/reza0043/chart-dna/actions/runs/33080372216>
2. پایین همان صفحه، بخش **Artifacts** → روی `AI-Boardroom-debug-APK` کلیک کنید.
3. فایل `AI-Boardroom-debug-APK.zip` دانلود می‌شود؛ آن را از حالت zip خارج کنید → داخلش `app-debug.apk` است (حدود ۱۹ مگابایت).
4. فایل را به گوشی منتقل کنید و با یک فایل‌منیجر روی آن ضربه بزنید تا نصب شود.
   اگر هشدار داد: **تنظیمات › نصب از منبع نامشخص / Install unknown apps** را برای همان فایل‌منیجر فعال کنید.

> این APK با امضای debug ساخته شده: برای نصب روی گوشی و تست کاملاً مناسب است، ولی برای انتشار در گوگل‌پلی نیست (برای پلی‌استور باید نسخهٔ release/AAB با keystore شخصی شما ساخته شود).

## راه ۲ — فعال کردن بیلد خودکار (۳۰ ثانیه، یک‌بار) تا از این پس با هر push خودش APK بسازد

بیلد خودکار الان خراب است، چون فایل workflow در `.github/workflows/build-apk.yml` دستور `checkout` روی شاخه‌ای به نام `arena/01a0367d-chart-dna` دارد که دیگر وجود ندارد (آخرین بیلد دستی با خطا رد شد). من نمی‌توانم آن فایل را درست کنم: گیت‌هاب به ربات اجازهٔ تغییر فایل‌های داخل `.github/workflows` را نمی‌دهد و اجازهٔ اجرای دستی workflow را هم ندارد؛ فقط صاحب ریپو می‌تواند.

مراحل:

1. این لینک را باز کنید و دکمهٔ مداد (Edit this file) را بزنید:
   <https://github.com/reza0043/chart-dna/edit/main/.github/workflows/build-apk.yml>
2. **کل** محتوای فایل را پاک کنید و محتوای فایل `apk-workflow/build-apk.yml` (همین پوشه) را کامل Paste کنید.
   (همین متن در `ais-mittings/.github-workflows/build-apk.yml` هم هست.)
3. `Commit changes` را بزنید. با آن push، workflow اصلاح‌شده خودش اجرا می‌شود.
4. چند دقیقه صبر کنید؛ در تب **Actions** که run سبز شد، از بخش **Artifacts** فایل APK را دانلود کنید.

از این پس هر بار که کدی به `main` push شود، APK جدید ساخته می‌شود و ۹۰ روز نگه داشته می‌شود.
همچنین می‌توانید دستی بیلد بگیرید: **Actions › Build AI Boardroom APK › Run workflow** (از هر شاخه‌ای که بخواهید؛ همان شاخه بیلد می‌شود).

## راه ۳ — بیلد روی گوشی/کامپیوتر خودتان

- با Android Studio (Narwhal 2025.1.1 یا جدیدتر): پوشهٔ `ais-mittings` را Open کنید، صبر کنید Gradle Sync تمام شود، سپس `Build › Build APK(s)` یا `Run ▶`.
- با خط فرمان (نیاز به JDK 17 و Android SDK): داخل `ais-mittings` دستور `./gradlew :app:assembleDebug` → خروجی `app/build/outputs/apk/debug/app-debug.apk`.

> نکته: در محیط کاری من (سندباکس) نه Java/Android SDK نصب است و نه شبکه به `dl.google.com` و `repo1.maven.org` باز است؛ به همین دلیل بیلد را روی GitHub Actions انجام می‌دهیم.

## نسخهٔ نهایی برای فروشگاه (release)

```
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
KEYSTORE_PATH=/مسیر/my-upload-key.jks STORE_PASSWORD=*** KEY_PASSWORD=*** ./gradlew :app:assembleRelease
```
اطلاعات بیشتر: `ais-mittings/README_BUILD.md` بخش ۳.

## مشخصات اپ

| مورد | مقدار |
|---|---|
| پکیج (applicationId) | `com.aistudio.aiboardroom.kxmpzq` |
| نسخه | 1.1 (versionCode 1) |
| حداقل اندروید | 7.0 (API 24) |
| هدف | API 36 |
| فناوری | Kotlin + Jetpack Compose، Room، OkHttp، Android Keystore |
| نوع بیلد موجود | debug (امضای پیش‌فرض) |
| حجم APK | ≈ 19.5 MB |
