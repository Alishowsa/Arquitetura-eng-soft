# Análise de Estilos Arquiteturais

## Enunciado da Atividade

> **Destrinchando os estilos**
>
> Com base nos estilos arquiteturais apresentados em aula, a saber, monolito, cliente-servidor, distribuído, ponto a ponto, publicador/assinante, pipes e filtros.
>
> Escolha 2 (dois) dos estilos e elabore uma análise para cada um, abordando os seguintes tópicos:
> - **Conceito e definição**: O que é esse estilo arquitetural e como ele funciona na prática?
> - **Casos de uso comuns**: Em quais tipos de sistemas, problemas ou cenários esse estilo é recomendado/utilizado pelo mercado? Cite ao menos 2 exemplos reais ou práticos.
> - **Principais vantagens**: Quais são os maiores benefícios ao adotar esse estilo? (ex: facilidade de implantação, desempenho, escalabilidade, etc.)
> - **Principais desvantagens**: Quais são os gargalos, limitações ou desafios de manutenção/desenvolvimento associados a ele?
>
> **Entrega**: publicar o arquivo .md com a análise dos dois estilos escolhidos em uma pasta aula-3 no repositório GitHub dedicado à disciplina.

---

## Estilos escolhidos: Cliente-Servidor e Publicador/Assinante

---

## Estilo 1: Cliente-Servidor

### O que é
É um modelo onde existem dois lados: o **servidor**, que guarda os dados e as regras do sistema, e o **cliente**, que pede informações ou serviços ao servidor. Na prática, o cliente manda uma solicitação, o servidor processa e devolve uma resposta. É o modelo mais comum de aplicação que usamos no dia a dia.

### Onde é usado (exemplos reais)
1. **Sites e aplicativos web**: quando você abre um site de compras, seu navegador (cliente) pede informações ao servidor da loja, que responde com os produtos.
2. **Aplicativos bancários**: o app do banco no celular (cliente) se conecta ao servidor do banco para consultar saldo, extrato, fazer transferências, etc.

### Vantagens
- Fácil de entender e de implementar, é o modelo mais tradicional.
- Os dados ficam centralizados no servidor, o que facilita manter tudo organizado e seguro.
- É possível atender vários clientes diferentes (celular, computador, tablet) usando o mesmo servidor.

### Desvantagens
- Se o servidor cair, ninguém consegue usar o sistema.
- Se muitas pessoas usarem ao mesmo tempo, o servidor pode ficar lento (sobrecarregado).
- O cliente depende de estar conectado à internet/rede para funcionar.

---

## Estilo 2: Publicador/Assinante (Pub/Sub)

### O que é
Nesse modelo, quem envia a informação (**publicador**) não manda diretamente para quem vai receber. Ele publica a mensagem em um "canal" ou "tópico", e quem tem interesse naquele assunto (**assinante**) recebe automaticamente, sem que os dois precisem se conhecer. Existe um intermediário (chamado de broker) que faz essa entrega.

### Onde é usado (exemplos reais)
1. **Notificações de aplicativos**: quando um pedido de delivery muda de status (saiu para entrega, chegou), o sistema publica essa mudança e todos os interessados (o app do cliente, do entregador, etc.) recebem a atualização automaticamente.
2. **Sistemas com vários serviços internos (microsserviços)**: uma loja online pode ter um serviço que avisa "produto vendido", e vários outros serviços (estoque, financeiro, entrega) recebem esse aviso e reagem cada um do seu jeito, sem estarem diretamente ligados entre si.

### Vantagens
- Os sistemas ficam mais independentes uns dos outros (baixo acoplamento).
- É fácil adicionar novos "ouvintes" (assinantes) sem mexer no resto do sistema.
- Funciona bem quando várias partes do sistema precisam reagir ao mesmo evento ao mesmo tempo.

### Desvantagens
- É mais difícil de configurar e manter, porque precisa de uma ferramenta extra (o broker) rodando corretamente.
- Fica mais complicado rastrear e entender o caminho completo de uma informação, já que ela passa por várias partes indiretamente.
- Pode haver atraso ou perda de mensagens, exigindo cuidado extra para garantir que tudo chegue certo.

---

## Conclusão
O Cliente-Servidor é mais simples e direto, ideal quando existe uma comunicação clara entre quem pede e quem responde. Já o Publicador/Assinante é melhor para sistemas maiores e mais distribuídos, onde várias partes precisam saber de um mesmo evento sem estarem diretamente conectadas entre si.
