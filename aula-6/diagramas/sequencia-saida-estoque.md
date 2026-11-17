# Sequência — Saída de estoque

**Pergunta:** quem chama quem quando o Time de Estoque dá baixa em mercadoria (venda no balcão, avaria, devolução ao fornecedor)?

Aqui aparecem os dois desfechos que importam: a saída que **zera o saldo** (o produto esgota e os vendedores são avisados) e a saída **maior que o saldo** (recusada sem gravar nada).

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

    TE->>+EC: POST /estoque (produto 8, SAIDA, qtd)
    EC->>+ES: registrarSaida(8, qtd, motivo, ESTOQUE)
    ES->>+PR: findById(8)
    PR-->>-ES: Produto (ATIVO, saldo 3)
    ES->>+P: baixarEstoque(qtd)

    alt qtd = 3 (zera o saldo)
        P->>P: saldo 3 → 0, status ATIVO → ESGOTADO
        P-->>ES: ESGOTOU
        ES->>+MR: save(Movimentacao SAIDA, saldoApós 0)
        MR-->>-ES: Movimentacao
        ES->>AV: produtoEsgotou(produto)
        ES-->>EC: Movimentacao
        EC-->>TE: redirect /estoque + "Saída de 3 un. ... Saldo: 0 (Esgotado)."
    else qtd = 5 (maior que o saldo)
        P-->>-ES: RegraDeNegocioException "Estoque insuficiente"
        ES-->>-EC: RegraDeNegocioException (transação desfeita)
        EC-->>-TE: redirect /estoque + erro "Estoque insuficiente de ...: saldo 3, pedido 5."
    end
```

## Leitura

- **A validação de saldo mora no `Produto`.** Nenhum outro lugar faz `quantidade - x`. Por isso não existe caminho que deixe o saldo negativo.
- **No caminho de erro, nada é gravado:** a exceção sai do `Produto` antes do `save` da movimentação, e a transação do service é desfeita.
- **Saída de produto descontinuado** segue o mesmo caminho do `else`, com a mensagem "está descontinuado". Ela aparece no diagrama de atividades ao lado, que é melhor para mostrar várias recusas.
