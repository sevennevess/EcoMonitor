package br.edu.unb.ecomonitor.protocol;

public record MensagemConfiguracao(String versao, String tipoMensagem, String tipoMedicao,
                                   String operador, Double limite) {}
