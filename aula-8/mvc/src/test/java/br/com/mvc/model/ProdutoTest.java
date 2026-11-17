package br.com.mvc.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.mvc.model.Produto.Transicao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProdutoTest {

    private static Produto produto(int estoqueInicial) {
        return Produto.cadastrar(
            "Caderno",
            "80 folhas",
            new BigDecimal("19.90"),
            estoqueInicial
        );
    }

    @Test
    void nasceAtivoComEstoqueEEsgotadoSemEstoque() {
        assertThat(produto(5).getStatus()).isEqualTo(StatusProduto.ATIVO);
        assertThat(produto(0).getStatus()).isEqualTo(StatusProduto.ESGOTADO);
    }

    @Test
    void baixarAteZeroEsgota() {
        Produto p = produto(3);

        assertThat(p.baixarEstoque(1)).isEqualTo(Transicao.NENHUMA);
        assertThat(p.getStatus()).isEqualTo(StatusProduto.ATIVO);

        assertThat(p.baixarEstoque(2)).isEqualTo(Transicao.ESGOTOU);
        assertThat(p.getQuantidade()).isZero();
        assertThat(p.getStatus()).isEqualTo(StatusProduto.ESGOTADO);
    }

    @Test
    void acrescerEmProdutoEsgotadoReativa() {
        Produto p = produto(0);

        assertThat(p.acrescerEstoque(10)).isEqualTo(
            Transicao.VOLTOU_AO_ESTOQUE
        );
        assertThat(p.getStatus()).isEqualTo(StatusProduto.ATIVO);
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
    void descontinuarVaiDeAtivoOuEsgotadoEEhFinal() {
        Produto ativo = produto(4);
        Produto esgotado = produto(0);

        ativo.descontinuar();
        esgotado.descontinuar();

        assertThat(ativo.getStatus()).isEqualTo(StatusProduto.DESCONTINUADO);
        assertThat(esgotado.getStatus()).isEqualTo(StatusProduto.DESCONTINUADO);
        assertThatThrownBy(ativo::descontinuar).hasMessageContaining(
            "já está descontinuado"
        );
        assertThatThrownBy(() -> ativo.acrescerEstoque(1)).hasMessageContaining(
            "descontinuado"
        );
        assertThatThrownBy(() -> ativo.baixarEstoque(1)).hasMessageContaining(
            "descontinuado"
        );
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
