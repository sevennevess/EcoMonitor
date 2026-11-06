"""
Gera os graficos do relatorio a partir de experimentos/resultados/*.csv.
Uso (na raiz do projeto):   pip install matplotlib   e depois   python experimentos/graficos.py
Saida: documentacao/relatorio/figuras/*.png (300 dpi, largura de uma coluna IEEE).
"""
import csv
import os
import sys

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.ticker import FuncFormatter
except ImportError:
    sys.exit("matplotlib nao instalado. Rode: pip install matplotlib")

RES = os.path.join("experimentos", "resultados")
FIG = os.path.join("documentacao", "relatorio", "figuras")

# Cor fixa por condicao (segue a entidade em todos os graficos)
COR = {"local": "#2a78d6", "atraso50": "#eb6834", "proxy0": "#1baf7a",
       "local_cargamax": "#6250d6", "perda5": "#e87ba4", "jitter": "#008300"}
NOME = {"local": "local", "atraso50": "atraso 25+25 ms", "proxy0": "emulador 0 ms",
        "local_cargamax": "local, carga máx.", "perda5": "perda 5%/sentido", "jitter": "atraso 25±15 ms"}
TINTA, TINTA2, GRADE = "#0b0b0b", "#52514e", "#e4e3df"

plt.rcParams.update({
    "font.size": 7.5, "font.family": "DejaVu Sans", "axes.edgecolor": TINTA2, "axes.labelcolor": TINTA,
    "xtick.color": TINTA2, "ytick.color": TINTA2, "axes.grid": True, "grid.color": GRADE,
    "grid.linewidth": 0.6, "axes.axisbelow": True, "axes.spines.top": False, "axes.spines.right": False,
    "legend.frameon": False, "legend.fontsize": 6.8, "lines.linewidth": 1.6, "savefig.dpi": 300,
})
numero = FuncFormatter(lambda v, _: f"{v:g}".replace(".", ","))


