import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import com.sun.management.ThreadMXBean;

/**
 * Estrategia 1 - Forca bruta / busca linear (cliente-alvo: Embarca Ltda.).
 *
 * Leituras de sensor: inteiros de 16 bits, faixa fixa e conhecida 0..65535.
 * Tamanhos 10^3, 10^5 e 10^7; cinco rodadas por tamanho.
 *
 * Como a faixa NAO cresce com n, a regiao de unicos satura em 65536 e o laco
 * interno para de crescer: o custo e ~ n*k/2, nao n^2/2, para n grande.
 *
 * Medicoes por rodada:
 *   tempo_ms     - execucao SEM instrumentacao (System.nanoTime)
 *   heap_bytes   - bytes alocados pela thread durante a execucao medida (ThreadMXBean)
 *   operacoes    - comparacoes + escritas. Para 10^3 e 10^5 vem de uma execucao
 *                  instrumentada; para 10^7 vem da contagem analitica exata O(n),
 *                  validada contra a instrumentada nos tamanhos menores.
 *   pilha_frames - sempre 1 (o algoritmo e iterativo, nao recorre)
 *   pilha_bytes  - pilha_frames * 48 (mesma estimativa usada no Quicksort)
 *
 * Uso:   javac ForcaBruta.java
 *        java ForcaBruta            (n = 10^7 leva cerca de 1 min por rodada)
 * Saida: forca_bruta.csv (separador virgula, decimal com ponto)
 */
public class Estrategia2 {

    static final int FAIXA = 65_536;               // 0..65535, inteiros de 16 bits
    static final int RODADAS = 5;
    static final int BYTES_POR_FRAME = 48;
    static final int PILHA_FRAMES = 1;             // iterativo: so o frame de dedupLinear
    static final int[] TAMANHOS = {1_000, 100_000, 10_000_000};
    static final int LIMITE_INSTRUMENTADO = 100_000;   // acima disso, contagem analitica
    static final long LIMITE_EMBARCA_BYTES = 256;
    static final String ARQUIVO = "forca_bruta.csv";
    static final String CABECALHO =
            "estrategia,n,rodada,tempo_ms,pilha_frames,pilha_bytes,heap_bytes,unicos,operacoes";

    static long operacoes;

    // ---------------- Algoritmo (versao medida, sem contadores) ----------------

    /** Deixa os valores unicos em v[0..k-1], na ordem de chegada. Devolve k. */
    static int dedupLinear(int[] v) {
        int n = v.length, k = 0;
        for (int i = 0; i <= n - 1; i++) {
            boolean achou = false;
            for (int j = 0; j <= k - 1; j++) {            // percorre a lista de unicos
                if (v[j] == v[i]) { achou = true; break; }
            }
            if (!achou) { v[k] = v[i]; k++; }             // nova: guarda no fim da lista
        }
        return k;
    }

    // ---------------- Versao instrumentada ----------------

    static int dedupLinearInstr(int[] v) {
        int n = v.length, k = 0;
        for (int i = 0; i <= n - 1; i++) {
            boolean achou = false;
            for (int j = 0; j <= k - 1; j++) {
                operacoes++;                                    // comparacao
                if (v[j] == v[i]) { achou = true; break; }
            }
            if (!achou) { operacoes++; v[k] = v[i]; k++; }      // escrita
        }
        return k;
    }

    /**
     * Conta em O(n) EXATAMENTE as operacoes de dedupLinearInstr, sem rodar a varredura.
     * Uma tabela de 65536 posicoes guarda onde cada valor entrou na lista de unicos:
     *   valor novo     -> k comparacoes (varredura completa) + 1 escrita
     *   valor repetido -> pos + 1 comparacoes (para na primeira ocorrencia)
     * Usada so para contar; NAO faz parte do algoritmo medido. Retorna {operacoes, unicos}.
     */
    static long[] contarAnalitico(int[] v) {
        int[] primeiraPos = new int[FAIXA];
        Arrays.fill(primeiraPos, -1);
        long ops = 0;
        int k = 0;
        for (int x : v) {
            int pos = primeiraPos[x];
            if (pos == -1) { ops += k + 1; primeiraPos[x] = k; k++; }
            else           { ops += pos + 1; }
        }
        return new long[]{ops, k};
    }

    // ---------------- Apoio ----------------

    /** Leituras reprodutiveis, uniformes em 0..65535 (mesma semente do Quicksort). */
    static int[] gerarEntrada(int n, int rodada) {
        Random r = new Random(1000L * n + rodada);
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = r.nextInt(FAIXA);
        return v;
    }

    static double mediana(double[] xs) {
        double[] c = xs.clone();
        Arrays.sort(c);
        return c[c.length / 2];
    }

