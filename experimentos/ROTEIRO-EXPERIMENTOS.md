# Roteiro dos experimentos de rede (A–E)

Tudo é executado **a partir da raiz do projeto** (onde está o `pom.xml`). Todos os números do relatório
devem vir dos arquivos de `experimentos/resultados/`. **Nunca escreva um resultado que não saiu destes testes.**

## 1. Jeito fácil: tudo de uma vez

1. Suba o servidor (`1-SERVIDOR.bat` ou IntelliJ). **Feche** o painel e os sensores (eles disputam o servidor).
   Evite usar o PC durante a medição (downloads, antivírus fazendo varredura etc. aparecem nos números).
2. Duplo clique em `6-EXPERIMENTOS.bat` (ou `java experimentos\RodarExperimentos.java`). Leva ~8 minutos.
   O programa sobe sozinho os servidores de eco TCP/UDP e os emuladores de rede, roda tudo e encerra os auxiliares.
3. Gráficos: `pip install matplotlib` (uma vez) e `python experimentos\graficos.py` → `documentacao\relatorio\figuras\`.

`6-EXPERIMENTOS.bat rapido` faz uma passada curta só para conferir que tudo funciona (salva em `resultados\teste-rapido\`; não use no relatório).
Uma nova execução completa move os CSVs anteriores para `resultados\anteriores\<data>\` (nada se mistura).
O ambiente (CPU, RAM, Windows, Java) é anotado automaticamente em `resultados\ambiente.txt`.

## 2. O que cada experimento mede

| Exp. | Comando (`java experimentos\Experimentos.java ...`) | O que mede | Como |
|---|---|---|---|
| A | `rtt <servidor> <condicao> [n=100]` | RTT de aplicação de `PING` (sem banco) e `MEDICAO` (com banco), separado do tempo no servidor | `rtt_ms` medido no cliente; `servidor_ms` vem do cabeçalho `X-Tempo-Servidor-Us`; `rede_estimada_ms = rtt − servidor` (inclui pilha TCP/HTTP e SO) |
| B | `throughput <servidor> <condicao> [reps=10]` | Vazão útil (só o corpo, sem cabeçalhos) em upload e download de 1 KB, 10 KB, 100 KB e 1 MB | `bytes × 8 / tempo` em Mbit/s |
| C | `concorrencia <servidor> <condicao> <clientes> [seg=30] [intervaloMs=100]` | Latência (média, mediana, DP, p95), taxa de mensagens e erros com N clientes simultâneos | N threads, cada uma com sua conexão e seu sensor, enviando `MEDICAO` |
| D | `tcp-cliente` / `udp-cliente <host> <porta> <condicao> [n] [bytes] [intervaloMs]` | Perda, duplicação, fora de ordem e RTT de eco para TCP e UDP com **a mesma carga** | mensagem numerada: 8 B sequência + 8 B tempo + preenchimento |
| E | A, B, C e D repetidos em cada condição | Efeito de atraso, perda e variação | `EmuladorRede.java` (seção 3) |

Detalhes metodológicos para citar no relatório:

- Antes de tudo, o servidor é **aquecido** por 15 s (`aquecer`: PING e MEDICAO sem registrar). Sem isso, a primeira
  condição medida sai mais lenta porque o JIT do servidor ainda não compilou o código (observado: tempo no servidor
  1,28 ms frio x 0,26 ms aquecido para MEDICAO).
- As primeiras 30 requisições de cada série (3 no throughput) são **aquecimento do cliente** e são descartadas. A primeira inclui handshake TCP e carregamento de classes (JIT).
- As requisições reutilizam a conexão (HTTP/1.1 keep-alive): o RTT medido **não** inclui handshake TCP, exceto na primeira.
- O desvio padrão é amostral (n−1). O p95 é o valor abaixo do qual ficam 95% das amostras.
- No D, o RTT é de **aplicação** (eco numerado); perdas e retransmissões de **transporte** devem ser lidas no Wireshark.
  TCP e UDP usam as mesmas n, bytes e intervalo (1000 mensagens de 100 B a cada 10 ms). No TCP, `TCP_NODELAY` está ligado.
- UDP: use `bytes <= 1400` para evitar fragmentação IP.

## 3. Condições de rede (Experimento E) com o `EmuladorRede.java`

O emulador é um proxy em espaço de usuário (não precisa de administrador nem de instalar nada):
`cliente → 127.0.0.1:portaLocal [atraso/perda] → destino`. Portas usadas pelo `RodarExperimentos.java`:

| Condição | Porta | Emulador | Experimentos |
|---|---|---|---|
| `local` | direto (8080, 9091, 9090) | nenhum | A, B, C (100 ms e carga máxima `local_cargamax`), D |
| `proxy0` | 8081 (HTTP), 9101 (TCP), 9201 (UDP) | atraso 0: mede o custo do próprio emulador | A, D |
| `atraso50` | 8082 (HTTP), 9102 (TCP), 9202 (UDP) | 25 ms por sentido (RTT +50 ms) | A, B, C, D |
| `perda5` | 9203 (UDP) | descarte de 5% em cada sentido (semente 42) | D/UDP |
| `jitter` | 9204 (UDP) | 25 ms ± 15 ms por sentido (uniforme; pode reordenar) | D/UDP |

**Limitação (escrever no relatório):** um proxy em espaço de usuário termina a conexão TCP dos dois lados, então **não
consegue provocar perda de segmentos TCP** (o kernel nunca vê a perda). Por isso a condição de perda vale só para UDP.
Para ver retransmissões TCP de verdade, use o **Clumsy** (Windows, precisa de administrador), seção 4.

Uso manual: `java experimentos\EmuladorRede.java tcp 8082 127.0.0.1:8080 25` ou
`java experimentos\EmuladorRede.java udp 9203 127.0.0.1:9090 0 5 0 42` (atraso, perda %, jitter, semente).

## 4. Opcional: Clumsy (perda de pacotes TCP real)

Clumsy (GitHub `jagt/clumsy`, usa WinDivert, roda como Administrador, funciona em loopback). Filtros
(pacotes de loopback só podem ser filtrados como `outbound`):

| Alvo | Filtro |
|---|---|
| HTTP/WebSocket (8080) | `tcp and outbound and (tcp.DstPort == 8080 or tcp.SrcPort == 8080)` |
| Eco TCP (9091) | `tcp and outbound and (tcp.DstPort == 9091 or tcp.SrcPort == 9091)` |

Com o Clumsy ligado (módulo **Drop**, 5%), rode com outro rótulo, por exemplo:
`java experimentos\Experimentos.java tcp-cliente 127.0.0.1 9091 clumsy_perda5 1000 100 10`
(o eco TCP precisa estar rodando: `java experimentos\Experimentos.java tcp-servidor 9091`). O script antigo
`experimentos\rodar-condicao.bat <rotulo>` roda A–D numa condição só, útil com o Clumsy.

## 5. Gráficos e planilha

`python experimentos\graficos.py` gera `fig_rtt.png`, `fig_vazao.png`, `fig_concorrencia.png` e `fig_transporte.png`.
Para planilha: os `.csv` usam vírgula como separador e ponto decimal (Google Sheets: Arquivo > Configurações > Localidade
*Estados Unidos* antes de importar; Excel: *Dados > Obter dados > De texto/CSV*).

| Arquivo | Conteúdo |
|---|---|
| `resumo.csv` | uma linha por série: n, média, mediana, DP, mín, máx, p95 |
| `rtt_<cond>.csv`, `throughput_<cond>.csv`, `concorrencia_<cond>_<N>clientes.csv`, `transporte_<proto>_<cond>.csv` | amostras brutas |
| `concorrencia_resumo.csv`, `transporte_resumo.csv` | tabelas prontas para C e D |
| `ambiente.txt` · `logs/` | hardware/software da execução · saída dos auxiliares (eco e emuladores) |
