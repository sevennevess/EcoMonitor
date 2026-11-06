import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Envia ao servidor mensagens propositalmente erradas e mostra a resposta ERRO de cada uma.
 * Serve para demonstrar a validacao do protocolo (video e relatorio). Nada disso e gravado no banco.
 * Uso: java simulador\TesteMensagensInvalidas.java [servidor]
 */
public class TesteMensagensInvalidas {
    static final HttpClient CLIENTE = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3)).build();
    static String servidor;
    static int casos, conformes;

    public static void main(String[] args) throws Exception {
        servidor = args.length > 0 ? args[0] : "http://127.0.0.1:8080";
        String chave = System.getenv().getOrDefault("ECOMONITOR_CHAVE_REGISTRO", "chave-de-teste-ecomonitor");
        String agora = LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Sensor valido usado como base (so mede TEMPERATURA)
        HttpResponse<String> reg = post("/api/sensores",
                "{\"versao\":\"1.0\",\"tipoMensagem\":\"REGISTRAR_SENSOR\",\"sensorId\":\"SENSOR_TESTE\","
                        + "\"nome\":\"Teste de validacao\",\"tipos\":[\"TEMPERATURA\"]}",
                Map.of("X-Chave-Registro", chave));
        Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(reg.body());
        if (!m.find()) {
            System.out.println("Nao foi possivel registrar o sensor de teste: HTTP " + reg.statusCode() + " " + reg.body());
            return;
        }
        Map<String, String> token = Map.of("X-Sensor-Token", m.group(1));
        String base = "\"versao\":\"1.0\",\"tipoMensagem\":\"MEDICAO\",\"sensorId\":\"SENSOR_TESTE\"";

        System.out.println("Servidor: " + servidor + "  (sensor de teste SENSOR_TESTE registrado)\n");

        caso("JSON malformado", 400, "/api/medicoes", "{isto nao e json", token);
        caso("Campo obrigatorio ausente (sem valor)", 400, "/api/medicoes",
                "{" + base + ",\"tipoMedicao\":\"TEMPERATURA\",\"unidade\":\"C\",\"timestamp\":\"" + agora + "\"}", token);
        caso("Versao do protocolo nao suportada", 400, "/api/medicoes",
                "{\"versao\":\"9.9\",\"tipoMensagem\":\"MEDICAO\",\"sensorId\":\"SENSOR_TESTE\",\"tipoMedicao\":\"TEMPERATURA\","
                        + "\"valor\":25,\"unidade\":\"C\",\"timestamp\":\"" + agora + "\"}", token);
        caso("Unidade incompativel (F em vez de C)", 400, "/api/medicoes",
                "{" + base + ",\"tipoMedicao\":\"TEMPERATURA\",\"valor\":25,\"unidade\":\"F\",\"timestamp\":\"" + agora + "\"}", token);
        caso("Valor fora da faixa (temperatura 500)", 422, "/api/medicoes",
                "{" + base + ",\"tipoMedicao\":\"TEMPERATURA\",\"valor\":500,\"unidade\":\"C\",\"timestamp\":\"" + agora + "\"}", token);
        caso("Tipo que o sensor nao mede (UMIDADE)", 422, "/api/medicoes",
                "{" + base + ",\"tipoMedicao\":\"UMIDADE\",\"valor\":50,\"unidade\":\"%\",\"timestamp\":\"" + agora + "\"}", token);
        caso("Timestamp invalido", 400, "/api/medicoes",
                "{" + base + ",\"tipoMedicao\":\"TEMPERATURA\",\"valor\":25,\"unidade\":\"C\",\"timestamp\":\"ontem\"}", token);
        caso("Medicao sem token do sensor", 401, "/api/medicoes",
                "{" + base + ",\"tipoMedicao\":\"TEMPERATURA\",\"valor\":25,\"unidade\":\"C\",\"timestamp\":\"" + agora + "\"}", Map.of());
        caso("Medicao com token errado", 401, "/api/medicoes",
                "{" + base + ",\"tipoMedicao\":\"TEMPERATURA\",\"valor\":25,\"unidade\":\"C\",\"timestamp\":\"" + agora + "\"}",
                Map.of("X-Sensor-Token", "token-falso"));
        caso("Sensor nao cadastrado", 404, "/api/medicoes",
                "{\"versao\":\"1.0\",\"tipoMensagem\":\"MEDICAO\",\"sensorId\":\"NAO_EXISTE\",\"tipoMedicao\":\"TEMPERATURA\","
                        + "\"valor\":25,\"unidade\":\"C\",\"timestamp\":\"" + agora + "\"}", token);
        caso("Registro com chave de provisionamento errada", 401, "/api/sensores",
                "{\"versao\":\"1.0\",\"tipoMensagem\":\"REGISTRAR_SENSOR\",\"sensorId\":\"INTRUSO\",\"nome\":\"x\",\"tipos\":[\"TEMPERATURA\"]}",
                Map.of("X-Chave-Registro", "chave-errada"));
        caso("Registro com sensorId invalido", 400, "/api/sensores",
                "{\"versao\":\"1.0\",\"tipoMensagem\":\"REGISTRAR_SENSOR\",\"sensorId\":\"a b\",\"nome\":\"x\",\"tipos\":[\"TEMPERATURA\"]}",
                Map.of("X-Chave-Registro", chave));
        caso("Login com senha errada", 401, "/api/login",
                "{\"versao\":\"1.0\",\"tipoMensagem\":\"LOGIN\",\"usuario\":\"admin\",\"senha\":\"errada\"}", Map.of());

        System.out.printf("%d de %d casos devolveram o codigo HTTP esperado. Veja os avisos no log do servidor.%n",
                conformes, casos);
    }

    static void caso(String descricao, int esperado, String rota, String corpo, Map<String, String> cabecalhos)
            throws Exception {
        HttpResponse<String> r = post(rota, corpo, cabecalhos);
        casos++;
        boolean ok = r.statusCode() == esperado;
        if (ok) conformes++;
        System.out.printf("[%s] %s%n      POST %s -> HTTP %d (esperado %d)%n      %s%n%n",
                ok ? "OK " : "!! ", descricao, rota, r.statusCode(), esperado, r.body());
    }

    static HttpResponse<String> post(String rota, String json, Map<String, String> cabecalhos) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(servidor + rota))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        cabecalhos.forEach(b::header);
        return CLIENTE.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
