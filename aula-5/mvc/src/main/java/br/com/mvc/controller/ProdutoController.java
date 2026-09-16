package br.com.mvc.controller;

import br.com.mvc.dto.ProdutoForm;
import br.com.mvc.model.Produto;
import br.com.mvc.service.ProdutoNaoEncontradoException;
import br.com.mvc.service.ProdutoService;
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

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("produtos", service.listar());
        return "produtos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("produtoForm", new ProdutoForm());
        return "produtos/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Produto produto = service.buscar(id);
        ProdutoForm form = new ProdutoForm();
        form.setNome(produto.getNome());
        form.setDescricao(produto.getDescricao());
        form.setPreco(produto.getPreco());
        form.setQuantidade(produto.getQuantidade());
        model.addAttribute("produtoForm", form);
        model.addAttribute("produtoId", id);
        return "produtos/formulario";
    }

    @PostMapping
    public String criar(
        @ModelAttribute ProdutoForm produtoForm,
        BindingResult result,
        Model model,
        RedirectAttributes redirect
    ) {
        return salvar(null, produtoForm, result, model, redirect);
    }

    @PostMapping("/{id}")
    public String atualizar(
        @PathVariable Long id,
        @ModelAttribute ProdutoForm produtoForm,
        BindingResult result,
        Model model,
        RedirectAttributes redirect
    ) {
        service.buscar(id);
        return salvar(id, produtoForm, result, model, redirect);
    }

    private String salvar(
        Long id,
        ProdutoForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect
    ) {
        model.addAttribute("produtoId", id);
        if (result.hasErrors()) {
            return "produtos/formulario";
        }
        try {
            service.salvar(id, form);
        } catch (IllegalArgumentException exception) {
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

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, Model model) {
        model.addAttribute("produto", service.buscar(id));
        return "produtos/excluir";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        service.excluir(id);
        redirect.addFlashAttribute("mensagem", "Produto excluído com sucesso.");
        return "redirect:/produtos";
    }

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado() {
        return "erro/404";
    }
}
