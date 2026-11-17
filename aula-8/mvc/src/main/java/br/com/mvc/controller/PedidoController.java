package br.com.mvc.controller;

import br.com.mvc.model.Pedido;
import br.com.mvc.model.RegraDeNegocioException;
import br.com.mvc.model.StatusPedido;
import br.com.mvc.service.FechamentoDePedido;
import br.com.mvc.service.PedidoNaoEncontradoException;
import br.com.mvc.service.PedidoService;
import br.com.mvc.service.ProdutoNaoEncontradoException;
import br.com.mvc.service.ProdutoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidos;
    private final FechamentoDePedido fechamento;
    private final ProdutoService produtos;
    private final String urlDoHub;

    public PedidoController(
        PedidoService pedidos,
        FechamentoDePedido fechamento,
        ProdutoService produtos,
        @Value("${hub.url:http://127.0.0.1:8090}") String urlDoHub
    ) {
        this.pedidos = pedidos;
        this.fechamento = fechamento;
        this.produtos = produtos;
        this.urlDoHub = urlDoHub;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pedidos", pedidos.listar());
        return "pedidos/lista";
    }

    @PostMapping
    public String abrir(
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String documento,
        HttpSession sessao,
        RedirectAttributes redirect
    ) {
        try {
            Pedido pedido = pedidos.abrir(
                nome,
                email,
                documento,
                PerfilController.atual(sessao)
            );
            return "redirect:/pedidos/" + pedido.getId();
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            redirect.addFlashAttribute("nome", nome);
            redirect.addFlashAttribute("email", email);
            redirect.addFlashAttribute("documento", documento);
            return "redirect:/pedidos";
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Pedido pedido = pedidos.buscar(id);
        model.addAttribute("pedido", pedido);
        model.addAttribute("aberto", pedido.getStatus() == StatusPedido.ABERTO);
        model.addAttribute("produtos", produtos.listarVendaveis());
        model.addAttribute("urlDoHub", urlDoHub);
        return "pedidos/detalhe";
    }

    @PostMapping("/{id}/itens")
    public String adicionarItem(
        @PathVariable Long id,
        @RequestParam(required = false) Long produtoId,
        @RequestParam(required = false) String quantidade,
        HttpSession sessao,
        RedirectAttributes redirect
    ) {
        try {
            if (produtoId == null) {
                throw new IllegalArgumentException("Escolha um produto.");
            }
            pedidos.adicionarItem(
                id,
                produtoId,
                CamposDeFormulario.quantidade(quantidade),
                PerfilController.atual(sessao)
            );
        } catch (
            IllegalArgumentException
            | RegraDeNegocioException
            | ProdutoNaoEncontradoException e
        ) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/itens/{itemId}/remover")
    public String removerItem(
        @PathVariable Long id,
        @PathVariable Long itemId,
        HttpSession sessao,
        RedirectAttributes redirect
    ) {
        try {
            pedidos.removerItem(id, itemId, PerfilController.atual(sessao));
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/fechar")
    public String fechar(
        @PathVariable Long id,
        HttpSession sessao,
        RedirectAttributes redirect
    ) {
        try {
            mensagemDoResultado(
                fechamento.fechar(id, PerfilController.atual(sessao)),
                redirect
            );
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/reenviar")
    public String reenviar(
        @PathVariable Long id,
        HttpSession sessao,
        RedirectAttributes redirect
    ) {
        try {
            mensagemDoResultado(
                fechamento.reenviarAoHub(id, PerfilController.atual(sessao)),
                redirect
            );
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado() {
        return "erro/404";
    }

    private static void mensagemDoResultado(
        Pedido pedido,
        RedirectAttributes redirect
    ) {
        if (pedido.getStatus() == StatusPedido.FATURADO) {
            redirect.addFlashAttribute(
                "mensagem",
                "Pedido faturado. Fatura nº " +
                    pedido.getFaturaId() +
                    " no hub."
            );
        } else {
            redirect.addFlashAttribute(
                "erro",
                "Pedido fechado e estoque baixado, mas o hub não faturou: " +
                    pedido.getErroFaturamento()
            );
        }
    }
}
