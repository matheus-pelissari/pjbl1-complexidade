# Análise teórica das 10 estratégias

## Convenções e decisões

- **Linguagem:** Java (JDK 17). A seção 6 do enunciado deixa a linguagem à escolha da equipe.
- **Previsto e medido:** cada estratégia traz a previsão teórica. Nas duas implementadas (1 e 2), as linhas **Medido** trazem a mediana de 5 rodadas de `dados/resultados_empiricos.csv`, e as diferenças em relação à previsão ficam registradas, não apagadas.
- **Custo por operação:** as previsões iniciais usavam ~1 ns por operação. As medições com n = 10⁷ deram duas réguas: c_A = 1,94 ns (Quicksort: 844 ms ÷ 4,36×10⁸ operações), para acesso espalhado, recursão e trocas; e c_B = 0,18 ns (força bruta: 58,0 s ÷ 3,27×10¹¹ operações), para varredura sequencial. Os tempos das estratégias não implementadas usam operações previstas × régua.
- **Pilha:** as linhas "Espaço: pilha" listam as variáveis locais. Nas medições e nas tabelas, cada frame conta 48 B (estimativa do tamanho de um frame; o que se mede é o número de frames).
- **Operações:** comparações + escritas.
- **Máquina:** os tempos medidos vêm de uma execução fora da máquina de referência da disciplina e precisam ser repetidos nela. Contagens de operações, frames e bytes não dependem da máquina.

## 1. Força bruta

Exata, preserva a ordem e usa 48 B (1 frame), mas leva cerca de 1 min com n = 10⁷: serve só para a Embarca.

```
k ← 0
para i de 0 até n−1:
    achou ← falso
    para j de 0 até k−1:
        se v[j] = v[i]: achou ← verdadeiro; interrompe
    se não achou: v[k] ← v[i]; k ← k + 1
retorna k        // v[0..k−1] = únicos, na ordem de chegada
```

**Ideia:** Mantém os únicos no início do próprio vetor. Cada leitura é comparada com os únicos já guardados e só entra se não estiver lá.

**Tempo: melhor:** Todos iguais: 1 comparação por leitura → n − 1 → Θ(n).

**Tempo: médio:** Entrada uniforme: k chega a ~65.536 após ~7×10⁵ leituras (coletor de cupons). Cada repetida é achada, em média, após k/2 comparações → ≈ n·k/2.

**Tempo: pior:** Todos distintos (só possível com n ≤ 65.536): 0 + 1 + … + (n−1) = n(n−1)/2 → Θ(n²). Com n > 65.536, k ≤ 65.536 limita a n·65.536 → O(n·k).

**Espaço: pilha:** i, j, k (3 int = 12 B) + achou (1 B) ≈ 13 B de variáveis locais. Sem recursão: 1 frame = 48 B pela convenção.

**Espaço: heap:** 0 B (reaproveita o vetor de entrada).

**Outros recursos:** Nenhum.

**Embarca:** **Aceitável**: 48 B de 256 B (≈ 19%), iguais para n = 10³, 10⁵ e 10⁷. Leva cerca de 1 min com n = 10⁷, mas a Embarca não tem limite de tempo.

**Datacenter:** **Inaceitável**: n = 10⁷ → ≈ 10⁷ × 32.768 ≈ 3,3×10¹¹ comparações. A previsão inicial, com 1 ns por comparação, era ≈ 5,5 min; o medido foi 58,0 s, 29× acima de 2 s.

**Medido:** n = 10³: 89,0 µs e 4,96×10⁵ operações; n = 10⁵: 406,2 ms e 2,26×10⁹; n = 10⁷: 58,0 s e 3,27×10¹¹. Pilha de 48 B (1 frame) e heap de 0 B em todas as rodadas. A contagem de operações confere com a previsão (diferença menor que 1%). O tempo saiu 5,6× menor porque cada operação custou 0,18 ns, não 1 ns: o laço interno percorre em sequência uma lista de no máximo 65.536 inteiros (256 KB), que cabe na cache, e o acesso é previsível.

**Correção:** Exata; preserva a ordem de chegada.

