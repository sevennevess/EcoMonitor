package br.edu.unb.ecomonitor.controller;

import br.edu.unb.ecomonitor.model.Configuracao;
import br.edu.unb.ecomonitor.protocol.MensagemConfiguracao;
import br.edu.unb.ecomonitor.protocol.Resposta;
import br.edu.unb.ecomonitor.service.AlertaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracoes")
public class ConfiguracaoController {
    private final AlertaService servico;

    public ConfiguracaoController(AlertaService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<Configuracao> listar() {
        return servico.listarConfiguracoes();
    }

    @PutMapping
    public Resposta atualizar(@RequestBody MensagemConfiguracao mensagem) {
        return servico.atualizarConfiguracao(mensagem);
    }
}
