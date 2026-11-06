package br.edu.unb.ecomonitor.service;

import br.edu.unb.ecomonitor.model.Sensor;
import br.edu.unb.ecomonitor.protocol.Eventos;
import br.edu.unb.ecomonitor.protocol.Protocolo;
import br.edu.unb.ecomonitor.repository.SensorRepository;
import br.edu.unb.ecomonitor.websocket.EventosWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
public class MonitorSensores {
    private static final Logger log = LoggerFactory.getLogger(MonitorSensores.class);
    private final SensorRepository sensores;
    private final EventosWebSocketHandler eventos;
    private final long timeoutSegundos;

    public MonitorSensores(SensorRepository sensores, EventosWebSocketHandler eventos,
                           @Value("${ecomonitor.sensor-timeout-segundos:15}") long timeoutSegundos) {
        this.sensores = sensores;
        this.eventos = eventos;
        this.timeoutSegundos = timeoutSegundos;
    }

    @Scheduled(fixedRate = 5000)
    public void verificarSensoresInativos() {
        LocalDateTime limite = LocalDateTime.now().minusSeconds(timeoutSegundos);
        for (Sensor s : sensores.listar()) {
            if (!"ATIVO".equals(s.status()) || s.ultimaComunicacao() == null) continue;
            if (LocalDateTime.parse(s.ultimaComunicacao()).isBefore(limite)) {
                sensores.marcarInativo(s.id());
                log.warn("Sensor desconectado (sem mensagens ha mais de {} s): {}", timeoutSegundos, s.id());
                eventos.publicar(Eventos.StatusSensor.criar(s.id(), "INATIVO", Protocolo.agora()));
            }
        }
    }
}