**Quando o veredito mudaria:** Com poucos distintos (k ≤ ~2.200), n·k/2 ≈ 1,1×10¹⁰ operações × 0,18 ns ≈ 2 s e a Datacenter passaria a aceitar.

## 2. Ordenação (Quicksort) + varredura de vizinhos

Com Quicksort, é exata e rápida para a Datacenter (0,84 s medidos com n = 10⁷), mas a recursão ocupa 2,5 KB de pilha, cerca de 10× o limite da Embarca.

```
se n = 0: retorna 0
quicksort(v, 0, n−1)
k ← 1
para i de 1 até n−1:
    se v[i] ≠ v[k−1]: v[k] ← v[i]; k ← k + 1
retorna k

quicksort(v, ini, fim):              // recursivo
    se ini ≥ fim: retorna
    p ← v[(ini + fim) / 2]           // pivô do meio
    i ← ini; j ← fim
    enquanto i ≤ j:                  // partição de Hoare
        enquanto v[i] < p: i ← i + 1
        enquanto v[j] > p: j ← j − 1
        se i ≤ j: troca(v[i], v[j]); i ← i + 1; j ← j − 1
    quicksort(v, ini, j)
    quicksort(v, i, fim)
```

**Ideia:** Depois de ordenar, as cópias de um mesmo valor ficam vizinhas. Uma varredura linear mantém só a primeira de cada grupo. O Quicksort ordena escolhendo um pivô, separando menores à esquerda e maiores à direita, e repetindo em cada lado.

**Tempo: melhor:** O pivô divide sempre ao meio: T(n) = 2T(n/2) + n → n·log₂ n → Θ(n log n). Com a partição de Hoare, até "todos iguais" divide ao meio. Varredura: n−1.

**Tempo: médio:** Entrada aleatória: ≈ 1,39·n·log₂ n comparações → Θ(n log n). Com n = 10⁷: ≈ 3,2×10⁸ comparações.

**Tempo: pior:** O pivô é sempre o menor ou o maior valor: as partes ficam com n−1 e 0 elementos → n + (n−1) + … + 1 ≈ n²/2 → Θ(n²). Com pivô do meio isso exige uma entrada construída de propósito. Com n = 10⁷ seriam ≈ 5×10¹³ comparações (~27 h a 1,94 ns por operação).

**Espaço: pilha:** Cada chamada guarda ini, fim, p, i, j e a referência ao vetor: ≈ 40 B por nível. Melhor caso: log₂ n níveis → n = 10³: ~10 níveis ≈ 400 B; n = 10⁷: ~24 níveis ≈ 1 KB. Na prática, algumas dezenas de níveis. Pior caso: n níveis → n = 10⁷: ≈ 400 MB → `StackOverflowError`. Medido, a 48 B por frame: 20 a 27 frames com n = 10³ (≈ 1,2 KB), 38 a 47 com n = 10⁵ (≈ 2,1 KB) e 49 a 53 com n = 10⁷ (≈ 2,5 KB). A recursão ficou cerca de 2× mais funda que log₂ n, porque o pivô do meio não divide os dados aleatórios exatamente ao meio.

**Espaço: heap:** 0 B (ordena dentro do próprio vetor).

**Outros recursos:** Nenhum.

**Embarca:** **Inaceitável**: mesmo no melhor caso previsto a pilha passa de 256 B já com n = 10³ (~400 B). O medido foi 1,2 KB com n = 10³ e 2,5 KB com n = 10⁷, cerca de 10× acima. Em 256 B cabem 5 frames de 48 B, o que só é garantido com n ≤ 4: com n = 5 a recursão já pode chegar a 6 frames (288 B).

**Datacenter:** **Aceitável**: ≈ 3,2×10⁸ comparações com acesso sequencial à memória (bom para o cache) → previsão de ≈ 0,3–1 s para n = 10⁷. Medido: 844 ms de mediana (717 a 915 ms nas 5 rodadas), folga de 2,4× sobre o limite.

**Medido:** n = 10³: 85,0 µs e 2,04×10⁴ operações; n = 10⁵: 9,3 ms e 3,22×10⁶; n = 10⁷: 844,0 ms e 4,36×10⁸. Heap de 0 B em todas as rodadas. As operações (comparações + escritas) ficaram entre 1,9 e 2,0·n·log₂ n nos três tamanhos, compatível com a previsão de 1,39·n·log₂ n só de comparações.

