import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import com.sun.management.ThreadMXBean;

/**
 * Estrategia 2 - Quicksort + varredura de vizinhos (cliente-alvo: Datacenter S.A.).
 *
 * Leituras de sensor: inteiros de 16 bits, faixa fixa e conhecida 0..65535.
 * Tamanhos 10^3, 10^5 e 10^7; cinco rodadas por tamanho.
 *
 * Medicoes por rodada:
 *   tempo_ms     - execucao SEM instrumentacao (System.nanoTime)
 *   heap_bytes   - bytes alocados pela thread durante a execucao medida (ThreadMXBean)
 *   operacoes    - comparacoes + escritas, contadas numa segunda execucao instrumentada
 *   pilha_frames - 1 (frame de dedupOrdenado) + profundidade maxima da recursao
 *   pilha_bytes  - pilha_frames * 48, onde 48 B/frame e uma ESTIMATIVA do layout de frame
 *                  do HotSpot x86-64. O valor medido e pilha_frames; os bytes sao derivados.
 *
 * Uso:   javac QuicksortVarredura.java
 *        java QuicksortVarredura
 * Saida: quicksort_varredura.csv (separador virgula, decimal com ponto)
 */
public class Estrategia1 {

    static final int FAIXA = 65_536;               // 0..65535, inteiros de 16 bits
    static final int RODADAS = 5;
    static final int BYTES_POR_FRAME = 48;
    static final int[] TAMANHOS = {1_000, 100_000, 10_000_000};
    static final double LIMITE_DATACENTER_MS = 2_000.0;
    static final String ARQUIVO = "quicksort_varredura.csv";
    static final String CABECALHO =
            "estrategia,n,rodada,tempo_ms,pilha_frames,pilha_bytes,heap_bytes,unicos,operacoes";

    static long operacoes;
    static int profundidade, profundidadeMax;

    // ---------------- Algoritmo (versao medida, sem contadores) ----------------

    /** Ordena v no lugar e deixa os valores unicos em v[0..k-1]. Devolve k. */
    static int dedupOrdenado(int[] v) {
        int n = v.length;
        if (n == 0) return 0;
        quicksort(v, 0, n - 1);
        int k = 1;
        for (int i = 1; i <= n - 1; i++) {
            if (v[i] != v[k - 1]) { v[k] = v[i]; k++; }   // diferente do ultimo unico: guarda
        }
        return k;
    }

    /** Quicksort com pivo do meio e particao de Hoare. */
    static void quicksort(int[] v, int ini, int fim) {
        if (ini >= fim) return;
        int p = v[ini + (fim - ini) / 2];
        int i = ini, j = fim;
        while (i <= j) {
            while (v[i] < p) i++;
            while (v[j] > p) j--;
            if (i <= j) { int t = v[i]; v[i] = v[j]; v[j] = t; i++; j--; }
        }
        quicksort(v, ini, j);
        quicksort(v, i, fim);
    }

    // ---------------- Versao instrumentada (operacoes e profundidade) ----------------

    static int dedupOrdenadoInstr(int[] v) {
        int n = v.length;
        if (n == 0) return 0;
        quicksortInstr(v, 0, n - 1);
        int k = 1;
        for (int i = 1; i <= n - 1; i++) {
            operacoes++;                                         // comparacao com o vizinho
            if (v[i] != v[k - 1]) { operacoes++; v[k] = v[i]; k++; }   // escrita
        }
        return k;
    }

    static void quicksortInstr(int[] v, int ini, int fim) {
        profundidade++;
        if (profundidade > profundidadeMax) profundidadeMax = profundidade;
        if (ini >= fim) { profundidade--; return; }
        int p = v[ini + (fim - ini) / 2];
        int i = ini, j = fim;
        while (i <= j) {
            while (true) { operacoes++; if (!(v[i] < p)) break; i++; }
            while (true) { operacoes++; if (!(v[j] > p)) break; j--; }
            if (i <= j) { operacoes += 2; int t = v[i]; v[i] = v[j]; v[j] = t; i++; j--; }
        }
        quicksortInstr(v, ini, j);
        quicksortInstr(v, i, fim);
        profundidade--;
    }

    // ---------------- Apoio ----------------

    /** Leituras reprodutiveis, uniformes em 0..65535 (mesma semente da forca bruta). */
    static int[] gerarEntrada(int n, int rodada) {
        Random r = new Random(1000L * n + rodada);
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = r.nextInt(FAIXA);
        return v;
    }

