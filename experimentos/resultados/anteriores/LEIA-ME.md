# Execuções anteriores (arquivadas automaticamente)

A execução **final**, usada no relatório, é a que está em `experimentos/resultados/` (05/11/2026 19:36).
As pastas abaixo guardam execuções completas anteriores, cada uma interrompida por um problema de metodologia
que foi corrigido antes da seguinte. São citadas na seção "Problemas e ajustes" do relatório.

| Pasta | O que mudou depois | Uso no relatório |
|---|---|---|
| `2026-11-05_19-19-22` | SQLite no modo de journal padrão (DELETE): cada gravação esperava um `fsync`. MEDICAO levava ~15 ms e picos de até 2 s; carga máxima ~46 msg/s. **Ajuste:** `journal_mode=WAL` + `synchronous=NORMAL`. | Números "antes" do ajuste do banco |
| `2026-11-05_19-26-51` | Sem aquecimento do servidor: a condição `local` (medida primeiro) pegou o JIT frio (tempo no servidor da MEDICAO 1,28 ms x 0,26 ms depois). **Ajuste:** fase `aquecer` de 15 s antes de medir e 30 requisições de aquecimento por série. | Justificativa do aquecimento |
| `2026-11-05_19-36-57` | O emulador dependia da resolução do timer do Windows: com timer de 15,6 ms, o atraso configurado de 50 ms virou ~63,7 ms. **Ajuste:** espera híbrida (dorme até 20 ms antes e completa ativamente). As condições `local` desta pasta não passam pelo emulador e servem como **repetição**. | Repetição de `local` (variabilidade da carga máxima) |