**Correção:** Exata. Não preserva a ordem de chegada (a saída sai ordenada).

**Quando o veredito mudaria:** Recursão só no lado menor (e laço no maior) garante no máximo log₂ n níveis, mas ainda ≈ 1,2 KB (24 frames de 48 B). Uma partição em 3 vias aproveita as ~150 cópias de cada valor e faz "todos iguais" virar Θ(n). Para a Embarca, só trocando por um sort sem recursão (ex.: Heapsort).

## 3. Tabela hash / conjunto

É rápida e exata para a Datacenter (Θ(n) em média), mas usa no mínimo ~512 KB, cerca de 2.000× o limite da Embarca.

```
T ← tabela hash vazia (capacidade m)
k ← 0
para i de 0 até n−1:
    se v[i] ∉ T: insere v[i] em T; v[k] ← v[i]; k ← k + 1
retorna k
```

**Ideia:** Conjunto com busca e inserção em tempo médio constante. Cada leitura consulta o conjunto antes de ser mantida.

**Tempo: melhor:** Todos iguais: n buscas O(1) → Θ(n).

**Tempo: médio:** Hash uniforme e fator de carga α constante: cada operação é O(1) esperado → Θ(n). Com n = 10⁷: ~10⁷ buscas.

**Tempo: pior:** Todas as chaves no mesmo balde: cada busca percorre até k itens → O(n·k) ≤ n·65.536 (sem domínio limitado seria O(n²)). No Java 8+, o `HashMap` transforma baldes com 8 ou mais itens em árvore → pior caso O(n log k).

**Espaço: pilha:** ≈ 12–20 B (i, k, índice do balde). Sem recursão: 1 frame = 48 B pela convenção.

**Espaço: heap:** Endereçamento aberto com `int[2¹⁷]` (α ≤ 0,5): 512 KB. `HashSet<Integer>` em Java: ~48 B por elemento (nó 32 B + Integer 16 B) → ≈ 3 MB + ~0,5 MB de tabela para k = 65.536.

**Outros recursos:** Nenhum.

**Embarca:** **Inaceitável**: ≥ 512 KB contra 256 B, cerca de 2.000× acima.

**Datacenter:** **Aceitável**: ≈ 0,05–0,5 s para n = 10⁷. O boxing do `HashSet<Integer>` deixa tudo ~5–10× mais lento que uma tabela de `int`. A curva extrapolada do gráfico usa o piso de 1 operação por leitura × c_A ≈ 19 ms (a mesma estimativa do bitmap); o custo real inclui cálculo do hash, sondagem e boxing, por isso a faixa aqui é mais alta. O veredito é o mesmo.

**Correção:** Exata; preserva a ordem de chegada.

**Quando o veredito mudaria:** Com a função h(x) = x e uma tabela de 65.536 posições não existe colisão: isso é exatamente o bitmap (estratégia 5). O hash só ganha quando o domínio é grande demais para um bitmap.

## 4. Árvore binária balanceada (AVL / Rubro-Negra)

Garante Θ(n log k) em qualquer entrada, mas usa ~2,6 MB e fica limítrofe nos 2 s da Datacenter por causa das falhas de cache.

```
A ← árvore balanceada vazia
k ← 0
para i de 0 até n−1:
    se não busca(A, v[i]): insere(A, v[i]); v[k] ← v[i]; k ← k + 1
retorna k
```

**Ideia:** Árvore de busca que se rebalanceia sozinha, usada como conjunto. Busca e inserção em O(log k) garantido, sem depender de uma boa função de hash.

**Tempo: melhor:** Todos iguais: árvore com 1 nó → Θ(n).

**Tempo: médio e pior:** Altura ≤ 1,44·log₂(k+2) na AVL e ≤ 2·log₂(k+1) na Rubro-Negra. Com k ≤ 65.536: no máximo ~23 (AVL) ou ~32 (RN) níveis → Θ(n log k), garantido. Com n = 10⁷: ≈ 1,6–2,3×10⁸ comparações.

**Espaço: pilha:** Inserção iterativa: O(1), ≈ 20 B. Inserção recursiva (comum na AVL): até ~23 níveis × ~40 B ≈ 1 KB.

