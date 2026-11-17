package br.com.mvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentacao tipo;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private Integer saldoApos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime registradaEm;

    protected Movimentacao() {}

    public Movimentacao(
        Produto produto,
        TipoMovimentacao tipo,
        int quantidade,
        Perfil perfil,
        String motivo,
        LocalDateTime registradaEm
    ) {
        this.produto = produto;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.saldoApos = produto.getQuantidade();
        this.perfil = perfil;
        this.motivo = motivo;
        this.registradaEm = registradaEm;
    }

    public Long getId() {
        return id;
    }

    public Produto getProduto() {
        return produto;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public Integer getSaldoApos() {
        return saldoApos;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getRegistradaEm() {
        return registradaEm;
    }
}
