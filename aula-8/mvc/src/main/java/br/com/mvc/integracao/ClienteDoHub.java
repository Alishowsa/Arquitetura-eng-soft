package br.com.mvc.integracao;

import java.math.BigDecimal;
import java.util.List;

public interface ClienteDoHub {
    Long faturar(PedidoParaFaturar pedido) throws FalhaNoHubException;

    record PedidoParaFaturar(
        Long pedidoId,
        Cliente cliente,
        List<Item> itens
    ) {}

    record Cliente(String nome, String email, String documento) {}

    record Item(String descricao, int quantidade, BigDecimal valorUnitario) {}
}
