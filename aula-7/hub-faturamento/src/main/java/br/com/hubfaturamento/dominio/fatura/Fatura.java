package br.com.hubfaturamento.dominio.fatura;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Fatura {

    private final Long id;
    private final Long pedidoId;
    private final Cliente cliente;
    private final List<ItemFaturado> itens;
    private final BigDecimal valorTotal;
    private final LocalDateTime criadaEm;
    private StatusFatura status;
    private String codigoBarras;
    private String chaveNfe;
    private String hashEmail;

    private Fatura(
        Long id,
        Long pedidoId,
        Cliente cliente,
        List<ItemFaturado> itens,
        LocalDateTime criadaEm,
        StatusFatura status,
        String codigoBarras,
        String chaveNfe,
        String hashEmail
    ) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.cliente = cliente;
        this.itens = List.copyOf(itens);
        this.valorTotal = this.itens
            .stream()
            .map(ItemFaturado::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.criadaEm = criadaEm;
        this.status = status;
        this.codigoBarras = codigoBarras;
        this.chaveNfe = chaveNfe;
        this.hashEmail = hashEmail;
    }

    public static Fatura criar(
        Long pedidoId,
        Cliente cliente,
        List<ItemFaturado> itens,
        LocalDateTime agora
    ) {
        if (pedidoId == null || pedidoId <= 0) {
            throw new FaturaInvalidaException(
                "A fatura precisa de um pedido válido."
            );
        }
        if (cliente == null) {
            throw new FaturaInvalidaException(
                "A fatura precisa de um cliente."
            );
        }
        if (itens == null || itens.isEmpty()) {
            throw new FaturaInvalidaException(
                "A fatura precisa de pelo menos um item."
            );
        }
        return new Fatura(
            null,
            pedidoId,
            cliente,
            itens,
            agora,
            StatusFatura.CRIADA,
            null,
            null,
            null
        );
    }

    public static Fatura restaurar(
        Long id,
        Long pedidoId,
        Cliente cliente,
        List<ItemFaturado> itens,
        LocalDateTime criadaEm,
        StatusFatura status,
        String codigoBarras,
        String chaveNfe,
        String hashEmail
    ) {
        return new Fatura(
            id,
            pedidoId,
            cliente,
            itens,
            criadaEm,
            status,
            codigoBarras,
            chaveNfe,
            hashEmail
        );
    }

    public Fatura comId(Long novoId) {
        return new Fatura(
            novoId,
            pedidoId,
            cliente,
            itens,
            criadaEm,
            status,
            codigoBarras,
            chaveNfe,
            hashEmail
        );
    }

    public void registrarBoleto(String codigo) {
        if (!DigitoVerificador.codigoDeBarrasValido(codigo)) {
            throw new FaturaInvalidaException(
                "Código de barras inválido: são 44 dígitos com DV módulo 11 na 5ª posição."
            );
        }
        if (codigoBarras != null) {
            if (codigoBarras.equals(codigo)) {
                return;
            }
            throw new FaturaInvalidaException(
                "A fatura " + id + " já tem outro boleto."
            );
        }
        codigoBarras = codigo;
        status =
            chaveNfe == null
                ? StatusFatura.COM_BOLETO
                : StatusFatura.FINALIZADA;
    }

    public void registrarNotaFiscal(String chaveDeAcesso) {
        if (!DigitoVerificador.chaveDeAcessoValida(chaveDeAcesso)) {
            throw new FaturaInvalidaException(
                "Chave de acesso da NF-e inválida: são 44 dígitos com DV módulo 11 no final."
            );
        }
        if (chaveNfe != null) {
            if (chaveNfe.equals(chaveDeAcesso)) {
                return;
            }
            throw new FaturaInvalidaException(
                "A fatura " + id + " já tem outra nota fiscal."
            );
        }
        chaveNfe = chaveDeAcesso;
        status =
            codigoBarras == null
                ? StatusFatura.COM_NOTA
                : StatusFatura.FINALIZADA;
    }

    public void registrarClienteNotificado(String hashSha256) {
        if (status != StatusFatura.FINALIZADA) {
            throw new FaturaInvalidaException(
                "Só uma fatura FINALIZADA pode ser enviada ao cliente."
            );
        }
        if (hashSha256 == null || !hashSha256.matches("[0-9a-f]{64}")) {
            throw new FaturaInvalidaException(
                "O hash do e-mail deve ser um SHA-256 (64 caracteres hexadecimais)."
            );
        }
        hashEmail = hashSha256;
        status = StatusFatura.NOTIFICADA;
    }

    public boolean prontaParaNotificar() {
        return status == StatusFatura.FINALIZADA;
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public List<ItemFaturado> getItens() {
        return itens;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public StatusFatura getStatus() {
        return status;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public String getChaveNfe() {
        return chaveNfe;
    }

    public String getHashEmail() {
        return hashEmail;
    }

    @Override
    public boolean equals(Object outro) {
        return (
            outro instanceof Fatura f && id != null && Objects.equals(id, f.id)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
