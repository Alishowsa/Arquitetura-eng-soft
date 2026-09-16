package br.com.mvc.service;

import br.com.mvc.dto.ProdutoForm;
import br.com.mvc.model.Produto;
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

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    public List<Produto> listar() {
        return repository.findAll(Sort.by("nome").ascending());
    }

    public Produto buscar(Long id) {
        return repository
            .findById(id)
            .orElseThrow(ProdutoNaoEncontradoException::new);
    }

    @Transactional
    public Produto salvar(Long id, ProdutoForm form) {
        validar(form);
        Produto produto = id == null ? new Produto() : buscar(id);
        produto.setNome(form.getNome().strip());
        produto.setDescricao(
            form.getDescricao() == null ? "" : form.getDescricao().strip()
        );
        produto.setPreco(form.getPreco());
        produto.setQuantidade(form.getQuantidade());
        return repository.save(produto);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscar(id));
    }

    private void validar(ProdutoForm form) {
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
        if (form.getQuantidade() == null || form.getQuantidade() < 0) {
            throw new IllegalArgumentException(
                "Informe uma quantidade inteira maior ou igual a zero."
            );
        }
    }
}
