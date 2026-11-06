package br.edu.unb.ecomonitor.model;

public record Sensor(String id, String nome, String tipos, String status,
                     String dataCadastro, String ultimaComunicacao) {}
