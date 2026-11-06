package br.edu.unb.ecomonitor.protocol;

public final class Eventos {
    private Eventos() {}

    public record NovaMedicao(String tipoEvento, String sensorId, String tipoMedicao,
                              double valor, String unidade, String timestamp) {
        public static NovaMedicao criar(String sensorId, String tipoMedicao, double valor,
                                        String unidade, String timestamp) {
            return new NovaMedicao("NOVA_MEDICAO", sensorId, tipoMedicao, valor, unidade, timestamp);
        }
    }

    public record NovoAlerta(String tipoEvento, String sensorId, String tipoMedicao,
                             double valor, double limite, String mensagem, String timestamp) {
        public static NovoAlerta criar(String sensorId, String tipoMedicao, double valor,
                                       double limite, String mensagem, String timestamp) {
            return new NovoAlerta("NOVO_ALERTA", sensorId, tipoMedicao, valor, limite, mensagem, timestamp);
        }
    }

    public record StatusSensor(String tipoEvento, String sensorId, String status, String timestamp) {
        public static StatusSensor criar(String sensorId, String status, String timestamp) {
            String tipo = "ATIVO".equals(status) ? "SENSOR_CONECTADO" : "SENSOR_DESCONECTADO";
            return new StatusSensor(tipo, sensorId, status, timestamp);
        }
    }
}
