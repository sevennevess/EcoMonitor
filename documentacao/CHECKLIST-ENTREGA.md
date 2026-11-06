# Checklist de entrega (prazo: 23:55, Moodle)

Autor: **Vitor Neves Garcia — 252003613** (trabalho individual).

## Situação

| Item | Situação |
|---|---|
| Código executável (servidor, painel, simuladores, scripts `.bat`) | **Pronto e testado** |
| Experimentos A–E (dados reais em `experimentos/resultados/`) | **Pronto** (rodados em 05/11/2026 nesta máquina) |
| Gráficos (`documentacao/relatorio/figuras/`) | **Pronto** |
| Relatório IEEE (`documentacao/relatorio/relatorio.tex`) | **Pronto, com nome e matrícula. Faltam só os itens em vermelho das capturas do Wireshark**, a versão do Wireshark e a URL do repositório |
| Capturas Wireshark | **Fazer** (`CAPTURA-1.bat` a `CAPTURA-3.bat` + roteiro em `ROTEIRO-WIRESHARK.md`, ~30 min) |
| Vídeo (≤ 10 min) | **Gravar** (roteiro em `ROTEIRO-VIDEO.md`) |
| Repositório GitHub | **Subir** (pasta inteira; o `.gitignore` já exclui `target/`, banco e IDE) |

## Gerar o PDF do relatório (Overleaf, 2 minutos)

1. overleaf.com → *New Project* → *Upload Project* → envie `documentacao/relatorio/relatorio-overleaf.zip`.
2. Clique em *Recompile*.
3. Depois das capturas: envie `captura1.png`, `captura2.png` e `captura3.png` para a pasta `figuras/` do projeto no
   Overleaf (*Upload*) e clique em *Recompile*: os quadros vermelhos viram as imagens sozinhos.
4. Procure **PREENCHER** (`Ctrl+F` no editor) e troque cada `\pend{...}` pelo valor anotado.
5. Confira: **no máximo 8 páginas** antes da Bibliografia/Anexos. *Menu > Download PDF*.

## Entregáveis

- [ ] **Relatório PDF**, formato IEEEtran, **até 8 páginas** (sem anexos), sem capa. Primeira página: título com o nº do laboratório (Projeto 1), nome e matrícula, 3–4 *keywords*, *abstract* de até 5 linhas.
      Seções: Introdução · Fundamentação Teórica · Ambiente Experimental e Análise de Resultados · Conclusões (5 a 10 linhas) · Bibliografia (3 a 10 fontes, citadas no texto).
      Obs.: o enunciado do projeto fala em 12 páginas, mas o guia de formato da professora diz 8 — use **8**.
- [ ] **Repositório** com README, dependências, comandos de execução, dados de teste não sensíveis (CSVs de `experimentos/resultados/`, capturas `.pcapng`)
- [ ] **Vídeo** ≤ 10 min (link testado em janela anônima)
- [x] **Tabela de contribuição individual** (anexo do relatório: trabalho individual, 100%) e referências
- [x] **Declaração de uso de bibliotecas, ferramentas e IA** (no relatório e no README) — revise o texto

## Requisitos obrigatórios (conferência final)

- [x] Cliente Web + servidor acessível pela rede · [x] ≥ 2 clientes simultâneos identificados independentemente
- [x] HTTP em ≥ 1 operação · [x] Canal em tempo real (WebSocket) com justificativa · [x] Protocolo próprio documentado (`PROTOCOLO.md`)
- [x] Persistência + histórico · [x] Alertas automáticos · [x] Autenticação + validação de entradas
- [x] Logs com timestamp, desconexões, mensagens inválidas, indisponibilidade · [x] RTT, throughput e tempo de resposta com metodologia
- [x] Demonstração executável com instruções · [x] Experimentos A–E com tabelas/gráficos (unidades e condições)
- [ ] Wireshark: ≥ 3 capturas comentadas e correlacionadas · [x] Diagrama de arquitetura, pontos de falha, medidas de proteção de dados

## Declaração de uso de IA

O código-fonte, os roteiros, os experimentos automatizados e o texto do relatório foram elaborados com o apoio do assistente
de IA **Claude (Anthropic)**. As medições vêm de execuções reais dos programas do repositório (`experimentos/resultados/`).
O autor revisou, testou e compreendeu o sistema. (Revise e ajuste o texto para refletir exatamente o que você fez.)
