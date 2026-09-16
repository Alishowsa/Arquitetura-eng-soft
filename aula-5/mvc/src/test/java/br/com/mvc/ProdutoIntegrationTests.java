package br.com.mvc;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.mvc.dto.ProdutoForm;
import br.com.mvc.repository.ProdutoRepository;
import br.com.mvc.service.ProdutoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProdutoIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProdutoRepository repository;

    @Autowired
    ProdutoService service;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void crudCompletoRenderizaTelasEPersisteAlteracoes() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/produtos"));
        mvc.perform(get("/home")).andExpect(redirectedUrl("/produtos"));
        mvc.perform(get("/produtos"))
            .andExpect(status().isOk())
            .andExpect(
                content().string(containsString("Nenhum produto cadastrado"))
            );
        mvc.perform(get("/produtos/novo"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Salvar produto")));
        mvc.perform(
            post("/produtos")
                .param("nome", "Caderno")
                .param("descricao", "80 folhas")
                .param("preco", "19.90")
                .param("quantidade", "10")
        )
            .andExpect(redirectedUrl("/produtos"))
            .andExpect(flash().attributeExists("mensagem"));
        var produto = repository.findAll().getFirst();
        Long id = produto.getId();
        assertThat(produto.getPreco()).isEqualByComparingTo("19.90");
        mvc.perform(get("/produtos"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Caderno")))
            .andExpect(content().string(containsString("R$ 19,90")));
        mvc.perform(get("/produtos/{id}/editar", id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Caderno")));
        mvc.perform(
            post("/produtos/{id}", id)
                .param("nome", "Caderno grande")
                .param("descricao", "160 folhas")
                .param("preco", "29.90")
                .param("quantidade", "5")
        ).andExpect(redirectedUrl("/produtos"));
        assertThat(repository.count()).isEqualTo(1);
        produto = repository.findById(id).orElseThrow();
        assertThat(produto.getNome()).isEqualTo("Caderno grande");
        assertThat(produto.getQuantidade()).isEqualTo(5);
        mvc.perform(get("/produtos/{id}/excluir", id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Confirmar exclusão")));
        assertThat(repository.existsById(id)).isTrue();
        mvc.perform(post("/produtos/{id}/excluir", id)).andExpect(
            redirectedUrl("/produtos")
        );
        assertThat(repository.count()).isZero();
    }

    @Test
    void rejeitaDadosInvalidosSemGravar() throws Exception {
        String[][] invalidos = {
            { "   ", "10.00", "1" },
            { "Produto", "0", "1" },
            { "Produto", "-1", "1" },
            { "Produto", "1.001", "1" },
            { "Produto", "10000000000", "1" },
            { "Produto", "10", "-1" },
            { "Produto", "abc", "1" },
            { "Produto", "10", "1.5" },
            { "Produto", "", "1" },
            { "Produto", "10", "" },
            { "Produto", "10", "2147483648" },
            { "x".repeat(101), "10", "1" },
        };
        for (String[] valores : invalidos) {
            mvc.perform(
                post("/produtos")
                    .param("nome", valores[0])
                    .param("preco", valores[1])
                    .param("quantidade", valores[2])
            )
                .andExpect(status().isOk())
                .andExpect(model().hasErrors())
                .andExpect(content().string(containsString("Revise os dados")));
        }
        mvc.perform(
            post("/produtos")
                .param("nome", "Produto")
                .param("preco", "10")
                .param("quantidade", "1")
                .param("descricao", "x".repeat(501))
        ).andExpect(model().hasErrors());
        assertThat(repository.count()).isZero();
    }

    @Test
    void atualizacaoInvalidaPreservaProdutoEFormulario() throws Exception {
        ProdutoForm form = formValido();
        Long id = service.salvar(null, form).getId();
        mvc.perform(
            post("/produtos/{id}", id)
                .param("nome", "Alterado")
                .param("preco", "-10")
                .param("quantidade", "2")
        )
            .andExpect(status().isOk())
            .andExpect(model().hasErrors())
            .andExpect(model().attribute("produtoId", id));
        assertThat(repository.findById(id).orElseThrow().getNome()).isEqualTo(
            "Caneta"
        );
    }

    @Test
    void produtoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/produtos/999999/editar")).andExpect(
            status().isNotFound()
        );
        mvc.perform(get("/produtos/999999/excluir")).andExpect(
            status().isNotFound()
        );
        mvc.perform(post("/produtos/999999/excluir")).andExpect(
            status().isNotFound()
        );
        mvc.perform(
            post("/produtos/999999")
                .param("nome", "Produto")
                .param("preco", "1")
                .param("quantidade", "0")
        ).andExpect(status().isNotFound());
    }

    @Test
    void dominioValidaMesmoSemController() {
        ProdutoForm form = formValido();
        form.setQuantidade(-1);
        assertThatThrownBy(() -> service.salvar(null, form)).isInstanceOf(
            IllegalArgumentException.class
        );
        assertThat(repository.count()).isZero();
    }

    @Test
    void aceitaEstoqueZeroEEscapaHtmlNaListagem() throws Exception {
        ProdutoForm form = formValido();
        form.setNome("<script>alert(1)</script>");
        form.setQuantidade(0);
        service.salvar(null, form);
        mvc.perform(get("/produtos"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")));
        assertThat(repository.findAll().getFirst().getQuantidade()).isZero();
    }

    private ProdutoForm formValido() {
        ProdutoForm form = new ProdutoForm();
        form.setNome("Caneta");
        form.setPreco(new BigDecimal("2.50"));
        form.setQuantidade(10);
        return form;
    }
}
