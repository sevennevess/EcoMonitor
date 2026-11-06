package br.edu.unb.ecomonitor.protocol;

public record Resposta(String versao, String tipoMensagem, String codigo, String mensagem) {

    public static Resposta confirmacao(String mensagem) {
        return new Resposta(Protocolo.VERSAO, "CONFIRMAR_RECEBIMENTO", "OK", mensagem);
    }

    public static Resposta erro(String codigo, String mensagem) {
        return new Resposta(Protocolo.VERSAO, "ERRO", codigo, mensagem);
    }
}
