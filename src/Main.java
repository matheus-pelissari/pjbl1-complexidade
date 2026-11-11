import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;
import com.sun.management.ThreadMXBean;

/**
 * Rotina de demonstracao (secao 6 do enunciado).
 *
 * Executa as duas estrategias implementadas nos tres tamanhos de entrada
 * (10^3, 10^5 e 10^7) e imprime tempo e memoria medidos:
 *   Estrategia2.java - forca bruta            (cliente Embarca Ltda.)
 *   Estrategia1.java - Quicksort + varredura  (cliente Datacenter S.A.)
 *
 * Aqui e uma execucao por tamanho, so para demonstrar. O benchmark completo
 * (5 rodadas, mediana, CSV) continua no main de cada estrategia.
 *
 * Medicoes:
 *   tempo  - System.nanoTime em volta da versao sem contadores
 *   heap   - bytes alocados pela thread durante a execucao (ThreadMXBean)
 *   pilha  - frames * 48 B. O numero de frames e medido; 48 B por frame e estimativa.
 *
 * Uso:  javac -d out src/*.java
 *       java -cp out Main        (a forca bruta com n = 10^7 leva cerca de 1 min)
 */
public class Main {

    static final int[] TAMANHOS = {1_000, 100_000, 10_000_000};
    static final int BYTES_POR_FRAME = 48;
    static final long LIMITE_EMBARCA_BYTES = 256;
    static final double LIMITE_DATACENTER_MS = 2_000.0;

    static final ThreadMXBean BEAN = (ThreadMXBean) ManagementFactory.getThreadMXBean();

    @SuppressWarnings("deprecation")   // getId() funciona em todas as versoes do Java
    static final long TID = Thread.currentThread().getId();

    public static void main(String[] args) {
        System.out.println("Entradas de borda (nao podem gerar erro):");
        borda("vazia", new int[]{});
        borda("sem duplicatas", new int[]{1, 2, 3, 4, 5});
        borda("com duplicatas", new int[]{5, 3, 5, 1, 3});
        System.out.println();

        // aquecimento da JIT, igual ao dos benchmarks
        for (int w = 0; w < 30; w++) {
            Estrategia2.dedupLinear(Estrategia2.gerarEntrada(4_000, 99));
            Estrategia1.dedupOrdenado(Estrategia1.gerarEntrada(10_000, 99));
        }

        System.out.printf("%-22s %10s %14s %8s %10s %8s %12s %8s%n",
                "estrategia", "n", "tempo_ms", "frames", "pilha_B", "heap_B", "mem_extra_B", "unicos");

        long maiorMemoriaForcaBruta = 0;
        double tempoQuicksortMaiorN = 0;

        for (int n : TAMANHOS) {
            // ---- Estrategia 1 do catalogo: forca bruta (iterativa, 1 frame) ----
            int[] a = Estrategia2.gerarEntrada(n, 1);
            long aloc0 = BEAN.getThreadAllocatedBytes(TID);
            long t0 = System.nanoTime();
            int k = Estrategia2.dedupLinear(a);
            long t1 = System.nanoTime();
            long heap = BEAN.getThreadAllocatedBytes(TID) - aloc0;
            long memoria = imprimir("forca_bruta", n, (t1 - t0) / 1e6, 1, heap, k);
            maiorMemoriaForcaBruta = Math.max(maiorMemoriaForcaBruta, memoria);

            // ---- Estrategia 2 do catalogo: Quicksort + varredura (recursiva) ----
            int[] b = Estrategia1.gerarEntrada(n, 1);
            aloc0 = BEAN.getThreadAllocatedBytes(TID);
            t0 = System.nanoTime();
            k = Estrategia1.dedupOrdenado(b);
            t1 = System.nanoTime();
            heap = BEAN.getThreadAllocatedBytes(TID) - aloc0;

            // profundidade da recursao: segunda execucao, instrumentada, na mesma entrada
            Estrategia1.operacoes = 0;
            Estrategia1.profundidade = 0;
            Estrategia1.profundidadeMax = 0;
            Estrategia1.dedupOrdenadoInstr(Estrategia1.gerarEntrada(n, 1));
            int frames = Estrategia1.profundidadeMax + 1;      // + frame de dedupOrdenado

            double ms = (t1 - t0) / 1e6;
            imprimir("quicksort_varredura", n, ms, frames, heap, k);
            tempoQuicksortMaiorN = ms;
        }

        System.out.println();
        System.out.printf(Locale.US, "Embarca (no maximo %d B extras): forca bruta usou no maximo %d B -> %s%n",
                LIMITE_EMBARCA_BYTES, maiorMemoriaForcaBruta,
                maiorMemoriaForcaBruta <= LIMITE_EMBARCA_BYTES ? "ATENDE" : "NAO ATENDE");
        System.out.printf(Locale.US, "Datacenter (n = 10^7 em menos de 2 s): Quicksort levou %.1f ms -> %s%n",
                tempoQuicksortMaiorN,
                tempoQuicksortMaiorN < LIMITE_DATACENTER_MS ? "ATENDE" : "NAO ATENDE");
    }

    /** Imprime uma linha da tabela e devolve a memoria extra (pilha + heap) em bytes. */
    static long imprimir(String estrategia, int n, double ms, int frames, long heap, int unicos) {
        long pilha = (long) frames * BYTES_POR_FRAME;
        System.out.printf(Locale.US, "%-22s %10d %14.3f %8d %10d %8d %12d %8d%n",
                estrategia, n, ms, frames, pilha, heap, pilha + heap, unicos);
        return pilha + heap;
    }

    /** Roda as duas estrategias numa entrada pequena e mostra os unicos de cada uma. */
    static void borda(String nome, int[] entrada) {
        int[] a = entrada.clone();
        int[] b = entrada.clone();
        int ka = Estrategia2.dedupLinear(a);
        int kb = Estrategia1.dedupOrdenado(b);
        System.out.printf("  %-15s %-16s -> forca bruta %-16s | quicksort %s%n", nome,
                Arrays.toString(entrada),
                Arrays.toString(Arrays.copyOf(a, ka)),
                Arrays.toString(Arrays.copyOf(b, kb)));
    }
}
