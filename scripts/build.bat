@echo off
REM Build NexoERP - Windows
cd /d "%~dp0.."
if not exist "mvnw.cmd" (
    echo No se encuentra mvnw.cmd. Ejecute desde la raiz del proyecto.
    exit /b 1
)
call mvnw.cmd clean package -DskipTests -q
if %ERRORLEVEL% neq 0 exit /b 1
echo Build OK: target\nexoerp-1.0.0.jar
exit /b 0
