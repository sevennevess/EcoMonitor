package br.edu.unb.ecomonitor.security;

import java.time.Instant;

public record Sessao(String usuario, String papel, Instant expiraEm) {}
