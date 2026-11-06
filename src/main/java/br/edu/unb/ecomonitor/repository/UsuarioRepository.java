package br.edu.unb.ecomonitor.repository;

import br.edu.unb.ecomonitor.model.Usuario;
import br.edu.unb.ecomonitor.protocol.Protocolo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepository {
    private final JdbcClient jdbc;

    public UsuarioRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Usuario> buscar(String login) {
        return jdbc.sql("SELECT * FROM usuarios WHERE login = :l")
                .param("l", login)
                .query((rs, linha) -> new Usuario(rs.getString("login"),
                        rs.getString("senha_hash"), rs.getString("papel")))
                .optional();
    }

    public void inserir(String login, String senhaHash, String papel) {
        jdbc.sql("INSERT INTO usuarios (login, senha_hash, papel, criado_em) VALUES (:l, :h, :p, :c)")
                .param("l", login).param("h", senhaHash).param("p", papel)
                .param("c", Protocolo.agora()).update();
    }
}
