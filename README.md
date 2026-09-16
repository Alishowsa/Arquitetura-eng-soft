# Arquitetura de Software

Repositório de atividades da disciplina de Arquitetura de Software — Prof. Dr. Pedro Henrique Nogueira Pizzutti.

## Entregas por aula

| Aula | Atividade | Entrega |
| --- | --- | --- |
| 2 | Nosso primeiro C4 | [Exemplo da biblioteca e sistema próprio de Delivery](aula-2/README.md). |
| 3 | Destrinchando os estilos | [Análise de Cliente-Servidor e Publicador/Assinante](aula-3/analise-estilos-arquiteturais.md). |
| 4 | Sua vez de arquitetar | [Contexto, contêineres e camadas de um sistema de vendas e estoque](aula-4/README.md). |
| 5 | Nosso primeiro MVC | [Cadastro de produtos com Spring MVC](aula-5/README.md). |

Os materiais enviados da aula 1 apresentam os conceitos introdutórios, sem uma entrega prática identificada. Os exercícios de C4 e estilos já existentes foram preservados; a organização agrupa cada entrega na pasta de sua aula.

## Executar o cadastro MVC

Com JDK 21 ou superior e `JAVA_HOME` configurado:

```powershell
cd aula-5/mvc
.\mvnw.cmd spring-boot:run
```

Acesse <http://localhost:8080/produtos>. Instruções, estrutura das camadas e testes estão no [README da aula 5](aula-5/README.md).
