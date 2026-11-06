@echo off
REM Apaga o banco SQLite para comecar do zero (pare o servidor antes).
cd /d "%~dp0"
if not exist ecomonitor.db (
  echo Nao ha banco para apagar.
  pause
  exit /b 0
)
del ecomonitor.db ecomonitor.db-wal ecomonitor.db-shm 2>nul
if exist ecomonitor.db (
  echo [ERRO] Nao foi possivel apagar: o servidor ainda esta rodando? Feche-o e tente de novo.
) else (
  echo Banco apagado. Na proxima subida o servidor recria as tabelas e os usuarios.
)
pause
