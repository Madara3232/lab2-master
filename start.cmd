@echo off
setlocal
cd /d "%~dp0"
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
"%JAVA%" -jar "target\lab1-0.0.1-SNAPSHOT.jar" --server.address=127.0.0.1 %*
exit /b %ERRORLEVEL%
