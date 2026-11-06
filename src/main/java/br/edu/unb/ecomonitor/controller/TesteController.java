package br.edu.unb.ecomonitor.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

/** Rotas auxiliares dos experimentos de rede (RTT e throughput). */
@RestController
@RequestMapping("/api")
public class TesteController {

    // Resposta minima, sem banco de dados: referencia para o RTT
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("pong", true);
    }

    // Upload: o servidor so informa quantos bytes recebeu
    @PostMapping(value = "/teste/carga", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Map<String, Object> receberCarga(@RequestBody byte[] corpo) {
        return Map.of("bytesRecebidos", corpo.length);
    }

    // Download: o servidor devolve N bytes (limitado a 5 MB)
    @GetMapping(value = "/teste/carga", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] enviarCarga(@RequestParam(name = "bytes", defaultValue = "1024") int bytes) {
        byte[] dados = new byte[Math.max(0, Math.min(bytes, 5_000_000))];
        Arrays.fill(dados, (byte) 'x');
        return dados;
    }
}