    /** Referencia independente para conferir k (usada fora da medicao). */
    static int contarDistintos(int[] v) {
        boolean[] visto = new boolean[FAIXA];
        int k = 0;
        for (int x : v) if (!visto[x]) { visto[x] = true; k++; }
        return k;
    }

    static double mediana(double[] xs) {
        double[] c = xs.clone();
        Arrays.sort(c);
        return c[c.length / 2];
    }

    // ---------------- Testes de corretude (n <= 10, conferiveis a mao) ----------------

    static void verificar(int[] entrada, int[] esperado) {
        int[] v = entrada.clone();
        int k = dedupOrdenado(v);
        int[] obtido = Arrays.copyOf(v, k);
        if (!Arrays.equals(obtido, esperado))
            throw new AssertionError("entrada " + Arrays.toString(entrada) + ": esperado "
                    + Arrays.toString(esperado) + ", obtido " + Arrays.toString(obtido));
        System.out.printf("  %-34s -> %s%n", Arrays.toString(entrada), Arrays.toString(obtido));
    }

    static void testesPequenos() {
        verificar(new int[]{}, new int[]{});                                      // vazio
        verificar(new int[]{42}, new int[]{42});                                  // 1 elemento
        verificar(new int[]{7, 7, 7, 7, 7}, new int[]{7});                        // todos iguais
        verificar(new int[]{1, 2, 3, 4, 5}, new int[]{1, 2, 3, 4, 5});            // sem duplicatas
        verificar(new int[]{5, 3, 5, 1, 3}, new int[]{1, 3, 5});                  // exemplo do roteiro
        verificar(new int[]{65535, 0, 65535, 0}, new int[]{0, 65535});            // extremos da faixa
        verificar(new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1, 0},
                new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});                       // ordem inversa
        verificar(new int[]{2, 2, 1, 1, 2, 1, 2, 1, 2, 1}, new int[]{1, 2});      // muitas repeticoes
    }

    // ---------------- Programa principal ----------------

    @SuppressWarnings("deprecation")   // getId() funciona em todas as versoes do Java
    public static void main(String[] args) throws Exception {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        long tid = Thread.currentThread().getId();

        System.out.println("Testes de corretude (n <= 10):");
        testesPequenos();
        System.out.println("OK: 8 casos\n");

        for (int w = 0; w < 30; w++) dedupOrdenado(gerarEntrada(10_000, 99));   // aquecimento da JIT

        double medianaMaior = 0;
        try (PrintWriter out = new PrintWriter(ARQUIVO)) {
            out.println(CABECALHO);
            for (int n : TAMANHOS) {
                double[] tempos = new double[RODADAS];
                for (int r = 1; r <= RODADAS; r++) {
                    int[] a = gerarEntrada(n, r);
                    int kReferencia = contarDistintos(a);

                    long aloc0 = bean.getThreadAllocatedBytes(tid);
                    long t0 = System.nanoTime();
                    int k = dedupOrdenado(a);
                    long t1 = System.nanoTime();
                    long heap = bean.getThreadAllocatedBytes(tid) - aloc0;
                    a = null;

                    if (k != kReferencia)
                        throw new AssertionError("k divergente: n=" + n + " r=" + r);

                    operacoes = 0; profundidade = 0; profundidadeMax = 0;
                    dedupOrdenadoInstr(gerarEntrada(n, r));
                    int frames = profundidadeMax + 1;              // + frame de dedupOrdenado
                    long pilha = (long) frames * BYTES_POR_FRAME;
                    double ms = (t1 - t0) / 1e6;
                    tempos[r - 1] = ms;

                    out.printf(Locale.US, "quicksort_varredura,%d,%d,%.3f,%d,%d,%d,%d,%d%n",
                            n, r, ms, frames, pilha, heap, k, operacoes);
                    out.flush();
                    System.out.printf(Locale.US,
                            "n=%-9d r=%d %10.3f ms  frames=%-3d pilha=%-5d B  heap=%d B  k=%-6d ops=%d%n",
                            n, r, ms, frames, pilha, heap, k, operacoes);
                }
                double med = mediana(tempos);
                medianaMaior = med;
                System.out.printf(Locale.US, "  mediana n=%d: %.3f ms%n%n", n, med);
            }
        }

        System.out.printf(Locale.US, "Datacenter (n = 10^7 em menos de 2 s): mediana %.1f ms -> %s%n",
                medianaMaior, medianaMaior < LIMITE_DATACENTER_MS ? "ATENDE" : "NAO ATENDE");
        System.out.println("CSV gravado em " + ARQUIVO);
    }
}