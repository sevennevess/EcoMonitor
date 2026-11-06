package br.edu.unb.ecomonitor.controller;

import br.edu.unb.ecomonitor.protocol.MensagemLogin;
import br.edu.unb.ecomonitor.protocol.RespostaLogin;
import br.edu.unb.ecomonitor.service.AutenticacaoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login")
public class AuthController {
    private final AutenticacaoService servico;

    public AuthController(AutenticacaoService servico) {
        this.servico = servico;
    }

    @PostMapping
    public RespostaLogin login(@RequestBody MensagemLogin mensagem) {
        return servico.login(mensagem);
    }
}
