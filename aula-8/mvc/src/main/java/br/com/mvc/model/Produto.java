package br.com.mvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProduto status;

    protected Produto() {}

    public static Produto cadastrar(
        String nome,
        String descricao,
        BigDecimal preco
    ) {
        Produto produto = new Produto();
        produto.alterarDados(nome, descricao, preco);
        produto.quantidade = 0;
        produto.status = StatusProduto.INDISPONIVEL;
        return produto;
    }

    public void alterarDados(String nome, String descricao, BigDecimal preco) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
    }

    public Transicao acrescerEstoque(int unidades) {
        exigirMovimentavel(unidades);
        boolean estavaIndisponivel = status == StatusProduto.INDISPONIVEL;
        quantidade += unidades;
        if (estavaIndisponivel) {
            status = StatusProduto.DISPONIVEL;
            return Transicao.FICOU_DISPONIVEL;
        }
        return Transicao.NENHUMA;
    }

    public Transicao baixarEstoque(int unidades) {
        exigirMovimentavel(unidades);
        if (unidades > quantidade) {
            throw new RegraDeNegocioException(
                "Estoque insuficiente de " +
                    nome +
                    ": saldo " +
                    quantidade +
                    ", pedido " +
                    unidades +
                    "."
            );
        }
        quantidade -= unidades;
        if (quantidade == 0) {
            status = StatusProduto.INDISPONIVEL;
            return Transicao.FICOU_INDISPONIVEL;
        }
        return Transicao.NENHUMA;
    }

    public void descontinuar() {
        if (status == StatusProduto.DESCONTINUADO) {
            throw new RegraDeNegocioException(nome + " já está descontinuado.");
        }
        status = StatusProduto.DESCONTINUADO;
    }

    public boolean isDescontinuado() {
        return status == StatusProduto.DESCONTINUADO;
    }

    private void exigirMovimentavel(int unidades) {
        if (status == StatusProduto.DESCONTINUADO) {
            throw new RegraDeNegocioException(
                nome + " está descontinuado e não pode ser movimentado."
            );
        }
        if (unidades <= 0) {
            throw new RegraDeNegocioException(
                "A quantidade movimentada deve ser maior que zero."
            );
        }
    }

    public enum Transicao {
        NENHUMA,
        FICOU_INDISPONIVEL,
        FICOU_DISPONIVEL,
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public StatusProduto getStatus() {
        return status;
    }
}
