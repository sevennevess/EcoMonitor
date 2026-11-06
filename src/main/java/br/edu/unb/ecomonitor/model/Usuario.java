package br.edu.unb.ecomonitor.model;

public record Usuario(String login, String senhaHash, String papel) {}
