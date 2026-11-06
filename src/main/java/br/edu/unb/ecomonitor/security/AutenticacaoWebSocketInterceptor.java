package br.edu.unb.ecomonitor.security;

import br.edu.unb.ecomonitor.service.AutenticacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class AutenticacaoWebSocketInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AutenticacaoWebSocketInterceptor.class);
    private final AutenticacaoService autenticacao;

    public AutenticacaoWebSocketInterceptor(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> atributos) {
        String token = extrairToken(request.getURI().getQuery());
        Sessao sessao = token == null ? null : autenticacao.validar(token);
        if (sessao == null) {
            log.warn("Conexao WebSocket recusada: token ausente ou invalido");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        atributos.put("usuario", sessao.usuario());
        log.info("Conexao WebSocket autorizada: usuario={}", sessao.usuario());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception excecao) {
    }

    private static String extrairToken(String consulta) {
        if (consulta == null) return null;
        for (String par : consulta.split("&")) {
            if (par.startsWith("token=")) return par.substring(6);
        }
        return null;
    }
}
