package br.com.mvc.model;

public enum StatusProduto {
    ATIVO("Ativo"),
    ESGOTADO("Esgotado"),
    DESCONTINUADO("Descontinuado");

    private final String rotulo;

    StatusProduto(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
