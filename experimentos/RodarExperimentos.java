import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Roda TODOS os experimentos (A a E) em sequencia, sem precisar abrir varios terminais.
 * Sobe sozinho os servidores de eco (TCP/UDP) e os emuladores de rede, e os encerra no final.
 * Pre-requisito: servidor EcoMonitor rodando em 127.0.0.1:8080, painel e simuladores FECHADOS.
 *
 * Uso (na raiz do projeto):  java experimentos\RodarExperimentos.java [completo|rapido]
 *   completo (padrao): parametros do relatorio, ~8 minutos
 *   rapido: poucas amostras, so para conferir que tudo funciona (~1 minuto; NAO use no relatorio)
 *
 * Condicoes (Experimento E), todas em 127.0.0.1:
 *   local    : direto, sem emulador (linha de base)
 *   proxy0   : passando pelo emulador com atraso 0 (isola o custo do proprio emulador)
 *   atraso50 : emulador com 25 ms por sentido (+50 ms no RTT)
 *   perda5   : emulador UDP com 5% de descarte por sentido (so Exp. D/UDP)
 *   jitter   : emulador UDP com 25 ms +- 15 ms por sentido (so Exp. D/UDP; pode reordenar)
 */
public class RodarExperimentos {
    static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    static final List<Process> auxiliares = new ArrayList<>();
    static Path resultados, logs;

    public static void main(String[] args) throws Exception {
        boolean rapido = args.length > 0 && args[0].equals("rapido");
        resultados = rapido ? Path.of("experimentos", "resultados", "teste-rapido") : Path.of("experimentos", "resultados");
        logs = resultados.resolve("logs");
        if (!Files.exists(Path.of("experimentos", "Experimentos.java"))) {
            System.out.println("Rode a partir da raiz do projeto (pasta que contem experimentos\\).");
            return;
        }
        if (!servidorNoAr()) {
            System.out.println("[ERRO] Servidor EcoMonitor nao responde em http://127.0.0.1:8080. Rode 1-SERVIDOR.bat antes.");
            return;
        }
        arquivarAnteriores();
        Files.createDirectories(logs);
        Runtime.getRuntime().addShutdownHook(new Thread(RodarExperimentos::encerrarAuxiliares));
        registrarAmbiente(rapido);

        // Servidores de eco e emuladores (portas fixas)
        auxiliar("eco_tcp", "experimentos/Experimentos.java", "tcp-servidor", "9091");
        auxiliar("eco_udp", "experimentos/Experimentos.java", "udp-servidor", "9090");
        auxiliar("emu_http_0ms", "experimentos/EmuladorRede.java", "tcp", "8081", "127.0.0.1:8080", "0");
        auxiliar("emu_http_25ms", "experimentos/EmuladorRede.java", "tcp", "8082", "127.0.0.1:8080", "25");
        auxiliar("emu_tcp_0ms", "experimentos/EmuladorRede.java", "tcp", "9101", "127.0.0.1:9091", "0");
        auxiliar("emu_tcp_25ms", "experimentos/EmuladorRede.java", "tcp", "9102", "127.0.0.1:9091", "25");
        auxiliar("emu_udp_0ms", "experimentos/EmuladorRede.java", "udp", "9201", "127.0.0.1:9090", "0");
        auxiliar("emu_udp_25ms", "experimentos/EmuladorRede.java", "udp", "9202", "127.0.0.1:9090", "25");
        auxiliar("emu_udp_perda5", "experimentos/EmuladorRede.java", "udp", "9203", "127.0.0.1:9090", "0", "5", "0", "42");
        auxiliar("emu_udp_jitter", "experimentos/EmuladorRede.java", "udp", "9204", "127.0.0.1:9090", "25", "0", "15", "7");
        System.out.println("Aguardando servidores auxiliares (os .java sao compilados na hora)...");
        Thread.sleep(8000);

        String direto = "http://127.0.0.1:8080", via0 = "http://127.0.0.1:8081", via25 = "http://127.0.0.1:8082";
        String nRtt = rapido ? "30" : "100", reps = rapido ? "3" : "10", seg = rapido ? "5" : "30", segMax = rapido ? "5" : "15";
        String nTr = rapido ? "200" : "1000";
        long t0 = System.nanoTime();

        titulo("AQUECIMENTO do servidor (nao entra nos resultados)");
        exp("aquecer", direto, rapido ? "3" : "15");

        titulo("CONDICAO local (linha de base)");
        exp("rtt", direto, "local", nRtt);
        exp("throughput", direto, "local", reps);
        for (String c : List.of("1", "2", "5")) exp("concorrencia", direto, "local", c, seg, "100");
        for (String c : List.of("1", "2", "5")) exp("concorrencia", direto, "local_cargamax", c, segMax, "0");
        exp("tcp-cliente", "127.0.0.1", "9091", "local", nTr, "100", "10");
        exp("udp-cliente", "127.0.0.1", "9090", "local", nTr, "100", "10");

        titulo("CONDICAO proxy0 (emulador com atraso 0: custo do proprio emulador)");
        exp("rtt", via0, "proxy0", nRtt);
        exp("tcp-cliente", "127.0.0.1", "9101", "proxy0", nTr, "100", "10");
        exp("udp-cliente", "127.0.0.1", "9201", "proxy0", nTr, "100", "10");

        titulo("CONDICAO atraso50 (25 ms por sentido)");
        exp("rtt", via25, "atraso50", nRtt);
        exp("throughput", via25, "atraso50", reps);
        for (String c : List.of("1", "2", "5")) exp("concorrencia", via25, "atraso50", c, seg, "100");
        exp("tcp-cliente", "127.0.0.1", "9102", "atraso50", nTr, "100", "10");
        exp("udp-cliente", "127.0.0.1", "9202", "atraso50", nTr, "100", "10");

        titulo("CONDICOES perda5 e jitter (somente UDP)");
        exp("udp-cliente", "127.0.0.1", "9203", "perda5", nTr, "100", "10");
        exp("udp-cliente", "127.0.0.1", "9204", "jitter", nTr, "100", "10");

        encerrarAuxiliares();
        System.out.printf("%nConcluido em %.1f min. Resultados em %s (resumo.csv e demais CSVs).%n",
                (System.nanoTime() - t0) / 60e9, resultados);
        System.out.println("Gere os graficos com: python experimentos\\graficos.py");
    }