    // ---------------- Testes de corretude (n <= 10, conferiveis a mao) ----------------

    static void verificar(int[] entrada, int[] esperado) {
        int[] v = entrada.clone();
        int k = dedupLinear(v);
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
        verificar(new int[]{5, 3, 5, 1, 3}, new int[]{5, 3, 1});                  // exemplo do roteiro
        verificar(new int[]{65535, 0, 65535, 0}, new int[]{65535, 0});            // extremos da faixa
        verificar(new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1, 0},
                new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1, 0});                       // ordem inversa
        verificar(new int[]{2, 2, 1, 1, 2, 1, 2, 1, 2, 1}, new int[]{2, 1});      // muitas repeticoes

        // contagem de operacoes conferida a mao (ver simulacao do roteiro)
        operacoes = 0; dedupLinearInstr(new int[]{5, 3, 5, 1, 3});
        if (operacoes != 6 + 3) throw new AssertionError("[5,3,5,1,3]: esperado 9 ops, obtido " + operacoes);
        operacoes = 0; dedupLinearInstr(new int[]{7, 7, 7, 7, 7});
        if (operacoes != 4 + 1) throw new AssertionError("todos iguais: esperado 5 ops, obtido " + operacoes);
        operacoes = 0; dedupLinearInstr(new int[]{1, 2, 3, 4, 5});
        if (operacoes != 10 + 5) throw new AssertionError("todos distintos: esperado 15 ops, obtido " + operacoes);
        System.out.println("  contagem de operacoes: [5,3,5,1,3]=9, todos iguais=5, todos distintos=15");
    }

    // ---------------- Programa principal ----------------

    @SuppressWarnings("deprecation")   // getId() funciona em todas as versoes do Java
    public static void main(String[] args) throws Exception {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        long tid = Thread.currentThread().getId();

        System.out.println("Testes de corretude (n <= 10):");
        testesPequenos();
        System.out.println("OK: 8 casos + 3 contagens\n");

        for (int w = 0; w < 30; w++) dedupLinear(gerarEntrada(4_000, 99));   // aquecimento da JIT

        long maiorMemoria = 0;
        try (PrintWriter out = new PrintWriter(ARQUIVO)) {
            out.println(CABECALHO);
            for (int n : TAMANHOS) {
                double[] tempos = new double[RODADAS];
                for (int r = 1; r <= RODADAS; r++) {
                    int[] b = gerarEntrada(n, r);

                    long aloc0 = bean.getThreadAllocatedBytes(tid);
                    long t0 = System.nanoTime();
                    int k = dedupLinear(b);
                    long t1 = System.nanoTime();
                    long heap = bean.getThreadAllocatedBytes(tid) - aloc0;
                    b = null;

                    long[] analitico = contarAnalitico(gerarEntrada(n, r));
                    if (analitico[1] != k)
                        throw new AssertionError("k divergente: n=" + n + " r=" + r);

                    long ops;
                    if (n <= LIMITE_INSTRUMENTADO) {
                        operacoes = 0;
                        dedupLinearInstr(gerarEntrada(n, r));
                        ops = operacoes;
                        if (ops != analitico[0])
                            throw new AssertionError("contagem analitica divergente: n=" + n + " r=" + r);
                    } else {
                        ops = analitico[0];
                    }

                    long pilha = (long) PILHA_FRAMES * BYTES_POR_FRAME;
                    maiorMemoria = Math.max(maiorMemoria, pilha + heap);
                    double ms = (t1 - t0) / 1e6;
                    tempos[r - 1] = ms;

                    out.printf(Locale.US, "forca_bruta,%d,%d,%.3f,%d,%d,%d,%d,%d%n",
                            n, r, ms, PILHA_FRAMES, pilha, heap, k, ops);
                    out.flush();
                    System.out.printf(Locale.US,
                            "n=%-9d r=%d %11.3f ms  frames=%d pilha=%d B  heap=%d B  k=%-6d ops=%d%n",
                            n, r, ms, PILHA_FRAMES, pilha, heap, k, ops);
                }
                System.out.printf(Locale.US, "  mediana n=%d: %.3f ms%n%n", n, mediana(tempos));
            }
        }

        System.out.println("Contagem analitica validada contra a instrumentada em 10^3 e 10^5.");
        System.out.printf("Embarca (no maximo 256 B extras): maior pilha+heap = %d B -> %s%n",
                maiorMemoria, maiorMemoria <= LIMITE_EMBARCA_BYTES ? "ATENDE" : "NAO ATENDE");
        System.out.println("CSV gravado em " + ARQUIVO);
    }
}