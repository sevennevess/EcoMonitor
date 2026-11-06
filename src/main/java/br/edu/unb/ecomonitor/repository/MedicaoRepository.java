package br.edu.unb.ecomonitor.repository;

import br.edu.unb.ecomonitor.model.Medicao;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MedicaoRepository {
    private final JdbcClient jdbc;

    public MedicaoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static Medicao mapear(ResultSet rs, int linha) throws SQLException {
        return new Medicao(rs.getLong("id"), rs.getString("sensor_id"),
                rs.getString("tipo_medicao"), rs.getDouble("valor"),
                rs.getString("unidade"), rs.getString("data_hora"));
    }

    public void inserir(String sensorId, String tipo, double valor, String unidade, String dataHora) {
        jdbc.sql("INSERT INTO medicoes (sensor_id, tipo_medicao, valor, unidade, data_hora) "
                        + "VALUES (:s, :t, :v, :u, :d)")
                .param("s", sensorId).param("t", tipo).param("v", valor)
                .param("u", unidade).param("d", dataHora).update();
    }

    // Filtros opcionais: so entram no SQL se foram informados
    public List<Medicao> consultar(String sensorId, String tipo, String de, String ate, int limite) {
        StringBuilder sql = new StringBuilder("SELECT * FROM medicoes WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (sensorId != null) { sql.append(" AND sensor_id = :sensorId"); params.put("sensorId", sensorId); }
        if (tipo != null) { sql.append(" AND tipo_medicao = :tipo"); params.put("tipo", tipo); }
        if (de != null) { sql.append(" AND data_hora >= :de"); params.put("de", de); }
        if (ate != null) { sql.append(" AND data_hora <= :ate"); params.put("ate", ate); }
        sql.append(" ORDER BY data_hora DESC LIMIT :limite");
        params.put("limite", limite);
        return jdbc.sql(sql.toString()).params(params).query(MedicaoRepository::mapear).list();
    }
}
