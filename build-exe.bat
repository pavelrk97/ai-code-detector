@echo off
setlocal
set "here=%~dp0"
cd /d "%here%"

where jpackage >nul 2>nul
if errorlevel 1 (
    echo jpackage was not found. Install JDK 17 or newer, not just a JRE.
    exit /b 1
)

where jar >nul 2>nul
if errorlevel 1 (
    echo jar was not found. Install JDK 17 or newer, not just a JRE.
    exit /b 1
)

call "%here%build.bat"
if errorlevel 1 exit /b 1

set "packageInput=%here%package-input"
set "dist=%here%dist"

if exist "%packageInput%" rmdir /s /q "%packageInput%"
if exist "%dist%" rmdir /s /q "%dist%"
mkdir "%packageInput%"
mkdir "%dist%"

jar --create --file "%packageInput%\ai-detector.jar" -C "%here%out" .
if errorlevel 1 (
    echo jar build failed
    exit /b 1
)

jpackage ^
  --type app-image ^
  --name "AI Code Detector" ^
  --input "%packageInput%" ^
  --main-jar ai-detector.jar ^
  --main-class aidetector.DesktopApp ^
  --dest "%dist%" ^
  --app-version 1.0.0 ^
  --vendor "Local" ^
  --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo exe build failed
    exit /b 1
)

if exist "%packageInput%" rmdir /s /q "%packageInput%"

echo.
echo App image created:
echo "%dist%\AI Code Detector\AI Code Detector.exe"
