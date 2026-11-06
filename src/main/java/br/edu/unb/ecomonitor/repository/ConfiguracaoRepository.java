package br.edu.unb.ecomonitor.repository;

import br.edu.unb.ecomonitor.model.Configuracao;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ConfiguracaoRepository {
    private final JdbcClient jdbc;

    public ConfiguracaoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static Configuracao mapear(ResultSet rs, int linha) throws SQLException {
        return new Configuracao(rs.getString("tipo_medicao"), rs.getString("operador"),
                rs.getDouble("limite"));
    }

    public List<Configuracao> listar() {
        return jdbc.sql("SELECT * FROM configuracoes ORDER BY tipo_medicao")
                .query(ConfiguracaoRepository::mapear).list();
    }

    public Optional<Configuracao> buscar(String tipo) {
        return jdbc.sql("SELECT * FROM configuracoes WHERE tipo_medicao = :t")
                .param("t", tipo).query(ConfiguracaoRepository::mapear).optional();
    }

    public void salvar(String tipo, String operador, double limite) {
        jdbc.sql("INSERT INTO configuracoes (tipo_medicao, operador, limite) VALUES (:t, :o, :l) "
                        + "ON CONFLICT(tipo_medicao) DO UPDATE SET operador = :o, limite = :l")
                .param("t", tipo).param("o", operador).param("l", limite).update();
    }
}
