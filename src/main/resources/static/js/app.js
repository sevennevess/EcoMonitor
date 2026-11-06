// ===== Configuração =====
const TIPOS = {
  TEMPERATURA: { nome: "Temperatura", unidade: "°C" },
  UMIDADE: { nome: "Umidade", unidade: "%" },
  QUALIDADE_AR: { nome: "Qualidade do ar", unidade: "índice simulado" }
};
const PALETA = ["#2d6a4f", "#1d4e89", "#e76f51", "#8d6a9f", "#e9c46a", "#2a9d8f"];
const MAX_PONTOS = 60; // pontos mantidos em cada linha do gráfico

// ===== Estado da página =====
const sensores = new Map();    // id do sensor -> dados vindos da API
const graficos = {};           // tipo de medição -> gráfico (Chart.js)
const coresSensor = new Map(); // id do sensor -> cor da linha
let limites = {};              // tipo -> { operador, limite }
let jaConectou = false;
let carregandoSensores = false;

// ===== Utilidades =====
// Escapa texto antes de colocar em innerHTML (evita injetar HTML vindo do servidor)
function esc(texto) {
  const mapa = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };
  return String(texto ?? "").replace(/[&<>"']/g, c => mapa[c]);
}

function fmt(ts) {
  return ts ? ts.replace("T", " ") : "—";
}

function hora(ts) {
  return ts && ts.includes("T") ? ts.split("T")[1] : (ts ?? "");
}

function corDoSensor(id) {
  if (!coresSensor.has(id)) {
    coresSensor.set(id, PALETA[coresSensor.size % PALETA.length]);
  }
  return coresSensor.get(id);
}

// ===== Sessão (login) =====
const CHAVE_SESSAO = "ecomonitor.sessao";
const sessao = JSON.parse(sessionStorage.getItem(CHAVE_SESSAO) ?? "null");

function sair() {
  sessionStorage.removeItem(CHAVE_SESSAO);
  location.href = "login.html";
}

// Todas as chamadas HTTP passam por aqui e levam o token do usuário
async function api(caminho, opcoes = {}) {
  const resposta = await fetch(caminho, {
    ...opcoes,
    headers: {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + (sessao?.token ?? "")
    }
  });
  if (resposta.status === 401) {
    sair(); // sessão inexistente ou expirada
    throw new Error("Sessão expirada");
  }
  const corpo = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    throw new Error(corpo?.mensagem ?? `HTTP ${resposta.status}`);
  }
  return corpo;
}

function violou(tipo, valor) {
  const c = limites[tipo];
  if (!c) return false;
  return c.operador === "MAIOR" ? valor > c.limite : valor < c.limite;
}

// ===== Cartões e gráficos =====
function montarCartoesEGraficos() {
  const areaCartoes = document.getElementById("cartoes");
  const areaGraficos = document.getElementById("graficos");

  for (const [tipo, cfg] of Object.entries(TIPOS)) {
    areaCartoes.insertAdjacentHTML("beforeend", `
      <article class="cartao" id="cartao-${tipo}">
        <h3>${cfg.nome}</h3>
        <p class="valor"><span class="num">--</span> <small>${cfg.unidade}</small></p>
        <p class="detalhe">aguardando dados...</p>
      </article>`);

    areaGraficos.insertAdjacentHTML("beforeend", `
      <article class="cartao">
        <h3>${cfg.nome} (${cfg.unidade})</h3>
        <div class="area-grafico"><canvas id="grafico-${tipo}"></canvas></div>
      </article>`);

    graficos[tipo] = new Chart(document.getElementById(`grafico-${tipo}`), {
      type: "line",
      data: { datasets: [] },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        scales: {
          x: {
            type: "linear",
            ticks: {
              maxTicksLimit: 6,
              callback: v => new Date(v).toLocaleTimeString("pt-BR")
            }
          }
        },
        plugins: { legend: { position: "bottom" } }
      }
    });
  }
}

function atualizarCartao(tipo, sensorId, timestamp, valor) {
  const cartao = document.getElementById(`cartao-${tipo}`);
  if (!cartao) return;
  cartao.querySelector(".num").textContent = valor.toFixed(1);
  cartao.querySelector(".detalhe").textContent = `${sensorId} · ${hora(timestamp)}`;
  cartao.classList.toggle("alerta", violou(tipo, valor));
}

