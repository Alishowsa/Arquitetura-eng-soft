package br.com.hubfaturamento.infraestrutura.saida.persistencia;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface FaturaTabelaRepository extends JpaRepository<FaturaTabela, Long> {
    Optional<FaturaTabela> findByPedidoId(Long pedidoId);
}