    static boolean servidorNoAr() {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/api/ping"))
                    .timeout(Duration.ofSeconds(3)).build(), HttpResponse.BodyHandlers.ofString());
            return r.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    static void titulo(String t) {
        System.out.println("\n==================== " + t + " ====================");
    }

    // Uma nova execucao nao se mistura com a anterior: os CSVs antigos vao para resultados\anteriores\<data>
    static void arquivarAnteriores() throws IOException {
        if (!Files.exists(resultados.resolve("resumo.csv"))) return;
        Path destino = resultados.resolve("anteriores")
                .resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        Files.createDirectories(destino);
        try (var arquivos = Files.list(resultados)) {
            for (Path p : arquivos.toList()) {
                String nome = p.getFileName().toString();
                if (nome.endsWith(".csv") || nome.equals("ambiente.txt") || nome.equals("logs")) {
                    Files.move(p, destino.resolve(nome));
                }
            }
        }
        System.out.println("Resultados anteriores movidos para " + destino);
    }

    static void auxiliar(String nome, String... comando) throws IOException {
        List<String> cmd = new ArrayList<>(List.of(JAVA));
        cmd.addAll(List.of(comando));
        File log = logs.resolve(nome + ".log").toFile();
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(log).start();
        auxiliares.add(p);
    }

    static void exp(String... argumentos) throws Exception {
        List<String> cmd = new ArrayList<>(List.of(JAVA, "-Decomonitor.resultados=" + resultados,
                "experimentos/Experimentos.java"));
        cmd.addAll(List.of(argumentos));
        System.out.println("\n>>> java experimentos\\Experimentos.java " + String.join(" ", argumentos));
        int saida = new ProcessBuilder(cmd).inheritIO().start().waitFor();
        if (saida != 0) System.out.println("[AVISO] o comando terminou com codigo " + saida);
    }

    static synchronized void encerrarAuxiliares() {
        for (Process p : auxiliares) p.descendants().forEach(ProcessHandle::destroy);
        for (Process p : auxiliares) p.destroy();
        auxiliares.clear();
    }

    // Anota hardware e software no inicio da execucao (o relatorio precisa disso)
    static void registrarAmbiente(boolean rapido) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Execucao: ").append(LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append(rapido ? " (modo rapido)" : " (modo completo)").append('\n');
        sb.append("Sistema: ").append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version"))
                .append(" (").append(System.getProperty("os.arch")).append(")\n");
        sb.append("Java: ").append(System.getProperty("java.vm.name")).append(' ').append(System.getProperty("java.version"))
                .append(" (").append(System.getProperty("java.vendor")).append(")\n");
        sb.append("Processadores logicos: ").append(Runtime.getRuntime().availableProcessors()).append('\n');
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            sb.append(String.format("Memoria RAM: %.1f GB%n", os.getTotalMemorySize() / 1e9));
        }
        if (System.getProperty("os.name").startsWith("Windows")) {
            sb.append("CPU: ").append(comando("powershell", "-NoProfile", "-Command",
                    "(Get-CimInstance Win32_Processor | Select-Object -First 1).Name")).append('\n');
            sb.append("Computador: ").append(comando("powershell", "-NoProfile", "-Command",
                    "$c = Get-CimInstance Win32_ComputerSystem; $c.Manufacturer + ' ' + $c.Model")).append('\n');
            sb.append("Windows: ").append(comando("powershell", "-NoProfile", "-Command",
                    "(Get-CimInstance Win32_OperatingSystem).Caption + ' build ' + (Get-CimInstance Win32_OperatingSystem).BuildNumber"))
                    .append('\n');
        }
        sb.append("Topologia: cliente, servidor e emuladores na mesma maquina, via 127.0.0.1 (loopback)\n");
        Files.createDirectories(resultados);
        Files.writeString(resultados.resolve("ambiente.txt"), sb.toString());
        System.out.print(sb);
    }

    static String comando(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String s = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return s;
        } catch (Exception e) {
            return "(nao identificado)";
        }
    }
}
