@echo off
REM Localiza um JDK (17 ou superior) e coloca no PATH. Chamado pelos outros .bat.
REM Ordem: JAVA_HOME -> java no PATH -> JDKs baixados pelo IntelliJ (%USERPROFILE%\.jdks) -> pastas comuns.

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" goto achou

for /f "delims=" %%J in ('where javac 2^>nul') do (
  for %%P in ("%%~dpJ..") do set "JAVA_HOME=%%~fP"
  goto achou
)

for /d %%D in ("%USERPROFILE%\.jdks\*") do if exist "%%D\bin\javac.exe" set "JAVA_HOME=%%D"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" goto achou

for /d %%D in ("%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Microsoft\*" "%ProgramFiles%\Zulu\*" "%ProgramFiles%\Amazon Corretto\*" "%ProgramFiles%\JetBrains\openjdk*") do (
  if exist "%%D\bin\javac.exe" set "JAVA_HOME=%%D"
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" goto achou

echo.
echo [ERRO] Nenhum JDK encontrado.
echo Instale um JDK 17 ou superior. Pelo IntelliJ: File ^> Project Structure ^> SDK ^> Download JDK.
echo Ou defina a variavel JAVA_HOME apontando para a pasta do JDK.
echo.
pause
exit /b 1

:achou
set "PATH=%JAVA_HOME%\bin;%PATH%"
exit /b 0