**Espaço: heap:** k nós × ~40 B (chave, 2 ponteiros, altura ou cor, cabeçalho do objeto) ≈ 2,6 MB. `TreeSet<Integer>` em Java: ~56 B por nó → ≈ 3,7 MB.

**Outros recursos:** Nenhum.

**Embarca:** **Inaceitável**: ≈ 2,6 MB contra 256 B, cerca de 10.000× acima. Em 256 B cabem uns 6 nós.

**Datacenter:** **Limítrofe**: ~2×10⁸ comparações, cada uma saltando para um nó em outro lugar da memória → ≈ 0,5–3 s. Medir antes de decidir. A curva extrapolada (1,6×10⁸ operações × c_A) dá ≈ 310 ms, mas c_A vem do Quicksort, que percorre a memória quase em sequência; na árvore cada comparação tende a custar mais. A curva é otimista e o veredito continua limítrofe.

**Correção:** Exata; preserva a ordem de chegada.

**Quando o veredito mudaria:** Se o domínio não fosse limitado e a entrada pudesse ser adversária (alguém forçando colisões no hash), a garantia de pior caso faria dela a escolha da Datacenter.

## 5. Bitmap sobre o domínio conhecido

É a melhor opção para a Datacenter (Θ(n), ~10–30 ms com n = 10⁷), mas os 8.192 B fixos são 32× o limite da Embarca.

```
B ← vetor de 1.024 longs (65.536 bits), zerado
k ← 0
para i de 0 até n−1:
    x ← v[i]; p ← x >>> 6; b ← x & 63     // palavra e bit
    se bit b de B[p] = 0:
        liga o bit b de B[p]; v[k] ← x; k ← k + 1
retorna k
```

**Ideia:** Um bit para cada valor possível do domínio: o bit x ligado significa "x já apareceu". Só funciona porque o domínio é fixo e pequeno (2¹⁶ valores).

**Tempo: melhor, médio e pior:** Exatamente n testes de bit, mais 1.024 palavras para zerar → Θ(n + U), que é Θ(n) quando n ≥ U. O tempo não depende dos valores. Com n = 10⁷: 10⁷ operações ≈ 10–30 ms.

**Espaço: pilha:** i, k, x, p, b ≈ 20 B. Sem recursão: 1 frame = 48 B pela convenção.

**Espaço: heap:** 65.536 bits = 8.192 B fixos, independentes de n: O(1) em relação a n, mas Θ(U) em relação ao domínio.

**Outros recursos:** Nenhum.

**Embarca:** **Inaceitável**: 8.192 B contra 256 B, 32× acima. Com 256 B = 2.048 bits, só caberia um domínio de 11 bits (0 a 2.047).

**Datacenter:** **Aceitável, a melhor opção**: ~100× abaixo do orçamento (extrapolado: 10⁷ operações × c_A ≈ 19 ms). Os 8 KB cabem no cache L1 (32–48 KB típicos), então quase não há falha de cache. Um `boolean[65536]` em Java ocuparia 64 KB (8× mais) e sairia do L1.

**Correção:** Exata; preserva a ordem de chegada.

**Quando o veredito mudaria:** Com leituras de 32 bits, o bitmap passaria a 2³² bits = 512 MB: ainda viável na Datacenter, mas hash e ordenação passariam a competir. Com 64 bits, seria impossível.

## 6. Filtro de Bloom

Na Datacenter funciona, mas perde para o bitmap em tudo: 78,5 KB e erro de 1% contra 8 KB exatos. Na Embarca, 256 B só aguentam ~213 valores distintos com erro de 1%.

```
F ← m bits zerados; funções de hash g₁ … gₕ
k ← 0
para i de 0 até n−1:
    se algum bit F[gⱼ(v[i])] = 0:         // certamente nova
        liga os h bits; v[k] ← v[i]; k ← k + 1
    senão: descarta v[i]                  // provavelmente repetida
retorna k
```

Com k valores distintos inseridos, m bits e h funções de hash, a taxa de falso positivo p, o tamanho m para um p desejado e o h ideal são:

