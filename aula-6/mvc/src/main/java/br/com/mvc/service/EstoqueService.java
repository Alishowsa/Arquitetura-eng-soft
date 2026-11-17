package br.com.mvc.service;

import br.com.mvc.model.Movimentacao;
import br.com.mvc.model.Perfil;
import br.com.mvc.model.Produto;
import br.com.mvc.model.Produto.Transicao;
import br.com.mvc.model.TipoMovimentacao;
import br.com.mvc.repository.MovimentacaoRepository;
import br.com.mvc.repository.ProdutoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EstoqueService {

    private final ProdutoRepository produtos;
    private final MovimentacaoRepository movimentacoes;
    private final AvisosAosVendedores avisos;

    public EstoqueService(
        ProdutoRepository produtos,
        MovimentacaoRepository movimentacoes,
        AvisosAosVendedores avisos
    ) {
        this.produtos = produtos;
        this.movimentacoes = movimentacoes;
        this.avisos = avisos;
    }

    public List<Movimentacao> historicoRecente() {
        return movimentacoes.findTop30ByOrderByIdDesc();
    }

    @Transactional
    public Movimentacao registrarEntrada(
        Long produtoId,
        Integer quantidade,
        String motivo,
        Perfil perfil
    ) {
        return movimentar(
            produtoId,
            TipoMovimentacao.ENTRADA,
            quantidade,
            motivo,
            perfil
        );
    }

    @Transactional
    public Movimentacao registrarSaida(
        Long produtoId,
        Integer quantidade,
        String motivo,
        Perfil perfil
    ) {
        return movimentar(
            produtoId,
            TipoMovimentacao.SAIDA,
            quantidade,
            motivo,
            perfil
        );
    }

    private Movimentacao movimentar(
        Long produtoId,
        TipoMovimentacao tipo,
        Integer quantidade,
        String motivo,
        Perfil perfil
    ) {
        if (perfil == null || !perfil.podeMovimentarEstoque()) {
            throw new OperacaoNaoPermitidaException(
                "Só o Time de Estoque ou o Administrador movimentam o estoque."
            );
        }
        if (quantidade == null) {
            throw new IllegalArgumentException("Informe a quantidade.");
        }
        if (motivo != null && motivo.length() > 200) {
            throw new IllegalArgumentException(
                "O motivo deve ter no máximo 200 caracteres."
            );
        }
        Produto produto = produtos
            .findById(produtoId)
            .orElseThrow(ProdutoNaoEncontradoException::new);

        Transicao transicao =
            tipo == TipoMovimentacao.ENTRADA
                ? produto.acrescerEstoque(quantidade)
                : produto.baixarEstoque(quantidade);

        Movimentacao registro = movimentacoes.save(
            new Movimentacao(
                produto,
                tipo,
                quantidade,
                perfil,
                textoOuNulo(motivo),
                LocalDateTime.now()
            )
        );

        switch (transicao) {
            case FICOU_INDISPONIVEL -> avisos.produtoFicouIndisponivel(produto);
            case FICOU_DISPONIVEL -> avisos.produtoFicouDisponivel(produto);
            case NENHUMA -> {
            }
        }
        return registro;
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
