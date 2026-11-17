# Atividades — Registrar uma saída de estoque

**Pergunta:** quais passos e decisões o sistema percorre até aceitar ou recusar uma saída?

O diagrama de sequência mostra **quem** faz cada parte. Este mostra **todas as condições**: cada losango é uma regra, e cada recusa tem o seu caminho.

```mermaid
flowchart TD
    inicio((" ")) --> recebe[Recebe produto, quantidade e motivo]
    recebe --> d1{Perfil é Time de Estoque<br/>ou Administrador?}
    d1 -- "[não]" --> r1[Recusa: 403 operação não permitida]
    d1 -- "[sim]" --> d2{Quantidade é um<br/>inteiro maior que zero?}
    d2 -- "[não]" --> r2[Recusa: quantidade inválida]
    d2 -- "[sim]" --> d3{Produto existe?}
    d3 -- "[não]" --> r3[Recusa: produto não encontrado]
    d3 -- "[sim]" --> d4{Produto está<br/>descontinuado?}
    d4 -- "[sim]" --> r4[Recusa: descontinuado<br/>não movimenta]
    d4 -- "[não]" --> d5{Quantidade ≤ saldo?}
    d5 -- "[não]" --> r5[Recusa: estoque insuficiente]
    d5 -- "[sim]" --> baixa[Baixa o saldo]
    baixa --> grava[Registra a movimentação<br/>com o saldo após]
    grava --> d6{Saldo chegou a zero?}
    d6 -- "[sim]" --> esgota[Marca ESGOTADO e<br/>avisa os vendedores]
    d6 -- "[não]" --> ok[Mostra o novo saldo]
    esgota --> ok
    ok --> fim((("&nbsp;")))
    r1 --> fim
    r2 --> fim
    r3 --> fim
    r4 --> fim
    r5 --> fim

    classDef recusa fill:#f3f4f8,stroke:#999,stroke-dasharray: 4 3,color:#555;
    class r1,r2,r3,r4,r5 recusa;
```

## Leitura

| Elemento   | Onde aparece                                                       |
| ---------- | ------------------------------------------------------------------ |
| Nó inicial | O círculo no topo                                                  |
| Ações      | "Baixa o saldo", "Registra a movimentação" etc.                    |
| Decisões   | Seis losangos, cada um com `[sim]` / `[não]`                       |
| Nó final   | Um só, onde todos os caminhos terminam, inclusive as cinco recusas |

**Ordem das decisões:** primeiro o que não exige banco (perfil, formato da quantidade), depois o que exige ler o produto (existe? descontinuado? saldo?). Um pedido inválido é recusado antes de qualquer consulta.

**Cada losango tem um teste** em `EstoqueIntegrationTests` ou `ProdutoTest`. Por exemplo, `vendedorNaoMovimentaNemCadastraNemDescontinua`, `quantidadeInvalidaViraMensagemDeErro`, `produtoDescontinuadoNaoMovimenta` e `saidaMaiorQueOSaldoNaoAlteraNada`.
