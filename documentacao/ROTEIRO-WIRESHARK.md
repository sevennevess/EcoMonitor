# As 3 capturas do Wireshark, passo a passo

O enunciado pede pelo menos **3 capturas comentadas** do tráfego real da aplicação. Cada captura gera três coisas:

1. um arquivo de captura `.pcapng` (vai para `experimentos\capturas\`);
2. um **print** da tela do Wireshark (vira figura do relatório, em `documentacao\relatorio\figuras\`);
3. alguns **números** que você copia para o relatório (os campos em vermelho `[PREENCHER]` da Seção III-B7).

Cada captura tem um script (`CAPTURA-1.bat`, `CAPTURA-2.bat`, `CAPTURA-3.bat`) que monta a situação sozinho:
você só começa e para a gravação. Tempo total: ~30 minutos.

Pasta do projeto: `C:\Users\vitor\Desktop\Agora\ecomonitor\`

---

## Parte 0 — Preparar o Wireshark (uma vez só)

1. Entre em **https://www.wireshark.org/download.html** e baixe **Windows x64 Installer**.
2. Instale clicando em *Next*. Quando aparecer o instalador do **Npcap**, deixe marcada a opção
   **"Support loopback traffic ('Npcap Loopback Adapter' will be created)"**. Conclua.
3. Abra o Wireshark. A tela inicial mostra uma lista de "interfaces" (placas de rede).
   A que importa aqui é **Adapter for loopback traffic capture**: ela grava o tráfego `127.0.0.1`,
   que é por onde os programas do projeto conversam dentro do seu PC.
4. Anote a versão (menu *Help > About Wireshark*): vai no relatório.

### O mínimo do Wireshark

| Para | Faça |
|---|---|
| **Começar a gravar** | duplo clique em *Adapter for loopback traffic capture* (os pacotes começam a rolar) |
| **Parar** | botão **quadrado vermelho** na barra de cima (ou `Ctrl+E`) |
| **Filtrar** | digite na barra *Apply a display filter...* (logo acima da lista) e aperte `Enter`. Verde = filtro certo; vermelho = digitado errado. Para tirar o filtro, apague e dê `Enter` |
| **Ver dentro de um pacote** | clique na linha; o painel do meio mostra as camadas. Clique na setinha `>` para abrir cada uma |
| **Salvar** | *File > Save As...*, escolha a pasta e o nome indicados abaixo |
| **Data e hora da captura** | *Statistics > Capture File Properties*, linha **First packet** |
| **Tirar print** | `Win + Shift + S`, arraste sobre a área, clique na notificação que aparece e aperte `Ctrl + S` |

Os prints devem ter **exatamente** estes nomes, na pasta
`C:\Users\vitor\Desktop\Agora\ecomonitor\documentacao\relatorio\figuras\`:
`captura1.png`, `captura2.png`, `captura3.png`. O relatório os encaixa sozinho.

---

## Captura 1 — HTTP em cima do TCP

**O que ela prova:** (1) o TCP abre a conexão com 3 pacotes (*handshake*: SYN, SYN-ACK, ACK);
(2) o HTTP viaja dentro do TCP, que viaja dentro do IP (*encapsulamento*); (3) várias medições usam a
**mesma** conexão (*keep-alive*); (4) tudo trafega **em texto puro**, sem criptografia.

**Gravar:**
1. Duplo clique em `1-SERVIDOR.bat`. Espere aparecer `Started EcomonitorApplication`. Deixe a janela aberta.
2. Abra o Wireshark (sem gravar ainda).
3. Duplo clique em `CAPTURA-1.bat`. Quando ele pedir, dê duplo clique em *Adapter for loopback traffic capture*
   no Wireshark, volte para a janela preta e aperte uma tecla.
4. Em ~15 s aparece **PRONTO**. Pare a gravação (quadrado vermelho).
5. *File > Save As* → `C:\Users\vitor\Desktop\Agora\ecomonitor\experimentos\capturas\captura1-http.pcapng`
6. **Não feche a janela preta ainda**: ela mostra o tempo de cada medição, ex. `HTTP 201 (5.9 ms)`. Anote os 6 valores.

**Olhar e anotar:**

| # | Faça | Anote para o relatório |
|---|---|---|
| a | Filtro `tcp.port == 8080`. As 3 primeiras linhas têm `[SYN]`, `[SYN, ACK]` e `[ACK]` na coluna *Info* (é o *handshake*) | a **porta de origem** do sensor: o número que não é 8080, ex. `53124` |
| b | Filtro `http`. Aparecem `POST /api/sensores`, 6× `POST /api/medicoes`, 5× `GET /api/ping` e as respostas `201`/`200` | — |
| c | Clique num `POST /api/medicoes`. No painel do meio aparecem as camadas: *Null/Loopback* → *Internet Protocol Version 4* → *Transmission Control Protocol* → *Hypertext Transfer Protocol* → *JavaScript Object Notation* | **Content-Length** (abra *Hypertext Transfer Protocol*) e o número da coluna **Length** dessa linha |
| d | Clique direito no `POST /api/medicoes` → *Follow* → *TCP Stream*. Abre uma janela com toda a conversa em texto: vermelho = sensor, azul = servidor. Todas as medições estão na mesma conversa (= *keep-alive*) e o token aparece legível (= sem criptografia). Feche essa janela | — |
| e | Filtro `http.response`. Clique numa resposta, abra *Hypertext Transfer Protocol* e ache `[Time since request: 0.00xxx seconds]`. Clique direito nela → *Apply as Column*. Agora há uma coluna com o tempo de cada resposta | os 6 tempos das respostas `201` de `/api/medicoes` (ignore a do `/api/sensores`) e os 5 das respostas `200` do ping. Multiplique por 1000 para ter ms |
| f | *Statistics > Capture File Properties* | data e hora (*First packet*) |

**Print (`captura1.png`):** filtro `tcp.port == 8080`, role até o topo para mostrar o *handshake* e alguns POST,
com um `POST /api/medicoes` selecionado e as camadas abertas no painel do meio.

**Mediana:** ordene os valores; com 6 valores, é a média dos dois do meio; com 5, é o do meio.

---

## Captura 2 — WebSocket e várias conexões ao mesmo tempo

**O que ela prova:** (1) o WebSocket começa como um pedido HTTP com `Upgrade: websocket` e resposta
`101 Switching Protocols`; (2) depois disso, o **servidor envia dados sem o navegador pedir**;
(3) várias conexões chegam à mesma porta 8080 e o servidor as separa pela porta de origem (*multiplexação*).

**Gravar:**
1. O servidor (`1-SERVIDOR.bat`) continua aberto.
2. Duplo clique em `CAPTURA-2.bat` e siga os 3 passos que ele mostra:
   - **Passo 1:** comece a gravar no Wireshark e aperte uma tecla na janela preta.
   - **Passo 2:** ele abre o painel numa janela normal e numa anônima. Entre como `admin` / `admin123`
     na normal e `visitante` / `visitante123` na anônima. Quando os dois mostrarem *tempo real: conectado*, aperte uma tecla.
   - **Passo 3:** ele abre 3 sensores. Espere ~30 s vendo os painéis se mexerem.
3. Pare a gravação. *File > Save As* → `...\experimentos\capturas\captura2-websocket.pcapng`. Feche as janelas dos sensores.

**Olhar e anotar:**

| # | Faça | Anote |
|---|---|---|
| a | Filtro `http.response.code == 101`. Aparecem 2 linhas `101 Switching Protocols` (uma por painel). Clique numa e abra *Hypertext Transfer Protocol*: tem `Upgrade: websocket` e `Sec-WebSocket-Accept` | — |
| b | Filtro `http.request.uri contains "/ws"`. É o pedido `GET /ws?token=...` do navegador: o token de sessão aparece na URL, legível | — |
| c | Filtro `websocket`. As linhas `WebSocket Text [FIN]` saem da porta **8080** para o navegador. Clique numa e, no painel do meio, abra *WebSocket* até ver o texto `{"tipoEvento":"NOVA_MEDICAO",...}`: é o servidor empurrando a medição | — |
| d | Filtro `tcp.port == 8080`, depois *Statistics > Conversations*, aba **TCP**, marque **Limit to display filter** | o **número de linhas** (cada linha é uma conexão; todas com *Port B* = 8080 e *Port A* diferente) |
| e | *Statistics > Capture File Properties* | data e hora |

**Print (`captura2.png`):** filtro `http.response.code == 101 || websocket`, mostrando as linhas `101` e os
`WebSocket Text`, com um quadro `NOVA_MEDICAO` selecionado.

---

## Captura 3 — perdas no UDP

**O que ela prova:** o UDP **não retransmite**: um datagrama perdido simplesmente some, e só dá para perceber
contando. O script põe um "emulador de rede" no meio do caminho que joga fora 5% dos pacotes em cada sentido.
Como o emulador é um programa entre o cliente e o servidor de eco, o Wireshark vê os dois lados dele e você
consegue contar quantos pacotes entraram e quantos saíram.

```
cliente  --(porta 9203)-->  emulador (descarta 5%)  --(porta 9090)-->  servidor de eco
cliente  <--(porta 9203)--  emulador (descarta 5%)  <--(porta 9090)--  servidor de eco
```

**Gravar** (o servidor EcoMonitor **não** é necessário):
1. Duplo clique em `CAPTURA-3.bat`. Espere a mensagem **AGORA, NO WIRESHARK**.
2. Comece a gravar no Wireshark, volte para a janela preta e aperte **ENTER**.
3. Em ~15 s aparece **PRONTO** com os números esperados (ex.: `ida: 955 encaminhados, 45 descartados |
   volta: 914 encaminhados, 41 descartados` e `perdidos=86`). **Anote esses números.**
4. Pare a gravação. *File > Save As* → `...\experimentos\capturas\captura3-udp.pcapng`

**Olhar e anotar:**

| # | Faça | Anote |
|---|---|---|
| a | Filtro `udp.port == 9203 \|\| udp.port == 9090` (as duas barras verticais significam "ou") | — |
| b | *Statistics > Conversations*, aba **UDP**, marque **Limit to display filter**. Aparecem 2 linhas | os 4 números das colunas **Packets A → B** e **Packets B → A** |
| c | Clique num datagrama da lista e veja o conteúdo no painel de baixo: os 8 primeiros bytes são o número de sequência da mensagem. Não há confirmação nem retransmissão | — |
| d | *Statistics > Capture File Properties* | data e hora |

Como ler as 2 linhas (o lado **A** é quem mandou o primeiro pacote):

| Linha | Packets A → B | Packets B → A |
|---|---|---|
| cliente ↔ `9203` | enviados pelo cliente (1000) | devolvidos ao cliente (ex. 914) |
| emulador ↔ `9090` | repassados ao eco (ex. 955) | respostas do eco (ex. 955) |

Perda na ida = 1000 − 955 = 45 · perda na volta = 955 − 914 = 41 · total = 86 = o `perdidos` do cliente.
Se os números do Wireshark baterem com os do script, a captura comprova as perdas. (Se aparecerem números dobrados,
por exemplo 2000 em vez de 1000, o Wireshark registrou cada pacote duas vezes: divida por 2 e comente isso.)

**Print (`captura3.png`):** a janela *Conversations* na aba UDP com as duas linhas.

---

## Colocar no relatório

1. Copie os três `.png` para `documentacao\relatorio\figuras\` (nomes exatos acima).
2. No Overleaf, envie os três `.png` para a pasta `figuras` do projeto (botão *Upload*) e clique em *Recompile*:
   os quadros vermelhos das capturas viram as suas imagens.
3. No editor do Overleaf, use `Ctrl + F` e procure **PREENCHER**. Troque cada `\pend{...}` pelo valor anotado
   (só o número, sem o `\pend{}`). Na Seção III-B7 e na Tabela "Correlação", os campos são, em ordem:
   data e hora de cada captura, porta de origem, Content-Length, Length, número de conexões, as 4 contagens da
   Captura 3, as medianas do `http.time` e do sensor, e as perdas.
4. Se preferir, **mande os números e os prints para o Claude** que ele preenche o `.tex` para você.
