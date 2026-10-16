@echo off
cd /d "%~dp0"
for /d %%J in ("%~dp0..\tools\jdk*") do set "JAVA_HOME=%%~fJ"
"%JAVA_HOME%\bin\java.exe" -jar "%~dp0target\lab1-0.0.1-SNAPSHOT.jar"
pause
