# Protocolo de aplicação do EcoMonitor — versão 1.0

Mensagens em **JSON (UTF-8)**. O protocolo define *o que* é dito (campos, validações, erros, sequência); o transporte é
dividido entre duas camadas com responsabilidades separadas:

| Camada | Uso | Direção |
|---|---|---|
| **HTTP/1.1** (REST, porta 8080) | Comandos e consultas: registrar sensor, enviar medição, login, consultar sensores/medições/alertas, ler e alterar limites | Cliente → servidor, com resposta (requisição/resposta) |
| **WebSocket** (`/ws`, porta 8080) | Eventos em tempo real | Servidor → clientes (o servidor ignora mensagens recebidas) |

Justificativa do WebSocket: o painel precisa receber medições e alertas assim que ocorrem, sem *polling*. O WebSocket mantém
uma conexão TCP aberta e permite o servidor **empurrar** eventos; HTTP puro exigiria consultas repetidas.

## 1. Identificação e autenticação

| Quem | Como se identifica | Onde |
|---|---|---|
| Sensor (registro) | Chave de provisionamento | Cabeçalho `X-Chave-Registro` em `POST /api/sensores` |
| Sensor (medições) | Token recebido no registro (guardado como SHA-256 no servidor) | Cabeçalho `X-Sensor-Token` em `POST /api/medicoes` |
| Usuário | Token de sessão obtido no `LOGIN` (válido por `ecomonitor.sessao-validade-horas`) | `Authorization: Bearer <token>` em `/api/**`; `?token=<token>` na URL do WebSocket |
| Papéis | `ADMIN` (altera limites) e `LEITURA` | `PUT /api/configuracoes` exige `ADMIN` |

Senhas: PBKDF2-HMAC-SHA256 com sal aleatório e 120 000 iterações. Nenhuma senha em texto puro no banco.
Rotas públicas: `POST /api/login`, `GET /api/ping`, `/api/teste/**` (só experimentos), e os dois POSTs de sensor (autenticados pelos cabeçalhos acima).

## 2. Mensagens (corpo JSON)

Todo cliente→servidor traz `versao` (`"1.0"`) e `tipoMensagem`.

### 2.1 REGISTRAR_SENSOR — `POST /api/sensores`
```json
{"versao":"1.0","tipoMensagem":"REGISTRAR_SENSOR","sensorId":"SENSOR_001","nome":"Sala 1","tipos":["TEMPERATURA","UMIDADE"]}
```
`sensorId`: 3 a 30 caracteres `[A-Za-z0-9_]`. `tipos`: lista não vazia de `TEMPERATURA`, `UMIDADE`, `QUALIDADE_AR`.
Resposta `201` (novo) ou `200` (já existia: dados atualizados e **token renovado**):
```json
{"versao":"1.0","tipoMensagem":"CONFIRMAR_RECEBIMENTO","codigo":"CRIADO","mensagem":"Sensor cadastrado: SENSOR_001","token":"<token do sensor>"}
```
O token só aparece nesta resposta.

### 2.2 MEDICAO — `POST /api/medicoes`
```json
{"versao":"1.0","tipoMensagem":"MEDICAO","sensorId":"SENSOR_001","tipoMedicao":"TEMPERATURA","valor":28.5,"unidade":"C","timestamp":"2026-11-04T16:30:00"}
```

| Campo | Significado | Validação |
|---|---|---|
| `versao` | Versão do protocolo | deve ser `1.0` |
| `tipoMensagem` | Operação | deve ser `MEDICAO` |
| `sensorId` | Sensor emissor | cadastrado e com token válido |
| `tipoMedicao` | Categoria | uma de `TEMPERATURA`, `UMIDADE`, `QUALIDADE_AR` e medida por esse sensor |
| `valor` | Valor medido | número dentro da faixa do tipo |
| `unidade` | Unidade | exata: `C`, `%`, `IQA` (índice simulado) |
| `timestamp` | Instante da medição | ISO-8601 local, ex.: `2026-11-04T16:30:00` |

