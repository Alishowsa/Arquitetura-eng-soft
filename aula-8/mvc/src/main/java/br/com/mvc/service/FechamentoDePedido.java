package br.com.mvc.service;

import br.com.mvc.integracao.ClienteDoHub;
import br.com.mvc.integracao.ClienteDoHub.Cliente;
import br.com.mvc.integracao.ClienteDoHub.Item;
import br.com.mvc.integracao.ClienteDoHub.PedidoParaFaturar;
import br.com.mvc.integracao.FalhaNoHubException;
import br.com.mvc.model.Pedido;
import br.com.mvc.model.Perfil;
import br.com.mvc.model.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// sem @Transactional de propósito: o hub é chamado depois do commit local, sem banco aberto esperando a rede
@Service
public class FechamentoDePedido {

    private static final Logger LOG = LoggerFactory.getLogger(
        FechamentoDePedido.class
    );

    private final PedidoService pedidos;
    private final ClienteDoHub hub;

    public FechamentoDePedido(PedidoService pedidos, ClienteDoHub hub) {
        this.pedidos = pedidos;
        this.hub = hub;
    }

    public Pedido fechar(Long pedidoId, Perfil perfil) {
        Pedido fechado = pedidos.fecharLocalmente(pedidoId, perfil);
        return enviarAoHub(fechado);
    }

    public Pedido reenviarAoHub(Long pedidoId, Perfil perfil) {
        PedidoService.exigirVendedor(perfil);
        Pedido pedido = pedidos.buscar(pedidoId);
        if (!pedido.podeReenviarAoHub()) {
            throw new RegraDeNegocioException(
                "Só pedidos com falha no faturamento podem ser reenviados."
            );
        }
        return enviarAoHub(pedido);
    }

    private Pedido enviarAoHub(Pedido pedido) {
        try {
            Long faturaId = hub.faturar(paraOHub(pedido));
            return pedidos.marcarFaturado(pedido.getId(), faturaId);
        } catch (FalhaNoHubException e) {
            LOG.warn(
                "Pedido {} fechado, mas não faturado: {}",
                pedido.getId(),
                e.getMessage()
            );
            return pedidos.marcarFalha(pedido.getId(), e.getMessage());
        }
    }

    private static PedidoParaFaturar paraOHub(Pedido pedido) {
        return new PedidoParaFaturar(
            pedido.getId(),
            new Cliente(
                pedido.getClienteNome(),
                pedido.getClienteEmail(),
                pedido.getClienteDocumento()
            ),
            pedido
                .getItens()
                .stream()
                .map(i ->
                    new Item(
                        i.getDescricao(),
                        i.getQuantidade(),
                        i.getPrecoUnitario()
                    )
                )
                .toList()
        );
    }
}
