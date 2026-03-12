@echo off
REM Ejecutar la aplicacion con Java 17 (requerido para Spring Boot 3.2).
REM Edita la linea siguiente con la ruta de tu JDK 17.
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo No se encontro JDK 17 en: %JAVA_HOME%
    echo Instala JDK 17 o edita la ruta en este archivo. Ver DEPLOYMENT.md
    pause
    exit /b 1
)
cd /d "%~dp0"
call mvnw.cmd -q -DskipTests spring-boot:run
pause
