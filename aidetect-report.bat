@echo off
setlocal
chcp 65001 >nul

set "here=%~dp0"
set "target=%~1"

if not exist "%here%out" (
    echo Build output was not found. Building first...
    call "%here%build.bat"
    if errorlevel 1 (
        echo.
        echo Build failed.
        pause
        exit /b 1
    )
)

if "%target%"=="" (
    echo Paste a file path, folder path, or GitHub/Git URL.
    echo Leave empty to analyze the local src folder.
    echo.
    set /p "target=Target: "
)

if "%target%"=="" (
    set "target=%here%src"
)

set "reports=%here%reports"
if not exist "%reports%" mkdir "%reports%"
set "report=%reports%\ai-detector-report.html"

echo.
echo Analyzing:
echo %target%
echo.

call "%here%aidetect.bat" "%target%" --format html --out "%report%" --evidence 5
if errorlevel 1 (
    echo.
    echo Analysis failed.
    pause
    exit /b 1
)

start "" "%report%"
echo.
echo Report opened:
echo %report%
echo.
pause
