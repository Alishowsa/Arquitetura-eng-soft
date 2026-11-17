package br.com.hubfaturamento.dominio.porta.entrada;

import br.com.hubfaturamento.dominio.fatura.Cliente;
import br.com.hubfaturamento.dominio.fatura.Fatura;
import br.com.hubfaturamento.dominio.fatura.ItemFaturado;
import java.util.List;

public interface FaturarPedido {
    Resultado faturar(PedidoFechado pedido);

    Fatura consultar(Long faturaId);

    record PedidoFechado(
        Long pedidoId,
        Cliente cliente,
        List<ItemFaturado> itens
    ) {}

    record Resultado(Fatura fatura, boolean jaExistia) {}
}
