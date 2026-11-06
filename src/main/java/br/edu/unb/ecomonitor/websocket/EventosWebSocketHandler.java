package br.edu.unb.ecomonitor.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventosWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(EventosWebSocketHandler.class);
    private final Set<WebSocketSession> sessoes = ConcurrentHashMap.newKeySet();
    private final JsonMapper json;

    public EventosWebSocketHandler(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sessao) {
        sessoes.add(sessao);
        log.info("Cliente WebSocket conectado: {} (total: {})", sessao.getId(), sessoes.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sessao, CloseStatus status) {
        sessoes.remove(sessao);
        log.info("Cliente WebSocket desconectado: {} (total: {})", sessao.getId(), sessoes.size());
    }

    @Override
    public void handleTransportError(WebSocketSession sessao, Throwable erro) {
        log.warn("Erro de transporte WebSocket {}: {}", sessao.getId(), erro.getMessage());
        sessoes.remove(sessao);
    }

    public void publicar(Object evento) {
        String texto;
        try {
            texto = json.writeValueAsString(evento);
        } catch (JacksonException e) {
            log.error("Falha ao converter evento para JSON", e);
            return;
        }
        TextMessage mensagem = new TextMessage(texto);
        for (WebSocketSession sessao : sessoes) {
            try {
                synchronized (sessao) {
                    if (sessao.isOpen()) {
                        sessao.sendMessage(mensagem);
                    }
                }
            } catch (IOException e) {
                log.warn("Falha ao enviar para {}: {}", sessao.getId(), e.getMessage());
                sessoes.remove(sessao);
            }
        }
    }
}