$$
p \approx \left(1 - e^{-hk/m}\right)^{h} \qquad m = -\frac{k \ln p}{(\ln 2)^2} \qquad h = \frac{m}{k}\ln 2
$$

**Ideia:** Um bitmap "comprimido": cada valor liga h bits escolhidos por hash. Nunca deixa passar uma duplicata (sem falso negativo), mas pode descartar uma leitura nova (falso positivo).

**Tempo: melhor, médio e pior:** h hashes por leitura em qualquer caso → Θ(n·h). Com h = 7 e n = 10⁷: 7×10⁷ operações ≈ 0,1–0,3 s.

**Espaço: pilha:** ≈ 20–30 B. Sem recursão: 1 frame = 48 B pela convenção.

**Espaço: heap:** m bits, escolhidos pelo grupo. Para p = 1%: ~9,6 bits por valor distinto → k = 65.536 exige ≈ 628 mil bits ≈ 78,5 KB.

**Outros recursos:** Nenhum.

**Embarca:** **Inaceitável**: 256 B = 2.048 bits garantem p ≤ 1% só até ~213 distintos. Com 65.536 distintos e h = 1, p ≈ 1 − e⁻³² ≈ 100%: descarta quase toda leitura nova.

**Datacenter:** **Aceitável, mas dominado pelo bitmap**: 78,5 KB e 7 hashes por leitura contra 8 KB e 1 acesso, e ainda com erro em vez de resultado exato (extrapolado: 7×10⁷ operações × c_A ≈ 136 ms).

**Correção:** Probabilística. Um falso positivo descarta uma leitura válida (perda de dado). A taxa sobe conforme o filtro enche. Se implementado, o código precisa expor m (ou p) e relatar a taxa observada.

**Quando o veredito mudaria:** Com um domínio enorme (ex.: identificadores de 64 bits) e muitos distintos, o bitmap é impossível e o Bloom vira a opção compacta.

## 7. Bogosort + varredura

Cabe em 48 B (1 frame), mas com n = 1000 precisaria em média de ≈ 4×10²⁵⁶⁷ embaralhamentos, cerca de 10²⁵⁵⁴ anos: inviável para os dois clientes.

```
enquanto não ordenado(v):
    embaralha(v)                          // Fisher–Yates, in-place
k ← varredura de vizinhos (igual à estratégia 2)
retorna k
```

**Ideia:** Embaralha o vetor até ele sair ordenado por acaso e então aplica a varredura de vizinhos da estratégia 2.

**Tempo: melhor:** Vetor já ordenado: uma verificação → Θ(n).

**Tempo: médio:** Se o valor i aparece mᵢ vezes, cada embaralhamento acerta com probabilidade (∏ mᵢ!)/n!. Número esperado de tentativas = n!/∏ mᵢ!, cada uma custando Θ(n) → Θ(n · n!/∏ mᵢ!). Todos distintos: Θ(n · n!).

**Tempo: pior:** Ilimitado: nada garante que termine (algoritmo de Las Vegas).

**Espaço: pilha:** ≈ 16–24 B. Sem recursão: 1 frame = 48 B pela convenção.

**Espaço: heap:** 0 B.

**Outros recursos:** Gerador de números aleatórios.

**Embarca:** **Inaceitável**: a memória cabe, mas com n = 10³ são 1000! ≈ 4,0×10²⁵⁶⁷ tentativas × 1.000 operações × 1,94 ns ≈ 7,8×10²⁵⁶¹ s ≈ 2,5×10²⁵⁵⁴ anos. Nunca termina na prática.

**Datacenter:** **Inaceitável**: com n = 10⁷ e ~150 cópias de cada valor, n!/∏ mᵢ! tem ordem de 65.536ⁿ ≈ 10^(4,8×10⁷) tentativas.

**Correção:** Exata, se terminar.

**Quando o veredito mudaria:** n ≤ 11: 11 × 11! × 1,94 ns ≈ 0,85 s cabe nos 2 s da Datacenter; com n = 12 já são ≈ 11 s. Ou entrada com quase todos os valores iguais, em que ∏ mᵢ! é enorme e poucas tentativas bastam.

## 8. Enumeração recursiva de subconjuntos (power set)

