# EcoMonitor — passo a passo (sem precisar saber programar)

Tudo fica nesta pasta: **`C:\Users\vitor\Desktop\Agora\ecomonitor\`**

Os arquivos que terminam em `.bat` são "botões": dê **duplo clique** neles no Explorador de Arquivos.
Cada um abre uma **janela preta** (o terminal). Não feche a janela do servidor enquanto estiver usando o sistema.

Se o Windows mostrar "O Windows protegeu o computador", clique em **Mais informações → Executar assim mesmo**.
Se aparecer um aviso do **Firewall** pedindo acesso para o Java, clique em **Permitir**.

---

## 1. Ver o sistema funcionando (para gravar o vídeo)

| Passo | Faça | O que acontece |
|---|---|---|
| 1 | Duplo clique em `LIMPAR-BANCO.bat` | Apaga os dados antigos (começa do zero). Pode pular se já estiver limpo |
| 2 | Duplo clique em `DEMO.bat` | Abre a janela do **servidor**, 3 janelas de **sensores** e o **navegador** com a tela de login. Na primeira vez pode demorar 1 a 3 minutos (ele baixa o que precisa) |
| 3 | No navegador, entre com `admin` / `admin123` | O painel aparece e começa a se atualizar sozinho (selo verde "tempo real: conectado") |
| 4 | Abra uma janela anônima (`Ctrl + Shift + N`), vá em `http://127.0.0.1:8080` e entre com `visitante` / `visitante123` | Segundo cliente ao mesmo tempo, com outro usuário |
| 5 | Para parar: feche as janelas pretas (sensores e servidor) | — |

Outros botões úteis para o vídeo (com o servidor aberto):

| Arquivo | Para que serve |
|---|---|
| `4-MENSAGENS-INVALIDAS.bat` | Manda 13 mensagens erradas e mostra o erro que o servidor devolve para cada uma |
| `5-SENSOR-INSTAVEL.bat` | Um sensor que "some" por 20 s: no painel ele fica `INATIVO` e depois volta a `ATIVO` |
| `1-SERVIDOR.bat`, `2-SENSORES.bat`, `3-PAINEL.bat` | As mesmas partes do `DEMO.bat`, uma de cada vez |

No painel, logado como `admin`, você pode mudar um limite de alerta (ex.: Temperatura para 27 → **Salvar**):
o cartão fica vermelho e um alerta aparece na lista. Logado como `visitante`, tentar salvar dá erro de permissão.

O roteiro do vídeo, minuto a minuto, está em `documentacao\ROTEIRO-VIDEO.md`.

## 2. Pelo IntelliJ (opcional)

1. Abra o IntelliJ → **File > Open** → escolha a pasta `C:\Users\vitor\Desktop\Agora\ecomonitor` → **Trust Project**.
2. Espere a barra de baixo terminar de carregar (ele lê o `pom.xml`).
3. No topo, ao lado do botão verde ▶, já aparece **EcoMonitor - Servidor**. Clique em ▶: o servidor sobe na aba *Run*.
4. Os sensores continuam pelo `2-SENSORES.bat`.

## 3. Experimentos (já foram feitos — só se quiser repetir)

Com o servidor aberto e o painel e os sensores **fechados**, duplo clique em `6-EXPERIMENTOS.bat` (~8 minutos).
Os resultados ficam em `experimentos\resultados\` e os gráficos em `documentacao\relatorio\figuras\`.
Os números que estão no relatório já vieram daí.

## 4. As 3 capturas do Wireshark

Siga `documentacao\ROTEIRO-WIRESHARK.md`. Resumo:

| Captura | Precisa do servidor? | Botão | Salvar como |
|---|---|---|---|
| 1 — HTTP e TCP | sim (`1-SERVIDOR.bat`) | `CAPTURA-1.bat` | `experimentos\capturas\captura1-http.pcapng` + print `documentacao\relatorio\figuras\captura1.png` |
| 2 — WebSocket | sim | `CAPTURA-2.bat` | `experimentos\capturas\captura2-websocket.pcapng` + print `...\figuras\captura2.png` |
| 3 — perdas UDP | não | `CAPTURA-3.bat` | `experimentos\capturas\captura3-udp.pcapng` + print `...\figuras\captura3.png` |

## 5. Gerar o PDF do relatório

1. Entre em **https://www.overleaf.com** (crie uma conta grátis se não tiver).
2. **New Project → Upload Project** → escolha `C:\Users\vitor\Desktop\Agora\ecomonitor\documentacao\relatorio\relatorio-overleaf.zip`.
3. Clique em **Recompile**. O PDF aparece à direita.
4. Depois das capturas: envie `captura1.png`, `captura2.png` e `captura3.png` para a pasta `figuras` do projeto no
   Overleaf (botão *Upload*) e troque os textos vermelhos **[PREENCHER]** pelos números anotados.
5. **Menu → Download PDF**. Confira que o texto principal não passa de 8 páginas (os Anexos, depois da Bibliografia, não contam).

## Onde está cada coisa

| O quê | Caminho |
|---|---|
| Relatório (LaTeX) | `documentacao\relatorio\relatorio.tex` |
| Pacote para o Overleaf | `documentacao\relatorio\relatorio-overleaf.zip` |
| Gráficos e figuras | `documentacao\relatorio\figuras\` |
| Protocolo documentado | `documentacao\PROTOCOLO.md` |
| Roteiro do vídeo | `documentacao\ROTEIRO-VIDEO.md` |
| Roteiro do Wireshark | `documentacao\ROTEIRO-WIRESHARK.md` |
| Checklist de entrega | `documentacao\CHECKLIST-ENTREGA.md` |
| Resultados dos experimentos | `experimentos\resultados\` (`resumo.csv` e outros) |
| Código do servidor | `src\main\java\br\edu\unb\ecomonitor\` |
| Painel Web | `src\main\resources\static\` |
| Projeto inteiro para enviar | `C:\Users\vitor\Desktop\Agora\ecomonitor-entrega.zip` |

## Se algo der errado

| Problema | Solução |
|---|---|
| "Nenhum JDK encontrado" | No IntelliJ: *File > Project Structure > SDK > Download JDK* (versão 17 ou maior). Depois tente de novo |
| A janela do servidor fecha sozinha ou mostra `Port 8080 was already in use` | Já existe um servidor aberto. Feche todas as janelas pretas e tente de novo |
| O painel mostra "desconectado" | O servidor está fechado: abra `1-SERVIDOR.bat` |
| O painel volta para a tela de login | Normal depois de reiniciar o servidor: entre de novo |
| `LIMPAR-BANCO.bat` diz que não conseguiu apagar | Feche a janela do servidor antes |
