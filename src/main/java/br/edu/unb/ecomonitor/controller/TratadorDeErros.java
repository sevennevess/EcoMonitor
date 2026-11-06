package br.edu.unb.ecomonitor.controller;

import br.edu.unb.ecomonitor.protocol.ProtocoloException;
import br.edu.unb.ecomonitor.protocol.Resposta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {
    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(ProtocoloException.class)
    public ResponseEntity<Resposta> protocolo(ProtocoloException e) {
        log.warn("Mensagem rejeitada: {} - {}", e.getCodigo(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(Resposta.erro(e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Resposta> jsonInvalido(HttpMessageNotReadableException e) {
        log.warn("JSON malformado recebido");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Resposta.erro("JSON_INVALIDO", "Corpo da requisicao nao e um JSON valido"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Resposta> banco(DataAccessException e) {
        log.error("Falha no banco de dados", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Resposta.erro("ERRO_BANCO", "Falha ao acessar o banco de dados"));
    }
}
