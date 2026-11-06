package br.edu.unb.ecomonitor.protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Protocolo {
    public static final String VERSAO = "1.0";

    private Protocolo() {}

    public static String agora() {
        return LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
