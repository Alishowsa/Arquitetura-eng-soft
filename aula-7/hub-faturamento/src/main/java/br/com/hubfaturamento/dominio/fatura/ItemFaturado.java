package br.com.hubfaturamento.dominio.fatura;

import java.math.BigDecimal;

public record ItemFaturado(
    String descricao,
    int quantidade,
    BigDecimal valorUnitario
) {
    public ItemFaturado {
        if (
            descricao == null ||
            descricao.isBlank() ||
            descricao.strip().length() > 120
        ) {
            throw new FaturaInvalidaException(
                "Cada item precisa de uma descrição (até 120 caracteres)."
            );
        }
        if (quantidade <= 0) {
            throw new FaturaInvalidaException(
                "A quantidade de cada item deve ser maior que zero."
            );
        }
        if (
            valorUnitario == null ||
            valorUnitario.signum() <= 0 ||
            valorUnitario.stripTrailingZeros().scale() > 2
        ) {
            throw new FaturaInvalidaException(
                "O valor unitário deve ser positivo, com no máximo duas casas decimais."
            );
        }
        descricao = descricao.strip();
    }

    public BigDecimal subtotal() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
