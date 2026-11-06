package br.edu.unb.ecomonitor.service;

import br.edu.unb.ecomonitor.model.Alerta;
import br.edu.unb.ecomonitor.model.Configuracao;
import br.edu.unb.ecomonitor.model.TipoMedicao;
import br.edu.unb.ecomonitor.protocol.*;
import br.edu.unb.ecomonitor.repository.AlertaRepository;
import br.edu.unb.ecomonitor.repository.ConfiguracaoRepository;
import br.edu.unb.ecomonitor.websocket.EventosWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertaService {
    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);
    private final ConfiguracaoRepository configuracoes;
    private final AlertaRepository alertas;
    private final EventosWebSocketHandler eventos;
    private final Map<String, Configuracao> cache = new ConcurrentHashMap<>();
    private final Set<String> emAlerta = ConcurrentHashMap.newKeySet();

    public AlertaService(ConfiguracaoRepository configuracoes, AlertaRepository alertas,
                         EventosWebSocketHandler eventos) {
        this.configuracoes = configuracoes;
        this.alertas = alertas;
        this.eventos = eventos;
    }

    private Configuracao configuracaoDe(String tipo) {
        Configuracao c = cache.get(tipo);
        if (c == null) {
            c = configuracoes.buscar(tipo).orElse(null);
            if (c != null) cache.put(tipo, c);
        }
        return c;
    }

    // Gera alerta so na transicao "normal -> violando", para nao repetir a cada medicao
    public void avaliar(String sensorId, TipoMedicao tipo, double valor) {
        Configuracao c = configuracaoDe(tipo.name());
        if (c == null) return;
        boolean acima = "MAIOR".equals(c.operador());
        boolean violou = acima ? valor > c.limite() : valor < c.limite();
        String chave = sensorId + ":" + tipo.name();
        if (!violou) {
            emAlerta.remove(chave);
            return;
        }
        if (!emAlerta.add(chave)) return;

        String agora = Protocolo.agora();
        String mensagem = String.format(Locale.US, "%s %s do limite: %.1f (limite %.1f)",
                tipo.name(), acima ? "acima" : "abaixo", valor, c.limite());
        alertas.inserir(sensorId, tipo.name(), valor, c.limite(), c.operador(), mensagem, agora);
        log.warn("ALERTA {}: {}", sensorId, mensagem);
        eventos.publicar(Eventos.NovoAlerta.criar(sensorId, tipo.name(), valor, c.limite(), mensagem, agora));
    }

    public List<Alerta> listarAlertas(String sensorId, int limite) {
        return alertas.listar(sensorId, Math.max(1, Math.min(limite, 500)));
    }

    public List<Configuracao> listarConfiguracoes() {
        return configuracoes.listar();
    }

    public Resposta atualizarConfiguracao(MensagemConfiguracao m) {
        if (m == null || m.versao() == null || m.tipoMensagem() == null || m.tipoMedicao() == null
                || m.operador() == null || m.limite() == null) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "CAMPO_AUSENTE",
                    "Campos obrigatorios: versao, tipoMensagem, tipoMedicao, operador, limite");
        }
        if (!Protocolo.VERSAO.equals(m.versao())) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "VERSAO_NAO_SUPORTADA",
                    "Versao suportada: " + Protocolo.VERSAO);
        }
        if (!"CONFIGURAR_LIMITE".equals(m.tipoMensagem())) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "TIPO_MENSAGEM_INVALIDO",
                    "Esperado CONFIGURAR_LIMITE");
        }
        TipoMedicao tipo;
        try {
            tipo = TipoMedicao.valueOf(m.tipoMedicao());
        } catch (IllegalArgumentException e) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "TIPO_MEDICAO_INVALIDO",
                    "Tipo invalido: " + m.tipoMedicao());
        }
        if (!"MAIOR".equals(m.operador()) && !"MENOR".equals(m.operador())) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "OPERADOR_INVALIDO",
                    "Operador deve ser MAIOR ou MENOR");
        }
        configuracoes.salvar(tipo.name(), m.operador(), m.limite());
        cache.remove(tipo.name());
        emAlerta.removeIf(chave -> chave.endsWith(":" + tipo.name()));
        log.info("Limite atualizado: {} {} {}", tipo.name(), m.operador(), m.limite());
        return Resposta.confirmacao("Limite atualizado: " + tipo.name());
    }
}
