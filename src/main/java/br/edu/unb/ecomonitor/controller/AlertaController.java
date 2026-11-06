package br.edu.unb.ecomonitor.controller;

import br.edu.unb.ecomonitor.model.Alerta;
import br.edu.unb.ecomonitor.service.AlertaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {
    private final AlertaService servico;

    public AlertaController(AlertaService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<Alerta> listar(
            @RequestParam(name = "sensorId", required = false) String sensorId,
            @RequestParam(name = "limite", defaultValue = "50") int limite) {
        return servico.listarAlertas(sensorId, limite);
    }
}
