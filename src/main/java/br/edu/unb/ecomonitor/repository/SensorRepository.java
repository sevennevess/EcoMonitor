package br.edu.unb.ecomonitor.repository;

import br.edu.unb.ecomonitor.model.Sensor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class SensorRepository {
    private final JdbcClient jdbc;

    public SensorRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static Sensor mapear(ResultSet rs, int linha) throws SQLException {
        return new Sensor(rs.getString("id"), rs.getString("nome"), rs.getString("tipos"),
                rs.getString("status"), rs.getString("data_cadastro"),
                rs.getString("ultima_comunicacao"));
    }

    public List<Sensor> listar() {
        return jdbc.sql("SELECT * FROM sensores ORDER BY id")
                .query(SensorRepository::mapear).list();
    }

    public Optional<Sensor> buscarPorId(String id) {
        return jdbc.sql("SELECT * FROM sensores WHERE id = :id")
                .param("id", id).query(SensorRepository::mapear).optional();
    }

    public Optional<String> buscarTokenHash(String id) {
        return jdbc.sql("SELECT token_hash FROM sensores WHERE id = :id")
                .param("id", id).query(String.class).optional();
    }

    public void inserir(Sensor s, String tokenHash) {
        jdbc.sql("INSERT INTO sensores (id, nome, tipos, status, data_cadastro, token_hash) "
                        + "VALUES (:id, :nome, :tipos, :status, :dataCadastro, :tokenHash)")
                .param("id", s.id()).param("nome", s.nome()).param("tipos", s.tipos())
                .param("status", s.status()).param("dataCadastro", s.dataCadastro())
                .param("tokenHash", tokenHash)
                .update();
    }

    public void atualizarRegistro(String id, String nome, String tipos, String tokenHash) {
        jdbc.sql("UPDATE sensores SET nome = :nome, tipos = :tipos, token_hash = :h WHERE id = :id")
                .param("nome", nome).param("tipos", tipos).param("h", tokenHash)
                .param("id", id).update();
    }

    public void registrarComunicacao(String id, String instante) {
        jdbc.sql("UPDATE sensores SET status = 'ATIVO', ultima_comunicacao = :t WHERE id = :id")
                .param("t", instante).param("id", id).update();
    }

    public void marcarInativo(String id) {
        jdbc.sql("UPDATE sensores SET status = 'INATIVO' WHERE id = :id")
                .param("id", id).update();
    }
}