def ler(nome):
    caminho = os.path.join(RES, nome)
    if not os.path.exists(caminho):
        return []
    with open(caminho, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def salvar(fig, nome):
    os.makedirs(FIG, exist_ok=True)
    fig.savefig(os.path.join(FIG, nome), bbox_inches="tight")
    plt.close(fig)
    print("figura:", os.path.join(FIG, nome))


def resumo_de(linhas, exp, cond, param):
    for l in linhas:
        if l["experimento"] == exp and l["condicao"] == cond and l["parametro"] == param:
            return l
    return None


# ---------------------------------------------------------------- A: RTT (distribuicao, escala log)
def grafico_rtt():
    conds = [c for c in ("local", "proxy0", "atraso50") if os.path.exists(os.path.join(RES, f"rtt_{c}.csv"))]
    if not conds:
        return
    dados, posicoes, cores = [], [], []
    for g, op in enumerate(("PING", "MEDICAO")):
        for i, c in enumerate(conds):
            amostras = [float(l["rtt_ms"]) for l in ler(f"rtt_{c}.csv") if l["operacao"] == op]
            dados.append(amostras)
            posicoes.append(g * (len(conds) + 1) + i)
            cores.append(COR[c])
    fig, ax = plt.subplots(figsize=(3.5, 2.3))
    bp = ax.boxplot(dados, positions=posicoes, widths=0.7, patch_artist=True, showfliers=True,
                    medianprops={"color": TINTA, "linewidth": 1.2},
                    flierprops={"marker": ".", "markersize": 2.5, "markeredgecolor": TINTA2},
                    whiskerprops={"color": TINTA2}, capprops={"color": TINTA2})
    for caixa, cor in zip(bp["boxes"], cores):
        caixa.set_facecolor(cor)
        caixa.set_edgecolor(cor)
        caixa.set_alpha(0.85)
    ax.set_yscale("log")
    ax.yaxis.set_major_formatter(numero)
    centro = (len(conds) - 1) / 2
    ax.set_xticks([centro, len(conds) + 1 + centro])
    ax.set_xticklabels(["PING (sem banco)", "MEDICAO (grava no SQLite)"])
    ax.set_ylabel("RTT da aplicação (ms, escala log)")
    for c in conds:
        ax.plot([], [], "s", color=COR[c], label=NOME[c])
    ax.legend(loc="upper left", ncol=1)
    salvar(fig, "fig_rtt.png")


# ---------------------------------------------------------------- B: vazao util x tamanho
def grafico_vazao():
    linhas = ler("resumo.csv")
    tamanhos = [1024, 10240, 102400, 1048576]
    fig, ax = plt.subplots(figsize=(3.5, 2.3))
    tem = False
    for c in ("local", "atraso50"):
        for direcao, estilo, marca in (("UPLOAD", "-", "o"), ("DOWNLOAD", "--", "s")):
            xs, ys, es = [], [], []
            for b in tamanhos:
                l = resumo_de(linhas, "THROUGHPUT", c, f"{direcao}_{b}B_vazao_mbps")
                if l and int(l["n"]) > 0:
                    xs.append(b / 1024)
                    ys.append(float(l["media"]))
                    es.append(float(l["desvio_padrao"]))
            if xs:
                tem = True
                ax.errorbar(xs, ys, yerr=es, color=COR[c], linestyle=estilo, marker=marca, markersize=4,
                            capsize=2, elinewidth=0.8)
    if not tem:
        plt.close(fig)
        return
    # Condicao rotulada direto na linha; a legenda so explica o estilo (upload x download)
    ax.text(1.6, 40, "local", color=TINTA, fontsize=7, ha="left")
    ax.text(3.2, 0.2, "atraso 25+25 ms", color=TINTA, fontsize=7, ha="left")
    ax.plot([], [], "-o", color=TINTA2, markersize=4, label="upload")
    ax.plot([], [], "--s", color=TINTA2, markersize=4, label="download")
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.xaxis.set_major_formatter(numero)
    ax.yaxis.set_major_formatter(numero)
    ax.set_xticks([1, 10, 100, 1024])
    ax.set_xticklabels(["1 KB", "10 KB", "100 KB", "1 MB"])
    ax.set_xlabel("Tamanho do corpo transferido")
    ax.set_ylabel("Vazão útil (Mbit/s, escala log)")
    ax.legend(loc="lower right")
    salvar(fig, "fig_vazao.png")


# ---------------------------------------------------------------- C: concorrencia (dois paineis, sem eixo duplo)
def grafico_concorrencia():
    linhas = ler("concorrencia_resumo.csv")
    if not linhas:
        return
    fig, (a1, a2) = plt.subplots(2, 1, figsize=(3.5, 3.5), sharex=True)
    rotulos = {"local": "local (envio a cada 100 ms)", "local_cargamax": "local, carga máxima (sem pausa)",
               "atraso50": "atraso 25+25 ms (a cada 100 ms)"}
    for c in ("atraso50", "local", "local_cargamax"):
        sel = sorted([l for l in linhas if l["condicao"] == c], key=lambda l: int(l["clientes"]))
        if not sel:
            continue
        x = [int(l["clientes"]) for l in sel]
        a1.plot(x, [float(l["lat_mediana_ms"]) for l in sel], "-o", color=COR[c], markersize=4, label=rotulos[c])
        a1.plot(x, [float(l["lat_p95_ms"]) for l in sel], ":", marker="^", color=COR[c], markersize=3.5)
        a2.plot(x, [float(l["taxa_msg_s"]) for l in sel], "-o", color=COR[c], markersize=4)
        abaixo = c == "atraso50"  # evita sobrepor os numeros das duas series de baixo
        for xi, l in zip(x, sel):
            a2.annotate(f"{float(l['taxa_msg_s']):.0f}", (xi, float(l["taxa_msg_s"])), textcoords="offset points",
                        xytext=(0, -9 if abaixo else 4), ha="center", fontsize=5.8, color=TINTA)
    for a in (a1, a2):
        a.set_yscale("log")
        a.yaxis.set_major_formatter(numero)
    a1.set_ylabel("Latência MEDICAO (ms, log)\n— mediana   ··· p95")
    a2.set_ylabel("Taxa agregada\n(mensagens/s, log)")
    a2.set_ylim(2, 10000)
    handles, labels = a1.get_legend_handles_labels()
    a2.legend(handles, labels, loc="center left", bbox_to_anchor=(0.0, 0.56), fontsize=6)
    a2.set_xlabel("Clientes simultâneos")
    a2.set_xticks([1, 2, 5])
    fig.align_ylabels()
    salvar(fig, "fig_concorrencia.png")


# ---------------------------------------------------------------- D: TCP x UDP
def grafico_transporte():
    linhas = ler("transporte_resumo.csv")
    if not linhas:
        return
    ordem = [c for c in ("local", "proxy0", "atraso50", "perda5", "jitter") if any(l["condicao"] == c for l in linhas)]
    fig, ax = plt.subplots(figsize=(3.5, 2.4))
    largura = 0.38
    for k, (proto, cor, hachura) in enumerate((("TCP", "#2a78d6", None), ("UDP", "#eb6834", "////"))):
        for i, c in enumerate(ordem):
            l = next((l for l in linhas if l["protocolo"] == proto and l["condicao"] == c), None)
            x = i + (k - 0.5) * largura
            if l is None:  # o emulador nao provoca perda/jitter em TCP (ver relatorio)
                ax.text(x, 0.06, "não\nmedido", ha="center", va="bottom", fontsize=5.2, color=TINTA2)
                continue
            med, p95 = float(l["rtt_mediana_ms"]), float(l["rtt_p95_ms"])
            ax.bar(x, med, largura * 0.92, color=cor, hatch=hachura, edgecolor="white", linewidth=0.4,
                   label=proto if i == 0 else None)
            ax.errorbar(x, med, yerr=[[0], [p95 - med]], color=TINTA, capsize=2, elinewidth=0.8)
            notas = []
            if float(l["perda_pct"]) > 0:
                notas.append(f"{float(l['perda_pct']):.1f}% perdidas".replace(".", ","))
            if int(l["fora_de_ordem"]) > 0:
                notas.append(f"{l['fora_de_ordem']} fora\nde ordem")
            if notas:
                ax.annotate("\n".join(notas), (x, p95), textcoords="offset points", xytext=(0, 3),
                            ha="center", va="bottom", fontsize=5.6, color=TINTA)
    ax.set_yscale("log")
    ax.set_ylim(0.04, 1000)
    ax.yaxis.set_major_formatter(numero)
    ax.set_ylabel("RTT do eco (ms, escala log)\nbarra: mediana; traço: p95")
    ax.legend(loc="upper left", ncol=1)
    ax.set_xticks(range(len(ordem)))
    ax.set_xticklabels([NOME[c].replace(" ", "\n", 1) for c in ordem], fontsize=6.3)
    salvar(fig, "fig_transporte.png")


if __name__ == "__main__":
    if not os.path.isdir(RES):
        sys.exit("Rode a partir da raiz do projeto (pasta com experimentos/).")
    grafico_rtt()
    grafico_vazao()
    grafico_concorrencia()
    grafico_transporte()
