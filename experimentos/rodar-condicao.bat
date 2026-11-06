@echo off
REM Roda TODOS os experimentos para UMA condicao de rede (rotulo livre, ex.: local, lag50, perda5).
REM Pre-requisitos: servidor EcoMonitor rodando; em outros terminais:
REM   java experimentos\Experimentos.java tcp-servidor 9091
REM   java experimentos\Experimentos.java udp-servidor 9090
REM Feche o dashboard e os simuladores antes (eles competem pelo servidor).
REM Uso (na raiz do projeto): experimentos\rodar-condicao.bat local
if "%1"=="" (
  echo Uso: experimentos\rodar-condicao.bat nome_da_condicao
  exit /b 1
)
set S=http://127.0.0.1:8080
set C=%1
java experimentos\Experimentos.java rtt %S% %C% 100
java experimentos\Experimentos.java throughput %S% %C% 10
java experimentos\Experimentos.java concorrencia %S% %C% 1 30 100
java experimentos\Experimentos.java concorrencia %S% %C% 2 30 100
java experimentos\Experimentos.java concorrencia %S% %C% 5 30 100
java experimentos\Experimentos.java tcp-cliente 127.0.0.1 9091 %C% 1000 100 10
java experimentos\Experimentos.java udp-cliente 127.0.0.1 9090 %C% 1000 100 10
echo.
echo Concluido. Resultados em experimentos\resultados\ (resumo.csv e arquivos por experimento).