function adicionarPonto(tipo, sensorId, timestamp, valor) {
  const grafico = graficos[tipo];
  const x = Date.parse(timestamp);
  if (!grafico || Number.isNaN(x)) return;

  let serie = grafico.data.datasets.find(d => d.label === sensorId);
  if (!serie) {
    const cor = corDoSensor(sensorId);
    serie = {
      label: sensorId, data: [], borderColor: cor, backgroundColor: cor,
      borderWidth: 2, pointRadius: 0, tension: 0.3
    };
    grafico.data.datasets.push(serie);
  }
  serie.data.push({ x: x, y: valor });
  if (serie.data.length > MAX_PONTOS) serie.data.shift();
  grafico.update("none");
}

// ===== Sensores =====
function renderizarSensores() {
  const corpo = document.querySelector("#tabela-sensores tbody");
  const lista = [...sensores.values()].sort((a, b) => a.id.localeCompare(b.id));

  corpo.innerHTML = lista.length ? lista.map(s => `
    <tr>
      <td>${esc(s.id)}</td>
      <td>${esc(s.nome)}</td>
      <td>${esc(s.tipos.split(",").map(t => TIPOS[t]?.nome ?? t).join(", "))}</td>
      <td><span class="status status-${esc(s.status)}">${esc(s.status)}</span></td>
      <td>${esc(fmt(s.ultimaComunicacao))}</td>
    </tr>`).join("")
    : `<tr><td colspan="5">Nenhum sensor cadastrado</td></tr>`;

  // Atualiza o filtro de sensor do histórico mantendo a seleção atual
  const seletor = document.getElementById("f-sensor");
  const atual = seletor.value;
  seletor.innerHTML = `<option value="">Todos</option>` +
    lista.map(s => `<option value="${esc(s.id)}">${esc(s.id)}</option>`).join("");
  seletor.value = atual;
}

async function carregarSensores() {
  if (carregandoSensores) return;
  carregandoSensores = true;
  try {
    const lista = await api("/api/sensores");
    sensores.clear();
    lista.forEach(s => sensores.set(s.id, s));
    renderizarSensores();
  } finally {
    carregandoSensores = false;
  }
}

// ===== Alertas =====
function adicionarAlerta(a, destacar) {
  const lista = document.getElementById("lista-alertas");
  const item = document.createElement("li");
  if (destacar) item.className = "novo";
  item.innerHTML = `<strong>${esc(a.sensorId)}</strong> — ${esc(a.mensagem)}` +
    `<time>${esc(fmt(a.timestamp ?? a.dataHora))}</time>`;
  lista.prepend(item);
  while (lista.children.length > 30) lista.lastChild.remove();
  document.getElementById("sem-alertas").hidden = true;
}

async function carregarAlertas() {
  const dados = await api("/api/alertas?limite=20");
  document.getElementById("lista-alertas").innerHTML = "";
  document.getElementById("sem-alertas").hidden = dados.length > 0;
  dados.reverse().forEach(a => adicionarAlerta(a, false)); // do mais antigo ao mais novo
}

// ===== Dados iniciais dos gráficos =====
async function carregarGraficos() {
  for (const tipo of Object.keys(TIPOS)) {
    const lista = await api(`/api/medicoes?tipoMedicao=${tipo}&limite=${MAX_PONTOS}`);
    lista.reverse().forEach(m => adicionarPonto(tipo, m.sensorId, m.dataHora, m.valor));
    const ultima = lista[lista.length - 1];
    if (ultima) atualizarCartao(tipo, ultima.sensorId, ultima.dataHora, ultima.valor);
  }
}

// ===== Histórico =====
async function consultarHistorico(evento) {
  evento.preventDefault();
  const msg = document.getElementById("msg-historico");
  const corpo = document.querySelector("#tabela-historico tbody");

  const p = new URLSearchParams();
  const sensor = document.getElementById("f-sensor").value;
  const tipo = document.getElementById("f-tipo").value;
  const inicio = document.getElementById("f-inicio").value;
  const fim = document.getElementById("f-fim").value;
  if (sensor) p.set("sensorId", sensor);
  if (tipo) p.set("tipoMedicao", tipo);
  if (inicio) p.set("dataInicial", inicio + ":00");
  if (fim) p.set("dataFinal", fim + ":59");
  p.set("limite", document.getElementById("f-limite").value || "100");

  try {
    const dados = await api(`/api/medicoes?${p}`);
    corpo.innerHTML = dados.length ? dados.map(m => `
      <tr>
        <td>${esc(m.sensorId)}</td>
        <td>${esc(TIPOS[m.tipoMedicao]?.nome ?? m.tipoMedicao)}</td>
        <td>${esc(m.valor)}</td>
        <td>${esc(m.unidade)}</td>
        <td>${esc(fmt(m.dataHora))}</td>
      </tr>`).join("")
      : `<tr><td colspan="5">Nenhuma medição encontrada</td></tr>`;
    msg.textContent = `${dados.length} medições`;
  } catch (erro) {
    msg.textContent = "Erro: " + erro.message;
  }
}

