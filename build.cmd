@echo off
cd /d "%~dp0"
for /d %%J in ("%~dp0..\tools\jdk*") do set "JAVA_HOME=%%~fJ"
call "%~dp0..\tools\apache-maven-3.10.0\bin\mvn.cmd" "-Dmaven.repo.local=%~dp0.maven\repository" package
pause
