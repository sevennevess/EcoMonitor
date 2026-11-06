# Roteiro do vídeo de demonstração (máx. 10 min, apresentação individual)

**Como gravar a tela com sua voz:** no Windows 11, a Ferramenta de Captura grava a tela inteira com áudio
(`Win + Shift + R`, selecione a tela toda e ligue o microfone). Alternativa: OBS Studio.
Evite a Xbox Game Bar (`Win + Alt + R`): ela grava só uma janela, e você vai alternar entre várias.
Suba o vídeo no YouTube como **não listado** (ou no Drive com "qualquer pessoa com o link") e teste o link numa janela anônima.

## Antes de gravar (~5 min)

- [ ] Feche o servidor se estiver aberto e dê duplo clique em `LIMPAR-BANCO.bat` (o vídeo começa com o banco vazio).
- [ ] Dê duplo clique em `COMPILAR.bat` uma vez (assim a compilação não aparece no vídeo).
- [ ] Deixe abertos: o Explorador na pasta `C:\Users\vitor\Desktop\Agora\ecomonitor`, o IntelliJ (ou o Bloco de Notas) com
      `documentacao\PROTOCOLO.md`, os gráficos de `documentacao\relatorio\figuras\` e o Wireshark com a `captura1-http.pcapng` aberta.
- [ ] Zoom do navegador em 125% (`Ctrl` + `+`), notificações desligadas.
- [ ] **Não rode os experimentos ao vivo** (levam ~8 min): mostre só os gráficos e os resultados já coletados.
- [ ] Ensaie uma vez com cronômetro.

## Roteiro (o que clicar e o que falar)

| Tempo | Ação na tela | O que falar (resumo) |
|---|---|---|
| 0:00–0:40 | README ou primeira página do relatório | "Sou Vitor Neves Garcia, matrícula 252003613. Este é o EcoMonitor, um sistema de monitoramento ambiental com **sensores simulados**." Diga o que vai mostrar |
| 0:40–1:40 | Fig. 1 do relatório (diagrama) | Sensores → HTTP → servidor Spring Boot → banco SQLite → WebSocket → painéis. Arquitetura centralizada: o servidor é um ponto único de falha |
| 1:40–2:40 | `documentacao\PROTOCOLO.md` | O protocolo próprio em JSON: `REGISTRAR_SENSOR`, `MEDICAO`, `ERRO` com códigos, e os eventos do WebSocket. HTTP para comandos e consultas (pergunta/resposta); WebSocket para o servidor **empurrar** eventos sem o navegador ficar perguntando |
| 2:40–3:20 | Duplo clique em `1-SERVIDOR.bat` | Mostre o log com data e hora: tabelas criadas, usuários `admin` e `visitante` criados, servidor na porta 8080 |
| 3:20–4:20 | `3-PAINEL.bat` → entre como `admin`; abra uma janela anônima (`Ctrl+Shift+N`) em `http://127.0.0.1:8080` → entre como `visitante` | **Dois clientes ao mesmo tempo, cada um com seu usuário e papel** (ADMIN e LEITURA). No log do servidor: "Conexao WebSocket autorizada: usuario=admin" e "usuario=visitante" |
| 4:20–5:30 | Duplo clique em `2-SENSORES.bat` (abre 3 janelas) | Os dois painéis se atualizam sozinhos: cartões, gráficos, sensores `ATIVO`. As janelas dos sensores mostram `HTTP 201` e o tempo de cada envio |
| 5:30–6:30 | No painel do admin: limite de Temperatura para 27 → **Salvar**. No do visitante: tentar salvar | O cartão fica vermelho e o alerta aparece na hora nos dois painéis. O visitante recebe "Apenas ADMIN pode alterar limites". Volte o limite para 40 |
| 6:30–7:15 | Duplo clique em `4-MENSAGENS-INVALIDAS.bat`; depois, no painel, use o **Histórico** filtrando por um sensor | 13 mensagens erradas, cada uma recusada com seu código (`UNIDADE_INCOMPATIVEL`, `TOKEN_AUSENTE`, `JSON_INVALIDO`...) e aviso no log; nada é gravado. O histórico fica salvo e pode ser filtrado |
| 7:15–7:50 | Duplo clique em `5-SENSOR-INSTAVEL.bat`; depois feche a janela do servidor e abra o `1-SERVIDOR.bat` de novo | O SENSOR_004 fica `INATIVO` após 15 s sem mandar nada e volta a `ATIVO`. Com o servidor fora, o painel mostra "desconectado" e os sensores avisam "Connection refused"; ao voltar, tudo reconecta e os dados continuam lá (é preciso entrar de novo no painel) |
| 7:50–9:00 | Gráficos `fig_rtt`, `fig_vazao`, `fig_concorrencia`, `fig_transporte` | Um resultado de cada experimento (Seção III do relatório). Ex.: "RTT local abaixo de 1 ms; com 50 ms de atraso, 51 ms"; "o UDP perdeu 8,6% e reordenou 22% sem avisar a aplicação; o TCP garante entrega e ordem" |
| 9:00–9:40 | Wireshark com a captura 1 (e a 2, se der tempo) | Handshake SYN / SYN-ACK / ACK, `POST /api/medicoes` com o JSON legível (sem criptografia), `101 Switching Protocols` do WebSocket |
| 9:40–10:00 | — | Conclusão: o que funcionou, limitações (sem TLS, servidor único) e agradecimento. **Termine antes de 10:00** |

## Itens do enunciado que o vídeo precisa mostrar

- [ ] Interface Web com servidor acessível pela rede · [ ] ≥ 2 clientes simultâneos identificados · [ ] HTTP (consulta/cadastro/configuração)
- [ ] Canal em tempo real (WebSocket) com justificativa · [ ] Protocolo próprio documentado · [ ] Persistência e histórico consultável
- [ ] Alertas automáticos · [ ] Autenticação e validação de entradas · [ ] Logs com horário, desconexão, mensagem inválida, servidor fora do ar
- [ ] RTT/vazão/concorrência/TCP×UDP/condições de rede · [ ] Wireshark

Extra (se tiver outro computador ou celular na mesma rede Wi-Fi): abra `http://<IPv4 do seu PC>:8080` nele.
O `1-SERVIDOR.bat` mostra o IPv4 (use o `192.168.1.x`, não o `192.168.56.1`). Isso mostra que o servidor é acessível pela rede.
