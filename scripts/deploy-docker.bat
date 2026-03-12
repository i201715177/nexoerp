@echo off
REM Build imagen Docker y (opcional) levantar con docker-compose
cd /d "%~dp0.."
echo Building Docker image...
docker build -t nexoerp:latest .
if %ERRORLEVEL% neq 0 exit /b 1
echo Image nexoerp:latest built.
if "%1"=="up" (
  echo Starting stack...
  docker compose up -d
  echo Stack up. App: http://localhost:8080
)
exit /b 0
