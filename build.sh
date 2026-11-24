#!/usr/bin/env bash
# ==============================================================================
#  Social Agent Studio - Professional Build & Deployment Automation Script
#  اسكربت البناء والأتمتة الشامل لتطبيق Social Agent Studio (محلياً أو على سيرفر)
# ==============================================================================

set -eo pipefail

# الألوان والتنسيقات للواجهة الطرفية (Terminal UI)
COLOR_RESET="\033[0m"
COLOR_BOLD="\033[1m"
COLOR_CYAN="\033[38;5;51m"
COLOR_GREEN="\033[38;5;48m"
COLOR_YELLOW="\033[38;5;220m"
COLOR_RED="\033[38;5;196m"
COLOR_PURPLE="\033[38;5;141m"
COLOR_BLUE="\033[38;5;75m"
COLOR_GRAY="\033[38;5;245m"

print_banner() {
    echo -e "${COLOR_CYAN}${COLOR_BOLD}"
    echo "======================================================================"
    echo "       🚀 SOCIAL AGENT STUDIO - ENTERPRISE BUILD SYSTEM             "
    echo "       نظام البناء والأتمتة الشامل للتطبيق (Local & Server)         "
    echo "======================================================================"
    echo -e "${COLOR_RESET}"
}

log_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"
}

log_success() {
    echo -e "${COLOR_GREEN}${COLOR_BOLD}[SUCCESS]${COLOR_RESET} $1"
}

log_warning() {
    echo -e "${COLOR_YELLOW}[WARNING]${COLOR_RESET} $1"
}

log_error() {
    echo -e "${COLOR_RED}${COLOR_BOLD}[ERROR]${COLOR_RESET} $1"
}

show_help() {
    echo -e "${COLOR_BOLD}الاستخدام (Usage):${COLOR_RESET}"
    echo "  ./build.sh [COMMAND] [OPTIONS]"
    echo ""
    echo -e "${COLOR_BOLD}الأوامر المتاحة (Available Commands):${COLOR_RESET}"
    echo "  debug       بناء نسخة التطوير (Debug APK) وتجهيزها للاختبار والتثبيت"
    echo "  release     بناء النسخة الإنتاجية (Release APK)"
    echo "  test        تشغيل اختبارات الوحدة (Unit Tests) والتحقق من سلامة الأكواد"
    echo "  all         تشغيل الاختبارات وبناء نسختي Debug و Release"
    echo "  clean       تنظيف مجلدات البناء المؤقتة (Clean Build Artifacts)"
    echo "  install     تثبيت الـ APK مباشرة على الجهاز أو المحاكي المتصل عبر adb"
    echo "  docker      بناء المشروع بالكامل داخل حاوية Docker معزولة (للسيرفرات)"
    echo "  doctor      فحص متطلبات النظام والبيئة (Java, Android SDK, Gradle, ADB)"
    echo "  help        عرض هذه المساعدة"
    echo ""
    echo -e "${COLOR_BOLD}أمثلة:${COLOR_RESET}"
    echo "  ./build.sh debug"
    echo "  ./build.sh test"
    echo "  ./build.sh all"
    echo "  ./build.sh install"
    echo "  ./build.sh docker"
    echo ""
}

# 1. تحميل متغيرات البيئة من ملف .env إن وجد
load_env() {
    if [ -f ".env" ]; then
        log_info "جاري تحميل متغيرات البيئة من ملف .env..."
        set -a
        # shellcheck disable=SC1091
        source .env
        set +a
    elif [ -f ".env.example" ]; then
        log_warning "لم يتم العثور على ملف .env، جاري استخدام المتغيرات الافتراضية من .env.example"
    fi

    if [ -z "${GEMINI_API_KEY}" ]; then
        log_warning "تنبيه: لم يتم تعيين GEMINI_API_KEY. يمكنك تعيينه في ملف .env لتفعيل ميزات الذكاء الاصطناعي."
    else
        log_info "تم رصد مفتاح GEMINI_API_KEY بنجاح."
    fi
}

