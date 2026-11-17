# Arquitetura de Software

Repositório de atividades da disciplina de Arquitetura de Software — Prof. Dr. Pedro Henrique Nogueira Pizzutti.

## Entregas por aula

| Aula                                                                                                                                                                                                                                | Atividade                | Entrega                                                                                                                 |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------ | ----------------------------------------------------------------------------------------------------------------------- |
| 2                                                                                                                                                                                                                                   | Nosso primeiro C4        | [Exemplo da biblioteca e sistema próprio de Delivery](aula-2/README.md).                                                |
| 3                                                                                                                                                                                                                                   | Destrinchando os estilos | [Análise de Cliente-Servidor e Publicador/Assinante](aula-3/analise-estilos-arquiteturais.md).                          |
| 4                                                                                                                                                                                                                                   | Sua vez de arquitetar    | [Contexto, contêineres e camadas de um sistema de vendas e estoque](aula-4/README.md).                                  |
| 5                                                                                                                                                                                                                                   | Nosso primeiro MVC       | [Cadastro de produtos com Spring MVC](aula-5/README.md).                                                                |
| 6                                                                                                                                                                                                                                   | Estruturas dinâmicas     | [MVC com status do produto e controle de estoque, mais diagramas de sequência, atividades e estados](aula-6/README.md). |
| 7                                                                                                                                                                                                                                   | Arquitetura hexagonal    | [Hub de faturamento (RF01–RF04): portas e adaptadores para REST, filas, SOAP, SMTP e banco](aula-7/README.md).          |
| 8                                                                                                                                                                                                                                   | Microsserviços           | [Venda integrada ao hub por REST, com a análise dos problemas da rede e das soluções](aula-8/README.md).                |
| Os materiais enviados da aula 1 apresentam os conceitos introdutórios, sem uma entrega prática identificada. Os exercícios de C4 e estilos já existentes foram preservados; a organização agrupa cada entrega na pasta de sua aula. |

## Executar o cadastro MVC

Com JDK 21 ou superior e `JAVA_HOME` configurado:

```powershell
cd aula-5/mvc
.\mvnw.cmd spring-boot:run
```

Acesse <http://localhost:8080/produtos>. Instruções, estrutura das camadas e testes estão no [README da aula 5](aula-5/README.md).

## Executar as aulas 6 a 8

| Aula | Pasta                                         | Porta                            | Testes |
| ---- | --------------------------------------------- | -------------------------------- | ------ |
| 6    | `aula-6/mvc`                                  | <http://localhost:8080/produtos> | 18     |
| 7    | `aula-7/hub-faturamento`                      | <http://127.0.0.1:8090/faturas>  | 23     |
| 8    | `aula-8/mvc` (precisa do hub da aula 7 no ar) | <http://localhost:8080/pedidos>  | 26     |

Em cada pasta: `.\mvnw.cmd spring-boot:run` para subir e `.\mvnw.cmd test` para testar. As aulas 6 e 8 usam a mesma porta 8080, então suba uma de cada vez. O roteiro completo da integração está no [README da aula 8](aula-8/README.md).

