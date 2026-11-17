package br.com.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mvc.model.Perfil;
import br.com.mvc.model.Produto;
import br.com.mvc.model.StatusProduto;
import br.com.mvc.model.TipoMovimentacao;
import br.com.mvc.repository.MovimentacaoRepository;
import br.com.mvc.repository.ProdutoRepository;
import br.com.mvc.service.AvisosAosVendedores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EstoqueIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProdutoRepository produtos;

    @Autowired
    MovimentacaoRepository movimentacoes;

    @Autowired
    AvisosAosVendedores avisos;

    @BeforeEach
    void limpar() {
        movimentacoes.deleteAll();
        produtos.deleteAll();
    }

    private static MockHttpSession sessao(Perfil perfil) {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute("perfil", perfil);
        return sessao;
    }

    private Long cadastrar(String nome, int estoque) throws Exception {
        mvc.perform(
            post("/produtos")
                .session(sessao(Perfil.ADMINISTRADOR))
                .param("nome", nome)
                .param("preco", "10.00")
        ).andExpect(redirectedUrl("/produtos"));
        Long id = produtos
            .findAll()
            .stream()
            .filter(p -> p.getNome().equals(nome))
            .findFirst()
            .orElseThrow()
            .getId();
        if (estoque > 0) {
            movimentar(id, TipoMovimentacao.ENTRADA, estoque, Perfil.ESTOQUE);
        }
        return id;
    }

    private void movimentar(
        Long id,
        TipoMovimentacao tipo,
        int qtd,
        Perfil perfil
    ) throws Exception {
        mvc.perform(
            post("/estoque")
                .session(sessao(perfil))
                .param("produtoId", id.toString())
                .param("tipo", tipo.name())
                .param("quantidade", String.valueOf(qtd))
                .param("motivo", "teste")
        ).andExpect(redirectedUrl("/estoque"));
    }

    @Test
    void cicloCompletoIndisponivelDisponivelIndisponivelDescontinuado() throws Exception {
        Long id = cadastrar("Caneta azul", 0);
        assertThat(produtos.findById(id).orElseThrow().getStatus()).isEqualTo(
            StatusProduto.INDISPONIVEL
        );

        movimentar(id, TipoMovimentacao.ENTRADA, 10, Perfil.ESTOQUE);
        Produto p = produtos.findById(id).orElseThrow();
        assertThat(p.getQuantidade()).isEqualTo(10);
        assertThat(p.getStatus()).isEqualTo(StatusProduto.DISPONIVEL);

        movimentar(id, TipoMovimentacao.SAIDA, 10, Perfil.ESTOQUE);
        assertThat(produtos.findById(id).orElseThrow().getStatus()).isEqualTo(
            StatusProduto.INDISPONIVEL
        );
        assertThat(avisos.recentes()).anyMatch(a ->
            a.contains("Caneta azul ficou indisponível")
        );

        mvc.perform(
            post("/produtos/{id}/descontinuar", id).session(
                sessao(Perfil.ADMINISTRADOR)
            )
        )
            .andExpect(redirectedUrl("/produtos"))
            .andExpect(
                flash().attribute("mensagem", "Caneta azul foi descontinuado.")
            );
        assertThat(produtos.findById(id).orElseThrow().getStatus()).isEqualTo(
            StatusProduto.DESCONTINUADO
        );

        assertThat(movimentacoes.count()).isEqualTo(2);
        mvc.perform(get("/estoque").session(sessao(Perfil.ESTOQUE)))
            .andExpect(status().isOk())
            .andExpect(
                content().string(containsString("Últimas movimentações"))
            )
            .andExpect(content().string(containsString("Caneta azul")));
    }

    @Test
    void saidaMaiorQueOSaldoNaoAlteraNada() throws Exception {
        Long id = cadastrar("Borracha", 2);

        mvc.perform(
            post("/estoque")
                .session(sessao(Perfil.ESTOQUE))
                .param("produtoId", id.toString())
                .param("tipo", "SAIDA")
                .param("quantidade", "5")
        )
            .andExpect(redirectedUrl("/estoque"))
            .andExpect(
                flash().attribute(
                    "erro",
                    containsString("Estoque insuficiente")
                )
            );

        assertThat(
            produtos.findById(id).orElseThrow().getQuantidade()
        ).isEqualTo(2);
        assertThat(movimentacoes.count()).isEqualTo(1);
    }

    @Test
    void produtoDescontinuadoNaoMovimenta() throws Exception {
        Long id = cadastrar("Régua", 5);
        mvc.perform(
            post("/produtos/{id}/descontinuar", id).session(
                sessao(Perfil.ADMINISTRADOR)
            )
        );

        mvc.perform(
            post("/estoque")
                .session(sessao(Perfil.ESTOQUE))
                .param("produtoId", id.toString())
                .param("tipo", "ENTRADA")
                .param("quantidade", "1")
        ).andExpect(flash().attribute("erro", containsString("descontinuado")));
        assertThat(
            produtos.findById(id).orElseThrow().getQuantidade()
        ).isEqualTo(5);
    }

    @Test
    void quantidadeInvalidaViraMensagemDeErro() throws Exception {
        Long id = cadastrar("Cola", 1);
        for (String qtd : new String[] { "0", "-3", "abc", "" }) {
            mvc.perform(
                post("/estoque")
                    .session(sessao(Perfil.ESTOQUE))
                    .param("produtoId", id.toString())
                    .param("tipo", "ENTRADA")
                    .param("quantidade", qtd)
            ).andExpect(flash().attributeExists("erro"));
        }
        assertThat(movimentacoes.count()).isEqualTo(1);
    }

    @Test
    void vendedorNaoMovimentaNemCadastraNemDescontinua() throws Exception {
        Long id = cadastrar("Lápis", 3);
        MockHttpSession vendedor = sessao(Perfil.VENDEDOR);

        mvc.perform(
            post("/estoque")
                .session(vendedor)
                .param("produtoId", id.toString())
                .param("tipo", "SAIDA")
                .param("quantidade", "1")
        ).andExpect(status().isForbidden());
        mvc.perform(
            post("/produtos")
                .session(vendedor)
                .param("nome", "X")
                .param("preco", "1")
                .param("quantidade", "1")
        ).andExpect(status().isForbidden());
        mvc.perform(
            post("/produtos/{id}/descontinuar", id).session(vendedor)
        ).andExpect(status().isForbidden());
        mvc.perform(get("/produtos/novo").session(vendedor)).andExpect(
            status().isForbidden()
        );

        assertThat(produtos.findById(id).orElseThrow().getStatus()).isEqualTo(
            StatusProduto.DISPONIVEL
        );
        mvc.perform(get("/produtos").session(vendedor))
            .andExpect(status().isOk())
            .andExpect(
                content().string(not(containsString("Cadastrar produto")))
            )
            .andExpect(content().string(not(containsString("Descontinuar"))));
    }

    @Test
    void timeDeEstoqueMovimentaMasNaoDescontinua() throws Exception {
        Long id = cadastrar("Tesoura", 1);

        movimentar(id, TipoMovimentacao.ENTRADA, 4, Perfil.ESTOQUE);
        mvc.perform(
            post("/produtos/{id}/descontinuar", id).session(
                sessao(Perfil.ESTOQUE)
            )
        ).andExpect(status().isForbidden());

        assertThat(
            produtos.findById(id).orElseThrow().getQuantidade()
        ).isEqualTo(5);
    }

    @Test
    void produtoComHistoricoNaoPodeSerExcluidoSoDescontinuado() throws Exception {
        Long semHistorico = cadastrar("Clips", 0);
        Long comHistorico = cadastrar("Grampo", 1);
        movimentar(
            comHistorico,
            TipoMovimentacao.ENTRADA,
            1,
            Perfil.ADMINISTRADOR
        );

        mvc.perform(
            post("/produtos/{id}/excluir", comHistorico).session(
                sessao(Perfil.ADMINISTRADOR)
            )
        ).andExpect(flash().attribute("erro", containsString("Descontinue")));
        mvc.perform(
            post("/produtos/{id}/excluir", semHistorico).session(
                sessao(Perfil.ADMINISTRADOR)
            )
        ).andExpect(
            flash().attribute("mensagem", "Produto excluído com sucesso.")
        );

        assertThat(produtos.existsById(comHistorico)).isTrue();
        assertThat(produtos.existsById(semHistorico)).isFalse();
    }

    @Test
    void edicaoNaoMexeNoSaldoEValidaDados() throws Exception {
        Long id = cadastrar("Mochila", 7);
        MockHttpSession admin = sessao(Perfil.ADMINISTRADOR);

        mvc.perform(
            post("/produtos/{id}", id)
                .session(admin)
                .param("nome", "Mochila escolar")
                .param("preco", "99.90")
                .param("quantidade", "999")
        ).andExpect(redirectedUrl("/produtos"));
        Produto p = produtos.findById(id).orElseThrow();
        assertThat(p.getNome()).isEqualTo("Mochila escolar");
        assertThat(p.getQuantidade()).isEqualTo(7);

        mvc.perform(
            post("/produtos/{id}", id)
                .session(admin)
                .param("nome", " ")
                .param("preco", "-1")
        )
            .andExpect(status().isOk())
            .andExpect(model().hasErrors());
    }

    @Test
    void produtoNovoNasceIndisponivelComEstoqueZero() throws Exception {
        mvc.perform(
            post("/produtos")
                .session(sessao(Perfil.ADMINISTRADOR))
                .param("nome", "Estojo")
                .param("preco", "15.00")
                .param("quantidade", "50")
        ).andExpect(redirectedUrl("/produtos"));

        Produto p = produtos.findAll().getFirst();
        assertThat(p.getStatus()).isEqualTo(StatusProduto.INDISPONIVEL);
        assertThat(p.getQuantidade()).isZero();
        assertThat(movimentacoes.count()).isZero();
    }

    @Test
    void trocaDePerfilFicaNaSessaoENaoRedirecionaParaFora() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(
            post("/perfil")
                .session(sessao)
                .param("perfil", "ESTOQUE")
                .param("voltarPara", "/estoque")
        ).andExpect(redirectedUrl("/estoque"));
        assertThat(sessao.getAttribute("perfil")).isEqualTo(Perfil.ESTOQUE);

        mvc.perform(
            post("/perfil")
                .session(sessao)
                .param("perfil", "VENDEDOR")
                .param("voltarPara", "//malicioso.com")
        ).andExpect(redirectedUrl("/produtos"));
    }

    @Test
    void produtoInexistenteRetorna404() throws Exception {
        mvc.perform(
            get("/produtos/999999/editar").session(sessao(Perfil.ADMINISTRADOR))
        ).andExpect(status().isNotFound());
    }
}
