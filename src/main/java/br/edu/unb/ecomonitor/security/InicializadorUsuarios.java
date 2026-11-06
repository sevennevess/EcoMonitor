package br.edu.unb.ecomonitor.security;

import br.edu.unb.ecomonitor.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InicializadorUsuarios implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InicializadorUsuarios.class);
    private final UsuarioRepository usuarios;
    private final String senhaAdmin;
    private final String senhaVisitante;

    public InicializadorUsuarios(UsuarioRepository usuarios,
                                 @Value("${ecomonitor.usuarios.admin-senha:admin123}") String senhaAdmin,
                                 @Value("${ecomonitor.usuarios.visitante-senha:visitante123}") String senhaVisitante) {
        this.usuarios = usuarios;
        this.senhaAdmin = senhaAdmin;
        this.senhaVisitante = senhaVisitante;
    }

    @Override
    public void run(ApplicationArguments args) {
        criarSeAusente("admin", senhaAdmin, "ADMIN");
        criarSeAusente("visitante", senhaVisitante, "LEITURA");
    }

    private void criarSeAusente(String login, String senha, String papel) {
        if (usuarios.buscar(login).isEmpty()) {
            usuarios.inserir(login, Senhas.hashSenha(senha), papel);
            log.info("Usuario inicial criado: {} ({})", login, papel);
        }
    }
}
