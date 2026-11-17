package br.com.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mvc.model.Pedido;
import br.com.mvc.model.Perfil;
import br.com.mvc.model.Produto;
import br.com.mvc.model.RegraDeNegocioException;
import br.com.mvc.model.StatusPedido;
import br.com.mvc.model.StatusProduto;
import br.com.mvc.repository.MovimentacaoRepository;
import br.com.mvc.repository.PedidoRepository;
import br.com.mvc.repository.ProdutoRepository;
import br.com.mvc.service.FechamentoDePedido;
import br.com.mvc.service.PedidoService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class IntegracaoComHubTests {

    enum Modo {
        RESPONDE,
        DERRUBA_CONEXAO,
        DEMORA,
        RECUSA,
    }

    static final HubFalso HUB = HubFalso.iniciar();

    @DynamicPropertySource
    static void apontarParaOHubFalso(DynamicPropertyRegistry registro) {
        registro.add("hub.url", () -> "http://127.0.0.1:" + HUB.porta());
        registro.add("hub.timeout-ms", () -> "500");
    }

    @AfterAll
    static void pararHub() {
        HUB.parar();
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    PedidoService pedidos;

    @Autowired
    FechamentoDePedido fechamento;

    @Autowired
    PedidoRepository pedidoRepository;

    @Autowired
    ProdutoRepository produtos;

    @Autowired
    MovimentacaoRepository movimentacoes;

    @BeforeEach
    void limpar() {
        pedidoRepository.deleteAll();
        movimentacoes.deleteAll();
        produtos.deleteAll();
        HUB.modo = Modo.RESPONDE;
        HUB.recebidos.clear();
    }

    private Produto produto(String nome, int estoque) {
        return produtos.save(
            Produto.cadastrar(nome, "", new BigDecimal("12.50"), estoque)
        );
    }

    private Pedido pedidoCom(Produto produto, int quantidade) {
        Pedido p = pedidos.abrir(
            "Ana Souza",
            "ana@loja.com",
            "123.456.789-09",
            Perfil.VENDEDOR
        );
        pedidos.adicionarItem(
            p.getId(),
            produto.getId(),
            quantidade,
            Perfil.VENDEDOR
        );
        return p;
    }

    @Test
    void fecharPedidoBaixaEstoqueChamaOHubEGuardaOIdDaFatura() {
        Produto caderno = produto("Caderno", 5);
        Pedido pedido = pedidoCom(caderno, 2);

        Pedido fechado = fechamento.fechar(pedido.getId(), Perfil.VENDEDOR);

        assertThat(fechado.getStatus()).isEqualTo(StatusPedido.FATURADO);
        assertThat(fechado.getFaturaId()).isEqualTo(HUB.ultimoId.get());
        assertThat(
            produtos.findById(caderno.getId()).orElseThrow().getQuantidade()
        ).isEqualTo(3);
        assertThat(HUB.recebidos).hasSize(1);
        assertThat(HUB.recebidos.getFirst())
            .contains("\"pedidoId\":" + pedido.getId())
            .contains("\"documento\":\"12345678909\"")
            .contains("\"descricao\":\"Caderno\"")
            .contains("\"quantidade\":2")
            .contains("\"valorUnitario\":12.50");
    }

    @Test
    void hubForaDoArDeixaPedidoFechadoComFalhaEReenvioFatura() {
        Produto caneta = produto("Caneta", 10);
        Pedido pedido = pedidoCom(caneta, 4);
        HUB.modo = Modo.DERRUBA_CONEXAO;

        Pedido comFalha = fechamento.fechar(pedido.getId(), Perfil.VENDEDOR);

        assertThat(comFalha.getStatus()).isEqualTo(
            StatusPedido.FALHA_FATURAMENTO
        );
        assertThat(comFalha.getFaturaId()).isNull();
        assertThat(comFalha.getErroFaturamento()).contains("Hub fora do ar");
        assertThat(
            produtos.findById(caneta.getId()).orElseThrow().getQuantidade()
        ).isEqualTo(6);

        HUB.modo = Modo.RESPONDE;
        Pedido reenviado = fechamento.reenviarAoHub(
            pedido.getId(),
            Perfil.VENDEDOR
        );

        assertThat(reenviado.getStatus()).isEqualTo(StatusPedido.FATURADO);
        assertThat(reenviado.getTentativasDeFaturamento()).isEqualTo(2);
        assertThat(
            produtos.findById(caneta.getId()).orElseThrow().getQuantidade()
        ).isEqualTo(6);
    }

    @Test
    void hubLentoEstouraOTimeoutSemPrenderAVenda() {
        Pedido pedido = pedidoCom(produto("Régua", 3), 1);
        HUB.modo = Modo.DEMORA;

        long inicio = System.nanoTime();
        Pedido comFalha = fechamento.fechar(pedido.getId(), Perfil.VENDEDOR);
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(comFalha.getStatus()).isEqualTo(
            StatusPedido.FALHA_FATURAMENTO
        );
        assertThat(comFalha.getErroFaturamento()).contains("500 ms");
        assertThat(ms).isLessThan(1_500);
    }

    @Test
    void hubRecusandoOPedidoViraFalhaComOMotivo() {
        Pedido pedido = pedidoCom(produto("Cola", 3), 1);
        HUB.modo = Modo.RECUSA;

        Pedido comFalha = fechamento.fechar(pedido.getId(), Perfil.VENDEDOR);

        assertThat(comFalha.getErroFaturamento())
            .contains("HTTP 422")
            .contains("documento");
    }

    @Test
    void semEstoqueNadaEhBaixadoNemEnviadoEOPedidoContinuaAberto() {
        Produto lapis = produto("Lápis", 1);
        Produto borracha = produto("Borracha", 5);
        Pedido pedido = pedidos.abrir(
            "Ana",
            "ana@loja.com",
            "12345678909",
            Perfil.VENDEDOR
        );
        pedidos.adicionarItem(
            pedido.getId(),
            borracha.getId(),
            2,
            Perfil.VENDEDOR
        );
        pedidos.adicionarItem(
            pedido.getId(),
            lapis.getId(),
            3,
            Perfil.VENDEDOR
        );

        assertThatThrownBy(() ->
            fechamento.fechar(pedido.getId(), Perfil.VENDEDOR)
        )
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("Estoque insuficiente");

        assertThat(pedidos.buscar(pedido.getId()).getStatus()).isEqualTo(
            StatusPedido.ABERTO
        );
        assertThat(
            produtos.findById(borracha.getId()).orElseThrow().getQuantidade()
        ).isEqualTo(5);
        assertThat(movimentacoes.count()).isZero();
        assertThat(HUB.recebidos).isEmpty();
    }

    @Test
    void ultimaUnidadeVendidaEsgotaOProduto() {
        Produto mochila = produto("Mochila", 1);
        fechamento.fechar(pedidoCom(mochila, 1).getId(), Perfil.VENDEDOR);

        assertThat(
            produtos.findById(mochila.getId()).orElseThrow().getStatus()
        ).isEqualTo(StatusProduto.ESGOTADO);
    }

    @Test
    void fluxoPelasTelasComPerfilVendedor() throws Exception {
        MockHttpSession vendedor = new MockHttpSession();
        vendedor.setAttribute("perfil", Perfil.VENDEDOR);
        Produto tesoura = produto("Tesoura", 4);

        mvc.perform(
            post("/pedidos")
                .session(vendedor)
                .param("nome", "Bruno Lima")
                .param("email", "bruno@loja.com")
                .param("documento", "11.222.333/0001-81")
        ).andExpect(status().is3xxRedirection());
        Long id = pedidoRepository.findAll().getFirst().getId();

        mvc.perform(
            post("/pedidos/{id}/itens", id)
                .session(vendedor)
                .param("produtoId", tesoura.getId().toString())
                .param("quantidade", "2")
        ).andExpect(redirectedUrl("/pedidos/" + id));
        mvc.perform(post("/pedidos/{id}/fechar", id).session(vendedor))
            .andExpect(redirectedUrl("/pedidos/" + id))
            .andExpect(
                flash().attribute("mensagem", containsString("Pedido faturado"))
            );

        mvc.perform(get("/pedidos/{id}", id).session(vendedor))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Faturado")))
            .andExpect(content().string(containsString("ver no hub")));
        mvc.perform(
            post("/pedidos/{id}/itens", id)
                .session(vendedor)
                .param("produtoId", tesoura.getId().toString())
                .param("quantidade", "1")
        ).andExpect(
            flash().attribute("erro", containsString("já foi fechado"))
        );
    }

    @Test
    void timeDeEstoqueNaoVendeEPedidoInexistenteDa404() throws Exception {
        MockHttpSession estoque = new MockHttpSession();
        estoque.setAttribute("perfil", Perfil.ESTOQUE);

        mvc.perform(
            post("/pedidos")
                .session(estoque)
                .param("nome", "X")
                .param("email", "x@y.com")
                .param("documento", "12345678909")
        ).andExpect(status().isForbidden());
        mvc.perform(get("/pedidos/999999")).andExpect(status().isNotFound());
    }

    static final class HubFalso {

        private final HttpServer servidor;
        volatile Modo modo = Modo.RESPONDE;
        final List<String> recebidos = new CopyOnWriteArrayList<>();
        final AtomicLong ultimoId = new AtomicLong(100);

        private HubFalso(HttpServer servidor) {
            this.servidor = servidor;
        }

        static HubFalso iniciar() {
            try {
                HttpServer servidor = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", 0),
                    0
                );
                HubFalso hub = new HubFalso(servidor);
                servidor.createContext("/faturas", troca -> {
                    String corpo = new String(
                        troca.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                    );
                    switch (hub.modo) {
                        case DERRUBA_CONEXAO -> {
                            troca.close();
                            return;
                        }
                        case DEMORA -> dormir(1_500);
                        case RECUSA -> {
                            responder(
                                troca,
                                422,
                                "{\"detail\":\"O documento do cliente é inválido.\"}"
                            );
                            return;
                        }
                        case RESPONDE -> {
                        }
                    }
                    hub.recebidos.add(corpo);
                    long id = hub.ultimoId.incrementAndGet();
                    responder(
                        troca,
                        201,
                        "{\"id\":" + id + ",\"status\":\"COM_NOTA\"}"
                    );
                });
                servidor.setExecutor(Executors.newCachedThreadPool());
                servidor.start();
                return hub;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        int porta() {
            return servidor.getAddress().getPort();
        }

        void parar() {
            servidor.stop(0);
        }

        private static void responder(
            com.sun.net.httpserver.HttpExchange troca,
            int status,
            String json
        ) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            try {
                troca.sendResponseHeaders(status, bytes.length);
                troca.getResponseBody().write(bytes);
            } catch (IOException clienteDesistiu) {
                // o cliente já estourou o timeout e fechou a conexão
            } finally {
                troca.close();
            }
        }

        private static void dormir(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
