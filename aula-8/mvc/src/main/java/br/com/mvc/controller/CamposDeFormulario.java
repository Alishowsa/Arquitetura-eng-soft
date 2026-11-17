package br.com.mvc.controller;

final class CamposDeFormulario {

    private CamposDeFormulario() {}

    static Integer quantidade(String valor) {
        try {
            return Integer.valueOf(valor == null ? "" : valor.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Informe uma quantidade inteira."
            );
        }
    }
}
