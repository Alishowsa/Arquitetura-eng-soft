package br.com.hubfaturamento.infraestrutura.saida.persistencia;

import br.com.hubfaturamento.dominio.fatura.StatusFatura;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fatura")
class FaturaTabela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    Long pedidoId;

    @Column(nullable = false, length = 120)
    String clienteNome;

    @Column(nullable = false, length = 120)
    String clienteEmail;

    @Column(nullable = false, length = 14)
    String clienteDocumento;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "fatura_item",
        joinColumns = @JoinColumn(name = "fatura_id")
    )
    @OrderColumn(name = "posicao")
    List<ItemTabela> itens = new ArrayList<>();

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal valorTotal;

    @Column(nullable = false)
    LocalDateTime criadaEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    StatusFatura status;

    @Column(length = 44)
    String codigoBarras;

    @Column(length = 44)
    String chaveNfe;

    @Column(length = 64)
    String hashEmail;

    protected FaturaTabela() {}

    @Embeddable
    static class ItemTabela {

        @Column(nullable = false, length = 120)
        String descricao;

        @Column(nullable = false)
        int quantidade;

        @Column(nullable = false, precision = 12, scale = 2)
        BigDecimal valorUnitario;

        protected ItemTabela() {}

        ItemTabela(String descricao, int quantidade, BigDecimal valorUnitario) {
            this.descricao = descricao;
            this.quantidade = quantidade;
            this.valorUnitario = valorUnitario;
        }
    }
}
