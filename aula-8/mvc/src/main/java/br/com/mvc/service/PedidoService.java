package br.com.mvc.service;

import br.com.mvc.model.ItemPedido;
import br.com.mvc.model.Pedido;
import br.com.mvc.model.Perfil;
import br.com.mvc.model.Produto;
import br.com.mvc.repository.PedidoRepository;
import br.com.mvc.repository.ProdutoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    private final EstoqueService estoque;

    public PedidoService(
        PedidoRepository pedidos,
        ProdutoRepository produtos,
        EstoqueService estoque
    ) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.estoque = estoque;
    }

    public List<Pedido> listar() {
        return pedidos.findAllByOrderByIdDesc();
    }

    public Pedido buscar(Long id) {
        return pedidos
            .findById(id)
            .orElseThrow(PedidoNaoEncontradoException::new);
    }

    @Transactional
    public Pedido abrir(
        String nome,
        String email,
        String documento,
        Perfil perfil
    ) {
        exigirVendedor(perfil);
        return pedidos.save(
            Pedido.abrir(nome, email, documento, LocalDateTime.now())
        );
    }

    @Transactional
    public Pedido adicionarItem(
        Long pedidoId,
        Long produtoId,
        Integer quantidade,
        Perfil perfil
    ) {
        exigirVendedor(perfil);
        if (quantidade == null) {
            throw new IllegalArgumentException("Informe a quantidade.");
        }
        Pedido pedido = buscar(pedidoId);
        Produto produto = produtos
            .findById(produtoId)
            .orElseThrow(ProdutoNaoEncontradoException::new);
        pedido.adicionarItem(produto, quantidade);
        return pedido;
    }

    @Transactional
    public Pedido removerItem(Long pedidoId, Long itemId, Perfil perfil) {
        exigirVendedor(perfil);
        Pedido pedido = buscar(pedidoId);
        pedido.removerItem(itemId);
        return pedido;
    }

    @Transactional
    public Pedido fecharLocalmente(Long pedidoId, Perfil perfil) {
        exigirVendedor(perfil);
        Pedido pedido = buscar(pedidoId);
        pedido.fechar(LocalDateTime.now());
        for (ItemPedido item : pedido.getItens()) {
            estoque.baixarPorVenda(
                item.getProduto(),
                item.getQuantidade(),
                pedido.getId(),
                perfil
            );
        }
        return pedido;
    }

    @Transactional
    public Pedido marcarFaturado(Long pedidoId, Long faturaId) {
        Pedido pedido = buscar(pedidoId);
        pedido.registrarFatura(faturaId);
        return pedido;
    }

    @Transactional
    public Pedido marcarFalha(Long pedidoId, String motivo) {
        Pedido pedido = buscar(pedidoId);
        pedido.registrarFalhaNoFaturamento(motivo);
        return pedido;
    }

    static void exigirVendedor(Perfil perfil) {
        if (perfil == null || !perfil.podeVender()) {
            throw new OperacaoNaoPermitidaException(
                "Só o Vendedor ou o Administrador registram pedidos."
            );
        }
    }
}
