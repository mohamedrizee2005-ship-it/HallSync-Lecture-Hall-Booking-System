@echo off
setlocal
cd /d "%~dp0application"
call mvn -B -ntp package
if errorlevel 1 exit /b 1
java -cp "target\hallsync-connection-check.jar" lk.ac.kln.hallsync.HallSyncApp
if errorlevel 1 pause
