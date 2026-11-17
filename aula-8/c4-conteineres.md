# C4 nível 2 — O ecossistema antes e depois da divisão

## Antes: dois sistemas, cada um com seu código, banco e processo

```mermaid
flowchart LR
    vend([Vendedor, Administrador<br/>e Time de Estoque]):::pessoa

    subgraph EMP["Empresa"]
        VEN["Aplicação de vendas<br/>[MVC · Spring + Thymeleaf]"]
        HUB["Hub de faturamento<br/>[Hexagonal · Spring Boot]<br/>boleto, nota e e-mail"]
        DBV[("banco<br/>vendas")]
        DBH[("banco<br/>hub")]
    end

    BAN["Banco emissor"]:::externo
    NFE["Emissor de nota"]:::externo
    SMTP["Servidor de e-mail"]:::externo

    vend -- HTTP --> VEN
    VEN -- REST --> HUB
    VEN --> DBV
    HUB --> DBH
    HUB -- fila --> BAN
    HUB -- SOAP --> NFE
    HUB -- SMTP --> SMTP

    classDef pessoa fill:#0d35dd,color:#fff,stroke:#0d35dd;
    classDef externo fill:#f3f4f8,color:#555,stroke:#999,stroke-dasharray: 4 3;
```

## Depois: o Hub dividido por responsabilidade de negócio

Seta cheia é síncrona. Seta tracejada é assíncrona, por fila.

```mermaid
flowchart LR
    vend([Vendedor]):::pessoa

    subgraph INT["Serviços internos"]
        VEN["Vendas<br/>[MVC]"]
        FAT["Faturamento<br/>[Hexagonal]"]
        COB["Cobrança<br/>boletos"]
        FIS["Fiscal<br/>notas fiscais"]
        NOT["Notificação<br/>avisos"]
        MQ{{"broker<br/>RabbitMQ"}}

        DBV[("vendas")]
        DBF[("faturamento")]
        DBC[("cobranca")]
        DBS[("fiscal")]
        DBN[("notificacao")]
    end

    subgraph EXT["Fora da empresa"]
        BAN["Banco emissor"]:::externo
        NFE["Emissor da nota"]:::externo
        SMTP["Servidor de e-mail"]:::externo
    end

    vend --> VEN
    VEN -- "REST · precisa do ID" --> FAT
    FAT -- "SOAP · precisa da chave<br/>timeout 2s + circuit breaker" --> FIS
    FAT -. "fatura.boleto-solicitado" .-> MQ
    FAT -. "fatura.finalizada" .-> MQ
    MQ -. fila .-> COB
    MQ -. fila .-> NOT
    COB -. "cobranca.boleto-emitido" .-> MQ
    MQ -. "fila de retorno" .-> FAT

    COB --> BAN
    FIS --> NFE
    NOT --> SMTP

    VEN --- DBV
    FAT --- DBF
    COB --- DBC
    FIS --- DBS
    NOT --- DBN

    classDef pessoa fill:#0d35dd,color:#fff,stroke:#0d35dd;
    classDef externo fill:#f3f4f8,color:#555,stroke:#999,stroke-dasharray: 4 3;
```

## As quatro marcas, serviço por serviço

| Serviço     | Uma responsabilidade | Dado próprio  | Deploy independente | Conversa pela rede                                                    |
| ----------- | -------------------- | ------------- | ------------------- | --------------------------------------------------------------------- |
| Vendas      | pedidos e estoque    | `vendas`      | sim                 | chama o Faturamento por REST                                          |
| Faturamento | a fatura             | `faturamento` | sim                 | recebe REST; chama o Fiscal (SOAP); publica e consome filas           |
| Cobrança    | boletos              | `cobranca`    | sim                 | consome `fatura.boleto-solicitado`; publica `cobranca.boleto-emitido` |
| Fiscal      | notas fiscais        | `fiscal`      | sim                 | expõe SOAP; fala com o emissor externo                                |
| Notificação | avisos ao cliente    | `notificacao` | sim                 | consome `fatura.finalizada`; fala com o servidor de e-mail            |

**Teste de fronteira:** se eu mudar o banco de emissão de boletos (de um banco para outro), quais serviços são publicados? Só a **Cobrança**. Se a resposta fosse "Cobrança e Faturamento", a fronteira estaria errada.

## Contratos das mensagens

As filas são contratos públicos entre times. Cada mensagem carrega um `idMensagem` (para idempotência) e o `pedidoId` (para correlação nos logs).

```jsonc
// fila: fatura.boleto-solicitado   (publica: Faturamento · consome: Cobrança)
{ "idMensagem": "c1f7...", "pedidoId": 77, "faturaId": 42, "valor": 149.90, "vencimento": "2026-11-24" }

// fila: cobranca.boleto-emitido    (publica: Cobrança · consome: Faturamento)
{ "idMensagem": "9ab2...", "pedidoId": 77, "faturaId": 42, "codigoBarras": "34191000000000042000001499..." }

// fila: fatura.finalizada          (publica: Faturamento · consome: Notificação)
{ "idMensagem": "e04d...", "pedidoId": 77, "faturaId": 42, "emailCliente": "ana@loja.com",
  "codigoBarras": "34191...", "rps": "RPS-000042" }
```

A chamada síncrona ao Fiscal também tem contrato, que é o WSDL do serviço: `EmitirNota(faturaId, valor, cliente) → { rps, chaveAcesso }`.
