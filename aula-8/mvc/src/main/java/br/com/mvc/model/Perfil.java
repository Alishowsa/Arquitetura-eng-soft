package br.com.mvc.model;

public enum Perfil {
    VENDEDOR("Vendedor"),
    ADMINISTRADOR("Administrador"),
    ESTOQUE("Time de Estoque");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean podeCadastrarProdutos() {
        return this == ADMINISTRADOR;
    }

    public boolean podeMovimentarEstoque() {
        return this == ESTOQUE || this == ADMINISTRADOR;
    }

    public boolean podeVender() {
        return this == VENDEDOR || this == ADMINISTRADOR;
    }
}