# 2. فحص والتحقق من بيئة النظام (Java, SDK, Gradle)
check_environment() {
    log_info "فحص جاهزية بيئة العمل..."

    # فحص Java
    if ! command -v java &> /dev/null; then
        log_error "لم يتم العثور على Java! يرجى تثبيت JDK 17 أو JDK 21."
        exit 1
    fi

    JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    log_info "إصدار Java المثبت: ${COLOR_GREEN}${JAVA_VER}${COLOR_RESET}"

    # فحص Android SDK
    if [ -z "${ANDROID_HOME}" ] && [ -z "${ANDROID_SDK_ROOT}" ]; then
        # محاولة البحث عن مسارات SDK الشائعة
        POSSIBLE_SDK_PATHS=(
            "$HOME/Android/Sdk"
            "$HOME/Library/Android/sdk"
            "/opt/android-sdk"
            "/usr/lib/android-sdk"
            "/Android/sdk"
        )
        for path in "${POSSIBLE_SDK_PATHS[@]}"; do
            if [ -d "$path" ]; then
                export ANDROID_HOME="$path"
                export ANDROID_SDK_ROOT="$path"
                log_info "تم اكتشاف Android SDK تلقائياً في: $path"
                break
            fi
        done
    fi

    if [ -z "${ANDROID_HOME}" ] && [ -z "${ANDROID_SDK_ROOT}" ]; then
        log_warning "لم يتم تحديد ANDROID_HOME أو ANDROID_SDK_ROOT. إذا فشل البناء، يرجى تعيين المسار."
    else
        SDK_DIR="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
        log_info "مسار Android SDK: ${COLOR_GREEN}${SDK_DIR}${COLOR_RESET}"
    fi

    # تحديد أداة Gradle المناسبة
    if [ -f "./gradlew" ]; then
        GRADLE_CMD="./gradlew"
        chmod +x ./gradlew 2>/dev/null || true
    elif command -v gradle &> /dev/null; then
        GRADLE_CMD="gradle"
    else
        log_error "لم يتم العثور على Gradle أو ./gradlew! يرجى تثبيت Gradle أو تشغيل البناء عبر Docker."
        exit 1
    fi
    log_info "أداة البناء المستخدمة: ${COLOR_GREEN}${GRADLE_CMD}${COLOR_RESET}"
}

# 3. فحص بيئة النظام الشامل (Doctor)
cmd_doctor() {
    echo -e "${COLOR_BOLD}${COLOR_PURPLE}=== فحص جاهزية النظام والبيئة (Environment Doctor) ===${COLOR_RESET}"
    check_environment

    echo ""
    log_info "فحص أداة ADB (Android Debug Bridge)..."
    if command -v adb &> /dev/null; then
        log_success "ADB مثبت: $(adb version | head -n 1)"
        echo -e "${COLOR_BOLD}الأجهزة المتصلة:${COLOR_RESET}"
        adb devices -l
    else
        log_warning "أداة adb غير مثبتة أو غير موجودة في الـ PATH (مطلوبة فقط للتثبيت المباشر على الأجهزة)."
    fi

    echo ""
    log_info "فحص Docker (للبناء على السيرفرات)..."
    if command -v docker &> /dev/null; then
        log_success "Docker مثبت: $(docker --version)"
    else
        log_warning "Docker غير مثبت. يمكنك تثبيته إذا أردت البناء المستقل على سيرفر."
    fi

    echo ""
    log_success "فحص النظام اكتمل بنجاح!"
}

# 4. تنظيف ملفات البناء (Clean)
cmd_clean() {
    log_info "جاري تنظيف مخرجات البناء السابقة..."
    ${GRADLE_CMD} clean
    log_success "تم تنظيف مجلدات البناء بنجاح."
}

# 5. تشغيل الاختبارات (Test)
cmd_test() {
    log_info "جاري تشغيل اختبارات الوحدة (Unit Tests)..."
    ${GRADLE_CMD} :app:testDebugUnitTest --info
    log_success "جميع الاختبارات اجتيزت بنجاح! ✅"
}

# 6. بناء نسخة التطوير (Debug APK)
cmd_build_debug() {
    log_info "بدء تجميع وبناء نسخة التطوير (assembleDebug)..."
    
    mkdir -p build-outputs
    
    ${GRADLE_CMD} :app:assembleDebug
    
    # البحث عن ملف APK الناتج
    APK_DEBUG=$(find app/build/outputs/apk/debug -name "*.apk" 2>/dev/null | head -n 1 || true)
    
    if [ -n "$APK_DEBUG" ] && [ -f "$APK_DEBUG" ]; then
        DEST_APK="build-outputs/SocialAgent-debug.apk"
        cp "$APK_DEBUG" "$DEST_APK"
        APK_SIZE=$(du -h "$DEST_APK" | cut -f1)
        
        echo ""
        log_success "تم بناء نسخة Debug APK بنجاح! 🎉"
        echo -e "${COLOR_BOLD}ملف التطبيق:${COLOR_RESET} ${COLOR_CYAN}${DEST_APK}${COLOR_RESET}"
        echo -e "${COLOR_BOLD}الحجم:${COLOR_RESET} ${COLOR_GREEN}${APK_SIZE}${COLOR_RESET}"
        
        if command -v sha256sum &> /dev/null; then
            CHECKSUM=$(sha256sum "$DEST_APK" | awk '{print $1}')
            echo -e "${COLOR_BOLD}بصمة SHA256:${COLOR_RESET} ${COLOR_GRAY}${CHECKSUM}${COLOR_RESET}"
        fi
        
        echo ""
        echo -e "${COLOR_YELLOW}للتثبيت السريع عبر USB أو المحاكي:${COLOR_RESET}"
        echo "  adb install -r ${DEST_APK}"
    else
        log_error "تعذر العثور على ملف Debug APK بعد البناء!"
        exit 1
    fi
}