// ===== Limites de alerta =====
function renderizarLimites() {
  const corpo = document.querySelector("#tabela-limites tbody");
  corpo.innerHTML = Object.entries(limites).map(([tipo, c]) => `
    <tr>
      <td>${esc(TIPOS[tipo]?.nome ?? tipo)}</td>
      <td>
        <select id="op-${tipo}">
          <option value="MAIOR" ${c.operador === "MAIOR" ? "selected" : ""}>maior que</option>
          <option value="MENOR" ${c.operador === "MENOR" ? "selected" : ""}>menor que</option>
        </select>
      </td>
      <td><input type="number" step="any" id="lim-${tipo}" value="${c.limite}"></td>
      <td><button type="button" data-tipo="${tipo}">Salvar</button></td>
    </tr>`).join("");
  corpo.querySelectorAll("button").forEach(b =>
    b.addEventListener("click", () => salvarLimite(b.dataset.tipo)));
}

async function carregarLimites() {
  const lista = await api("/api/configuracoes");
  limites = {};
  lista.forEach(c => { limites[c.tipoMedicao] = { operador: c.operador, limite: c.limite }; });
  renderizarLimites();
}

async function salvarLimite(tipo) {
  const msg = document.getElementById("msg-limites");
  const mensagem = {
    versao: "1.0",
    tipoMensagem: "CONFIGURAR_LIMITE",
    tipoMedicao: tipo,
    operador: document.getElementById(`op-${tipo}`).value,
    limite: parseFloat(document.getElementById(`lim-${tipo}`).value)
  };
  try {
    const resposta = await api("/api/configuracoes", { method: "PUT", body: JSON.stringify(mensagem) });
    msg.textContent = resposta.mensagem;
    await carregarLimites();
  } catch (erro) {
    msg.textContent = "Erro: " + erro.message;
  }
}

// ===== WebSocket (tempo real) =====
function marcarWs(conectado) {
  const selo = document.getElementById("status-ws");
  selo.textContent = conectado ? "tempo real: conectado" : "desconectado";
  selo.className = "selo " + (conectado ? "selo-on" : "selo-off");
}

function tratarEvento(ev) {
  switch (ev.tipoEvento) {
    case "NOVA_MEDICAO": {
      atualizarCartao(ev.tipoMedicao, ev.sensorId, ev.timestamp, ev.valor);
      adicionarPonto(ev.tipoMedicao, ev.sensorId, ev.timestamp, ev.valor);
      const sensor = sensores.get(ev.sensorId);
      if (sensor) {
        sensor.ultimaComunicacao = ev.timestamp;
        sensor.status = "ATIVO";
        renderizarSensores();
      } else {
        carregarSensores(); // sensor novo, ainda desconhecido pela página
      }
      break;
    }
    case "NOVO_ALERTA":
      adicionarAlerta(ev, true);
      break;
    case "SENSOR_CONECTADO":
    case "SENSOR_DESCONECTADO":
      carregarSensores();
      break;
  }
}

function conectarWebSocket() {
  const protocolo = location.protocol === "https:" ? "wss" : "ws";
  const ws = new WebSocket(`${protocolo}://${location.host}/ws?token=${encodeURIComponent(sessao.token)}`);

  ws.onopen = () => {
    marcarWs(true);
    if (jaConectou) { // reconexão: recupera o que aconteceu durante a queda
      carregarSensores();
      carregarAlertas();
    }
    jaConectou = true;
  };
  ws.onclose = () => {
    marcarWs(false);
    setTimeout(async () => {
      try {
        await api("/api/configuracoes"); // se a sessão expirou, api() leva ao login
      } catch (erro) { /* servidor fora do ar: tenta conectar mesmo assim */ }
      conectarWebSocket();
    }, 3000);
  };
  ws.onerror = () => ws.close();
  ws.onmessage = (e) => {
    try {
      tratarEvento(JSON.parse(e.data));
    } catch (erro) {
      console.error("Evento inválido recebido", erro);
    }
  };
}

// ===== Início =====
async function iniciar() {
  if (!sessao) { sair(); return; }
  document.getElementById("usuario").textContent = `${sessao.usuario} (${sessao.papel})`;
  document.getElementById("btn-sair").addEventListener("click", sair);
  montarCartoesEGraficos();
  document.getElementById("form-historico").addEventListener("submit", consultarHistorico);
  try {
    await carregarLimites();   // primeiro: os cartões precisam dos limites
    await carregarSensores();
    await carregarGraficos();
    await carregarAlertas();
  } catch (erro) {
    console.error("Falha ao carregar dados iniciais", erro);
  }
  conectarWebSocket();
}

iniciar();
