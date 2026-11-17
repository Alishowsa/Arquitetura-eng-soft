package br.com.hubfaturamento.aplicacao;

import br.com.hubfaturamento.dominio.fatura.Cliente;
import br.com.hubfaturamento.dominio.fatura.CodigosDeTeste;
import br.com.hubfaturamento.dominio.fatura.ItemFaturado;
import java.math.BigDecimal;
import java.util.List;

final class FaturaTestDados {

    static final Cliente CLIENTE = new Cliente(
        "Ana Souza",
        "ana@loja.com",
        "12345678909"
    );
    static final List<ItemFaturado> ITENS = List.of(
        new ItemFaturado("Caderno", 2, new BigDecimal("19.90"))
    );
    static final String BOLETO = CodigosDeTeste.codigoDeBarras(
        "3419" + "1234" + "0000003980" + "0".repeat(24) + "7"
    );
    static final String CHAVE = CodigosDeTeste.chaveDeAcesso(
        "35" +
            "2611" +
            "11222333000181" +
            "55" +
            "001" +
            "000000042" +
            "1" +
            "12345678"
    );
    static final String HASH = "f".repeat(64);

    private FaturaTestDados() {}
}
