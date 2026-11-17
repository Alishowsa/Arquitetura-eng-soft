package br.com.mvc.service;

import br.com.mvc.dto.ProdutoForm;
import br.com.mvc.model.Perfil;
import br.com.mvc.model.Produto;
import br.com.mvc.model.RegraDeNegocioException;
import br.com.mvc.repository.MovimentacaoRepository;
import br.com.mvc.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository repository;
    private final MovimentacaoRepository movimentacoes;

    public ProdutoService(
        ProdutoRepository repository,
        MovimentacaoRepository movimentacoes
    ) {
        this.repository = repository;
        this.movimentacoes = movimentacoes;
    }

    public List<Produto> listar() {
        return repository.findAll(Sort.by("nome").ascending());
    }

    public List<Produto> listarMovimentaveis() {
        return listar()
            .stream()
            .filter(p -> !p.isDescontinuado())
            .toList();
    }

    public Produto buscar(Long id) {
        return repository
            .findById(id)
            .orElseThrow(ProdutoNaoEncontradoException::new);
    }

    @Transactional
    public Produto salvar(Long id, ProdutoForm form, Perfil perfil) {
        exigirAdministrador(perfil, "cadastrar ou editar produtos");
        validar(form, id == null);
        String nome = form.getNome().strip();
        String descricao =
            form.getDescricao() == null ? "" : form.getDescricao().strip();
        if (id == null) {
            return repository.save(
                Produto.cadastrar(
                    nome,
                    descricao,
                    form.getPreco(),
                    form.getQuantidade()
                )
            );
        }
        Produto produto = buscar(id);
        produto.alterarDados(nome, descricao, form.getPreco());
        return produto;
    }

    @Transactional
    public Produto descontinuar(Long id, Perfil perfil) {
        exigirAdministrador(perfil, "descontinuar produtos");
        Produto produto = buscar(id);
        produto.descontinuar();
        return produto;
    }

    @Transactional
    public void excluir(Long id, Perfil perfil) {
        exigirAdministrador(perfil, "excluir produtos");
        Produto produto = buscar(id);
        if (movimentacoes.existsByProdutoId(id)) {
            throw new RegraDeNegocioException(
                produto.getNome() +
                    " já tem movimentações de estoque. Descontinue em vez de excluir, para manter o histórico."
            );
        }
        repository.delete(produto);
    }

    private static void exigirAdministrador(Perfil perfil, String acao) {
        if (perfil == null || !perfil.podeCadastrarProdutos()) {
            throw new OperacaoNaoPermitidaException(
                "Só o Administrador pode " + acao + "."
            );
        }
    }

    private void validar(ProdutoForm form, boolean cadastro) {
        if (
            form.getNome() == null ||
            form.getNome().isBlank() ||
            form.getNome().strip().length() > 100
        ) {
            throw new IllegalArgumentException(
                "Informe um nome com 1 a 100 caracteres."
            );
        }
        if (form.getDescricao() != null && form.getDescricao().length() > 500) {
            throw new IllegalArgumentException(
                "A descrição deve ter no máximo 500 caracteres."
            );
        }
        if (
            form.getPreco() == null ||
            form.getPreco().signum() <= 0 ||
            form.getPreco().compareTo(new BigDecimal("9999999999.99")) > 0 ||
            form.getPreco().stripTrailingZeros().scale() > 2
        ) {
            throw new IllegalArgumentException(
                "Informe um preço positivo, de até 9.999.999.999,99, com no máximo duas casas decimais."
            );
        }
        if (
            cadastro &&
            (form.getQuantidade() == null || form.getQuantidade() < 0)
        ) {
            throw new IllegalArgumentException(
                "Informe um estoque inicial inteiro maior ou igual a zero."
            );
        }
    }
}
