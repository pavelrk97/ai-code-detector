@echo off
setlocal
set "here=%~dp0"
if not exist "%here%out" (
    echo not built yet, run build.bat first
    exit /b 1
)
java -cp "%here%out" aidetector.Main %*
