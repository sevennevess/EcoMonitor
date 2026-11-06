package br.edu.unb.ecomonitor.model;

public record Medicao(long id, String sensorId, String tipoMedicao,
                      double valor, String unidade, String dataHora) {}
