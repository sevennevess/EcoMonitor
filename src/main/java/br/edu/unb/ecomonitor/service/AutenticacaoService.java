package br.edu.unb.ecomonitor.service;

import br.edu.unb.ecomonitor.model.Usuario;
import br.edu.unb.ecomonitor.protocol.*;
import br.edu.unb.ecomonitor.repository.UsuarioRepository;
import br.edu.unb.ecomonitor.security.Senhas;
import br.edu.unb.ecomonitor.security.Sessao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutenticacaoService {
    private static final Logger log = LoggerFactory.getLogger(AutenticacaoService.class);
    private final UsuarioRepository usuarios;
    private final long validadeHoras;
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();

    public AutenticacaoService(UsuarioRepository usuarios,
                               @Value("${ecomonitor.sessao-validade-horas:8}") long validadeHoras) {
        this.usuarios = usuarios;
        this.validadeHoras = validadeHoras;
    }

    public RespostaLogin login(MensagemLogin m) {
        if (m == null || m.versao() == null || m.tipoMensagem() == null
                || m.usuario() == null || m.senha() == null) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "CAMPO_AUSENTE",
                    "Campos obrigatorios: versao, tipoMensagem, usuario, senha");
        }
        if (!Protocolo.VERSAO.equals(m.versao())) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "VERSAO_NAO_SUPORTADA",
                    "Versao suportada: " + Protocolo.VERSAO);
        }
        if (!"LOGIN".equals(m.tipoMensagem())) {
            throw new ProtocoloException(HttpStatus.BAD_REQUEST, "TIPO_MENSAGEM_INVALIDO", "Esperado LOGIN");
        }
        Usuario u = usuarios.buscar(m.usuario()).orElse(null);
        if (u == null || !Senhas.verificarSenha(m.senha(), u.senhaHash())) {
            log.warn("Falha de login para o usuario informado");
            throw new ProtocoloException(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS",
                    "Usuario ou senha invalidos");
        }
        String token = Senhas.novoToken();
        sessoes.put(token, new Sessao(u.login(), u.papel(), Instant.now().plusSeconds(validadeHoras * 3600)));
        log.info("Login realizado: usuario={} papel={}", u.login(), u.papel());
        return RespostaLogin.ok(u.login(), u.papel(), token);
    }

    public Sessao validar(String token) {
        Sessao sessao = sessoes.get(token);
        if (sessao == null) return null;
        if (sessao.expiraEm().isBefore(Instant.now())) {
            sessoes.remove(token);
            return null;
        }
        return sessao;
    }
}
