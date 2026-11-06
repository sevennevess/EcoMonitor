@echo off
title Captura 3 - Perdas UDP
REM Monta o cenario da Captura 3 do Wireshark: eco UDP + emulador com 5% de perda por sentido.
REM NAO precisa do servidor EcoMonitor.
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
REM Se uma execucao anterior foi fechada no meio, o eco e o emulador ficam presos nas portas 9090/9203
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -match 'udp-servidor 9090|EmuladorRede.java udp 9203' } | ForEach-Object { Write-Host ('Encerrando sobra de execucao anterior: PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force }"
java experimentos\Captura3.java
echo.
echo Salve a captura em %~dp0experimentos\capturas\captura3-udp.pcapng
echo IMPORTANTE: feche esta janela so depois de aparecer PRONTO.
pause