| Tipo | Unidade | Faixa aceita |
|---|---|---|
| TEMPERATURA | `C` | −50 a 100 |
| UMIDADE | `%` | 0 a 100 |
| QUALIDADE_AR | `IQA` | 0 a 500 |

Resposta `201`: `{"versao":"1.0","tipoMensagem":"CONFIRMAR_RECEBIMENTO","codigo":"OK","mensagem":"Medicao registrada"}`

### 2.3 LOGIN — `POST /api/login`
```json
{"versao":"1.0","tipoMensagem":"LOGIN","usuario":"admin","senha":"admin123"}
```
Resposta `200`: `{"versao":"1.0","tipoMensagem":"LOGIN_OK","codigo":"OK","mensagem":"Autenticado","usuario":"admin","papel":"ADMIN","token":"<sessao>"}`

### 2.4 CONFIGURAR_LIMITE — `PUT /api/configuracoes` (ADMIN)
```json
{"versao":"1.0","tipoMensagem":"CONFIGURAR_LIMITE","tipoMedicao":"TEMPERATURA","operador":"MAIOR","limite":40}
```
`operador`: `MAIOR` (alerta quando valor > limite) ou `MENOR` (valor < limite). Limites iniciais (ilustrativos, não oficiais):
temperatura MAIOR 40, umidade MENOR 30, qualidade do ar MAIOR 80.

### 2.5 CONSULTAR_HISTORICO — `GET /api/medicoes` (consulta, sem corpo)
Parâmetros opcionais: `sensorId`, `tipoMedicao`, `dataInicial`, `dataFinal` (ISO-8601, comparação textual), `limite` (1–1000, padrão 100).
Resposta: lista JSON de `{id, sensorId, tipoMedicao, valor, unidade, dataHora}`, da mais recente para a mais antiga.
Outras consultas: `GET /api/sensores`, `GET /api/sensores/{id}`, `GET /api/alertas?sensorId=&limite=`, `GET /api/configuracoes`.

### 2.6 ERRO — resposta a qualquer mensagem rejeitada
```json
{"versao":"1.0","tipoMensagem":"ERRO","codigo":"UNIDADE_INCOMPATIVEL","mensagem":"Unidade esperada para TEMPERATURA: C"}
```
A mensagem rejeitada **não** altera o banco e gera um aviso no log do servidor.

## 3. Códigos de erro

| Código | HTTP | Causa |
|---|---|---|
| `JSON_INVALIDO` | 400 | Corpo não é JSON válido |
| `CAMPO_AUSENTE` | 400 | Falta campo obrigatório |
| `VERSAO_NAO_SUPORTADA` | 400 | `versao` ≠ `1.0` |
| `TIPO_MENSAGEM_INVALIDO` | 400 | `tipoMensagem` incompatível com a rota |
| `ID_INVALIDO` | 400 | `sensorId` fora do padrão |
| `TIPO_MEDICAO_INVALIDO` | 400 | Tipo de medição desconhecido |
| `UNIDADE_INCOMPATIVEL` | 400 | Unidade diferente da esperada para o tipo |
| `TIMESTAMP_INVALIDO` | 400 | Formato de data inválido |
| `OPERADOR_INVALIDO` | 400 | Operador diferente de `MAIOR`/`MENOR` |
| `TIPO_NAO_SUPORTADO_PELO_SENSOR` | 422 | O sensor não mede esse tipo |
| `VALOR_FORA_DA_FAIXA` | 422 | Valor fora da faixa válida |
| `NAO_AUTENTICADO` / `SESSAO_INVALIDA` | 401 | Sem token de usuário / token inválido ou expirado |
| `CREDENCIAIS_INVALIDAS` | 401 | Usuário ou senha incorretos |
| `CHAVE_REGISTRO_INVALIDA` | 401 | Chave de provisionamento errada |
| `TOKEN_AUSENTE` / `TOKEN_INVALIDO` | 401 | Token do sensor ausente / errado |
| `SEM_PERMISSAO` | 403 | Papel sem permissão |
| `SENSOR_DESCONHECIDO` | 404 | Sensor não cadastrado |
| `ERRO_BANCO` | 500 | Falha ao acessar o SQLite |

