@echo off
REM Compila o projeto e gera target\ecomonitor.jar (usa o Maven Wrapper: nao precisa instalar Maven).
REM Rode de novo sempre que alterar o codigo do servidor.
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
echo Usando JDK em: %JAVA_HOME%
echo Compilando... na primeira vez o Maven baixa as dependencias, pode levar alguns minutos.
call "%~dp0mvnw.cmd" -q -B package -DskipTests
if errorlevel 1 (
  echo.
  echo [ERRO] A compilacao falhou. Veja as mensagens acima.
  if not "%1"=="nopause" pause
  exit /b 1
)
echo Pronto: target\ecomonitor.jar
if not "%1"=="nopause" pause
exit /b 0
