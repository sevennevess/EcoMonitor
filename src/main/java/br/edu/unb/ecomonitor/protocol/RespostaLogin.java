package br.edu.unb.ecomonitor.protocol;

public record RespostaLogin(String versao, String tipoMensagem, String codigo, String mensagem,
                            String usuario, String papel, String token) {
    public static RespostaLogin ok(String usuario, String papel, String token) {
        return new RespostaLogin(Protocolo.VERSAO, "LOGIN_OK", "OK", "Autenticado", usuario, papel, token);
    }
}