## 4. Eventos WebSocket (`ws://<host>:8080/ws?token=<sessao>`)

O handshake é recusado (HTTP 401) sem token de sessão válido. Eventos são objetos JSON com `tipoEvento`:

```json
{"tipoEvento":"NOVA_MEDICAO","sensorId":"SENSOR_001","tipoMedicao":"TEMPERATURA","valor":28.5,"unidade":"C","timestamp":"2026-11-04T16:30:00"}
{"tipoEvento":"NOVO_ALERTA","sensorId":"SENSOR_001","tipoMedicao":"TEMPERATURA","valor":45.0,"limite":40.0,"mensagem":"TEMPERATURA acima do limite: 45.0 (limite 40.0)","timestamp":"2026-11-04T16:30:02"}
{"tipoEvento":"SENSOR_CONECTADO","sensorId":"SENSOR_001","status":"ATIVO","timestamp":"2026-11-04T16:30:00"}
{"tipoEvento":"SENSOR_DESCONECTADO","sensorId":"SENSOR_001","status":"INATIVO","timestamp":"2026-11-04T16:31:00"}
```

Regras de negócio:
- **Alerta** é gerado na *transição* de normal para violando o limite (não a cada medição); volta ao normal quando um valor dentro do limite chega. Cada alerta é gravado e enviado como `NOVO_ALERTA`.
- **Desconexão**: sensor sem enviar medições por mais que `ecomonitor.sensor-timeout-segundos` (padrão 15 s) vira `INATIVO` (verificação a cada 5 s). Ao voltar a enviar, vira `ATIVO`.
- O servidor mantém o conjunto de sessões abertas e envia cada evento a todas; sessão com falha de envio é removida.

## 5. Sequências

```
Sensor                                   Servidor                                   Painéis (WS)
  | POST /api/sensores (X-Chave-Registro)  |                                            |
  |--------------------------------------->| valida, grava (hash do token)              |
  |<--- 201 CONFIRMAR_RECEBIMENTO + token -|                                            |
  | POST /api/medicoes (X-Sensor-Token)    |                                            |
  |--------------------------------------->| valida, grava, avalia limites              |
  |<--- 201 CONFIRMAR_RECEBIMENTO ---------|-- NOVA_MEDICAO / NOVO_ALERTA -------------->|
  |  (mensagem inválida)                   |                                            |
  |--------------------------------------->| rejeita, registra no log                   |
  |<--- 4xx ERRO + codigo -----------------|                                            |

Usuário (navegador)                       Servidor
  | POST /api/login ---------------------->| confere hash da senha
  |<--- 200 LOGIN_OK + token --------------|
  | GET /api/... (Authorization: Bearer) ->|
  | GET /ws?token=... (Upgrade: websocket) |
  |<--- 101 Switching Protocols -----------|<=== eventos enviados pelo servidor ===
```

## 6. Tratamento de falhas

| Situação | Comportamento |
|---|---|
| Mensagem malformada/inválida | Resposta `ERRO` com código; nada é gravado; aviso no log |
| Servidor indisponível | Simulador tenta registrar de novo a cada 3 s e continua enviando; painel reconecta o WebSocket a cada 3 s e recarrega os dados |
| Sensor silencioso | Marcado `INATIVO` e evento `SENSOR_DESCONECTADO` |
| Token de sensor recusado (401/404) | Simulador se registra de novo e obtém novo token |
| Sessão de usuário expirada | Painel volta ao login |

## 7. Limitações conhecidas

- Sem TLS: senha de login, tokens e medições trafegam em texto puro (visível no Wireshark). Mitigação: HTTPS/WSS.
- Token do WebSocket na URL (limitação dos navegadores, que não enviam cabeçalhos customizados no handshake).
- Sessões de usuário apenas em memória: reiniciar o servidor exige novo login.
- Servidor único (ponto único de falha) e SQLite com uma conexão (uma escrita por vez).
- Valores de qualidade do ar são um índice **simulado**, não medições reais.
