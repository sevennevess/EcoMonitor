package br.edu.unb.ecomonitor.service;

import br.edu.unb.ecomonitor.model.Medicao;
import br.edu.unb.ecomonitor.model.Sensor;
import br.edu.unb.ecomonitor.model.TipoMedicao;
import br.edu.unb.ecomonitor.protocol.*;
import br.edu.unb.ecomonitor.repository.MedicaoRepository;
import br.edu.unb.ecomonitor.repository.SensorRepository;
import br.edu.unb.ecomonitor.websocket.EventosWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

@Service
public class MedicaoService {
    private static final Logger log = LoggerFactory.getLogger(MedicaoService.class);
    private final SensorRepository sensores;
    private final MedicaoRepository medicoes;
    private final AlertaService alertas;
    private final EventosWebSocketHandler eventos;

    public MedicaoService(SensorRepository sensores, MedicaoRepository medicoes,
                          AlertaService alertas, EventosWebSocketHandler eventos) {
        this.sensores = sensores;
        this.medicoes = medicoes;
        this.alertas = alertas;
        this.eventos = eventos;
    }

    public Resposta registrar(MensagemMedicao m) {
        if (m == null || m.versao() == null || m.tipoMensagem() == null || m.sensorId() == null
                || m.tipoMedicao() == null || m.valor() == null || m.unidade() == null
                || m.timestamp() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "CAMPO_AUSENTE",
                    "Campos obrigatorios: versao, tipoMensagem, sensorId, tipoMedicao, valor, unidade, timestamp");
        }
        if (!Protocolo.VERSAO.equals(m.versao())) {
            throw erro(HttpStatus.BAD_REQUEST, "VERSAO_NAO_SUPORTADA", "Versao suportada: " + Protocolo.VERSAO);
        }
        if (!"MEDICAO".equals(m.tipoMensagem())) {
            throw erro(HttpStatus.BAD_REQUEST, "TIPO_MENSAGEM_INVALIDO", "Esperado MEDICAO");
        }
        Sensor sensor = sensores.buscarPorId(m.sensorId()).orElseThrow(() ->
                erro(HttpStatus.NOT_FOUND, "SENSOR_DESCONHECIDO", "Sensor nao cadastrado: " + m.sensorId()));

        TipoMedicao tipo;
        try {
            tipo = TipoMedicao.valueOf(m.tipoMedicao());
        } catch (IllegalArgumentException e) {
            throw erro(HttpStatus.BAD_REQUEST, "TIPO_MEDICAO_INVALIDO", "Tipo invalido: " + m.tipoMedicao());
        }
        if (!Arrays.asList(sensor.tipos().split(",")).contains(tipo.name())) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY, "TIPO_NAO_SUPORTADO_PELO_SENSOR",
                    "Este sensor nao mede " + tipo.name());
        }
        if (!tipo.getUnidade().equals(m.unidade())) {
            throw erro(HttpStatus.BAD_REQUEST, "UNIDADE_INCOMPATIVEL",
                    "Unidade esperada para " + tipo.name() + ": " + tipo.getUnidade());
        }
        double valor = m.valor();
        if (valor < tipo.getMinimo() || valor > tipo.getMaximo()) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY, "VALOR_FORA_DA_FAIXA",
                    "Faixa valida: " + tipo.getMinimo() + " a " + tipo.getMaximo());
        }
        try {
            LocalDateTime.parse(m.timestamp());
        } catch (DateTimeParseException e) {
            throw erro(HttpStatus.BAD_REQUEST, "TIMESTAMP_INVALIDO", "Use o formato 2026-11-04T16:30:00");
        }

        String agora = Protocolo.agora();
        medicoes.inserir(m.sensorId(), tipo.name(), valor, m.unidade(), m.timestamp());
        sensores.registrarComunicacao(m.sensorId(), agora);
        log.info("Medicao recebida: {} {} = {} {}", m.sensorId(), tipo.name(), valor, m.unidade());

        if (!"ATIVO".equals(sensor.status())) {
            log.info("Sensor conectado: {}", m.sensorId());
            eventos.publicar(Eventos.StatusSensor.criar(m.sensorId(), "ATIVO", agora));
        }
        eventos.publicar(Eventos.NovaMedicao.criar(m.sensorId(), tipo.name(), valor, m.unidade(), m.timestamp()));
        alertas.avaliar(m.sensorId(), tipo, valor);

        return Resposta.confirmacao("Medicao registrada");
    }

    public List<Medicao> consultar(String sensorId, String tipo, String de, String ate, int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 1000));
        return medicoes.consultar(sensorId, tipo, de, ate, limiteSeguro);
    }

    private ProtocoloException erro(HttpStatus status, String codigo, String mensagem) {
        return new ProtocoloException(status, codigo, mensagem);
    }
}
