package br.edu.unb.ecomonitor.model;

public record Alerta(long id, String sensorId, String tipoMedicao, double valor,
                     double limite, String operador, String mensagem, String dataHora) {}
