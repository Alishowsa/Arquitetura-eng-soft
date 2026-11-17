package br.com.mvc.repository;

import br.com.mvc.model.Movimentacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoRepository
    extends JpaRepository<Movimentacao, Long>
{
    List<Movimentacao> findTop30ByOrderByIdDesc();

    boolean existsByProdutoId(Long produtoId);
}
