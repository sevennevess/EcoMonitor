import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o cenario da Captura 3 do Wireshark (perdas UDP): sobe o eco UDP (porta 9090) e o emulador
 * com 5% de descarte por sentido (porta 9203), espera voce comecar a gravar, envia 1000 mensagens
 * UDP numeradas pelo emulador e mostra quantos pacotes devem aparecer em cada trecho.
 * Uso (na raiz do projeto): java experimentos\Captura3.java   (ou duplo clique em CAPTURA-3.bat)
 */
public class Captura3 {
    static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    static final Path PASTA = Path.of("experimentos", "capturas");
    static final List<Process> auxiliares = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Files.createDirectories(PASTA);
        Runtime.getRuntime().addShutdownHook(new Thread(Captura3::encerrar));
        Path logEco = PASTA.resolve("captura3-eco.log");
        Path logEmu = PASTA.resolve("captura3-emulador.log");

        System.out.println("Preparando: servidor de eco UDP (porta 9090) e emulador com 5% de perda (porta 9203)...");
        iniciar(logEco, "experimentos/Experimentos.java", "udp-servidor", "9090");
        iniciar(logEmu, "experimentos/EmuladorRede.java", "udp", "9203", "127.0.0.1:9090", "0", "5", "0", "42");
        esperarTexto(logEco, "ouvindo");
        esperarTexto(logEmu, "Emulador UDP");

        System.out.println();
        System.out.println("=====================================================================");
        System.out.println(" AGORA, NO WIRESHARK:");
        System.out.println("  1. De dois cliques em 'Adapter for loopback traffic capture'");
        System.out.println("     (a lista de pacotes comeca a rolar = esta gravando).");
        System.out.println("  2. Volte para esta janela e aperte ENTER.");
        System.out.println("=====================================================================");
        new BufferedReader(new InputStreamReader(System.in)).readLine();

        System.out.println("Enviando 1000 mensagens UDP de 100 bytes (uma a cada 10 ms), ~15 s...");
        int saida = new ProcessBuilder(JAVA, "-Decomonitor.resultados=" + PASTA.resolve("captura3-resultados"),
                "experimentos/Experimentos.java", "udp-cliente", "127.0.0.1", "9203", "captura3", "1000", "100", "10")
                .inheritIO().start().waitFor();
        if (saida != 0) System.out.println("[AVISO] o cliente terminou com codigo " + saida);

        Thread.sleep(3000); // o emulador grava os contadores a cada 2 s
        String ultima = ultimaLinha(logEmu, "ida:");
        System.out.println();
        System.out.println("=====================================================================");
        System.out.println(" PRONTO. Pare a gravacao no Wireshark (quadrado vermelho).");
        System.out.println();
        System.out.println(" Contadores do emulador: " + (ultima == null ? "(nao encontrados)" : ultima.substring(20)));
        System.out.println(" No Wireshark (Statistics > Conversations > aba UDP) voce deve ver:");
        System.out.println("   conversa cliente <-> 127.0.0.1:9203 : A->B = 1000 (enviadas pelo cliente)");
        System.out.println("                                         B->A = 'volta encaminhados' acima");
        System.out.println("   conversa emulador <-> 127.0.0.1:9090: A->B = 'ida encaminhados' acima");
        System.out.println("                                         B->A = o mesmo numero (o eco devolve tudo)");
        System.out.println(" Perdidas = 1000 - (volta encaminhados) = 'perdidos' impresso pelo cliente acima.");
        System.out.println("=====================================================================");
        encerrar();
    }

    static void iniciar(Path log, String... comando) throws IOException {
        List<String> cmd = new ArrayList<>(List.of(JAVA));
        cmd.addAll(List.of(comando));
        auxiliares.add(new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(log.toFile()).start());
    }

    static void esperarTexto(Path log, String texto) throws Exception {
        for (int i = 0; i < 300; i++) {
            if (Files.exists(log) && Files.readString(log).contains(texto)) return;
            Thread.sleep(100);
        }
        throw new IllegalStateException("Nao iniciou: veja " + log + " (porta 9090 ou 9203 ja em uso?)");
    }

    static String ultimaLinha(Path log, String contendo) throws IOException {
        String achada = null;
        for (String l : Files.readAllLines(log)) if (l.contains(contendo)) achada = l;
        return achada;
    }

    static synchronized void encerrar() {
        for (Process p : auxiliares) p.destroy();
        auxiliares.clear();
    }
}
