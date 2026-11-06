package br.edu.unb.ecomonitor.controller;

import br.edu.unb.ecomonitor.model.Medicao;
import br.edu.unb.ecomonitor.protocol.MensagemMedicao;
import br.edu.unb.ecomonitor.protocol.Resposta;
import br.edu.unb.ecomonitor.service.MedicaoService;
import br.edu.unb.ecomonitor.service.SensorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicoes")
public class MedicaoController {
    private final MedicaoService servico;
    private final SensorService sensores;

    public MedicaoController(MedicaoService servico, SensorService sensores) {
        this.servico = servico;
        this.sensores = sensores;
    }

    @PostMapping
    public ResponseEntity<Resposta> receber(
            @RequestBody MensagemMedicao mensagem,
            @RequestHeader(name = "X-Sensor-Token", required = false) String token) {
        sensores.autenticarSensor(mensagem == null ? null : mensagem.sensorId(), token);
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.registrar(mensagem));
    }

    @GetMapping
    public List<Medicao> consultar(
            @RequestParam(name = "sensorId", required = false) String sensorId,
            @RequestParam(name = "tipoMedicao", required = false) String tipoMedicao,
            @RequestParam(name = "dataInicial", required = false) String dataInicial,
            @RequestParam(name = "dataFinal", required = false) String dataFinal,
            @RequestParam(name = "limite", defaultValue = "100") int limite) {
        return servico.consultar(sensorId, tipoMedicao, dataInicial, dataFinal, limite);
    }
}
