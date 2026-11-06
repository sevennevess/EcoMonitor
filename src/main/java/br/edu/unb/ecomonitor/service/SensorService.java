package br.edu.unb.ecomonitor.service;

import br.edu.unb.ecomonitor.model.Sensor;
import br.edu.unb.ecomonitor.model.TipoMedicao;
import br.edu.unb.ecomonitor.protocol.*;
import br.edu.unb.ecomonitor.repository.SensorRepository;
import br.edu.unb.ecomonitor.security.Senhas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SensorService {
    private static final Logger log = LoggerFactory.getLogger(SensorService.class);
    private final SensorRepository repositorio;
    private final String chaveRegistro;

    public SensorService(SensorRepository repositorio,
                         @Value("${ecomonitor.chave-registro:chave-de-teste-ecomonitor}") String chaveRegistro) {
        this.repositorio = repositorio;
        this.chaveRegistro = chaveRegistro;
    }

    public List<Sensor> listar() {
        return repositorio.listar();
    }

    public Sensor buscar(String id) {
        return repositorio.buscarPorId(id).orElseThrow(() ->
                new ProtocoloException(HttpStatus.NOT_FOUND, "SENSOR_DESCONHECIDO",
                        "Sensor nao encontrado: " + id));
    }

    public RespostaRegistro cadastrar(MensagemRegistro m, String chave) {
        if (chave == null || !Senhas.iguais(chave, chaveRegistro)) {
            throw new ProtocoloException(HttpStatus.UNAUTHORIZED, "CHAVE_REGISTRO_INVALIDA",
                    "Chave de registro ausente ou invalida");
        }
        if (m == null || m.versao() == null || m.tipoMensagem() == null || m.sensorId() == null
                || m.nome() == null || m.tipos() == null || m.tipos().isEmpty()) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "CAMPO_AUSENTE",
                    "Campos obrigatorios: versao, tipoMensagem, sensorId, nome, tipos");
        }
        if (!Protocolo.VERSAO.equals(m.versao())) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "VERSAO_NAO_SUPORTADA",
                    "Versao suportada: " + Protocolo.VERSAO);
        }
        if (!"REGISTRAR_SENSOR".equals(m.tipoMensagem())) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "TIPO_MENSAGEM_INVALIDO",
                    "Esperado REGISTRAR_SENSOR");
        }
        if (!m.sensorId().matches("[A-Za-z0-9_]{3,30}")) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "ID_INVALIDO",
                    "sensorId deve ter 3 a 30 caracteres: letras, numeros ou _");
        }
        for (String t : m.tipos()) {
            try {
                TipoMedicao.valueOf(t);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ProtocoloException(HttpStatus.BAD_REQUEST, "TIPO_MEDICAO_INVALIDO",
                        "Tipo invalido: " + t);
            }
        }

        String token = Senhas.novoToken();
        String hash = Senhas.sha256(token);
        String tipos = String.join(",", m.tipos());

        if (repositorio.buscarPorId(m.sensorId()).isPresent()) {
            repositorio.atualizarRegistro(m.sensorId(), m.nome(), tipos, hash);
            log.info("Sensor re-registrado (token renovado): {}", m.sensorId());
            return RespostaRegistro.de("ATUALIZADO", "Sensor re-registrado: " + m.sensorId(), token);
        }
        repositorio.inserir(new Sensor(m.sensorId(), m.nome(), tipos, "INATIVO",
                Protocolo.agora(), null), hash);
        log.info("Sensor cadastrado: {}", m.sensorId());
        return RespostaRegistro.de("CRIADO", "Sensor cadastrado: " + m.sensorId(), token);
    }

    // Verifica o token enviado pelo sensor no cabecalho X-Sensor-Token
    public void autenticarSensor(String sensorId, String token) {
        if (sensorId == null) return; // campos ausentes sao tratados pelo MedicaoService
        String esperado = repositorio.buscarTokenHash(sensorId).orElseThrow(() ->
                new ProtocoloException(HttpStatus.NOT_FOUND, "SENSOR_DESCONHECIDO",
                        "Sensor nao cadastrado: " + sensorId));
        if (token == null || token.isBlank()) {
            throw new ProtocoloException(HttpStatus.UNAUTHORIZED, "TOKEN_AUSENTE",
                    "Envie o cabecalho X-Sensor-Token");
        }
        if (!Senhas.iguais(Senhas.sha256(token), esperado)) {
            throw new ProtocoloException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO",
                    "Token invalido para o sensor " + sensorId);
        }
    }
}
