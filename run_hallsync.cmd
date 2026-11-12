@echo off
setlocal
if /i "%~1"=="--console" goto launch
cmd /k ""%~f0" --console"
exit /b
:launch
title HallSync - University of Kelaniya
cd /d "%~dp0application"
if errorlevel 1 goto missing_folder
where /q java
if errorlevel 1 goto missing_java
where /q javac
if errorlevel 1 goto missing_java
where /q mvn
if errorlevel 1 goto missing_maven
echo Building HallSync. The first build needs internet access.
call mvn -B -ntp clean package
if errorlevel 1 goto build_failed
echo Starting HallSync. Keep this window open.
java -jar "target\hallsync.jar"
echo HallSync stopped. Read any error above. Keep passwords private.
pause
exit /b
:missing_folder
echo Extract the whole ZIP first. The application folder must be beside this launcher.
pause
exit /b 1
:missing_java
echo Java JDK is missing from PATH. Check java -version and javac -version.
pause
exit /b 1
:missing_maven
echo Maven is missing from PATH. Check mvn -version in a new Command Prompt.
pause
exit /b 1
:build_failed
echo The build stopped. Read the Maven error above. Check internet access and your JDK.
pause
exit /b 1
