@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
if not exist logs mkdir logs
where java >nul 2>nul
if errorlevel 1 (
 echo Java 17 or newer is required.
 pause
 exit /b 1
)
set "UPDATE_MODE=--ask"
if /I "%~1"=="--check" set "UPDATE_MODE=--check"
if /I "%~1"=="--install" set "UPDATE_MODE=--install"
if exist updater.jar (
 set "TEMP_UPDATER=%TEMP%\malefic-updater-%RANDOM%-%RANDOM%.jar"
 copy /Y updater.jar "!TEMP_UPDATER!" >nul
 if errorlevel 1 (
  echo Could not create independent updater copy. >> logs\updater-console.log
  exit /b 2
 )
 java -cp "!TEMP_UPDATER!" veil.DesktopUpdater "%CD%" !UPDATE_MODE! >> logs\updater-console.log 2>&1
 set "UPDATE_RESULT=!errorlevel!"
 del /q "!TEMP_UPDATER!" >nul 2>nul
 if not "!UPDATE_RESULT!"=="0" (
  echo Updater recovery failed. See logs\updater.log
  exit /b !UPDATE_RESULT!
 )
)
if /I "%~1"=="--check" exit /b 0
echo Launching The Malefic Veil. Game diagnostics: logs\game-console.log
java -jar MaleficVeil.jar >> logs\game-console.log 2>&1
set "GAME_RESULT=!errorlevel!"
echo Game exited with status !GAME_RESULT! >> logs\game-console.log
exit /b !GAME_RESULT!
