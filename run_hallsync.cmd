@echo off
setlocal
title HallSync - Lecture Hall Booking System
cd /d "%~dp0application"
where /q java
if errorlevel 1 goto missing_java
where /q javac
if errorlevel 1 goto missing_java
where /q mvn
if errorlevel 1 goto missing_maven
echo Building HallSync. The first build needs internet access for Maven dependencies.
call mvn -B -ntp package
if errorlevel 1 goto build_failed
echo Opening HallSync. Keep your database passwords private.
java -cp "target\hallsync-connection-check.jar" lk.ac.kln.hallsync.HallSyncApp
if errorlevel 1 goto app_failed
exit /b 0

:missing_java
echo Java JDK was not found. Check java -version and javac -version in a new terminal.
echo See README.md. Your existing JDK installation can be reused.
pause
exit /b 1

:missing_maven
echo Maven was not found. Add its bin folder to PATH and open a new terminal.
echo Check mvn -version. See README.md for the startup order.
pause
exit /b 1

:build_failed
echo The build stopped. Read the Maven error above; check the internet connection and JDK.
pause
exit /b 1

:app_failed
echo HallSync stopped. Keep the error above when asking for help; keep passwords private.
pause
exit /b 1
