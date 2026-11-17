# Como os serviços conversam

Cada seta do C4 vira uma conversa pela rede. Antes de desenhar a seta, uma pergunta decide o tipo.

## O critério

```mermaid
flowchart TD
    ini((" ")) --> q{Preciso da resposta<br/>para continuar?}
    q -- "[sim]" --> s["Síncrona<br/>espero a resposta"]
    q -- "[não]" --> a["Assíncrona<br/>mando e sigo"]
    s --> fim((("&nbsp;")))
    a --> fim
```

|                 | Síncrona                                               | Assíncrona                                              |
| --------------- | ------------------------------------------------------ | ------------------------------------------------------- |
| Analogia        | uma **ligação**: você liga e espera a pessoa responder | um **recado**: você manda e vai fazer outra coisa       |
| Quem chama      | fica parado esperando                                  | publica e segue a vida                                  |
| Disponibilidade | os dois precisam estar no ar                           | o destino pode estar fora por um tempo, e a fila guarda |
| Tecnologias     | REST, SOAP                                             | fila (RabbitMQ)                                         |

## Síncrona: Vendas pede a fatura

Vendas precisa do **número da fatura** para mostrar ao vendedor e gravar no pedido. Sem a resposta, não termina o fechamento.

```mermaid
sequenceDiagram
    participant V as Vendas
    participant F as Faturamento

    activate V
    V->>+F: POST /faturas {pedidoId 77, valor 149,90}
    Note left of V: esperando
    F-->>-V: 201 {faturaId 42}
    V->>V: grava a fatura 42 no pedido 77
    deactivate V
```

## Síncrona: Faturamento pede a nota ao Fiscal

A fatura precisa do **RPS/chave de acesso** da nota. É síncrona, e por isso é a seta mais perigosa: se o Fiscal demorar, o Faturamento demora, e Vendas também. É nela que entram o timeout e o circuit breaker de [resiliencia.md](resiliencia.md).

```mermaid
sequenceDiagram
    participant F as Faturamento
    participant S as Fiscal

    activate F
    F->>+S: EmitirNota(fatura 42, R$ 149,90)
    Note left of F: esperando (no máximo 2 s)
    S-->>-F: RPS-000042
    deactivate F
```

## Assíncrona: Faturamento pede o boleto à Cobrança

O Faturamento **não precisa** do código de barras para responder a Vendas. Publica o pedido e segue. A Cobrança busca quando puder, e devolve o boleto por outra fila.

```mermaid
sequenceDiagram
    participant F as Faturamento
    participant Q as Fila
    participant C as Cobrança

    activate F
    F->>+Q: publica boleto-solicitado (fatura 42)
    deactivate Q
    deactivate F
    Note left of F: segue a vida

    C->>+Q: busca a mensagem
    Q-->>-C: fatura 42, R$ 149,90
    activate C
    C->>C: gera o boleto no banco emissor
    C->>Q: publica boleto-emitido (fatura 42, código)
    deactivate C
```

## Assíncrona: a Notificação manda o e-mail

Quando a fatura fica finalizada (boleto + nota), o Faturamento publica `fatura.finalizada`. A Notificação manda o e-mail no seu ritmo. Se o servidor de e-mail estiver fora, a mensagem espera na fila e **o faturamento não para**.

## No nosso ecossistema

```mermaid
flowchart TD
    V[Vendas] -- "precisa do ID" --> F[Faturamento]
    F -- "precisa da chave" --> S[Fiscal]
    F -. "fila" .-> C[Cobrança]
    F -. "fila" .-> N[Notificação]
```

_Seta cheia: síncrona. Tracejada: assíncrona._

## Uma decisão que vale registrar

Na Aula 7, a porta `NotificarCliente` era **síncrona**: o Hub enviava o e-mail e recebia o hash na hora. Com a Notificação virando um microsserviço atrás de uma fila, ela vira **assíncrona**. O Faturamento não espera mais o e-mail sair. Ganhamos falha isolada (e-mail fora do ar não trava o faturamento) e pagamos com consistência eventual: por alguns segundos a fatura está `FINALIZADA`, mas ainda não `NOTIFICADA`.
