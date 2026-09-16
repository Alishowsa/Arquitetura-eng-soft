# Atividade — Modelo C4 de um Sistema de Delivery

## 1. Sistema escolhido

O sistema escolhido é uma plataforma de delivery de comida. Ela permite que clientes encontrem restaurantes, façam pedidos, realizem pagamentos e acompanhem a entrega.

### Arquivos da atividade

- `c1-context.mmd`: diagrama de contexto do sistema.
- `c2-context.mmd`: diagrama de contêineres.
- `c3-components.mmd`: diagrama de componentes da API Backend.
- `c4-code.mmd`: diagrama de código do componente de pedidos.

## 2. Nível 1 — Diagrama de contexto

O diagrama de contexto apresenta o sistema como um todo, seus usuários e os sistemas externos com os quais ele se comunica.

```mermaid
flowchart LR
    Cliente["Cliente<br/>Pesquisa restaurantes, faz pedidos e acompanha entregas"]
    Restaurante["Restaurante<br/>Recebe e prepara os pedidos"]
    Entregador["Entregador<br/>Retira e entrega os pedidos"]
    Delivery["Sistema de Delivery<br/>Gerencia cardápios, pedidos, pagamentos e entregas"]
    Pagamento["Gateway de Pagamento<br/>Processa pagamentos"]
    Mapas["Serviço de Mapas<br/>Fornece localização e rotas"]

    Cliente -->|Faz e acompanha pedidos| Delivery
    Restaurante -->|Atualiza cardápio e pedido| Delivery
    Entregador -->|Atualiza a entrega| Delivery
    Delivery -->|Solicita cobranças e estornos| Pagamento
    Pagamento -->|Retorna o resultado do pagamento| Delivery
    Delivery -->|Consulta localizações e rotas| Mapas
    Mapas -->|Retorna mapas e rotas| Delivery
```

## 3. Nível 2 — Diagrama de contêineres

Este nível mostra as principais aplicações e bases de dados que formam o sistema.

```mermaid
flowchart LR
    Cliente[Cliente]
    Restaurante[Restaurante]
    Entregador[Entregador]
    Pagamento[Gateway de Pagamento]
    Mapas[Serviço de Mapas]

    subgraph Sistema["Sistema de Delivery"]
        AppCliente["Aplicativo do Cliente<br/>Interface para pedidos"]
        PortalRestaurante["Portal do Restaurante<br/>Gerenciamento de cardápio e pedidos"]
        AppEntregador["Aplicativo do Entregador<br/>Gerenciamento de entregas"]
        API["API Backend<br/>Regras de negócio e integração"]
        Banco[("Banco de Dados<br/>Usuários, produtos, pedidos e entregas")]
    end

    Cliente --> AppCliente
    Restaurante --> PortalRestaurante
    Entregador --> AppEntregador
    AppCliente -->|HTTPS/JSON| API
    PortalRestaurante -->|HTTPS/JSON| API
    AppEntregador -->|HTTPS/JSON| API
    API -->|Lê e grava| Banco
    API -->|Solicita cobranças e estornos| Pagamento
    Pagamento -->|Retorna o resultado do pagamento| API
    API -->|Consulta localizações e rotas| Mapas
    Mapas -->|Retorna mapas e rotas| API
```

## 4. Nível 3 — Diagrama de componentes da API

O terceiro nível detalha os componentes internos do contêiner **API Backend**.

```mermaid
flowchart LR
    Apps["Aplicativos e portal"]
    Gateway["Gateway de Pagamento"]
    Mapas["Serviço de Mapas"]
    Banco[(Banco de Dados)]

    subgraph API["API Backend"]
        Auth["Componente de Autenticação<br/>Login e autorização"]
        Catalogo["Componente de Catálogo<br/>Restaurantes e cardápios"]
        Pedidos["Componente de Pedidos<br/>Criação e atualização de pedidos"]
        Pagamentos["Componente de Pagamentos<br/>Cobranças e confirmações"]
        Entregas["Componente de Entregas<br/>Entregadores, localização e status"]
        Notificacoes["Componente de Notificações<br/>Avisos sobre o andamento do pedido"]
    end

    Apps --> Auth
    Apps --> Catalogo
    Apps --> Pedidos
    Apps --> Entregas
    Pedidos --> Pagamentos
    Pedidos --> Entregas
    Pedidos --> Notificacoes
    Pagamentos --> Gateway
    Entregas --> Mapas
    Auth --> Banco
    Catalogo --> Banco
    Pedidos --> Banco
    Pagamentos --> Banco
    Entregas --> Banco
    Notificacoes --> Banco
```

## 5. Nível 4 — Diagrama de código do componente de pedidos

O quarto nível representa uma possível estrutura de classes do componente de pedidos.

```mermaid
classDiagram
    class PedidoController {
        +criarPedido(dados)
        +consultarPedido(id)
        +atualizarStatus(id, status)
    }

    class PedidoService {
        +criarPedido(dados)
        +calcularTotal(itens)
        +atualizarStatus(id, status)
    }

    class PedidoRepository {
        +salvar(pedido)
        +buscarPorId(id)
        +atualizar(pedido)
    }

    class PagamentoService {
        +processarPagamento(pedido)
    }

    class EntregaService {
        +solicitarEntregador(pedido)
    }

    class Pedido {
        +id: UUID
        +status: StatusPedido
        +valorTotal: Decimal
        +adicionarItem(item)
        +alterarStatus(status)
    }

    PedidoController --> PedidoService
    PedidoService --> PedidoRepository
    PedidoService --> PagamentoService
    PedidoService --> EntregaService
    PedidoRepository --> Pedido
    PedidoService --> Pedido
```

## 6. Conclusão

O modelo C4 permite observar o mesmo sistema em diferentes níveis de abstração. O diagrama de contexto mostra as relações externas; o de contêineres apresenta as aplicações e os dados; o de componentes detalha a organização interna da API; e o diagrama de código representa as classes de uma parte específica do sistema.
