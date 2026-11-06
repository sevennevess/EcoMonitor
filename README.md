# EcoMonitor

Sistema distribuído de monitoramento ambiental em tempo real — **Projeto 1, CIC0124 Redes de Computadores (UnB, 2026)**.
Sensores **simulados** (temperatura, umidade e índice de qualidade do ar **fictício**) enviam medições por HTTP a um servidor
Java/Spring Boot, que valida, grava em SQLite, gera alertas e publica eventos em tempo real por WebSocket para painéis Web.
Os dados exibidos não são medições ambientais reais.

> Primeira vez? Leia **`PASSO-A-PASSO.md`** (explicação sem termos técnicos, com o caminho de cada arquivo).

## Como rodar (Windows, 1 minuto)

Requisito único: um **JDK 17 ou superior**. Se você tem o IntelliJ, já tem (ele baixa em `%USERPROFILE%\.jdks`).
Os scripts acham o JDK sozinhos; Maven **não** precisa estar instalado (o projeto traz o Maven Wrapper).

| Duplo clique em | O que faz |
|---|---|
| **`DEMO.bat`** | Sobe **tudo**: servidor, 3 sensores (uma janela cada) e abre o painel no navegador |
| `1-SERVIDOR.bat` | Só o servidor (porta 8080). Na 1ª vez compila o projeto (~1–3 min, baixa dependências) |
| `2-SENSORES.bat` | 3 sensores simulados (`SENSOR_001`, `002`, `003`). Servidor em outro PC: `2-SENSORES.bat http://IP:8080` |
| `3-PAINEL.bat` | Abre `http://127.0.0.1:8080` no navegador |
| `4-MENSAGENS-INVALIDAS.bat` | Envia 13 mensagens erradas e mostra o `ERRO` devolvido para cada uma (validação do protocolo) |
| `5-SENSOR-INSTAVEL.bat` | Sensor que some por 20 s a cada 5 ciclos: o servidor o marca `INATIVO` e depois `ATIVO` |
| `6-EXPERIMENTOS.bat` | Roda os Experimentos A–E (~8 min) e gera os gráficos. **Feche painel e sensores antes** |
| `COMPILAR.bat` | Recompila depois de alterar o código do servidor |
| `LIMPAR-BANCO.bat` | Apaga o banco para começar do zero (com o servidor parado) |
| `CAPTURA-1.bat` a `CAPTURA-3.bat` | Montam o cenário de cada captura do Wireshark (ver `documentacao/ROTEIRO-WIRESHARK.md`) |

Usuários de teste (dados não sensíveis):

| Usuário | Senha | Papel |
|---|---|---|
| `admin` | `admin123` | ADMIN (pode alterar limites de alerta) |
| `visitante` | `visitante123` | LEITURA |

**Dois clientes simultâneos:** entre como `admin` numa janela normal e como `visitante` numa janela anônima (`Ctrl+Shift+N`).
**Pela rede:** o servidor escuta em todas as interfaces; de outro PC, abra `http://<IPv4 do servidor>:8080`
(o `1-SERVIDOR.bat` mostra os IPv4; use o da sua rede Wi-Fi/cabo, ex. `192.168.1.x`, e não o de adaptadores virtuais como
o do VirtualBox `192.168.56.1`; na primeira execução, permita o Java no Firewall do Windows).

### Pelo IntelliJ IDEA

1. *File > Open* e escolha esta pasta (a que tem o `pom.xml`). Espere o Maven importar (barra inferior).
2. Se pedir SDK: *File > Project Structure > SDK* e escolha um JDK 17+ (ou *Download JDK*).
3. No topo, a configuração **EcoMonitor - Servidor** já aparece (vem da pasta `.run/`). Clique em ▶.
4. Sensores: no terminal do IntelliJ (`Alt+F12`), rode `.\2-SENSORES.bat`, ou
   `java simulador\SimuladorSensor.java SENSOR_001 TEMPERATURA,UMIDADE 2000`.

### Pelo terminal (qualquer sistema)

```
mvnw.cmd spring-boot:run                      (Linux/macOS: ./mvnw spring-boot:run)
java simulador\SimuladorSensor.java SENSOR_001 TEMPERATURA,UMIDADE 2000 http://127.0.0.1:8080
```
Argumentos do simulador: `<sensorId> <tipos> [intervaloMs] [servidor] [ciclosAteDesconectar]`.
Chave de registro padrão `chave-de-teste-ecomonitor` (em `application.properties`; o simulador lê `ECOMONITOR_CHAVE_REGISTRO`).

