# Aula 8 — Análise: o que pode dar errado na integração venda → hub

A venda (MVC, porta 8080) e o hub de faturamento (hexagonal, porta 8090) são **dois processos com dois bancos**, conversando pela rede. Os slides chamam isso de "o preço da rede": cada seta pode **atrasar**, **chegar duas vezes** ou **não caber em uma transação**. Abaixo, cada ameaça aplicada ao fechamento de um pedido.

| #   | Ameaça (slide)             | Na integração                                        | Situação no código                                                                         |
| --- | -------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| 1   | Atrasar                    | Hub fora do ar ou lento                              | Mitigado: timeout + estado de falha + reenvio. Proposto: outbox com fila e circuit breaker |
| 2   | Chegar duas vezes          | A resposta se perde e o reenvio faturaria duas vezes | **Resolvido**: idempotência por `pedidoId` no hub                                          |
| 3   | Não caber em uma transação | Venda cai entre o commit e a chamada ao hub          | Proposto: outbox                                                                           |

---

## Problema 1 — Hub fora do ar ou lento demais

**Como acontece.** O hub está parado (deploy, queda, banco dele indisponível) ou sobrecarregado. O `POST /faturas` recebe conexão recusada ou passa do tempo limite.

**O que acontece com o pedido.**

- O pedido **já foi fechado e o estoque já foi baixado** (transação local confirmada), mas fica **sem fatura**: status `FALHA_FATURAMENTO`, `faturaId` vazio e o erro gravado.
- Sem fatura não há boleto, nota fiscal nem e-mail para esse pedido.
- Com o hub **lento** e sem timeout, cada fechamento prenderia uma thread da venda pelo tempo que o hub levasse. Com vários vendedores ao mesmo tempo, as threads acabam e **a venda inteira fica lenta, até nas telas que nem usam o hub**. É a **falha em cascata** do slide: a culpa é do hub, mas quem para é a venda.

**O que o negócio percebe.**

- O vendedor vê "o hub não faturou" e fica em dúvida se a venda valeu.
- O cliente não recebe boleto: **a empresa entregou e não cobrou**.
- O estoque diminuiu, mas a venda não aparece no faturamento: o relatório de vendas não bate com o financeiro.
- Se ninguém clicar em **Reenviar ao hub**, o pedido fica esquecido.

**O que já está implementado** (em `aula-8/mvc`):

| Medida                                                             | Onde                                                                                                 | Efeito                                                                                                                                                             |
| ------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Timeout** de conexão e de leitura (`hub.timeout-ms`, padrão 3 s) | `ClienteRestDoHub`                                                                                   | A venda nunca espera mais que o limite. O teste `hubLentoEstouraOTimeoutSemPrenderAVenda` confirma que um hub de 1,5 s com limite de 0,5 s falha em menos de 1,5 s |
| A transação do banco **não fica aberta esperando a rede**          | `FechamentoDePedido` (sem `@Transactional`) chama `PedidoService.fecharLocalmente` e só depois o hub | O banco de vendas não fica travado enquanto o hub responde                                                                                                         |
| Estado `FALHA_FATURAMENTO` com o motivo e o número de tentativas   | `Pedido.registrarFalhaNoFaturamento`                                                                 | A falha fica visível, não some                                                                                                                                     |
| Botão **Reenviar ao hub**                                          | `FechamentoDePedido.reenviarAoHub`                                                                   | Recupera sem refazer o pedido nem baixar o estoque de novo                                                                                                         |

**O que ainda falta, e a solução proposta.**

1. **Circuit breaker no `ClienteRestDoHub`.** O timeout limita cada chamada, mas com o hub caído **toda** venda ainda gasta até 3 s para falhar. Com o circuito: depois de 5 falhas seguidas ele **abre** e as chamadas falham na hora, sem rede. Após 30 s ele fica **meio aberto** e deixa uma passar; se der certo, **fecha**. Isso protege as threads da venda.
2. **Comunicação assíncrona com outbox.** O vendedor não precisa do ID da fatura para entregar a mercadoria. Então, na **mesma transação** de `fecharLocalmente`, grava-se uma linha `faturamento_pendente` (a caixa de saída), e um processo separado publica numa fila que o hub consome com um **novo adaptador de entrada**, chamando a mesma porta `FaturarPedido`. Por ser hexagonal, o núcleo do hub não muda. O ID da fatura volta por uma fila de retorno, como o boleto já volta hoje. Com o hub fora, as mensagens esperam na fila e são faturadas sozinhas quando ele volta, sem ninguém clicar em reenviar.

**Como reproduzir.** Suba só a venda (sem o hub), feche um pedido: ele fica _Falha no faturamento_, com o estoque baixado. Suba o hub e clique em _Reenviar ao hub_: ele fica _Faturado_.

---

## Problema 2 — O hub fatura, mas a resposta se perde (fatura duplicada)

