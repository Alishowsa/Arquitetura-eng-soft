package br.com.hubfaturamento.dominio.porta.saida;

import br.com.hubfaturamento.dominio.fatura.Fatura;
import java.util.Optional;

public interface GuardarFatura {
    Fatura salvar(Fatura fatura);

    Optional<Fatura> buscarPorId(Long faturaId);

    Optional<Fatura> buscarPorPedido(Long pedidoId);
}
