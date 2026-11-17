package br.com.mvc.controller;

import br.com.mvc.model.Movimentacao;
import br.com.mvc.model.RegraDeNegocioException;
import br.com.mvc.model.TipoMovimentacao;
import br.com.mvc.service.EstoqueService;
import br.com.mvc.service.ProdutoNaoEncontradoException;
import br.com.mvc.service.ProdutoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private final EstoqueService estoque;
    private final ProdutoService produtos;

    public EstoqueController(EstoqueService estoque, ProdutoService produtos) {
        this.estoque = estoque;
        this.produtos = produtos;
    }

    @GetMapping
    public String tela(Model model) {
        model.addAttribute("produtos", produtos.listarMovimentaveis());
        model.addAttribute("historico", estoque.historicoRecente());
        model.addAttribute("tipos", TipoMovimentacao.values());
        return "estoque";
    }

    @PostMapping
    public String movimentar(
        @RequestParam(required = false) Long produtoId,
        @RequestParam TipoMovimentacao tipo,
        @RequestParam(required = false) String quantidade,
        @RequestParam(required = false) String motivo,
        HttpSession sessao,
        RedirectAttributes redirect
    ) {
        try {
            if (produtoId == null) {
                throw new IllegalArgumentException("Escolha um produto.");
            }
            Integer unidades = CamposDeFormulario.quantidade(quantidade);
            Movimentacao m =
                tipo == TipoMovimentacao.ENTRADA
                    ? estoque.registrarEntrada(
                          produtoId,
                          unidades,
                          motivo,
                          PerfilController.atual(sessao)
                      )
                    : estoque.registrarSaida(
                          produtoId,
                          unidades,
                          motivo,
                          PerfilController.atual(sessao)
                      );
            redirect.addFlashAttribute(
                "mensagem",
                "%s de %d un. de %s registrada. Saldo: %d (%s).".formatted(
                    m.getTipo().getRotulo(),
                    m.getQuantidade(),
                    m.getProduto().getNome(),
                    m.getSaldoApos(),
                    m.getProduto().getStatus().getRotulo()
                )
            );
        } catch (
            IllegalArgumentException
            | RegraDeNegocioException
            | ProdutoNaoEncontradoException e
        ) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/estoque";
    }
}
