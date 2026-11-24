# 🚀 دليل البناء والتشغيل الشامل - Social Agent Studio
### Enterprise Build & Deployment Guide (Local & Server)

---

## 📌 الفهرس
1. [المتطلبات الأساسية](#1-المتطلبات-الأساسية)
2. [التشغيل والبناء محلياً (Linux / macOS)](#2-التشغيل-والبناء-محلياً-linux--macos)
3. [التشغيل والبناء على أنظمة Windows](#3-التشغيل-والبناء-على-أنظمة-windows)
4. [البناء على سيرفر أو VPS باستخدام Docker](#4-البناء-على-سيرفر-أو-vps-باستخدام-docker)
5. [أتمتة البناء السحابي عبر GitHub Actions (CI/CD)](#5-أتمتة-البناء-السحابي-عبر-github-actions-cicd)
6. [تثبيت التطبيق على الجهاز المتصل (ADB)](#6-تثبيت-التطبيق-على-الجهاز-المتصل-adb)
7. [إعداد مفتاح الذكاء الاصطناعي (Gemini API)](#7-إعداد-مفتاح-الذكاء-الاصطناعي-gemini-api)
8. [حل المشاكل الشائعة (Troubleshooting)](#8-حل-المشاكل-الشائعة-troubleshooting)

---

## 1. المتطلبات الأساسية

* **Java Development Kit (JDK)**: إصدار **21** (أو 17 كحد أدنى).
* **Android SDK**: منصة `platforms;android-35` وأدوات البناء `build-tools;35.0.0`.
* **Gradle**: 8.10+ (أو استخدام الـ Gradle Wrapper الافتراضي).
* **Docker** *(اختياري للسيرفرات)*: لتشغيل البناء المعزول بدون الحاجة لتثبيت الـ SDK يدوياً.

---

## 2. التشغيل والبناء محلياً (Linux / macOS)

تم توفير اسكربت تحكم احترافي وذكي `build.sh` يغطي كافة المهام:

### منح صلاحية التنفيذ (أول مرة فقط):
```bash
chmod +x build.sh
```

### الأوامر المتاحة:

| الأمر | الوظيفة |
| :--- | :--- |
| `./build.sh debug` | بناء نسخة التطوير السريعة `SocialAgent-debug.apk` |
| `./build.sh release` | بناء النسخة الإنتاجية `SocialAgent-release-unsigned.apk` |
| `./build.sh test` | تشغيل اختبارات الوحدة وفحص سلامة الكود |
| `./build.sh all` | تشغيل الاختبارات وبناء نسختي Debug و Release معاً |
| `./build.sh install` | تثبيت الـ APK مباشرة على هاتفك أو المحاكي المتصل |
| `./build.sh clean` | تنظيف ملفات الكاش ومجلدات البناء السابقة |
| `./build.sh doctor` | فحص كامل لبيئة النظام والتأكد من تثبيت Java و SDK و ADB |
| `./build.sh docker` | تجميع وبناء الـ APK داخل حاوية Docker مباشرة |

> **أين تجد ملف الـ APK بعد البناء؟**  
> يقوم الاسكربت بنسخ الملفات الجاهزة تلقائياً إلى مجلد: `build-outputs/SocialAgent-debug.apk`.

---

## 3. التشغيل والبناء على أنظمة Windows

إذا كنت تعمل على نظام ويندوز (CMD أو PowerShell)، يمكنك استخدام اسكربت `build.bat`:

```cmd
:: لبناء نسخة التطوير
build.bat debug

:: لبناء النسخة الإنتاجية
build.bat release

:: لتشغيل الاختبارات
build.bat test

:: لتنظيف المشروع وإعادة البناء الشامل
build.bat all
```

---

## 4. البناء على سيرفر أو VPS باستخدام Docker

إذا كان لديك سيرفر سحابي (مثل DigitalOcean, Hetzner, AWS EC2, GCP, أو أي سيرفر Ubuntu/Debian) وتريد بناء التطبيق أوتوماتيكياً دون تثبيت Android Studio أو الـ SDK:

### الطريقة الأولى: استخدام Docker Compose (الأسهل)
```bash
# 1. تشغيل البناء واستخراج الـ APK
docker compose up --build

# 2. ستجد الـ APK تم تصديره فوراً إلى:
ls -lh build-outputs/SocialAgent-debug.apk
```

### الطريقة الثانية: باستخدام Docker مباشرة
```bash
# بناء صورة الـ Container
docker build -t social-agent-builder -f Dockerfile .

# تشغيل البناء واستخراج الـ APK
docker run --rm -v "$(pwd)/build-outputs:/project/build-outputs" social-agent-builder
```

---

## 5. أتمتة البناء السحابي عبر GitHub Actions (CI/CD)

يحتوي المشروع على مسار عمل جاهز في المسار:
`.github/workflows/build-apk.yml`

* عند كل `git push` إلى فرع `main` أو `master`، يقوم GitHub تلقائياً بـ:
  1. تجهيز بيئة Java 21 و Android SDK 35.
  2. تشغيل كافة اختبارات الوحدة.
  3. بناء نسختي الـ APK (Debug و Release).
  4. رفع ملفات الـ APK كـ **Artifacts** قابلة للتحميل مباشرة من صفحة الـ Actions في مستودعك.

### إضافة مفتاح Gemini API في GitHub:
1. اذهب إلى إعدادات المستودع `Settings` > `Secrets and variables` > `Actions`.
2. أضف سراً جديداً باسم `GEMINI_API_KEY` وضع قيمته.

---

## 6. تثبيت التطبيق على الجهاز المتصل (ADB)

1. قم بتوصيل هاتفك الأندرويد بالكمبيوتر عبر كابل USB.
2. فعّل خيار **تصحيح أخطاء USB (USB Debugging)** من خيارات المطور في الهاتف.
3. نفّذ الأمر التالي:
```bash
./build.sh install
```
سيقوم الاسكربت بفحص الجهاز وتثبيت التطبيق وتشغيله فوراً على شاشة هاتفك!

---

## 7. إعداد مفتاح الذكاء الاصطناعي (Gemini API)

1. انسخ ملف المتغيرات النموذجي:
```bash
cp .env.example .env
```
2. افتح ملف `.env` وضع مفتاحك:
```env
GEMINI_API_KEY=AIzaSyD...
```
* يقوم ملف `build.sh` وملف `build.gradle.kts` بقراءة هذا المفتاح تلقائياً وحقنه في تطبيق الأندرويد عبر `BuildConfig.GEMINI_API_KEY`.

---

## 8. حل المشاكل الشائعة (Troubleshooting)

### أ) خطأ: `ANDROID_HOME is not set`
قم بتصدير مسار الـ SDK في ملف `~/.bashrc` أو `~/.zshrc`:
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
```

### ب) خطأ: نقص الذاكرة أثناء البناء (Out of Memory)
تم ضبط إعدادات الذاكرة مسبقاً في `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```
إذا كان جهازك يحتوي على ذاكرة محدودة، يمكنك تقليلها إلى `-Xmx2048m`.