**Como acontece.** O hub recebe o pedido, cria a fatura, emite a NF-e e pede o boleto, mas a **resposta não chega** à venda: o tempo de leitura estoura um instante antes, a conexão cai ou um proxy derruba a resposta. Para a venda a chamada **falhou**, mas no hub ela **deu certo**.

**O que aconteceria sem proteção.**

- A venda marca `FALHA_FATURAMENTO` sem o ID da fatura que já existe.
- No **Reenviar ao hub**, o hub receberia o mesmo pedido de novo e **criaria uma segunda fatura**: duas NF-e, dois boletos e dois e-mails. O cliente pode **pagar em dobro**, o fiscal precisa **cancelar a nota duplicada** no prazo da SEFAZ, e a primeira fatura fica **órfã** (nenhum pedido aponta para ela).

**Por que isso não acontece aqui: idempotência.** Fazer duas vezes dá o mesmo resultado que fazer uma.

| Peça                                 | Onde                                                             | O que faz                                                                                               |
| ------------------------------------ | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| Chave de idempotência = `pedidoId`   | `FechamentoDePedido.paraOHub` (venda)                            | A venda **sempre** envia o mesmo `pedidoId`, inclusive no reenvio                                       |
| "Já faturei este pedido?"            | `FaturamentoService.faturar` (hub, aplicação)                    | Se existe fatura para o `pedidoId`, devolve a existente e **não** emite nota, boleto nem e-mail de novo |
| Restrição única                      | `FaturaTabela.pedidoId` com `unique = true` (hub, adaptador JPA) | Garante a regra mesmo com duas requisições simultâneas                                                  |
| `201` na criação, `200` na repetição | `FaturaController` (hub, adaptador REST)                         | Quem chama sabe que foi uma repetição                                                                   |

Testes que provam: `mesmoPedidoDeNovoDevolve200ComAMesmaFatura` (hub) e `hubForaDoArDeixaPedidoFechadoComFalhaEReenvioFatura` (venda).

**Consequência boa.** Com o hub idempotente, a venda pode **tentar de novo automaticamente** em caso de timeout, com segurança. Uma retentativa com espera crescente (1 s, 2 s, 4 s) seria o próximo passo.

**Cuidado que tomei no cliente HTTP.** O `HttpURLConnection` do Java (usado pelo `SimpleClientHttpRequestFactory`) **reenvia um POST sozinho** quando a conexão cai. É uma repetição escondida, exatamente esse problema. O `ClienteRestDoHub` usa o `java.net.http.HttpClient` (`JdkClientHttpRequestFactory`), que não faz isso. Descobri isso pelos testes, quando o hub falso derrubava a conexão.

---

## Problema 3 — A venda cai entre o commit e a chamada ao hub

**Como acontece.** `fecharLocalmente` confirma a transação (pedido `FECHADO`, estoque baixado) e, **antes** de chamar o hub, o processo da venda cai (deploy, falta de memória, máquina reiniciada).

**O que acontece com o pedido.** Ele fica `FECHADO`: nem `FATURADO`, nem `FALHA_FATURAMENTO`. Como só pedidos com falha mostram o botão de reenvio, **ninguém percebe**, e o pedido nunca é faturado. Banco e rede são **dois lugares**, e o "tudo ou nada" da transação só vale dentro do banco.

**O que o negócio percebe.** O mesmo do problema 1, mas pior: não há nenhum alerta. A venda simplesmente some do faturamento.

**Solução: outbox (a mesma do problema 1).** A intenção de faturar é gravada **na mesma transação** que fecha o pedido. Se a venda cair, a linha na caixa de saída continua lá, e o processo de envio publica quando a venda voltar. A mensagem pode sair duas vezes (cai depois de publicar e antes de marcar como enviada), mas a **idempotência do problema 2** absorve a repetição. Os dois padrões funcionam juntos: o outbox garante **pelo menos uma** entrega, e a idempotência garante **uma** fatura.

**Mitigação simples, enquanto não há outbox:** uma rotina periódica que procura pedidos `FECHADO` há mais de alguns minutos e os passa para `FALHA_FATURAMENTO`, ou os reenvia, já que o hub é idempotente.

---

## Resumo

| Ameaça                               | Solução                                       | Status                                               |
| ------------------------------------ | --------------------------------------------- | ---------------------------------------------------- |
| Hub lento                            | Timeout                                       | ✅ implementado                                      |
| Hub fora do ar                       | Estado de falha + reenvio manual              | ✅ implementado                                      |
|                                      | Circuit breaker                               | proposto                                             |
|                                      | Fila + outbox (assíncrono)                    | proposto                                             |
| Resposta perdida → duas faturas      | Idempotência por `pedidoId` + restrição única | ✅ implementado no hub                               |
|                                      | POST sem reenvio escondido no cliente HTTP    | ✅ implementado                                      |
| Venda cai entre o commit e a chamada | Outbox                                        | proposto (mitigação: varredura de `FECHADO` antigos) |
