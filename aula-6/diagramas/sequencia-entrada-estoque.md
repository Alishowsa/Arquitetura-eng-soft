# Sequência — Entrada de estoque

**Pergunta:** quem chama quem, e em que ordem, quando o Time de Estoque registra a chegada de mercadoria?

Caso escolhido: o produto estava **ESGOTADO** e volta a ter saldo. É o caso que mais mostra colaboração, porque além de gravar a movimentação o sistema avisa os vendedores (`attVendedores()` no diagrama de estados).

```mermaid
sequenceDiagram
    autonumber
    actor TE as Time de Estoque
    participant EC as EstoqueController
    participant ES as EstoqueService
    participant PR as ProdutoRepository
    participant P as Produto
    participant MR as MovimentacaoRepository
    participant AV as AvisosAosVendedores

    TE->>+EC: POST /estoque (produto 8, ENTRADA, 10, "NF 1234")
    EC->>+ES: registrarEntrada(8, 10, "NF 1234", ESTOQUE)
    ES->>ES: perfil pode movimentar?
    ES->>+PR: findById(8)
    PR-->>-ES: Produto (ESGOTADO, saldo 0)
    ES->>+P: acrescerEstoque(10)
    P->>P: saldo 0 → 10, status ESGOTADO → ATIVO
    P-->>-ES: VOLTOU_AO_ESTOQUE
    ES->>+MR: save(Movimentacao ENTRADA, saldoApós 10)
    MR-->>-ES: Movimentacao
    ES->>AV: produtoVoltouAoEstoque(produto)
    ES-->>-EC: Movimentacao
    EC-->>-TE: redirect /estoque + "Entrada de 10 un. registrada. Saldo: 10 (Ativo)."
```

## Leitura

- **Quem decide o status é o `Produto`** (passos 6 e 7), não o service nem o controller. O service só pergunta o que mudou (`Transicao`) para decidir se avisa alguém.
- **Tudo dentro de uma transação** (`@Transactional` no `EstoqueService`): o novo saldo do produto e o registro da movimentação são gravados juntos ou nenhum dos dois.
- **A permissão é checada no service** (passo 3), e não só na tela. Mesmo chamando a rota direto, o Vendedor recebe 403.