Com n = 1000 já são 2¹⁰⁰⁰ ≈ 10³⁰¹ subconjuntos e ≈ 56 KB de memória extra (48 KB de pilha + 8 KB de heap). Com n = 10⁷ a recursão estoura a pilha antes de terminar o primeiro ramo.

```
subconj(i, S):                            // recursivo
    se i = n:
        se S não tem repetidos e tamanho(S) > tamanho(melhor):
            melhor ← cópia de S
        retorna
    subconj(i+1, S)                       // sem v[i]
    subconj(i+1, S + v[i])                // com v[i]

subconj(0, vazio); retorna melhor         // maior subconjunto sem repetição = os únicos
```

**Ideia:** Gera todos os 2ⁿ subconjuntos e escolhe o maior sem valores repetidos. Ele contém exatamente um representante de cada valor.

**Tempo: melhor, médio e pior:** Sempre gera todos. Recorrência T(n) = 2T(n−1) + O(1) → Θ(2ⁿ) chamadas; verificar cada folha custa O(n) → Θ(n·2ⁿ). Com n = 10³: 2¹⁰⁰⁰ ≈ 1,07×10³⁰¹ subconjuntos.

**Espaço: pilha:** Profundidade n, com 48 B por frame: n + 1 frames. n = 10³: ≈ 48 KB. n = 10⁷: ≈ 480 MB → `StackOverflowError` (a pilha padrão de uma thread Java, ~1 MB, aguenta cerca de 2×10⁴ níveis de 48 B).

**Espaço: heap:** S e melhor: O(n) cada. Como dois `int[]` de n posições, ≈ 8n B: 8 KB com n = 10³ e 80 MB com n = 10⁷. Se guardar todos os subconjuntos: Θ(n·2ⁿ).

**Outros recursos:** Nenhum.

**Embarca:** **Inaceitável**: com n = 10³ são ≈ 56 KB (48 KB de pilha + 8 KB de heap) contra 256 B (≈ 219×), e o tempo é astronômico. A memória extra, 48 × (n + 1) + 8n bytes, só cabe em 256 B com n ≤ 3.

**Datacenter:** **Inaceitável**: 10³⁰¹ subconjuntos com n = 10³; com n = 10⁷ nem chega a rodar (estouro de pilha).

**Correção:** Exata.

**Quando o veredito mudaria:** n ≤ 25: 25 × 2²⁵ × 1,94 ns ≈ 1,6 s cabe nos 2 s da Datacenter; com n = 26 já são ≈ 3,4 s. Ou quando o problema de fato pedir subconjuntos (ex.: escolher o melhor grupo de sensores).

## 9. Codificação unária em disco

Cumpre os 256 B de RAM empurrando o custo para o disco: com n = 10⁷ são 41 a 328 GB gravados e de 82 s a 11 min só de escrita.

```
para i de 0 até n−1:
    grava no disco v[i] símbolos '1' seguidos de um '0'

// a eliminação compara as leituras pelo comprimento das sequências,
// lendo-as de volta do disco; a RAM guarda só contadores
```

**Ideia:** Representa cada leitura em unário (o valor x vira x símbolos) no disco e mantém em RAM só contadores. O catálogo não fixa a etapa de comparação, então esta análise usa só o custo mínimo de gravar a codificação, que já basta para rejeitá-la.

**Tempo: melhor:** Todos os valores 0: só n separadores → Θ(n).

**Tempo: médio:** Valores uniformes, média 32.767,5: Σvᵢ ≈ 32.767,5·n símbolos → Θ(Σvᵢ). É pseudo-polinomial: depende dos valores, não só de n. A comparação só aumenta esse custo.

**Tempo: pior:** Todos os valores 65.535: 65.535·n símbolos.

**Espaço: pilha:** ≈ 16–32 B de contadores. Sem recursão: 1 frame = 48 B pela convenção.

**Espaço: heap:** 0 B em RAM → O(1).

**Outros recursos:** Disco: n = 10³ → ≈ 3,3×10⁷ símbolos (~4 MB em bits). n = 10⁷ → ≈ 3,3×10¹¹ símbolos: 41 GB em bits ou 328 GB em bytes.

