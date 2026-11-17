package br.com.mvc.controller;

import br.com.mvc.dto.ProdutoForm;
import br.com.mvc.model.Produto;
import br.com.mvc.model.RegraDeNegocioException;
import br.com.mvc.service.AvisosAosVendedores;
import br.com.mvc.service.OperacaoNaoPermitidaException;
import br.com.mvc.service.ProdutoNaoEncontradoException;
import br.com.mvc.service.ProdutoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;
    private final AvisosAosVendedores avisos;

    public ProdutoController(
        ProdutoService service,
        AvisosAosVendedores avisos
    ) {
        this.service = service;
        this.avisos = avisos;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("produtos", service.listar());
        model.addAttribute("avisos", avisos.recentes());
        return "produtos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model, HttpSession sessao) {
        exigirAdministrador(sessao);
        model.addAttribute("produtoForm", new ProdutoForm());
        return "produtos/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(
        @PathVariable Long id,
        Model model,
        HttpSession sessao
    ) {
        exigirAdministrador(sessao);
        Produto produto = service.buscar(id);
        ProdutoForm form = new ProdutoForm();
        form.setNome(produto.getNome());
        form.setDescricao(produto.getDescricao());
        form.setPreco(produto.getPreco());
        model.addAttribute("produtoForm", form);
        model.addAttribute("produtoId", id);
        model.addAttribute("saldoAtual", produto.getQuantidade());
        return "produtos/formulario";
    }

    @PostMapping
    public String criar(
        @ModelAttribute ProdutoForm produtoForm,
        BindingResult result,
        Model model,
        RedirectAttributes redirect,
        HttpSession sessao
    ) {
        return salvar(null, produtoForm, result, model, redirect, sessao);
    }

    @PostMapping("/{id}")
    public String atualizar(
        @PathVariable Long id,
        @ModelAttribute ProdutoForm produtoForm,
        BindingResult result,
        Model model,
        RedirectAttributes redirect,
        HttpSession sessao
    ) {
        model.addAttribute("saldoAtual", service.buscar(id).getQuantidade());
        return salvar(id, produtoForm, result, model, redirect, sessao);
    }

    private String salvar(
        Long id,
        ProdutoForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect,
        HttpSession sessao
    ) {
        exigirAdministrador(sessao);
        model.addAttribute("produtoId", id);
        if (result.hasErrors()) {
            return "produtos/formulario";
        }
        try {
            service.salvar(id, form, PerfilController.atual(sessao));
        } catch (IllegalArgumentException | RegraDeNegocioException exception) {
            result.reject("produto.invalido", exception.getMessage());
            return "produtos/formulario";
        }
        redirect.addFlashAttribute(
            "mensagem",
            id == null
                ? "Produto cadastrado com sucesso."
                : "Produto atualizado com sucesso."
        );
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/descontinuar")
    public String descontinuar(
        @PathVariable Long id,
        RedirectAttributes redirect,
        HttpSession sessao
    ) {
        try {
            Produto produto = service.descontinuar(
                id,
                PerfilController.atual(sessao)
            );
            redirect.addFlashAttribute(
                "mensagem",
                produto.getNome() + " foi descontinuado."
            );
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/produtos";
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(
        @PathVariable Long id,
        Model model,
        HttpSession sessao
    ) {
        exigirAdministrador(sessao);
        model.addAttribute("produto", service.buscar(id));
        return "produtos/excluir";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(
        @PathVariable Long id,
        RedirectAttributes redirect,
        HttpSession sessao
    ) {
        try {
            service.excluir(id, PerfilController.atual(sessao));
            redirect.addFlashAttribute(
                "mensagem",
                "Produto excluído com sucesso."
            );
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/produtos";
    }

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado() {
        return "erro/404";
    }

    private static void exigirAdministrador(HttpSession sessao) {
        if (!PerfilController.atual(sessao).podeCadastrarProdutos()) {
            throw new OperacaoNaoPermitidaException(
                "Só o Administrador cadastra, edita, descontinua e exclui produtos."
            );
        }
    }
}
