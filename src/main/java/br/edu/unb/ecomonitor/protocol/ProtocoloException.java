package br.edu.unb.ecomonitor.protocol;

import org.springframework.http.HttpStatus;

public class ProtocoloException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;

    public ProtocoloException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
}