# 7. بناء النسخة الإنتاجية (Release APK)
cmd_build_release() {
    log_info "بدء تجميع وبناء النسخة الإنتاجية (assembleRelease)..."
    
    mkdir -p build-outputs
    
    ${GRADLE_CMD} :app:assembleRelease
    
    APK_RELEASE=$(find app/build/outputs/apk/release -name "*.apk" 2>/dev/null | head -n 1 || true)
    
    if [ -n "$APK_RELEASE" ] && [ -f "$APK_RELEASE" ]; then
        DEST_APK="build-outputs/SocialAgent-release-unsigned.apk"
        cp "$APK_RELEASE" "$DEST_APK"
        APK_SIZE=$(du -h "$DEST_APK" | cut -f1)
        
        echo ""
        log_success "تم بناء نسخة Release APK بنجاح! 🚀"
        echo -e "${COLOR_BOLD}ملف التطبيق:${COLOR_RESET} ${COLOR_CYAN}${DEST_APK}${COLOR_RESET}"
        echo -e "${COLOR_BOLD}الحجم:${COLOR_RESET} ${COLOR_GREEN}${APK_SIZE}${COLOR_RESET}"
        
        if command -v sha256sum &> /dev/null; then
            CHECKSUM=$(sha256sum "$DEST_APK" | awk '{print $1}')
            echo -e "${COLOR_BOLD}بصمة SHA256:${COLOR_RESET} ${COLOR_GRAY}${CHECKSUM}${COLOR_RESET}"
        fi
        
        echo -e "${COLOR_YELLOW}ملاحظة:${COLOR_RESET} لتوقيع الـ Release APK رسمياً لمتجر Google Play، استخدم apksigner مع مفتاح الإنتاج الخاص بك."
    else
        log_error "تعذر العثور على ملف Release APK بعد البناء!"
        exit 1
    fi
}

# 8. التثبيت على الجهاز المتصل عبر ADB
cmd_install() {
    if ! command -v adb &> /dev/null; then
        log_error "أداة adb غير مثبتة!"
        exit 1
    fi

    # التأكد من وجود APK
    if [ ! -f "build-outputs/SocialAgent-debug.apk" ]; then
        log_info "لم يتم العثور على APK تم بناؤه مسبقاً، جاري البناء أولاً..."
        cmd_build_debug
    fi

    log_info "جاري التحقق من الأجهزة المتصلة..."
    DEVICES_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l || echo "0")
    
    if [ "$DEVICES_COUNT" -eq 0 ]; then
        log_error "لم يتم العثور على أي هاتف أو محاكي أندرويد متصل عبر adb!"
        echo "يرجى توصيل هاتفك وتفعيل وضع تصحيح الأخطاء (USB Debugging) أو تشغيل محاكي Android."
        exit 1
    fi

    log_info "جاري تثبيت SocialAgent-debug.apk على الجهاز المتصل..."
    adb install -r -d build-outputs/SocialAgent-debug.apk
    log_success "تم تثبيت التطبيق بنجاح على الجهاز! 📱✨"
    
    echo -e "${COLOR_CYAN}لتشغيل التطبيق مباشرة:${COLOR_RESET}"
    echo "  adb shell am start -n com.aistudio.socialagent.kzxmbq/com.example.MainActivity"
}

# 9. البناء داخل حاوية Docker (للسيرفرات والموزعين)
cmd_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker غير مثبت على هذا الجهاز!"
        exit 1
    fi

    log_info "بدء البناء داخل حاوية Docker معزولة تماماً..."
    mkdir -p build-outputs
    
    docker build -t social-agent-builder -f Dockerfile .
    
    log_info "تشغيل الحاوية واستخراج ملفات الـ APK..."
    docker run --rm \
        -v "$(pwd)/build-outputs:/project/build-outputs" \
        -e GEMINI_API_KEY="${GEMINI_API_KEY}" \
        social-agent-builder
        
    log_success "اكتمل البناء بواسطة Docker بنجاح! تم حفظ الـ APK داخل مجلد build-outputs/"
}

# ==============================================================================
# نقطة الدخول الرئيسية (Entry Point)
# ==============================================================================

print_banner
load_env

ACTION="${1:-debug}"

case "$ACTION" in
    debug)
        check_environment
        cmd_build_debug
        ;;
    release)
        check_environment
        cmd_build_release
        ;;
    test)
        check_environment
        cmd_test
        ;;
    all)
        check_environment
        cmd_test
        cmd_build_debug
        cmd_build_release
        ;;
    clean)
        check_environment
        cmd_clean
        ;;
    install)
        check_environment
        cmd_install
        ;;
    docker)
        cmd_docker
        ;;
    doctor)
        cmd_doctor
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "أمر غير معروف: $ACTION"
        show_help
        exit 1
        ;;
esac

echo ""
log_success "انتهت العملية بنجاح! ✨"
