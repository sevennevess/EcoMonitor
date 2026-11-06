@echo off
title EcoMonitor - Demo
REM Sobe TUDO para a demonstracao: servidor (janela propria), 3 sensores (uma janela cada) e o painel.
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
if not exist target\ecomonitor.jar call "%~dp0COMPILAR.bat" nopause || exit /b 1

curl.exe -s -o nul http://127.0.0.1:8080/api/ping && (
  echo Servidor ja estava rodando.
  goto sensores
)
start "EcoMonitor - Servidor" cmd /k java -jar target\ecomonitor.jar
echo Aguardando o servidor subir...
set /a tentativas=0
:espera
timeout /t 1 /nobreak >nul
curl.exe -s -o nul http://127.0.0.1:8080/api/ping && goto sensores
set /a tentativas+=1
if %tentativas% lss 60 goto espera
echo [ERRO] O servidor nao respondeu em 60 s. Veja a janela "EcoMonitor - Servidor".
pause
exit /b 1

:sensores
echo Servidor no ar. Iniciando 3 sensores simulados...
call "%~dp02-SENSORES.bat"
timeout /t 3 /nobreak >nul
call "%~dp03-PAINEL.bat"
echo.
echo Painel aberto no navegador. Usuarios: admin / admin123  e  visitante / visitante123
echo Para parar: feche as janelas dos sensores e a do servidor.
timeout /t 10
