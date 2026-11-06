package br.edu.unb.ecomonitor.protocol;

public record MensagemMedicao(String versao, String tipoMensagem, String sensorId,
                              String tipoMedicao, Double valor, String unidade,
                              String timestamp) {}
