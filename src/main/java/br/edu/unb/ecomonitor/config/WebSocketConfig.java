package br.edu.unb.ecomonitor.config;

import br.edu.unb.ecomonitor.security.AutenticacaoWebSocketInterceptor;
import br.edu.unb.ecomonitor.websocket.EventosWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final EventosWebSocketHandler handler;
    private final AutenticacaoWebSocketInterceptor autenticacao;

    public WebSocketConfig(EventosWebSocketHandler handler, AutenticacaoWebSocketInterceptor autenticacao) {
        this.handler = handler;
        this.autenticacao = autenticacao;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // "*" so em desenvolvimento; em producao restringir as origens permitidas
        registry.addHandler(handler, "/ws")
                .addInterceptors(autenticacao)
                .setAllowedOrigins("*");
    }
}
