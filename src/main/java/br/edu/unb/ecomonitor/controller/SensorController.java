package br.edu.unb.ecomonitor.controller;

import br.edu.unb.ecomonitor.model.Sensor;
import br.edu.unb.ecomonitor.protocol.MensagemRegistro;
import br.edu.unb.ecomonitor.protocol.RespostaRegistro;
import br.edu.unb.ecomonitor.service.SensorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensores")
public class SensorController {
    private final SensorService servico;

    public SensorController(SensorService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<Sensor> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public Sensor consultar(@PathVariable("id") String id) {
        return servico.buscar(id);
    }

    @PostMapping
    public ResponseEntity<RespostaRegistro> cadastrar(
            @RequestBody MensagemRegistro mensagem,
            @RequestHeader(name = "X-Chave-Registro", required = false) String chave) {
        RespostaRegistro resposta = servico.cadastrar(mensagem, chave);
        HttpStatus status = "CRIADO".equals(resposta.codigo()) ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(resposta);
    }
}
