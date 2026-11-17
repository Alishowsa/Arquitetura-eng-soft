package br.com.hubfaturamento.infraestrutura.saida.persistencia;

import br.com.hubfaturamento.dominio.fatura.Cliente;
import br.com.hubfaturamento.dominio.fatura.Fatura;
import br.com.hubfaturamento.dominio.fatura.ItemFaturado;
import br.com.hubfaturamento.dominio.porta.saida.GuardarFatura;
import br.com.hubfaturamento.infraestrutura.saida.persistencia.FaturaTabela.ItemTabela;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class GuardarFaturaComJpa implements GuardarFatura {

    private final FaturaTabelaRepository tabela;

    GuardarFaturaComJpa(FaturaTabelaRepository tabela) {
        this.tabela = tabela;
    }

    @Override
    public Fatura salvar(Fatura fatura) {
        return paraDominio(tabela.save(paraTabela(fatura)));
    }

    @Override
    public Optional<Fatura> buscarPorId(Long faturaId) {
        return tabela.findById(faturaId).map(GuardarFaturaComJpa::paraDominio);
    }

    @Override
    public Optional<Fatura> buscarPorPedido(Long pedidoId) {
        return tabela
            .findByPedidoId(pedidoId)
            .map(GuardarFaturaComJpa::paraDominio);
    }

    private static FaturaTabela paraTabela(Fatura f) {
        FaturaTabela linha = new FaturaTabela();
        linha.id = f.getId();
        linha.pedidoId = f.getPedidoId();
        linha.clienteNome = f.getCliente().nome();
        linha.clienteEmail = f.getCliente().email();
        linha.clienteDocumento = f.getCliente().documento();
        f.getItens().forEach(i ->
            linha.itens.add(
                new ItemTabela(i.descricao(), i.quantidade(), i.valorUnitario())
            )
        );
        linha.valorTotal = f.getValorTotal();
        linha.criadaEm = f.getCriadaEm();
        linha.status = f.getStatus();
        linha.codigoBarras = f.getCodigoBarras();
        linha.chaveNfe = f.getChaveNfe();
        linha.hashEmail = f.getHashEmail();
        return linha;
    }

    private static Fatura paraDominio(FaturaTabela linha) {
        return Fatura.restaurar(
            linha.id,
            linha.pedidoId,
            new Cliente(
                linha.clienteNome,
                linha.clienteEmail,
                linha.clienteDocumento
            ),
            linha.itens
                .stream()
                .map(i ->
                    new ItemFaturado(i.descricao, i.quantidade, i.valorUnitario)
                )
                .toList(),
            linha.criadaEm,
            linha.status,
            linha.codigoBarras,
            linha.chaveNfe,
            linha.hashEmail
        );
    }
}
