@echo off
setlocal
if exist out rmdir /s /q out
javac -d out src\aidetector\*.java src\aidetector\core\*.java src\aidetector\signals\*.java src\aidetector\input\*.java src\aidetector\report\*.java
if errorlevel 1 (
    echo build failed
    exit /b 1
)
echo build ok
