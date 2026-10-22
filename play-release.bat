@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
 echo Java 17 or newer is required.
 pause
 exit /b 1
)
if exist updater.jar java -cp updater.jar veil.DesktopUpdater "%CD%"
java -jar MaleficVeil.jar
