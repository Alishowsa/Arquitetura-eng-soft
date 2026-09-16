package br.com.mvc.dto;

import java.math.BigDecimal;

// Objeto exclusivo da entrada do formulário: o usuário não fornece o ID da entidade.
public class ProdutoForm {
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private Integer quantidade;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
}