**Embarca:** **Inaceitável**: cumpre os 256 B de RAM, mas viola a premissa. Um sensor não tem dezenas de GB de armazenamento, e tanta escrita desgasta a memória flash. O custo foi só escondido em outro recurso.

**Datacenter:** **Inaceitável**: só gravar a 500 MB/s leva ≈ 82 s (41 GB) a ≈ 11 min (328 GB), de 40× a 330× acima de 2 s.

**Correção:** Exata.

**Quando o veredito mudaria:** Valores muito pequenos (ex.: 0 a 15) num dispositivo com RAM praticamente nula e disco sobrando.

## 10. Matriz de adjacência n×n de auditoria

Gasta n² bits que ninguém pediu: 125 KB com n = 10³ (≈ 488× o limite da Embarca) e 12,5 TB com n = 10⁷, além de ≈ 4,9 h de cálculo.

```
M ← matriz n×n de bits
para i de 0 até n−1:
    para j de 0 até n−1:
        M[i][j] ← (v[i] = v[j])

k ← 0
para i de 0 até n−1:
    se não existe j < i com M[i][j]: v[k] ← v[i]; k ← k + 1
retorna k
```

**Ideia:** Registra o resultado de todas as comparações entre pares numa matriz de "auditoria" e depois decide quem é duplicata consultando essa matriz.

**Tempo: melhor, médio e pior:** Preenche a matriz inteira para qualquer entrada, inclusive com todos iguais: n² comparações → Θ(n²). Com n = 10⁷: 10¹⁴ operações × c_B (0,18 ns, varredura sequencial) ≈ 1,8×10⁴ s ≈ 4,9 h. A previsão inicial, com 1 ns por operação, era ≈ 28 h; a contagem é a mesma.

**Espaço: pilha:** ≈ 16 B. Sem recursão: 1 frame = 48 B pela convenção.

**Espaço: heap:** n² bits. n = 10³ → 125 KB; n = 10⁵ → 1,25 GB; n = 10⁷ → 12,5 TB. Em Java, `boolean[][]` usa 1 B por célula (8× mais), e um array só não passa de 2³¹−1 posições: uma matriz achatada falha já com n > 46.340.

**Outros recursos:** Nenhum.

**Embarca:** **Inaceitável**: n = 10³ → 125 KB contra 256 B, ≈ 488× acima. Descontados os 48 B do frame, sobram 208 B = 1.664 bits: o maior n que cabe é 40 (40² = 1.600 bits).

**Datacenter:** **Inaceitável**: 10¹⁴ comparações (≈ 4,9 h, cerca de 8.900× o limite de 2 s) e 12,5 TB de memória.

**Correção:** Exata, mas guarda informação inútil: metade da matriz é simétrica e a diagonal é sempre verdadeira.

**Quando o veredito mudaria:** Se houvesse exigência real de rastrear cada comparação (auditoria, regulação) e n fosse pequeno, na Datacenter: o tempo cabe nos 2 s até n ≈ 106 mil (≈ 1,4 GB de memória).

## Resumo por cliente

Só a força bruta cabe na Embarca. Para a Datacenter, o bitmap é o mais rápido pela estimativa, e o Quicksort também passa. **Par implementado: 1 (força bruta) para a Embarca + 2 (Quicksort + varredura) para a Datacenter.**

### Justificativa do par implementado

**Embarca: estratégia 1.** O limite de 256 B elimina toda estratégia que guarda uma estrutura auxiliar: o bitmap, a menor delas, precisa de 8.192 B (32× o limite); Bloom, hash e árvore vão de dezenas de KB a 2,6 MB. O Quicksort passa do limite só com a pilha de recursão (1,2 KB já com n = 10³). Sobram as que usam apenas contadores: a força bruta, o Bogosort (≈ 10²⁵⁵⁴ anos com n = 10³) e a unária em disco (41 GB de disco com n = 10⁷). Só a força bruta termina sem depender de outro recurso.

**Datacenter: estratégia 2.** Cinco estratégias cabem nos 2 s com n = 10⁷: Quicksort (medido), bitmap, hash, Bloom e árvore (estimados). Contra as alternativas descartadas:

