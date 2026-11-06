package br.edu.unb.ecomonitor.protocol;

public record MensagemLogin(String versao, String tipoMensagem, String usuario, String senha) {}
