package br.edu.unb.ecomonitor.protocol;

public record RespostaRegistro(String versao, String tipoMensagem, String codigo,
                               String mensagem, String token) {
    public static RespostaRegistro de(String codigo, String mensagem, String token) {
        return new RespostaRegistro(Protocolo.VERSAO, "CONFIRMAR_RECEBIMENTO", codigo, mensagem, token);
    }
}
