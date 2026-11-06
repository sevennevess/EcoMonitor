@echo off
title Captura 2 - WebSocket e varias conexoes
REM Monta o cenario da Captura 2 do Wireshark: dois paineis (admin e visitante) e 3 sensores.
REM Precisa do servidor rodando (1-SERVIDOR.bat).
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
curl.exe -s -o nul http://127.0.0.1:8080/api/ping
if errorlevel 1 goto semservidor

echo.
echo =====================================================================
echo  CAPTURA 2 - WebSocket e varias conexoes ao mesmo tempo
echo  PASSO 1 - NO WIRESHARK: de dois cliques em
echo            "Adapter for loopback traffic capture" para gravar.
echo            Depois volte aqui e aperte qualquer tecla.
echo =====================================================================
pause >nul

echo.
echo PASSO 2 - Abrindo o painel em uma janela normal e em uma janela anonima...
start "" http://127.0.0.1:8080/login.html
set "CHROME=%ProgramFiles%\Google\Chrome\Application\chrome.exe"
if exist "%CHROME%" goto chrome
set "EDGE=%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe"
if exist "%EDGE%" goto edge
echo Abra voce mesmo uma janela anonima (Ctrl+Shift+N) em http://127.0.0.1:8080
goto logins
:chrome
start "" "%CHROME%" --incognito http://127.0.0.1:8080/login.html
goto logins
:edge
start "" "%EDGE%" --inprivate http://127.0.0.1:8080/login.html
:logins
echo.
echo  Na janela NORMAL entre com:   admin     / admin123
echo  Na janela ANONIMA entre com:  visitante / visitante123
echo  Quando os dois paineis mostrarem "tempo real: conectado", volte aqui
echo  e aperte qualquer tecla.
pause >nul

echo.
echo PASSO 3 - Iniciando 3 sensores (abrem 3 janelas)...
call "%~dp02-SENSORES.bat"
echo.
echo =====================================================================
echo  Espere uns 30 segundos olhando os paineis se atualizarem.
echo  Depois: pare a gravacao no Wireshark (quadrado vermelho), salve em
echo  %~dp0experimentos\capturas\captura2-websocket.pcapng
echo  e feche as 3 janelas dos sensores.
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
