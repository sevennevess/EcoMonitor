import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Emulador de condicoes de rede em espaco de usuario (Experimento E), alternativa ao Clumsy que
 * nao precisa de privilegio de administrador. Fica entre o cliente e o servidor como um proxy:
 *
 *   cliente --> 127.0.0.1:portaLocal [emulador: atraso / perda] --> destino
 *
 * TCP: atrasa cada bloco de bytes em cada sentido (ordem preservada). Nao consegue emular PERDA de
 *      segmentos TCP: o emulador termina a conexao TCP dos dois lados, entao o kernel nunca ve perda.
 * UDP: atrasa, aplica variacao (jitter, pode reordenar) e descarta datagramas com a probabilidade
 *      dada, de forma independente em cada sentido.
 *
 * Uso (a partir da raiz do projeto):
 *   java experimentos\EmuladorRede.java tcp <portaLocal> <host:porta> <atrasoMsPorSentido>
 *   java experimentos\EmuladorRede.java udp <portaLocal> <host:porta> <atrasoMsPorSentido> [perdaPct=0] [jitterMs=0] [semente]
 * Ex.: java experimentos\EmuladorRede.java tcp 8081 127.0.0.1:8080 25   (RTT da aplicacao +50 ms)
 */
public class EmuladorRede {
    // O timer do Windows pode ter resolucao de 15,6 ms (depende de outros processos e de a janela estar
    // em segundo plano): dormir ate 20 ms antes do instante e completar a espera ativamente.
    static final long MARGEM_NS = 20_000_000L;
    static final AtomicLong ida = new AtomicLong(), volta = new AtomicLong();
    static final AtomicLong perdidosIda = new AtomicLong(), perdidosVolta = new AtomicLong();

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("""
                    Uso:
                      java experimentos\\EmuladorRede.java tcp <portaLocal> <host:porta> <atrasoMsPorSentido>
                      java experimentos\\EmuladorRede.java udp <portaLocal> <host:porta> <atrasoMsPorSentido> [perdaPct=0] [jitterMs=0] [semente]
                    """);
            return;
        }
        int portaLocal = Integer.parseInt(args[1]);
        String[] destino = args[2].split(":");
        InetSocketAddress alvo = new InetSocketAddress(destino[0], Integer.parseInt(destino[1]));
        long atrasoMs = Long.parseLong(args[3]);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> relatorio("final")));
        // Contadores a cada 2 s, so quando mudaram (a ultima linha do log sempre tem os totais)
        Thread estat = new Thread(() -> {
            long anterior = 0;
            while (true) {
                LockSupport.parkNanos(2_000_000_000L);
                long atual = ida.get() + volta.get() + perdidosIda.get() + perdidosVolta.get();
                if (atual != anterior) {
                    relatorio("parcial");
                    anterior = atual;
                }
            }
        });
        estat.setDaemon(true);
        estat.start();

        if (args[0].equals("tcp")) {
            System.out.printf("%s Emulador TCP 127.0.0.1:%d -> %s | atraso %d ms por sentido%n",
                    agora(), portaLocal, args[2], atrasoMs);
            tcp(portaLocal, alvo, atrasoMs);
        } else {
            double perda = args.length > 4 ? Double.parseDouble(args[4]) / 100.0 : 0;
            long jitterMs = args.length > 5 ? Long.parseLong(args[5]) : 0;
            Random r = args.length > 6 ? new Random(Long.parseLong(args[6])) : new Random();
            System.out.printf("%s Emulador UDP 127.0.0.1:%d -> %s | atraso %d ms, jitter +-%d ms, perda %.1f%% por sentido%n",
                    agora(), portaLocal, args[2], atrasoMs, jitterMs, perda * 100);
            udp(portaLocal, alvo, atrasoMs, jitterMs, perda, r);
        }
    }

    static String agora() {
        return LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    static void esperarAte(long alvoNs) {
        long resta;
        while ((resta = alvoNs - System.nanoTime()) > MARGEM_NS) LockSupport.parkNanos(resta - MARGEM_NS);
        while (System.nanoTime() < alvoNs) Thread.yield();
    }

    static void relatorio(String tipo) {
        System.out.printf("%s [%s] ida: %d encaminhados, %d descartados | volta: %d encaminhados, %d descartados%n",
                agora(), tipo, ida.get(), perdidosIda.get(), volta.get(), perdidosVolta.get());
    }

    // ======================================================================
    // TCP: um par de threads (leitor -> fila -> escritor) para cada sentido
    // ======================================================================

    record Bloco(long liberarNs, byte[] dados) {}

    static void tcp(int portaLocal, InetSocketAddress alvo, long atrasoMs) throws IOException {
        try (ServerSocket ss = new ServerSocket(portaLocal, 50, InetAddress.getByName("127.0.0.1"))) {
            while (true) {
                Socket cliente = ss.accept();
                try {
                    Socket servidor = new Socket();
                    servidor.connect(alvo, 5000);
                    cliente.setTcpNoDelay(true);
                    servidor.setTcpNoDelay(true);
                    // Fecha os dois sockets so depois que os dois sentidos terminarem
                    AtomicLong sentidosAbertos = new AtomicLong(2);
                    Runnable fim = () -> {
                        if (sentidosAbertos.decrementAndGet() == 0) {
                            try { cliente.close(); servidor.close(); } catch (IOException ignorada) { }
                        }
                    };
                    encaminhar(cliente, servidor, atrasoMs, ida, fim);
                    encaminhar(servidor, cliente, atrasoMs, volta, fim);
                } catch (IOException e) {
                    System.out.println("Destino indisponivel: " + e.getMessage());
                    cliente.close();
                }
            }
        }
    }

    static void encaminhar(Socket de, Socket para, long atrasoMs, AtomicLong contador, Runnable fim) {
        BlockingQueue<Bloco> fila = new LinkedBlockingQueue<>();
        long atrasoNs = atrasoMs * 1_000_000L;
        Thread leitor = new Thread(() -> {
            byte[] buf = new byte[65536];
            try {
                InputStream in = de.getInputStream();
                int n;
                while ((n = in.read(buf)) != -1) {
                    fila.put(new Bloco(System.nanoTime() + atrasoNs, Arrays.copyOf(buf, n)));
                }
            } catch (IOException | InterruptedException e) {
                // conexao encerrada
            }
            fila.add(new Bloco(System.nanoTime() + atrasoNs, null)); // fim do fluxo
        });
        Thread escritor = new Thread(() -> {
            try {
                OutputStream out = para.getOutputStream();
                while (true) {
                    Bloco b = fila.take();
                    esperarAte(b.liberarNs());
                    if (b.dados() == null) {
                        para.shutdownOutput();
                        break;
                    }
                    out.write(b.dados());
                    out.flush();
                    contador.incrementAndGet();
                }
            } catch (IOException | InterruptedException e) {
                // conexao derrubada: encerra os dois lados para o outro sentido nao ficar preso
                try { de.close(); para.close(); } catch (IOException ignorada) { }
            } finally {
                fim.run();
            }
        });
        leitor.setDaemon(true);
        escritor.setDaemon(true);
        leitor.start();
        escritor.start();
    }

    // ======================================================================
    // UDP: cada cliente ganha um socket proprio do lado do servidor (como um NAT simples)
    // ======================================================================

    /** Fila de envios por instante de liberacao, com espera precisa (ver MARGEM_NS). */
    static class Agenda {
        record Tarefa(long quandoNs, long ordem, Runnable acao) {}
        private final PriorityQueue<Tarefa> fila = new PriorityQueue<>(
                Comparator.comparingLong(Tarefa::quandoNs).thenComparingLong(Tarefa::ordem));
        private long contador;

        Agenda() {
            Thread t = new Thread(this::executar);
            t.setDaemon(true);
            t.start();
        }

        synchronized void agendar(Runnable acao, long atrasoUs) {
            fila.add(new Tarefa(System.nanoTime() + atrasoUs * 1000, contador++, acao));
            notifyAll();
        }

        private void executar() {
            while (true) {
                Tarefa pronta = null;
                synchronized (this) {
                    Tarefa t = fila.peek();
                    try {
                        if (t == null) {
                            wait();
                            continue;
                        }
                        long resta = t.quandoNs() - System.nanoTime();
                        if (resta > MARGEM_NS) {
                            wait(Math.max(1, (resta - MARGEM_NS) / 1_000_000));
                            continue;
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (t.quandoNs() <= System.nanoTime()) pronta = fila.poll();
                }
                if (pronta != null) pronta.acao().run();
                else Thread.yield();
            }
        }
    }

    static void udp(int portaLocal, InetSocketAddress alvo, long atrasoMs, long jitterMs, double perda, Random r)
            throws IOException {
        Agenda agenda = new Agenda();
        Map<SocketAddress, DatagramSocket> saidas = new ConcurrentHashMap<>();
        try (DatagramSocket frente = new DatagramSocket(new InetSocketAddress("127.0.0.1", portaLocal))) {
            byte[] buf = new byte[65536];
            while (true) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                frente.receive(p);
                SocketAddress cliente = p.getSocketAddress();
                byte[] dados = Arrays.copyOf(p.getData(), p.getLength());
                DatagramSocket saida = saidas.computeIfAbsent(cliente, c -> novaSaida(frente, c, agenda, atrasoMs, jitterMs, perda, r));
                if (sortear(r, perda)) {
                    perdidosIda.incrementAndGet();
                    continue;
                }
                agenda.agendar(() -> {
                    try {
                        saida.send(new DatagramPacket(dados, dados.length, alvo));
                        ida.incrementAndGet();
                    } catch (IOException e) {
                        System.out.println("Falha ao encaminhar: " + e.getMessage());
                    }
                }, atraso(r, atrasoMs, jitterMs));
            }
        }
    }

    static DatagramSocket novaSaida(DatagramSocket frente, SocketAddress cliente, Agenda agenda,
                                    long atrasoMs, long jitterMs, double perda, Random r) {
        try {
            DatagramSocket saida = new DatagramSocket();
            Thread respostas = new Thread(() -> {
                byte[] buf = new byte[65536];
                while (!saida.isClosed()) {
                    try {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        saida.receive(p);
                        byte[] dados = Arrays.copyOf(p.getData(), p.getLength());
                        if (sortear(r, perda)) {
                            perdidosVolta.incrementAndGet();
                            continue;
                        }
                        agenda.agendar(() -> {
                            try {
                                frente.send(new DatagramPacket(dados, dados.length, cliente));
                                volta.incrementAndGet();
                            } catch (IOException e) {
                                System.out.println("Falha ao devolver: " + e.getMessage());
                            }
                        }, atraso(r, atrasoMs, jitterMs));
                    } catch (IOException e) {
                        break;
                    }
                }
            });
            respostas.setDaemon(true);
            respostas.start();
            return saida;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static synchronized boolean sortear(Random r, double probabilidade) {
        return probabilidade > 0 && r.nextDouble() < probabilidade;
    }

    // Atraso em microssegundos: base +- jitter (uniforme), nunca negativo
    static synchronized long atraso(Random r, long atrasoMs, long jitterMs) {
        double ms = atrasoMs + (jitterMs > 0 ? (r.nextDouble() * 2 - 1) * jitterMs : 0);
        return Math.max(0, Math.round(ms * 1000));
    }
}
