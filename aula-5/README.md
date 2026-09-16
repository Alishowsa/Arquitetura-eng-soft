# Aula 5 — Nosso primeiro MVC

Aplicação Spring MVC com cadastro completo de produtos, construída a partir do `mvc.zip` fornecido em aula. O CSS enviado foi integrado às telas, com complementos para formulários, mensagens e navegação.

O enunciado fornecido pede construir uma aplicação MVC com Spring MVC. O cadastro de **produtos** foi escolhido para concretizar a atividade; os materiais enviados não especificam a entidade do cadastro demonstrado em aula.

## Funcionalidades

- Cadastrar produtos com nome, descrição, preço e quantidade em estoque.
- Consultar a lista de produtos em ordem de nome.
- Editar todos os campos do cadastro.
- Excluir com uma tela de confirmação; abrir a confirmação não exclui dados.
- Exibir mensagens de sucesso, erros de entrada e produto não encontrado.
- Validar no servidor nome obrigatório, limites de tamanho, preço positivo com até duas casas decimais e quantidade inteira não negativa.
- Persistir os dados em arquivo H2, mantendo os cadastros após reiniciar.

## Executar

Requisito: JDK 21 ou superior, com `JAVA_HOME` configurado. O Maven Wrapper baixa o Maven e as dependências no primeiro uso, portanto precisa de internet.

Windows / PowerShell, a partir da raiz do repositório:

```powershell
cd aula-5/mvc
.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```sh
cd aula-5/mvc
sh mvnw spring-boot:run
```

Abra <http://localhost:8080/produtos>. `/` e `/home` redirecionam para o cadastro. Para encerrar, use `Ctrl+C`.

O banco fica em `aula-5/mvc/data/` quando executado como acima e não é versionado. A aplicação é um exercício local, sem autenticação, e escuta apenas em `127.0.0.1`. Não é um sistema de produção nem implementa o fluxo completo de pedidos da aula 4, que é uma atividade de arquitetura independente.

## Organização por camada — package by layer

| Pasta | Papel |
| --- | --- |
| `controller` | Recebe requisições, trata erros de formato e seleciona as views. |
| `dto` | Transporta os dados digitados no formulário sem expor o ID da entidade à vinculação de campos. |
| `service` | Aplica regras de negócio e delimita transações. |
| `repository` | Acessa a persistência com Spring Data JPA. |
| `model` | Representa a entidade Produto. |
| `resources/view` | Telas Thymeleaf: lista, formulário, confirmação de exclusão e erro. |
| `resources/static` | CSS utilizado pelas telas. |

View e Controller compõem a apresentação; Service concentra o domínio; Repository realiza o acesso aos dados. A entidade representa os dados do produto. A configuração `spring.thymeleaf.prefix=classpath:/view/` do projeto original foi mantida.

## Testes

```powershell
cd aula-5/mvc
.\mvnw.cmd test
```

Os testes de integração usam banco H2 em memória separado do cadastro local. Verificam o CRUD pelas rotas MVC, a renderização das telas, validações, confirmação de exclusão e IDs inexistentes.

Roteiro manual: cadastrar “Caderno” por `19.90` e quantidade `10`; conferir a lista; editar preço e quantidade; cancelar uma exclusão; confirmar outra; tentar enviar dados inválidos; reiniciar a aplicação e conferir a permanência de um produto salvo.

## Referências

- Slides da aula 5 e projeto `mvc.zip` fornecidos pelo professor.
- [Spring — formulários com MVC e Thymeleaf](https://spring.io/guides/gs/handling-form-submission/).
- [Spring Boot — testes com MockMvc](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/AutoConfigureMockMvc.html).
