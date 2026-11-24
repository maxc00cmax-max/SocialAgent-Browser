@echo off
REM ==============================================================================
REM  Social Agent Studio - Windows Build Script
REM  اسكربت البناء لأنظمة ويندوز (Windows CMD / PowerShell)
REM ==============================================================================

setlocal EnableDelayedExpansion

chcp 65001 >nul
echo.
echo ======================================================================
echo        🚀 SOCIAL AGENT STUDIO - WINDOWS BUILD SYSTEM
echo ======================================================================
echo.

set ACTION=%1
if "%ACTION%"=="" set ACTION=debug

REM Load .env if present
if exist ".env" (
    echo [INFO] Loading variables from .env...
    for /F "usebackq tokens=1,* delims==" %%G in (".env") do (
        set "%%G=%%H"
    )
)

REM Determine Gradle command
set GRADLE_CMD=gradle
if exist "gradlew.bat" set GRADLE_CMD=call gradlew.bat

if "%ACTION%"=="debug" goto BUILD_DEBUG
if "%ACTION%"=="release" goto BUILD_RELEASE
if "%ACTION%"=="test" goto RUN_TESTS
if "%ACTION%"=="clean" goto CLEAN
if "%ACTION%"=="all" goto BUILD_ALL
if "%ACTION%"=="help" goto SHOW_HELP
goto UNKNOWN

:BUILD_DEBUG
echo [INFO] Building Debug APK...
if not exist "build-outputs" mkdir build-outputs
%GRADLE_CMD% :app:assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    exit /b %ERRORLEVEL%
)
echo [SUCCESS] Debug APK built successfully! Check app\build\outputs\apk\debug\
exit /b 0

:BUILD_RELEASE
echo [INFO] Building Release APK...
if not exist "build-outputs" mkdir build-outputs
%GRADLE_CMD% :app:assembleRelease
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Release build failed!
    exit /b %ERRORLEVEL%
)
echo [SUCCESS] Release APK built successfully! Check app\build\outputs\apk\release\
exit /b 0

:RUN_TESTS
echo [INFO] Running Unit Tests...
%GRADLE_CMD% :app:testDebugUnitTest
exit /b %ERRORLEVEL%

:CLEAN
echo [INFO] Cleaning project...
%GRADLE_CMD% clean
echo [SUCCESS] Clean completed.
exit /b 0

:BUILD_ALL
call %0 clean
call %0 test
call %0 debug
call %0 release
exit /b 0

:SHOW_HELP
echo Usage: build.bat [debug ^| release ^| test ^| clean ^| all ^| help]
exit /b 0

:UNKNOWN
echo [ERROR] Unknown command: %ACTION%
goto SHOW_HELP
