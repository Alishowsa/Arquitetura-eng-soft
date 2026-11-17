package br.com.mvc.service;

import br.com.mvc.model.Produto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AvisosAosVendedores {

    private static final Logger LOG = LoggerFactory.getLogger(
        AvisosAosVendedores.class
    );
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern(
        "dd/MM HH:mm"
    );
    private static final int LIMITE = 10;

    private final Deque<String> recentes = new ArrayDeque<>();

    public void produtoFicouIndisponivel(Produto produto) {
        registrar(
            produto.getNome() +
                " ficou indisponível (estoque zerado). Não ofereça até a reposição."
        );
    }

    public void produtoFicouDisponivel(Produto produto) {
        registrar(
            produto.getNome() +
                " está disponível (" +
                produto.getQuantidade() +
                " un.)."
        );
    }

    public synchronized List<String> recentes() {
        return List.copyOf(recentes);
    }

    private synchronized void registrar(String texto) {
        String aviso = LocalDateTime.now().format(HORA) + " · " + texto;
        LOG.info("[aviso aos vendedores] {}", aviso);
        recentes.addFirst(aviso);
        while (recentes.size() > LIMITE) {
            recentes.removeLast();
        }
    }
}
