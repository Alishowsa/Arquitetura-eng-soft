package br.com.mvc.controller;

import br.com.mvc.model.Perfil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PerfilController {

    static final String ATRIBUTO = "perfil";

    static Perfil atual(HttpSession sessao) {
        Object valor = sessao.getAttribute(ATRIBUTO);
        return valor instanceof Perfil perfil ? perfil : Perfil.VENDEDOR;
    }

    @PostMapping("/perfil")
    public String trocar(
        @RequestParam Perfil perfil,
        @RequestParam(defaultValue = "/produtos") String voltarPara,
        HttpSession sessao
    ) {
        sessao.setAttribute(ATRIBUTO, perfil);
        boolean caminhoInterno =
            voltarPara.startsWith("/") && !voltarPara.startsWith("//");
        return "redirect:" + (caminhoInterno ? voltarPara : "/produtos");
    }
}
