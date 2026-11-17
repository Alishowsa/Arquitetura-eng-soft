package br.com.hubfaturamento.dominio.fatura;

public record Cliente(String nome, String email, String documento) {
    public Cliente {
        if (nome == null || nome.isBlank() || nome.strip().length() > 120) {
            throw new FaturaInvalidaException(
                "Informe o nome do cliente (até 120 caracteres)."
            );
        }
        if (
            email == null ||
            !email.strip().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
        ) {
            throw new FaturaInvalidaException(
                "Informe um e-mail de cliente válido."
            );
        }
        String digitos =
            documento == null ? "" : documento.replaceAll("\\D", "");
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new FaturaInvalidaException(
                "O documento do cliente deve ser um CPF (11 dígitos) ou CNPJ (14 dígitos)."
            );
        }
        nome = nome.strip();
        email = email.strip();
        documento = digitos;
    }
}
