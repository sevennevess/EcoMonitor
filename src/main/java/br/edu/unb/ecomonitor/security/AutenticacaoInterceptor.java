package br.edu.unb.ecomonitor.security;

import br.edu.unb.ecomonitor.protocol.ProtocoloException;
import br.edu.unb.ecomonitor.service.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {
    private final AutenticacaoService autenticacao;

    public AutenticacaoInterceptor(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String metodo = req.getMethod();
        String caminho = req.getRequestURI();

        if ("OPTIONS".equals(metodo) || caminho.equals("/api/login")) return true;
        // Rotas usadas apenas nos experimentos de rede (sem dados sensiveis)
        if (caminho.equals("/api/ping") || caminho.startsWith("/api/teste/")) return true;
        if ("POST".equals(metodo) && (caminho.equals("/api/sensores") || caminho.equals("/api/medicoes"))) {
            return true; // autenticados por chave de registro / token do sensor
        }

        String cabecalho = req.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            throw new ProtocoloException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO",
                    "Envie o cabecalho Authorization: Bearer <token>");
        }
        Sessao sessao = autenticacao.validar(cabecalho.substring(7));
        if (sessao == null) {
            throw new ProtocoloException(HttpStatus.UNAUTHORIZED, "SESSAO_INVALIDA",
                    "Sessao invalida ou expirada");
        }
        if ("PUT".equals(metodo) && caminho.equals("/api/configuracoes") && !"ADMIN".equals(sessao.papel())) {
            throw new ProtocoloException(HttpStatus.FORBIDDEN, "SEM_PERMISSAO",
                    "Apenas ADMIN pode alterar limites");
        }
        return true;
    }
}
