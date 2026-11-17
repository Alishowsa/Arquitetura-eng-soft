package br.com.mvc.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mvc.model.Produto.Transicao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProdutoTest {

    private static Produto produto(int estoque) {
        Produto produto = Produto.cadastrar(
            "Caderno",
            "80 folhas",
            new BigDecimal("19.90")
        );
        if (estoque > 0) {
            produto.acrescerEstoque(estoque);
        }
        return produto;
    }

    @Test
    void nasceIndisponivelComEstoqueZero() {
        Produto novo = produto(0);

        assertThat(novo.getStatus()).isEqualTo(StatusProduto.INDISPONIVEL);
        assertThat(novo.getQuantidade()).isZero();
    }

    @Test
    void baixarAteZeroDeixaIndisponivel() {
        Produto p = produto(3);

        assertThat(p.baixarEstoque(1)).isEqualTo(Transicao.NENHUMA);
        assertThat(p.getStatus()).isEqualTo(StatusProduto.DISPONIVEL);

        assertThat(p.baixarEstoque(2)).isEqualTo(Transicao.FICOU_INDISPONIVEL);
        assertThat(p.getQuantidade()).isZero();
        assertThat(p.getStatus()).isEqualTo(StatusProduto.INDISPONIVEL);
    }

    @Test
    void acrescerEmProdutoIndisponivelDeixaDisponivel() {
        Produto p = produto(0);

        assertThat(p.acrescerEstoque(10)).isEqualTo(Transicao.FICOU_DISPONIVEL);
        assertThat(p.getStatus()).isEqualTo(StatusProduto.DISPONIVEL);
        assertThat(p.acrescerEstoque(1)).isEqualTo(Transicao.NENHUMA);
        assertThat(p.getQuantidade()).isEqualTo(11);
    }

    @Test
    void naoBaixaMaisDoQueOSaldo() {
        Produto p = produto(2);

        assertThatThrownBy(() -> p.baixarEstoque(3))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("Estoque insuficiente");
        assertThat(p.getQuantidade()).isEqualTo(2);
    }

    @Test
    void quantidadeDaMovimentacaoPrecisaSerPositiva() {
        Produto p = produto(2);

        assertThatThrownBy(() -> p.acrescerEstoque(0)).hasMessageContaining(
            "maior que zero"
        );
        assertThatThrownBy(() -> p.baixarEstoque(-1)).hasMessageContaining(
            "maior que zero"
        );
    }

    @Test
    void descontinuarVaiDeDisponivelOuIndisponivelEEhFinal() {
        Produto disponivel = produto(4);
        Produto indisponivel = produto(0);

        disponivel.descontinuar();
        indisponivel.descontinuar();

        assertThat(disponivel.getStatus()).isEqualTo(
            StatusProduto.DESCONTINUADO
        );
        assertThat(indisponivel.getStatus()).isEqualTo(
            StatusProduto.DESCONTINUADO
        );
        assertThatThrownBy(disponivel::descontinuar).hasMessageContaining(
            "já está descontinuado"
        );
        assertThatThrownBy(() ->
            disponivel.acrescerEstoque(1)
        ).hasMessageContaining("descontinuado");
        assertThatThrownBy(() ->
            disponivel.baixarEstoque(1)
        ).hasMessageContaining("descontinuado");
    }

    @Test
    void perfisTemPermissoesDiferentes() {
        assertThat(Perfil.ADMINISTRADOR.podeCadastrarProdutos()).isTrue();
        assertThat(Perfil.ESTOQUE.podeCadastrarProdutos()).isFalse();
        assertThat(Perfil.VENDEDOR.podeCadastrarProdutos()).isFalse();

        assertThat(Perfil.ESTOQUE.podeMovimentarEstoque()).isTrue();
        assertThat(Perfil.ADMINISTRADOR.podeMovimentarEstoque()).isTrue();
        assertThat(Perfil.VENDEDOR.podeMovimentarEstoque()).isFalse();
    }
}
