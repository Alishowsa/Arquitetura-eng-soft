package br.com.hubfaturamento.dominio.fatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FaturaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(
        2026,
        11,
        17,
        10,
        0
    );
    private static final Cliente CLIENTE = new Cliente(
        "Ana Souza",
        "ana@loja.com",
        "123.456.789-09"
    );
    private static final List<ItemFaturado> ITENS = List.of(
        new ItemFaturado("Caderno", 2, new BigDecimal("19.90")),
        new ItemFaturado("Caneta", 10, new BigDecimal("2.50"))
    );

    private static final String BOLETO = CodigosDeTeste.codigoDeBarras(
        "3419" + "1234" + "0000006480" + "0".repeat(24) + "1"
    );
    private static final String CHAVE = CodigosDeTeste.chaveDeAcesso(
        "35" +
            "2611" +
            "11222333000181" +
            "55" +
            "001" +
            "000000001" +
            "1" +
            "00007919"
    );

    private static Fatura nova() {
        return Fatura.criar(31L, CLIENTE, ITENS, AGORA).comId(12L);
    }

    @Test
    void nasceCriadaComValorTotalSomadoDosItens() {
        Fatura f = nova();

        assertThat(f.getStatus()).isEqualTo(StatusFatura.CRIADA);
        assertThat(f.getValorTotal()).isEqualByComparingTo("64.80");
        assertThat(f.getCliente().documento()).isEqualTo("12345678909");
    }

    @Test
    void naoExisteFaturaSemItensSemClienteOuComItemInvalido() {
        assertThatThrownBy(() ->
            Fatura.criar(31L, CLIENTE, List.of(), AGORA)
        ).hasMessageContaining("pelo menos um item");
        assertThatThrownBy(() ->
            Fatura.criar(31L, null, ITENS, AGORA)
        ).hasMessageContaining("cliente");
        assertThatThrownBy(() ->
            Fatura.criar(0L, CLIENTE, ITENS, AGORA)
        ).hasMessageContaining("pedido");
        assertThatThrownBy(() ->
            new ItemFaturado("X", 0, BigDecimal.TEN)
        ).hasMessageContaining("quantidade");
        assertThatThrownBy(() ->
            new ItemFaturado("X", 1, BigDecimal.ZERO)
        ).hasMessageContaining("valor unitário");
        assertThatThrownBy(() ->
            new ItemFaturado("X", 1, new BigDecimal("1.999"))
        ).hasMessageContaining("duas casas");
    }

    @Test
    void clienteExigeEmailEDocumentoValidos() {
        assertThatThrownBy(() ->
            new Cliente("Ana", "sem-arroba", "12345678909")
        ).hasMessageContaining("e-mail");
        assertThatThrownBy(() ->
            new Cliente("Ana", "a@b.com", "123")
        ).hasMessageContaining("CPF");
        assertThat(
            new Cliente("Loja", "a@b.com", "11.222.333/0001-81").documento()
        ).hasSize(14);
    }

    @Test
    void boletoDepoisNotaFinaliza() {
        Fatura f = nova();
        f.registrarBoleto(BOLETO);
        assertThat(f.getStatus()).isEqualTo(StatusFatura.COM_BOLETO);

        f.registrarNotaFiscal(CHAVE);
        assertThat(f.getStatus()).isEqualTo(StatusFatura.FINALIZADA);
        assertThat(f.prontaParaNotificar()).isTrue();
    }

    @Test
    void notaDepoisBoletoTambemFinaliza() {
        Fatura f = nova();
        f.registrarNotaFiscal(CHAVE);
        assertThat(f.getStatus()).isEqualTo(StatusFatura.COM_NOTA);

        f.registrarBoleto(BOLETO);
        assertThat(f.getStatus()).isEqualTo(StatusFatura.FINALIZADA);
    }

    @Test
    void mesmoBoletoDuasVezesNaoMudaNadaMasOutroBoletoEhErro() {
        Fatura f = nova();
        f.registrarBoleto(BOLETO);
        f.registrarBoleto(BOLETO);

        assertThat(f.getStatus()).isEqualTo(StatusFatura.COM_BOLETO);
        String outroValido = CodigosDeTeste.codigoDeBarras(
            "3419" + "1234" + "0000006480" + "0".repeat(24) + "2"
        );
        assertThatThrownBy(() ->
            f.registrarBoleto(outroValido)
        ).hasMessageContaining("já tem outro boleto");
    }

    @Test
    void recusaCodigoDeBarrasEChaveComTamanhoOuDvErrado() {
        Fatura f = nova();
        assertThatThrownBy(() -> f.registrarBoleto("123")).hasMessageContaining(
            "44 dígitos"
        );
        String dvErrado =
            BOLETO.substring(0, 4) +
            ((BOLETO.charAt(4) - '0' + 1) % 10) +
            BOLETO.substring(5);
        assertThatThrownBy(() ->
            f.registrarBoleto(dvErrado)
        ).hasMessageContaining("DV");

        String chaveDvErrado =
            CHAVE.substring(0, 43) + ((CHAVE.charAt(43) - '0' + 1) % 10);
        assertThatThrownBy(() ->
            f.registrarNotaFiscal(chaveDvErrado)
        ).hasMessageContaining("Chave de acesso");
    }

    @Test
    void soNotificaFaturaFinalizadaComHashSha256() {
        Fatura f = nova();
        String hash = "a".repeat(64);
        assertThatThrownBy(() ->
            f.registrarClienteNotificado(hash)
        ).hasMessageContaining("FINALIZADA");

        f.registrarBoleto(BOLETO);
        f.registrarNotaFiscal(CHAVE);
        assertThatThrownBy(() ->
            f.registrarClienteNotificado("abc")
        ).hasMessageContaining("SHA-256");
        f.registrarClienteNotificado(hash);

        assertThat(f.getStatus()).isEqualTo(StatusFatura.NOTIFICADA);
        assertThat(f.getHashEmail()).isEqualTo(hash);
    }
}
