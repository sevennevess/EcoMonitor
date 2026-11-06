package br.edu.unb.ecomonitor.protocol;

import java.util.List;

public record MensagemRegistro(String versao, String tipoMensagem, String sensorId,
                               String nome, List<String> tipos) {}
