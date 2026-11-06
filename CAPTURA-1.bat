@echo off
title Captura 1 - HTTP e TCP
REM Monta o cenario da Captura 1 do Wireshark: um sensor envia 6 medicoes (uma a cada 2 s)
REM e depois sao feitos 5 PINGs. Precisa do servidor rodando (1-SERVIDOR.bat).
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
curl.exe -s -o nul http://127.0.0.1:8080/api/ping
if errorlevel 1 goto semservidor

echo.
echo =====================================================================
echo  CAPTURA 1 - HTTP e TCP
echo  AGORA, NO WIRESHARK:
echo   1. De dois cliques em "Adapter for loopback traffic capture"
echo      (a lista de pacotes comeca a rolar = esta gravando).
echo   2. Volte para esta janela e aperte qualquer tecla.
echo =====================================================================
pause >nul

echo Enviando 6 medicoes do sensor SENSOR_CAPTURA (uma a cada 2 s)...
java simulador\SimuladorSensor.java SENSOR_CAPTURA TEMPERATURA 2000 http://127.0.0.1:8080 0 6
echo.
echo Enviando 5 PINGs...
for /l %%i in (1,1,5) do (
  curl.exe -s http://127.0.0.1:8080/api/ping
  echo.
)
echo.
echo =====================================================================
echo  PRONTO. Pare a gravacao no Wireshark (quadrado vermelho) e salve em
echo  %~dp0experimentos\capturas\captura1-http.pcapng
echo  Depois siga o roteiro: documentacao\ROTEIRO-WIRESHARK.md
echo =====================================================================
pause
exit /b 0

:semservidor
echo.
echo [ERRO] O servidor nao esta rodando.
echo Abra o 1-SERVIDOR.bat, espere aparecer "Started EcomonitorApplication"
echo e rode este arquivo de novo.
pause
exit /b 1
