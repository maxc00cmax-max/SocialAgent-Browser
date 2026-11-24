# ==============================================================================
#  Social Agent Studio - Enterprise Headless Android Builder (Docker)
#  بيئة بناء معزولة وسحابية لتوليد ملفات الـ APK تلقائياً على أي سيرفر أو VPS
# ==============================================================================

FROM eclipse-temurin:21-jdk-jammy

LABEL maintainer="Social Agent Studio"
LABEL description="Headless Android Build Container for Social Agent Studio"

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

# 1. تثبيت الحزم الأساسية
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    curl \
    git \
    bash \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# 2. تحميل وتثبيت أدوات سطر أوامر أندرويد (Android Command-line Tools)
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /tmp && \
    mv /tmp/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm -rf /tmp/cmdline-tools.zip

# 3. قبول التراخيص وتثبيت مكونات Android SDK 35 و Build-tools
RUN yes | sdkmanager --licenses > /dev/null && \
    sdkmanager "platform-tools" \
               "platforms;android-35" \
               "build-tools;35.0.0"

# 4. تثبيت Gradle
ENV GRADLE_VERSION=8.11.1
RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle && \
    rm -f /tmp/gradle.zip

WORKDIR /project

# نسخ ملفات التكوين لتهيئة الكاش
COPY gradle.properties settings.gradle.kts build.gradle.kts /project/
COPY gradle /project/gradle/
COPY app/build.gradle.kts /project/app/

# نسخ كامل كود المشروع
COPY . /project/

# إنشاء مجلد المخرجات والتأكد من الصلاحيات
RUN mkdir -p /project/build-outputs && chmod +x /project/build.sh || true

# نقطة التشغيل الافتراضية للبناء واستخراج الـ APK
ENTRYPOINT ["/bin/bash", "-c", "\
    echo '🚀 جاري تشغيل البناء واختبارات الجودة...' && \
    gradle :app:testDebugUnitTest && \
    gradle :app:assembleDebug && \
    mkdir -p /project/build-outputs && \
    cp app/build/outputs/apk/debug/*.apk /project/build-outputs/SocialAgent-debug.apk && \
    echo '✅ تم استخراج SocialAgent-debug.apk بنجاح داخل مجلد build-outputs/' && \
    ls -lh /project/build-outputs/SocialAgent-debug.apk\
"]