## Arquitetura

```
Simuladores (Java) --HTTP/JSON--> Servidor Spring Boot --JDBC--> SQLite (ecomonitor.db, modo WAL)
                                        |
Painéis Web (HTML/CSS/JS) <--HTTP (consultas)--+--WebSocket (eventos em tempo real)
```

- `src/main/java/br/edu/unb/ecomonitor/` — servidor: `controller` (API REST), `service` (regras), `repository` (SQL), `model`, `protocol` (mensagens e erros), `security` (login, tokens), `websocket`, `config`
- `src/main/resources/static/` — painel Web (`index.html`, `login.html`, `css/`, `js/`) · `schema.sql` — tabelas
- `simulador/` — `SimuladorSensor.java` (sensor simulado) e `TesteMensagensInvalidas.java` (validação)
- `experimentos/` — `Experimentos.java` (medições), `EmuladorRede.java` (atraso/perda), `RodarExperimentos.java` (roda tudo),
  `graficos.py`, `resultados/` (CSVs) e `capturas/` (Wireshark)
- `documentacao/` — `PROTOCOLO.md`, roteiros (Wireshark, vídeo, experimentos), checklist e relatório (`relatorio/`)
- Logs do servidor com data/hora: console e `logs/ecomonitor.log`

## API HTTP e WebSocket

Resumo (detalhes, campos, erros e exemplos em `documentacao/PROTOCOLO.md`):

| Método | Rota | Operação |
|---|---|---|
| POST | `/api/login` | Autenticar usuário |
| GET / POST | `/api/sensores` · GET `/api/sensores/{id}` | Listar / cadastrar (chave) / consultar sensores |
| POST / GET | `/api/medicoes` | Enviar (token do sensor) / consultar histórico com filtros |
| GET | `/api/alertas` | Consultar alertas |
| GET / PUT | `/api/configuracoes` | Ler / alterar limites (PUT: ADMIN) |
| GET | `/api/ping` · `/api/teste/carga` | Apoio aos experimentos |
| WS | `/ws?token=...` | Eventos em tempo real |

## Experimentos

`6-EXPERIMENTOS.bat` (ou `java experimentos\RodarExperimentos.java`) roda, com o servidor no ar:
A (RTT, 100 amostras por condição), B (vazão 1 KB–1 MB), C (1, 2 e 5 clientes), D (TCP × UDP, 1000 mensagens) e
E (condições `local`, `proxy0`, `atraso50`, `perda5`, `jitter` via `EmuladorRede.java`, sem precisar de administrador).
Resultados em `experimentos/resultados/` e gráficos (`pip install matplotlib`) em `documentacao/relatorio/figuras/`.
Detalhes em `experimentos/ROTEIRO-EXPERIMENTOS.md`.

## Dependências

- Servidor (Maven, `pom.xml`): Spring Boot 4.1.2 (Web MVC, WebSocket, JDBC), `org.xerial:sqlite-jdbc`, Chart.js 4.4.7 (WebJar,
  servido pelo próprio servidor: o painel funciona sem internet).
- Simulador e experimentos: só o JDK (programas Java de arquivo único). Gráficos: Python 3 + matplotlib.
- Análise: Wireshark (com Npcap e suporte a loopback).

## Segurança (resumo e limites)

Senhas com PBKDF2 + sal; tokens aleatórios (sensores guardados como SHA-256); entradas validadas; `/api/**` e WebSocket exigem autenticação.
**Sem TLS**: o tráfego é legível na rede (útil para o Wireshark; em produção usar HTTPS/WSS). Sessões em memória. Servidor único.

## Uso de IA e ferramentas

Desenvolvido com apoio do assistente de IA Claude (Anthropic); o autor executou, testou e revisou o código.
Bibliotecas e ferramentas: Spring Boot, SQLite JDBC (xerial), Chart.js, IntelliJ IDEA, Maven, Python/matplotlib, Wireshark/Npcap.

## Autor

Trabalho individual: **Vitor Neves Garcia** — matrícula **252003613** (todas as etapas).
