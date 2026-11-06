import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SimuladorSensor {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: java SimuladorSensor.java <sensorId> <tipos> [intervaloMs] [servidor] [ciclosAteDesconectar] [totalCiclos]");
            System.out.println("Ex.: java simulador\\SimuladorSensor.java SENSOR_001 TEMPERATURA,UMIDADE 2000");
            return;
        }
        String sensorId = args[0];
        List<String> tipos = Arrays.asList(args[1].split(","));
        long intervalo = args.length > 2 ? Long.parseLong(args[2]) : 2000;
        String servidor = args.length > 3 ? args[3] : "http://localhost:8080";
        int ciclosAteDesconectar = args.length > 4 ? Integer.parseInt(args[4]) : 0;
        int totalCiclos = args.length > 5 ? Integer.parseInt(args[5]) : 0; // 0 = sem fim
        String chave = System.getenv().getOrDefault("ECOMONITOR_CHAVE_REGISTRO", "chave-de-teste-ecomonitor");

        HttpClient cliente = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        Random aleatorio = new Random();
        Map<String, Double> valores = new HashMap<>();

        String tiposJson = tipos.stream().map(t -> "\"" + t + "\"").collect(Collectors.joining(","));
        String registro = String.format(
                "{\"versao\":\"1.0\",\"tipoMensagem\":\"REGISTRAR_SENSOR\",\"sensorId\":\"%s\","
                        + "\"nome\":\"Simulador %s\",\"tipos\":[%s]}", sensorId, sensorId, tiposJson);

        String token = registrar(cliente, servidor, registro, chave);
        if (token == null) return;

        int ciclos = 0;
        while (true) {
            for (String tipo : tipos) {
                double valor = proximoValor(tipo, valores, aleatorio);
                String json = String.format(Locale.US,
                        "{\"versao\":\"1.0\",\"tipoMensagem\":\"MEDICAO\",\"sensorId\":\"%s\","
                                + "\"tipoMedicao\":\"%s\",\"valor\":%.1f,\"unidade\":\"%s\",\"timestamp\":\"%s\"}",
                        sensorId, tipo, valor, unidade(tipo), agora());
                long inicio = System.nanoTime();
                try {
                    HttpResponse<String> resp = enviar(cliente, servidor + "/api/medicoes", json,
                            Map.of("X-Sensor-Token", token));
                    double rttMs = (System.nanoTime() - inicio) / 1_000_000.0;
                    System.out.printf(Locale.US, "%s %s=%.1f -> HTTP %d (%.1f ms)%n",
                            sensorId, tipo, valor, resp.statusCode(), rttMs);
                    if (resp.statusCode() == 401 || resp.statusCode() == 404) {
                        System.out.println("Servidor nao reconhece o sensor/token. Registrando de novo...");
                        token = registrar(cliente, servidor, registro, chave);
                        if (token == null) return;
                    }
                } catch (Exception e) {
                    System.out.println("Falha ao enviar (servidor indisponivel?): " + e.getMessage());
                }
            }
            ciclos++;
            if (totalCiclos > 0 && ciclos >= totalCiclos) {
                System.out.println("Fim: " + ciclos + " ciclos enviados.");
                return;
            }
            if (ciclosAteDesconectar > 0 && ciclos % ciclosAteDesconectar == 0) {
                System.out.println("Simulando desconexao por 20 s...");
                Thread.sleep(20000);
            }
            Thread.sleep(intervalo);
        }
    }

    // Registra o sensor e devolve o token. Se o servidor estiver fora do ar, tenta de novo.
    // Devolve null se o servidor rejeitar o registro (ex.: chave errada).
    static String registrar(HttpClient cliente, String servidor, String registro, String chave) throws Exception {
        Pattern padraoToken = Pattern.compile("\"token\":\"([^\"]+)\"");
        while (true) {
            try {
                HttpResponse<String> resp = enviar(cliente, servidor + "/api/sensores", registro,
                        Map.of("X-Chave-Registro", chave));
                if (resp.statusCode() == 200 || resp.statusCode() == 201) {
                    Matcher m = padraoToken.matcher(resp.body());
                    if (m.find()) {
                        System.out.println("Sensor registrado (HTTP " + resp.statusCode() + ")");
                        return m.group(1);
                    }
                }
                System.out.println("Registro rejeitado: HTTP " + resp.statusCode() + " " + resp.body());
                return null;
            } catch (Exception e) {
                System.out.println("Servidor indisponivel, nova tentativa em 3 s...");
                Thread.sleep(3000);
            }
        }
    }

    static HttpResponse<String> enviar(HttpClient cliente, String url, String json,
                                       Map<String, String> cabecalhos) throws Exception {
        HttpRequest.Builder construtor = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        cabecalhos.forEach(construtor::header);
        return cliente.send(construtor.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String agora() {
        return LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    static String unidade(String tipo) {
        return switch (tipo) {
            case "TEMPERATURA" -> "C";
            case "UMIDADE" -> "%";
            default -> "IQA";
        };
    }

    static double base(String tipo) {
        return switch (tipo) {
            case "TEMPERATURA" -> 26;
            case "UMIDADE" -> 60;
            default -> 40;
        };
    }

    // Caminhada aleatoria perto de um valor base, com picos raros (3%) para disparar alertas
    static double proximoValor(String tipo, Map<String, Double> valores, Random r) {
        double passo = switch (tipo) {
            case "TEMPERATURA" -> 0.6;
            case "UMIDADE" -> 1.5;
            default -> 3.0;
        };
        double atual = valores.getOrDefault(tipo, base(tipo));
        atual += (r.nextDouble() - 0.5) * 2 * passo;
        atual += (base(tipo) - atual) * 0.05;
        valores.put(tipo, atual);
        if (r.nextDouble() < 0.03) {
            return switch (tipo) {
                case "TEMPERATURA" -> 41 + r.nextDouble() * 4;
                case "UMIDADE" -> 20 + r.nextDouble() * 8;
                default -> 85 + r.nextDouble() * 15;
            };
        }
        return atual;
    }
}
