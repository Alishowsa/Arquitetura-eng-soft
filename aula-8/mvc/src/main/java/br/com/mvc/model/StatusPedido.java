package br.com.mvc.model;

public enum StatusPedido {
    ABERTO("Aberto"),
    FECHADO("Fechado, aguardando o hub"),
    FATURADO("Faturado"),
    FALHA_FATURAMENTO("Falha no faturamento");

    private final String rotulo;

    StatusPedido(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
