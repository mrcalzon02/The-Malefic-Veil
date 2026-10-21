@echo off
setlocal
cd /d "%~dp0"
if not exist out mkdir out
javac --release 17 -d out src\main\java\veil\*.java
if errorlevel 1 (echo Java 17 compiler build failed. & pause & exit /b 1)
java -cp out veil.WorldSelfTest
if errorlevel 1 (echo City invariants failed. & pause & exit /b 1)
java -cp out veil.ArenaSystemsTest
if errorlevel 1 (echo Arena host regression failed. & pause & exit /b 1)
java -Djava.awt.headless=true -cp out veil.RenderSmokeTest
if errorlevel 1 (echo Renderer smoke test failed. & pause & exit /b 1)
java -cp out veil.LanSmokeTest
if errorlevel 1 (echo LAN smoke test failed. & pause & exit /b 1)
java -cp out veil.MaleficVeil
