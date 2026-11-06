import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ferramenta dos experimentos de rede do EcoMonitor. Executar a partir da raiz do projeto:
 *   java experimentos\Experimentos.java <modo> ...
 * Veja experimentos\ROTEIRO-EXPERIMENTOS.md. Resultados em experimentos\resultados\.
 */
public class Experimentos {
    static final Path PASTA = Path.of(System.getProperty("ecomonitor.resultados", "experimentos/resultados"));
    static final int AQUECIMENTO = 30; // requisicoes iniciais descartadas (JIT do cliente e conexao)
    static final String CHAVE = System.getenv().getOrDefault("ECOMONITOR_CHAVE_REGISTRO", "chave-de-teste-ecomonitor");

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { uso(); return; }
        Files.createDirectories(PASTA);
        try {
            switch (args[0]) {
                case "aquecer" -> aquecer(exigir(args, 1), inteiro(args, 2, 10));
                case "rtt" -> rtt(exigir(args, 1), condicao(args, 2), inteiro(args, 3, 100));
                case "throughput" -> throughput(exigir(args, 1), condicao(args, 2), inteiro(args, 3, 10));
                case "concorrencia" -> concorrencia(exigir(args, 1), condicao(args, 2),
                        Integer.parseInt(exigir(args, 3)), inteiro(args, 4, 30), inteiro(args, 5, 100));
                case "tcp-servidor" -> tcpServidor(inteiro(args, 1, 9091));
                case "udp-servidor" -> udpServidor(inteiro(args, 1, 9090));
                case "tcp-cliente" -> clienteTransporte(true, exigir(args, 1), Integer.parseInt(exigir(args, 2)),
                        condicao(args, 3), inteiro(args, 4, 1000), inteiro(args, 5, 100), inteiro(args, 6, 10));
                case "udp-cliente" -> clienteTransporte(false, exigir(args, 1), Integer.parseInt(exigir(args, 2)),
                        condicao(args, 3), inteiro(args, 4, 1000), inteiro(args, 5, 100), inteiro(args, 6, 10));
                default -> uso();
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            uso();
        }
    }

    static void uso() {
        System.out.println("""
                Uso (a partir da raiz do projeto):
                  java experimentos\\Experimentos.java aquecer <servidor> [segundos=10]
                  java experimentos\\Experimentos.java rtt <servidor> <condicao> [n=100]
                  java experimentos\\Experimentos.java throughput <servidor> <condicao> [repeticoes=10]
                  java experimentos\\Experimentos.java concorrencia <servidor> <condicao> <clientes> [segundos=30] [intervaloMs=100]
                  java experimentos\\Experimentos.java tcp-servidor [porta=9091]
                  java experimentos\\Experimentos.java udp-servidor [porta=9090]
                  java experimentos\\Experimentos.java tcp-cliente <host> <porta> <condicao> [n=1000] [bytes=100] [intervaloMs=10]
                  java experimentos\\Experimentos.java udp-cliente <host> <porta> <condicao> [n=1000] [bytes=100] [intervaloMs=10]
                <servidor> ex.: http://127.0.0.1:8080   <condicao>: rotulo sem espacos (ex.: local, lag50, perda5)
                """);
    }

    static String exigir(String[] a, int i) {
        if (a.length <= i) throw new IllegalArgumentException("argumentos insuficientes");
        return a[i];
    }

    static int inteiro(String[] a, int i, int padrao) {
        return a.length > i ? Integer.parseInt(a[i]) : padrao;
    }

