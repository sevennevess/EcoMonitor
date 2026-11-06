@echo off
title Sensor instavel - SENSOR_004
REM Sensor que fica 20 s em silencio a cada 5 ciclos: o servidor o marca INATIVO (timeout 15 s)
REM e depois ATIVO de novo quando ele volta. Mostra o tratamento de desconexoes.
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
set "SERVIDOR=%~1"
if "%SERVIDOR%"=="" set "SERVIDOR=http://127.0.0.1:8080"
java simulador\SimuladorSensor.java SENSOR_004 UMIDADE 2000 %SERVIDOR% 5
pause
