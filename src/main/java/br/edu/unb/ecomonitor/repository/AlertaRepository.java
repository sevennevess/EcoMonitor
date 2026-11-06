package br.edu.unb.ecomonitor.repository;

import br.edu.unb.ecomonitor.model.Alerta;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AlertaRepository {
    private final JdbcClient jdbc;

    public AlertaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static Alerta mapear(ResultSet rs, int linha) throws SQLException {
        return new Alerta(rs.getLong("id"), rs.getString("sensor_id"), rs.getString("tipo_medicao"),
                rs.getDouble("valor"), rs.getDouble("limite"), rs.getString("operador"),
                rs.getString("mensagem"), rs.getString("data_hora"));
    }

    public void inserir(String sensorId, String tipo, double valor, double limite,
                        String operador, String mensagem, String dataHora) {
        jdbc.sql("INSERT INTO alertas (sensor_id, tipo_medicao, valor, limite, operador, mensagem, data_hora) "
                        + "VALUES (:s, :t, :v, :l, :o, :m, :d)")
                .param("s", sensorId).param("t", tipo).param("v", valor).param("l", limite)
                .param("o", operador).param("m", mensagem).param("d", dataHora).update();
    }

    public List<Alerta> listar(String sensorId, int limite) {
        StringBuilder sql = new StringBuilder("SELECT * FROM alertas WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (sensorId != null) { sql.append(" AND sensor_id = :sensorId"); params.put("sensorId", sensorId); }
        sql.append(" ORDER BY id DESC LIMIT :limite");
        params.put("limite", limite);
        return jdbc.sql(sql.toString()).params(params).query(AlertaRepository::mapear).list();
    }
}