    static String condicao(String[] a, int i) {
        String c = exigir(a, i);
        if (!c.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("condicao so aceita letras, numeros, _ e -");
        return c;
    }

    // ======================================================================
    // Utilidades: HTTP, estatistica e arquivos
    // ======================================================================

    static HttpClient novoCliente() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5)).build();
    }

    static HttpRequest postJson(String url, String json, String cabecalho, String valor) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json").header(cabecalho, valor)
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
    }

    static String agora() {
        return LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    static String registrarSensor(HttpClient c, String servidor, String id) throws Exception {
        String json = String.format("{\"versao\":\"1.0\",\"tipoMensagem\":\"REGISTRAR_SENSOR\",\"sensorId\":\"%s\","
                + "\"nome\":\"Experimento %s\",\"tipos\":[\"TEMPERATURA\"]}", id, id);
        HttpResponse<String> r = c.send(postJson(servidor + "/api/sensores", json, "X-Chave-Registro", CHAVE),
                HttpResponse.BodyHandlers.ofString());
        Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(r.body());
        if ((r.statusCode() == 200 || r.statusCode() == 201) && m.find()) return m.group(1);
        throw new IllegalStateException("Registro do sensor falhou: HTTP " + r.statusCode() + " " + r.body());
    }

    static HttpRequest medicao(String servidor, String sensorId, String token) {
        String json = String.format(Locale.US, "{\"versao\":\"1.0\",\"tipoMensagem\":\"MEDICAO\",\"sensorId\":\"%s\","
                + "\"tipoMedicao\":\"TEMPERATURA\",\"valor\":25.0,\"unidade\":\"C\",\"timestamp\":\"%s\"}", sensorId, agora());
        return postJson(servidor + "/api/medicoes", json, "X-Sensor-Token", token);
    }

    record Estat(int n, double media, double mediana, double desvio, double min, double max, double p95) {}

    static Estat estat(List<Double> valores) {
        int n = valores.size();
        if (n == 0) return new Estat(0, 0, 0, 0, 0, 0, 0);
        double[] v = valores.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double soma = 0;
        for (double x : v) soma += x;
        double media = soma / n;
        double mediana = n % 2 == 1 ? v[n / 2] : (v[n / 2 - 1] + v[n / 2]) / 2;
        double quad = 0;
        for (double x : v) quad += (x - media) * (x - media);
        double desvio = n > 1 ? Math.sqrt(quad / (n - 1)) : 0;
        double p95 = v[(int) Math.min(n - 1, Math.ceil(0.95 * n) - 1)];
        return new Estat(n, media, mediana, desvio, v[0], v[n - 1], p95);
    }

    static synchronized void anexar(String arquivo, String cabecalho, String linha) throws IOException {
        Path p = PASTA.resolve(arquivo);
        boolean novo = Files.notExists(p);
        try (var w = Files.newBufferedWriter(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (novo) w.write(cabecalho + "\n");
            w.write(linha + "\n");
        }
    }

    static void escrever(String arquivo, String cabecalho, List<String> linhas) throws IOException {
        List<String> todas = new ArrayList<>();
        todas.add(cabecalho);
        todas.addAll(linhas);
        Files.write(PASTA.resolve(arquivo), todas);
        System.out.println("Dados brutos: " + PASTA.resolve(arquivo));
    }

    static void resumo(String exp, String cond, String param, Estat s, String obs) throws IOException {
        String linha = String.format(Locale.US, "%s,%s,%s,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%s,%s",
                exp, cond, param, s.n(), s.media(), s.mediana(), s.desvio(), s.min(), s.max(), s.p95(), obs, agora());
        anexar("resumo.csv", "experimento,condicao,parametro,n,media,mediana,desvio_padrao,min,max,p95,observacao,data_hora", linha);
        System.out.printf(Locale.US, "[%s | %s | %s] n=%d media=%.3f mediana=%.3f dp=%.3f min=%.3f max=%.3f p95=%.3f %s%n",
                exp, cond, param, s.n(), s.media(), s.mediana(), s.desvio(), s.min(), s.max(), s.p95(), obs);
    }

    // ======================================================================
    // Aquecimento: exercita PING e MEDICAO sem registrar nada, para o JIT do servidor
    // compilar o caminho quente antes das medicoes (senao a 1a condicao medida sai mais lenta)
    // ======================================================================

    static void aquecer(String servidor, int segundos) throws Exception {
        HttpClient c = novoCliente();
        String token = registrarSensor(c, servidor, "EXP_AQUECIMENTO");
        long fim = System.nanoTime() + segundos * 1_000_000_000L;
        int n = 0;
        while (System.nanoTime() < fim) {
            c.send(HttpRequest.newBuilder(URI.create(servidor + "/api/ping")).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            c.send(medicao(servidor, "EXP_AQUECIMENTO", token), HttpResponse.BodyHandlers.discarding());
            n += 2;
        }
        System.out.println("Aquecimento do servidor: " + n + " requisicoes em " + segundos + " s (descartadas)");
    }

    // ======================================================================
    // Experimento A: RTT (aplicacao) x tempo de processamento do servidor
    // ======================================================================

    static void rtt(String servidor, String cond, int n) throws Exception {
        if (n < 30) System.out.println("AVISO: o enunciado exige pelo menos 30 medicoes por condicao.");
        HttpClient c = novoCliente();
        String token = registrarSensor(c, servidor, "EXP_RTT");
        List<String> linhas = new ArrayList<>();

        for (String operacao : List.of("PING", "MEDICAO")) {
            List<Double> rtts = new ArrayList<>(), srvs = new ArrayList<>(), rede = new ArrayList<>();
            int falhas = 0;
            for (int i = -AQUECIMENTO; i < n; i++) {
                HttpRequest req = operacao.equals("PING")
                        ? HttpRequest.newBuilder(URI.create(servidor + "/api/ping")).timeout(Duration.ofSeconds(30)).GET().build()
                        : medicao(servidor, "EXP_RTT", token);
                long t0 = System.nanoTime();
                try {
                    HttpResponse<String> r = c.send(req, HttpResponse.BodyHandlers.ofString());
                    double ms = (System.nanoTime() - t0) / 1e6;
                    if (r.statusCode() >= 400) { falhas++; continue; }
                    double srv = Long.parseLong(r.headers().firstValue("X-Tempo-Servidor-Us").orElse("0")) / 1000.0;
                    if (i >= 0) {
                        rtts.add(ms); srvs.add(srv); rede.add(ms - srv);
                        linhas.add(String.format(Locale.US, "%s,%s,%d,%.3f,%.3f,%.3f", cond, operacao, i + 1, ms, srv, ms - srv));
                    }
                } catch (IOException e) {
                    falhas++;
                }
                Thread.sleep(20);
            }
            String obs = "falhas=" + falhas + " aquecimento=" + AQUECIMENTO;
            resumo("RTT", cond, operacao + "_rtt_ms", estat(rtts), obs);
            resumo("RTT", cond, operacao + "_servidor_ms", estat(srvs), obs);
            resumo("RTT", cond, operacao + "_rede_estimada_ms", estat(rede), obs);
        }
        escrever("rtt_" + cond + ".csv", "condicao,operacao,i,rtt_ms,servidor_ms,rede_estimada_ms", linhas);
    }

    // ======================================================================
    // Experimento B: throughput (volume util / tempo)
    // ======================================================================

    static void throughput(String servidor, String cond, int reps) throws Exception {
        HttpClient c = novoCliente();
        int[] tamanhos = {1_024, 10_240, 102_400, 1_048_576};
        List<String> linhas = new ArrayList<>();

        for (String direcao : List.of("UPLOAD", "DOWNLOAD")) {
            for (int bytes : tamanhos) {
                List<Double> tempos = new ArrayList<>(), vazoes = new ArrayList<>();
                int falhas = 0;
                byte[] carga = new byte[bytes];
                Arrays.fill(carga, (byte) 'x');
                for (int i = -3; i < reps; i++) {
                    HttpRequest req = direcao.equals("UPLOAD")
                            ? HttpRequest.newBuilder(URI.create(servidor + "/api/teste/carga")).timeout(Duration.ofSeconds(30))
                                    .header("Content-Type", "application/octet-stream")
                                    .POST(HttpRequest.BodyPublishers.ofByteArray(carga)).build()
                            : HttpRequest.newBuilder(URI.create(servidor + "/api/teste/carga?bytes=" + bytes))
                                    .timeout(Duration.ofSeconds(30)).GET().build();
                    long t0 = System.nanoTime();
                    try {
                        HttpResponse<byte[]> r = c.send(req, HttpResponse.BodyHandlers.ofByteArray());
                        double s = (System.nanoTime() - t0) / 1e9;
                        boolean ok = r.statusCode() == 200 && (direcao.equals("UPLOAD") || r.body().length == bytes);
                        if (!ok) { falhas++; continue; }
                        if (i >= 0) {
                            double mbps = bytes * 8.0 / s / 1e6; // so o corpo (volume util), sem cabecalhos
                            tempos.add(s * 1000); vazoes.add(mbps);
                            linhas.add(String.format(Locale.US, "%s,%s,%d,%d,%.3f,%.3f", cond, direcao, bytes, i + 1, s * 1000, mbps));
                        }
                    } catch (IOException e) {
                        falhas++;
                    }
                }
                resumo("THROUGHPUT", cond, direcao + "_" + bytes + "B_vazao_mbps", estat(vazoes), "falhas=" + falhas);
                resumo("THROUGHPUT", cond, direcao + "_" + bytes + "B_tempo_ms", estat(tempos), "falhas=" + falhas);
            }
        }
        escrever("throughput_" + cond + ".csv", "condicao,direcao,bytes,i,tempo_ms,vazao_mbps", linhas);
    }

    // ======================================================================
    // Experimento C: concorrencia (N clientes simultaneos, cada um com sua conexao)
    // ======================================================================

    static void concorrencia(String servidor, String cond, int clientes, int segundos, int intervaloMs) throws Exception {
        HttpClient[] http = new HttpClient[clientes];
        String[] tokens = new String[clientes];
        for (int i = 0; i < clientes; i++) {
            http[i] = novoCliente();
            tokens[i] = registrarSensor(http[i], servidor, "EXP_C" + (i + 1));
        }
        List<Double> latencias = Collections.synchronizedList(new ArrayList<>());
        List<String> linhas = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger ok = new AtomicInteger(), erros = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        long[] inicio = new long[1];
        long duracaoNs = segundos * 1_000_000_000L;
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < clientes; i++) {
            final int idx = i;
            Thread t = new Thread(() -> {
                String id = "EXP_C" + (idx + 1);
                try {
                    largada.await();
                    while (System.nanoTime() < inicio[0] + duracaoNs) {
                        long t0 = System.nanoTime();
                        try {
                            HttpResponse<String> r = http[idx].send(medicao(servidor, id, tokens[idx]),
                                    HttpResponse.BodyHandlers.ofString());
                            double ms = (System.nanoTime() - t0) / 1e6;
                            if (r.statusCode() == 201) {
                                ok.incrementAndGet();
                                latencias.add(ms);
                                linhas.add(String.format(Locale.US, "%s,%d,%s,%.3f", cond, clientes, id, ms));
                            } else {
                                erros.incrementAndGet();
                            }
                        } catch (IOException e) {
                            erros.incrementAndGet();
                        }
                        if (intervaloMs > 0) Thread.sleep(intervaloMs);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            t.start();
            threads.add(t);
        }
        inicio[0] = System.nanoTime();
        largada.countDown();
        for (Thread t : threads) t.join();
        double duracao = (System.nanoTime() - inicio[0]) / 1e9;

        Estat e = estat(new ArrayList<>(latencias));
        double taxa = ok.get() / duracao;
        String param = clientes + "clientes_intervalo" + intervaloMs + "ms_latencia_ms";
        resumo("CONCORRENCIA", cond, param, e, String.format(Locale.US, "taxa_msg_s=%.2f ok=%d erros=%d duracao_s=%.1f",
                taxa, ok.get(), erros.get(), duracao));
        anexar("concorrencia_resumo.csv",
                "condicao,clientes,intervalo_ms,duracao_s,mensagens_ok,erros,taxa_msg_s,lat_media_ms,lat_mediana_ms,lat_dp_ms,lat_p95_ms,lat_max_ms",
                String.format(Locale.US, "%s,%d,%d,%.1f,%d,%d,%.2f,%.3f,%.3f,%.3f,%.3f,%.3f", cond, clientes, intervaloMs,
                        duracao, ok.get(), erros.get(), taxa, e.media(), e.mediana(), e.desvio(), e.p95(), e.max()));
        escrever("concorrencia_" + cond + "_" + clientes + "clientes.csv", "condicao,clientes,sensor,latencia_ms",
                new ArrayList<>(linhas));
    }

    // ======================================================================
    // Experimento D: microteste TCP x UDP (eco com registro numerado)
    // Cada mensagem: 8 bytes de sequencia + 8 bytes de nanoTime + preenchimento.
    // TCP e UDP enviam o mesmo numero de mensagens, do mesmo tamanho, no mesmo ritmo.
    // ======================================================================

    static void tcpServidor(int porta) throws Exception {
        try (ServerSocket ss = new ServerSocket(porta)) {
            System.out.println("Eco TCP ouvindo na porta " + porta + " (Ctrl+C para parar)");
            while (true) {
                Socket s = ss.accept();
                new Thread(() -> ecoTcp(s)).start();
            }
        }
    }

    static void ecoTcp(Socket s) {
        try (s) {
            s.setTcpNoDelay(true);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Conexao TCP encerrada: " + e.getMessage());
        }
    }

    static void udpServidor(int porta) throws Exception {
        try (DatagramSocket ds = new DatagramSocket(porta)) {
            System.out.println("Eco UDP ouvindo na porta " + porta + " (Ctrl+C para parar)");
            byte[] buf = new byte[2048];
            while (true) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                ds.receive(p);
                ds.send(new DatagramPacket(p.getData(), p.getLength(), p.getSocketAddress()));
            }
        }
    }

    static class Coletor {
        final double[] rtt;
        final int n;
        int recebidos, duplicados, foraDeOrdem;
        long maiorSeq = -1;

        Coletor(int n) {
            this.n = n;
            this.rtt = new double[n];
            Arrays.fill(rtt, Double.NaN);
        }

        synchronized void registrar(long seq, long enviadoNs) {
            if (seq < 0 || seq >= n) return;
            if (!Double.isNaN(rtt[(int) seq])) { duplicados++; return; }
            rtt[(int) seq] = (System.nanoTime() - enviadoNs) / 1e6;
            recebidos++;
            if (seq < maiorSeq) foraDeOrdem++; else maiorSeq = seq;
        }

        synchronized int recebidos() { return recebidos; }
    }

    static byte[] montar(long seq, int bytes) {
        return ByteBuffer.allocate(bytes).putLong(seq).putLong(System.nanoTime()).array();
    }

    static void clienteTransporte(boolean tcp, String host, int porta, String cond, int n, int bytes, int intervaloMs)
            throws Exception {
        if (bytes < 16) throw new IllegalArgumentException("bytes deve ser >= 16");
        if (!tcp && bytes > 1400) throw new IllegalArgumentException("UDP: use bytes <= 1400 (evita fragmentacao)");
        Coletor col = new Coletor(n);
        String proto = tcp ? "TCP" : "UDP";
        String extra = "";
        long t0 = System.nanoTime();

        if (tcp) {
            try (Socket s = new Socket()) {
                s.setTcpNoDelay(true); // sem Nagle: cada mensagem sai logo
                long c0 = System.nanoTime();
                s.connect(new InetSocketAddress(host, porta), 5000);
                extra = String.format(Locale.US, "conexao_ms=%.3f", (System.nanoTime() - c0) / 1e6);
                Thread rx = new Thread(() -> {
                    try {
                        DataInputStream in = new DataInputStream(s.getInputStream());
                        byte[] buf = new byte[bytes];
                        while (true) {
                            in.readFully(buf);
                            ByteBuffer b = ByteBuffer.wrap(buf);
                            col.registrar(b.getLong(), b.getLong());
                        }
                    } catch (IOException e) {
                        // fim da conexao
                    }
                });
                rx.start();
                OutputStream out = s.getOutputStream();
                for (int i = 0; i < n; i++) {
                    out.write(montar(i, bytes));
                    out.flush();
                    if (intervaloMs > 0) Thread.sleep(intervaloMs);
                }
                long limite = System.nanoTime() + 10_000_000_000L; // espera ate 10 s pelas respostas
                while (col.recebidos() < n && System.nanoTime() < limite) Thread.sleep(50);
            }
        } else {
            InetAddress destino = InetAddress.getByName(host);
            try (DatagramSocket ds = new DatagramSocket()) {
                ds.setSoTimeout(200);
                Thread rx = new Thread(() -> {
                    byte[] buf = new byte[2048];
                    while (!ds.isClosed()) {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        try {
                            ds.receive(p);
                            ByteBuffer b = ByteBuffer.wrap(p.getData(), 0, p.getLength());
                            col.registrar(b.getLong(), b.getLong());
                        } catch (SocketTimeoutException e) {
                            // continua esperando
                        } catch (IOException e) {
                            break;
                        }
                    }
                });
                rx.start();
                for (int i = 0; i < n; i++) {
                    byte[] m = montar(i, bytes);
                    ds.send(new DatagramPacket(m, m.length, destino, porta));
                    if (intervaloMs > 0) Thread.sleep(intervaloMs);
                }
                Thread.sleep(2000); // espera datagramas atrasados
            }
        }
        double duracao = (System.nanoTime() - t0) / 1e9;
        finalizarTransporte(proto, cond, col, n, bytes, intervaloMs, duracao, extra);
    }

    static void finalizarTransporte(String proto, String cond, Coletor col, int n, int bytes, int intervaloMs,
                                    double duracao, String extra) throws IOException {
        List<Double> rtts = new ArrayList<>();
        List<String> linhas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            boolean recebido = !Double.isNaN(col.rtt[i]);
            if (recebido) rtts.add(col.rtt[i]);
            linhas.add(String.format(Locale.US, "%s,%s,%d,%d,%s", proto, cond, i, recebido ? 1 : 0,
                    recebido ? String.format(Locale.US, "%.3f", col.rtt[i]) : ""));
        }
        Estat e = estat(rtts);
        int recebidos = col.recebidos();
        int perdidos = n - recebidos;
        double perda = 100.0 * perdidos / n;

        escrever("transporte_" + proto.toLowerCase() + "_" + cond + ".csv", "protocolo,condicao,seq,recebido,rtt_ms", linhas);
        anexar("transporte_resumo.csv",
                "protocolo,condicao,n,bytes,intervalo_ms,recebidos,perdidos,perda_pct,fora_de_ordem,duplicados,"
                        + "rtt_media_ms,rtt_mediana_ms,rtt_dp_ms,rtt_p95_ms,rtt_max_ms,duracao_s,observacao",
                String.format(Locale.US, "%s,%s,%d,%d,%d,%d,%d,%.2f,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%s",
                        proto, cond, n, bytes, intervaloMs, recebidos, perdidos, perda, col.foraDeOrdem, col.duplicados,
                        e.media(), e.mediana(), e.desvio(), e.p95(), e.max(), duracao, extra));
        resumo("TRANSPORTE", cond, proto + "_" + n + "msg_" + bytes + "B_rtt_ms", e,
                String.format(Locale.US, "perdidos=%d perda_pct=%.2f fora_de_ordem=%d duplicados=%d %s",
                        perdidos, perda, col.foraDeOrdem, col.duplicados, extra));
    }
}
