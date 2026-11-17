package br.com.mvc.controller;

import br.com.mvc.model.Perfil;
import br.com.mvc.service.OperacaoNaoPermitidaException;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class PerfilNasTelas {

    @ModelAttribute("perfilAtual")
    public Perfil perfilAtual(HttpSession sessao) {
        return PerfilController.atual(sessao);
    }

    @ModelAttribute("perfis")
    public List<Perfil> perfis() {
        return List.of(Perfil.values());
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String naoPermitido(
        OperacaoNaoPermitidaException e,
        Model model,
        HttpSession sessao
    ) {
        model.addAttribute("motivo", e.getMessage());
        model.addAttribute("perfilAtual", PerfilController.atual(sessao));
        model.addAttribute("perfis", perfis());
        return "erro/403";
    }
}
