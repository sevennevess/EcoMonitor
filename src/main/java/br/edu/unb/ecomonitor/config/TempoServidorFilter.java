package br.edu.unb.ecomonitor.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Mede o tempo de processamento da aplicacao (filtro + controller + banco) e o devolve no
 * cabecalho X-Tempo-Servidor-Us (microssegundos). Serve para separar, no Experimento A,
 * o RTT medido pelo cliente do tempo gasto dentro do servidor.
 * O corpo da resposta e retido ate o fim para que o cabecalho ainda possa ser escrito.
 */
@Component
public class TempoServidorFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        ContentCachingResponseWrapper embrulho = new ContentCachingResponseWrapper(response);
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, embrulho);
        } finally {
            long micros = (System.nanoTime() - inicio) / 1000;
            embrulho.setHeader("X-Tempo-Servidor-Us", Long.toString(micros));
            embrulho.copyBodyToResponse();
        }
    }
}
