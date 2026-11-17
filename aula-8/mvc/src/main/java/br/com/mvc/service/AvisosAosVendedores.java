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

    public void produtoEsgotou(Produto produto) {
        registrar(produto.getNome() + " esgotou. Não ofereça até a reposição.");
    }

    public void produtoVoltouAoEstoque(Produto produto) {
        registrar(
            produto.getNome() +
                " voltou ao estoque (" +
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
