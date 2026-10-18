@echo off
echo =======================================================================
echo [IgnitionAI] Starting Spring Boot Gateway Service
echo Forwarding to: http://127.0.0.1:8000 (FastAPI ML Service)
echo Listening on:  http://0.0.0.0:8080
echo =======================================================================
cd /d "%~dp0"
set "JAVA_HOME=D:\jdk-17\PFiles64\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew.bat :spring-backend:run
