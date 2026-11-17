package br.com.mvc.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String clienteNome;

    @Column(nullable = false, length = 120)
    private String clienteEmail;

    @Column(nullable = false, length = 14)
    private String clienteDocumento;

    @OneToMany(
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @JoinColumn(name = "pedido_id")
    @OrderBy("id")
    private List<ItemPedido> itens = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedido status;

    @Column(nullable = false)
    private LocalDateTime abertoEm;

    private LocalDateTime fechadoEm;

    private Long faturaId;

    @Column(length = 300)
    private String erroFaturamento;

    @Column(nullable = false)
    private int tentativasDeFaturamento;

    protected Pedido() {}

    public static Pedido abrir(
        String nome,
        String email,
        String documento,
        LocalDateTime agora
    ) {
        if (nome == null || nome.isBlank() || nome.strip().length() > 120) {
            throw new RegraDeNegocioException(
                "Informe o nome do cliente (até 120 caracteres)."
            );
        }
        if (
            email == null ||
            !email.strip().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
        ) {
            throw new RegraDeNegocioException(
                "Informe um e-mail de cliente válido."
            );
        }
        String digitos =
            documento == null ? "" : documento.replaceAll("\\D", "");
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new RegraDeNegocioException(
                "O documento deve ser um CPF (11 dígitos) ou CNPJ (14 dígitos)."
            );
        }
        Pedido pedido = new Pedido();
        pedido.clienteNome = nome.strip();
        pedido.clienteEmail = email.strip();
        pedido.clienteDocumento = digitos;
        pedido.status = StatusPedido.ABERTO;
        pedido.abertoEm = agora;
        return pedido;
    }

    public void adicionarItem(Produto produto, int quantidade) {
        exigirAberto();
        if (quantidade <= 0) {
            throw new RegraDeNegocioException(
                "A quantidade do item deve ser maior que zero."
            );
        }
        if (produto.getStatus() != StatusProduto.ATIVO) {
            throw new RegraDeNegocioException(
                produto.getNome() +
                    " está " +
                    produto.getStatus().getRotulo().toLowerCase() +
                    " e não pode ser vendido."
            );
        }
        itens
            .stream()
            .filter(i -> i.getProduto().getId().equals(produto.getId()))
            .findFirst()
            .ifPresentOrElse(
                i -> i.somar(quantidade),
                () -> itens.add(new ItemPedido(produto, quantidade))
            );
    }

    public void removerItem(Long itemId) {
        exigirAberto();
        if (!itens.removeIf(i -> i.getId().equals(itemId))) {
            throw new RegraDeNegocioException("Item não encontrado no pedido.");
        }
    }

    public void fechar(LocalDateTime agora) {
        exigirAberto();
        if (itens.isEmpty()) {
            throw new RegraDeNegocioException(
                "Adicione pelo menos um item antes de fechar o pedido."
            );
        }
        status = StatusPedido.FECHADO;
        fechadoEm = agora;
    }

    public void registrarFatura(Long idDaFatura) {
        exigirAguardandoHub();
        if (idDaFatura == null) {
            throw new RegraDeNegocioException(
                "O hub não devolveu o ID da fatura."
            );
        }
        tentativasDeFaturamento++;
        faturaId = idDaFatura;
        erroFaturamento = null;
        status = StatusPedido.FATURADO;
    }

    public void registrarFalhaNoFaturamento(String motivo) {
        exigirAguardandoHub();
        tentativasDeFaturamento++;
        erroFaturamento =
            motivo == null
                ? "Falha desconhecida."
                : motivo.substring(0, Math.min(300, motivo.length()));
        status = StatusPedido.FALHA_FATURAMENTO;
    }

    public boolean podeReenviarAoHub() {
        return status == StatusPedido.FALHA_FATURAMENTO;
    }

    public BigDecimal getTotal() {
        return itens
            .stream()
            .map(ItemPedido::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void exigirAberto() {
        if (status != StatusPedido.ABERTO) {
            throw new RegraDeNegocioException(
                "O pedido " + id + " já foi fechado e não pode ser alterado."
            );
        }
    }

    private void exigirAguardandoHub() {
        if (
            status != StatusPedido.FECHADO &&
            status != StatusPedido.FALHA_FATURAMENTO
        ) {
            throw new RegraDeNegocioException(
                "O pedido " + id + " não está aguardando faturamento."
            );
        }
    }

    public Long getId() {
        return id;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public List<ItemPedido> getItens() {
        return List.copyOf(itens);
    }

    public StatusPedido getStatus() {
        return status;
    }

    public Long getFaturaId() {
        return faturaId;
    }

    public String getErroFaturamento() {
        return erroFaturamento;
    }

    public int getTentativasDeFaturamento() {
        return tentativasDeFaturamento;
    }
}
