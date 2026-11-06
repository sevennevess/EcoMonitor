@echo off
REM Abre 3 janelas, cada uma com um sensor simulado enviando medicoes a cada 2 s.
REM Servidor em outro PC: 2-SENSORES.bat http://192.168.0.10:8080
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
set "SERVIDOR=%~1"
if "%SERVIDOR%"=="" set "SERVIDOR=http://127.0.0.1:8080"
start "Sensor 1 - SENSOR_001" cmd /k java simulador\SimuladorSensor.java SENSOR_001 TEMPERATURA,UMIDADE 2000 %SERVIDOR%
start "Sensor 2 - SENSOR_002" cmd /k java simulador\SimuladorSensor.java SENSOR_002 QUALIDADE_AR 2000 %SERVIDOR%
start "Sensor 3 - SENSOR_003" cmd /k java simulador\SimuladorSensor.java SENSOR_003 TEMPERATURA,QUALIDADE_AR 2000 %SERVIDOR%
echo 3 sensores iniciados contra %SERVIDOR% (feche as janelas para parar).