- **Bitmap (5):** seria cerca de 40× mais rápido pela estimativa (19 ms) e preserva a ordem de chegada, mas só é viável porque o domínio é pequeno e conhecido. O Quicksort não depende disso.
- **Tabela hash (3) e árvore (4):** alocam de 0,5 a 2,6 MB no heap; o Quicksort aloca 0 B. A árvore ainda fica limítrofe nos 2 s.
- **Filtro de Bloom (6):** erra até 1%; o Quicksort é exato.

O custo da escolha: a saída sai ordenada, não na ordem de chegada, e o tempo (0,84 s) é maior que o estimado para o bitmap.

**O par.** As duas ficam em extremos opostos: uma iterativa, com memória constante e tempo que cresce com n·k; outra recursiva, rápida, em que a pilha cresce com n. Nenhuma atende ao outro cliente: a força bruta leva 58 s onde o limite é 2 s, e o Quicksort usa 2,5 KB onde o limite é 256 B.

### Tabela-resumo

**1. Força bruta**

- Tempo (pior caso): O(n·k), O(n²) se distintos
- Pilha: 48 B (1 frame)
- Heap: 0
- Embarca (≤ 256 B): Aceitável (48 B medidos)
- Datacenter (10⁷ em < 2 s): Inaceitável (58 s medidos)

**2. Quicksort + varredura**

- Tempo (pior caso): Θ(n log n) médio; Θ(n²) pior
- Pilha: 2,5 KB medidos (49 a 53 frames); até n níveis
- Heap: 0
- Embarca (≤ 256 B): Inaceitável (~10× só de pilha)
- Datacenter (10⁷ em < 2 s): Aceitável (0,84 s medidos)

**3. Tabela hash**

- Tempo (pior caso): Θ(n) médio; O(n·k) pior
- Pilha: 48 B (1 frame)
- Heap: ≥ 512 KB
- Embarca (≤ 256 B): Inaceitável (~2.000×)
- Datacenter (10⁷ em < 2 s): Aceitável (~0,05–0,5 s)

**4. Árvore balanceada**

- Tempo (pior caso): Θ(n log k)
- Pilha: ~20 B a ~1 KB
- Heap: ~2,6 MB
- Embarca (≤ 256 B): Inaceitável (~10.000×)
- Datacenter (10⁷ em < 2 s): Limítrofe (~0,5–3 s)

**5. Bitmap**

- Tempo (pior caso): Θ(n)
- Pilha: 48 B (1 frame)
- Heap: 8.192 B
- Embarca (≤ 256 B): Inaceitável (32×)
- Datacenter (10⁷ em < 2 s): Aceitável, o melhor (~10–30 ms)

**6. Filtro de Bloom**

- Tempo (pior caso): Θ(n·h)
- Pilha: 48 B (1 frame)
- Heap: ~78,5 KB a 1%
- Embarca (≤ 256 B): Inaceitável (erro ≈ 100%)
- Datacenter (10⁷ em < 2 s): Aceitável, mas pior que o bitmap

**7. Bogosort**

- Tempo (pior caso): ilimitado; Θ(n·n!) médio
- Pilha: 48 B (1 frame)
- Heap: 0
- Embarca (≤ 256 B): Inaceitável (~10²⁵⁵⁴ anos)
- Datacenter (10⁷ em < 2 s): Inaceitável

**8. Power set**

- Tempo (pior caso): Θ(n·2ⁿ)
- Pilha: O(n): ~48 KB com n = 10³
- Heap: O(n)
- Embarca (≤ 256 B): Inaceitável (~219×)
- Datacenter (10⁷ em < 2 s): Inaceitável (estouro de pilha)

**9. Unária em disco**

- Tempo (pior caso): Θ(Σvᵢ)
- Pilha: 48 B (1 frame)
- Heap: 0 (41–328 GB em disco)
- Embarca (≤ 256 B): Inaceitável (disco)
- Datacenter (10⁷ em < 2 s): Inaceitável (82 s a 11 min)

**10. Matriz n×n**

- Tempo (pior caso): Θ(n²)
- Pilha: 48 B (1 frame)
- Heap: n² bits (12,5 TB com n = 10⁷)
- Embarca (≤ 256 B): Inaceitável (~488×)
- Datacenter (10⁷ em < 2 s): Inaceitável (~4,9 h)
