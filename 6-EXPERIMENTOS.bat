@echo off
title EcoMonitor - Experimentos
REM Roda os Experimentos A a E de uma vez (~8 min). O servidor precisa estar rodando (1-SERVIDOR.bat).
REM FECHE o painel e os sensores antes: eles disputam o servidor e distorcem as medidas.
REM Teste rapido (nao use no relatorio): 6-EXPERIMENTOS.bat rapido
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
java experimentos\RodarExperimentos.java %1
echo.
where python >nul 2>nul && (
  echo Gerando graficos...
  python experimentos\graficos.py
)
pause
