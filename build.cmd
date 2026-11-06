@echo off
cd /d "%~dp0"
call mvnw.cmd package %*
exit /b %ERRORLEVEL%
