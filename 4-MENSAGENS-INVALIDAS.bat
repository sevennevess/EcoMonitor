@echo off
title EcoMonitor - Mensagens invalidas
REM Envia ao servidor uma serie de mensagens erradas e mostra o ERRO devolvido para cada uma.
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
set "SERVIDOR=%~1"
if "%SERVIDOR%"=="" set "SERVIDOR=http://127.0.0.1:8080"
java simulador\TesteMensagensInvalidas.java %SERVIDOR%
pause
