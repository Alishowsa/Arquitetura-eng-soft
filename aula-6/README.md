# Aula 6 — Estruturas dinâmicas: status do produto e controle de estoque

Evolução do MVC da [Aula 5](../aula-5/README.md) com o que o diagrama de contexto da aula pede: o **Administrador cadastra produtos**, o **Time de Estoque controla o estoque** e o **Vendedor** consulta. O produto ganha o ciclo de vida do slide "Status do Product" (`ATIVO ⇄ ESGOTADO → DESCONTINUADO`).

Junto com o código vão os **diagramas dinâmicos** desses fluxos: sequência, atividades e estados.

## Entregas

| Item                               | Onde                                                                             |
| ---------------------------------- | -------------------------------------------------------------------------------- |
| Aplicação MVC com status e estoque | [mvc/](mvc/)                                                                     |
| Sequência: entrada de estoque      | [diagramas/sequencia-entrada-estoque.md](diagramas/sequencia-entrada-estoque.md) |
| Sequência: saída de estoque        | [diagramas/sequencia-saida-estoque.md](diagramas/sequencia-saida-estoque.md)     |
| Atividades: saída de estoque       | [diagramas/atividades-saida-estoque.md](diagramas/atividades-saida-estoque.md)   |
| Estados: produto                   | [diagramas/estados-produto.md](diagramas/estados-produto.md)                     |

## Regras implementadas

| Regra                                                                                  | Onde fica                                              |
| -------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| Produto tem status `ATIVO`, `ESGOTADO` ou `DESCONTINUADO`                              | `model/StatusProduto`                                  |
| Nasce `ATIVO` se o estoque inicial for maior que 0, senão `ESGOTADO`                   | `Produto.cadastrar`                                    |
| Saída que zera o saldo → `ESGOTADO`, e os vendedores são avisados                      | `Produto.baixarEstoque` + `AvisosAosVendedores`        |
| Entrada em produto esgotado → `ATIVO`, e os vendedores são avisados                    | `Produto.acrescerEstoque` + `AvisosAosVendedores`      |
| Saída maior que o saldo, ou quantidade ≤ 0, é recusada                                 | `Produto.baixarEstoque` / `acrescerEstoque`            |
| Produto descontinuado não é movimentado e não volta a ativo                            | `Produto.exigirMovimentavel` / `descontinuar`          |
| Só o **Administrador** cadastra, edita, descontinua e exclui                           | `ProdutoService` (e `ProdutoController` para as telas) |
| Só o **Time de Estoque** ou o **Administrador** registram entrada e saída              | `EstoqueService`                                       |
| Toda movimentação fica registrada: tipo, quantidade, saldo após, perfil, motivo e data | `model/Movimentacao`                                   |
| O saldo só muda por movimentação: a edição do produto não altera a quantidade          | `Produto` sem setter de quantidade/status              |
| Produto com histórico não pode ser excluído, só descontinuado                          | `ProdutoService.excluir`                               |

## Decisões de arquitetura

**1. As regras de estado ficam no Model, na entidade.** `Produto` não tem `setStatus` nem `setQuantidade`. Os únicos jeitos de mudar o saldo são `acrescerEstoque` e `baixarEstoque`, e eles decidem o status. Assim, o diagrama de estados é exatamente o código, e nenhum Controller ou Service consegue deixar o produto `ATIVO` com saldo zero.

**2. O Service coordena, a entidade decide.** O `EstoqueService` checa o perfil, busca o produto, pede a movimentação, grava o registro e, conforme a `Transicao` devolvida (`ESGOTOU`, `VOLTOU_AO_ESTOQUE`), dispara o aviso. É a separação Controller → Service → Model da Aula 5, agora com o Model fazendo mais que guardar dados.

**3. Movimentação é histórico, não só saldo.** Cada entrada ou saída vira uma linha em `Movimentacao`, com o saldo depois dela. Isso responde "quem tirou 10 canetas ontem?", algo que só o número do saldo não responde. Por isso um produto com histórico não pode ser excluído, só descontinuado.

**4. Permissão por perfil, conferida no Service.** Ainda não há login, então o perfil é escolhido no topo da tela e guardado na sessão, começando como **Vendedor**, o de menor permissão. A tela esconde os botões que o perfil não pode usar, mas a regra de verdade está no Service: chamar a rota direto devolve **403**.

**5. Avisos aos vendedores.** As ações `attVendedor()` / `attVendedores()` do diagrama do slide viraram o componente `AvisosAosVendedores`, que registra o aviso no log e mostra os últimos na tela de produtos. Num sistema real, seria um e-mail ou uma notificação, trocando só esse componente.

## Os diagramas e o código andam juntos

| Diagrama             | Responde                                                   | Conferido pelos testes                            |
| -------------------- | ---------------------------------------------------------- | ------------------------------------------------- |
| Sequência da entrada | Quem chama quem quando o produto esgotado volta ao estoque | `cicloCompletoAtivoEsgotadoAtivoDescontinuado`    |
| Sequência da saída   | Saída que esgota e saída recusada por saldo                | `saidaMaiorQueOSaldoNaoAlteraNada`                |
| Atividades da saída  | As 6 decisões até aceitar ou recusar                       | Um teste por losango em `EstoqueIntegrationTests` |
| Estados do produto   | As setas permitidas e as proibidas                         | `ProdutoTest`, um teste por seta                  |

### Quando usar cada diagrama

| Diagrama   | Olha para     | Mostra            | Eixo              | Usei para...                                                    |
| ---------- | ------------- | ----------------- | ----------------- | --------------------------------------------------------------- |
| Sequência  | uma interação | quem chama quem   | tempo             | mostrar Controller → Service → Produto → Repository colaborando |
| Atividades | um processo   | passos e decisões | fluxo de controle | listar todas as recusas possíveis de uma saída                  |
| Estados    | um objeto     | estados e eventos | ciclo de vida     | garantir que só transições válidas acontecem com o produto      |

## Executar

Requisito: JDK 21 ou superior.

```powershell
cd aula-6/mvc
.\mvnw.cmd spring-boot:run      # http://localhost:8080
.\mvnw.cmd test                 # 18 testes
```

Roteiro para demonstrar:

1. No topo, troque o perfil para **Administrador** e cadastre "Caneta" com estoque inicial **0**. Ela aparece como **Esgotado**.
2. Troque para **Time de Estoque**, vá em **Estoque** e registre uma **entrada** de 10. Ela fica **Ativo**, e aparece o aviso "voltou ao estoque" em Produtos.
3. Registre uma **saída** de 10. Ela volta para **Esgotado**, com o aviso "esgotou".
4. Tente uma saída de 1: aparece "Estoque insuficiente".
5. Como **Administrador**, clique em **Descontinuar**. O produto some da lista de movimentação.
6. Como **Vendedor**, os botões de cadastro e estoque não aparecem, e acessar `/produtos/novo` direto devolve 403.

O banco fica em `aula-6/mvc/data/` e não é versionado.
