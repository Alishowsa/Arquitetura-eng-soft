package br.com.hubfaturamento.infraestrutura.entrada.rest;

import br.com.hubfaturamento.dominio.fatura.Cliente;
import br.com.hubfaturamento.dominio.fatura.Fatura;
import br.com.hubfaturamento.dominio.fatura.FaturaInvalidaException;
import br.com.hubfaturamento.dominio.fatura.ItemFaturado;
import br.com.hubfaturamento.dominio.porta.entrada.FaturarPedido;
import br.com.hubfaturamento.dominio.porta.entrada.FaturarPedido.PedidoFechado;
import br.com.hubfaturamento.dominio.porta.entrada.FaturarPedido.Resultado;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/faturas")
class FaturaController {

    private final FaturarPedido faturarPedido;

    FaturaController(FaturarPedido faturarPedido) {
        this.faturarPedido = faturarPedido;
    }

    @PostMapping
    ResponseEntity<FaturaJson> faturar(@RequestBody PedidoJson corpo) {
        Resultado resultado = faturarPedido.faturar(corpo.paraPorta());
        FaturaJson json = FaturaJson.de(resultado.fatura());
        if (resultado.jaExistia()) {
            return ResponseEntity.ok(json);
        }
        return ResponseEntity.created(URI.create("/faturas/" + json.id())).body(
            json
        );
    }

    @GetMapping("/{id}")
    FaturaJson consultar(@PathVariable Long id) {
        return FaturaJson.de(faturarPedido.consultar(id));
    }

    record ClienteJson(String nome, String email, String documento) {}

    record ItemJson(
        String descricao,
        Integer quantidade,
        BigDecimal valorUnitario
    ) {}

    record PedidoJson(
        Long pedidoId,
        ClienteJson cliente,
        List<ItemJson> itens
    ) {
        PedidoFechado paraPorta() {
            if (cliente == null) {
                throw new FaturaInvalidaException(
                    "Informe o cliente do pedido."
                );
            }
            List<ItemFaturado> itensDoDominio =
                itens == null
                    ? List.of()
                    : itens
                          .stream()
                          .map(i ->
                              new ItemFaturado(
                                  i.descricao(),
                                  i.quantidade() == null ? 0 : i.quantidade(),
                                  i.valorUnitario()
                              )
                          )
                          .toList();
            return new PedidoFechado(
                pedidoId,
                new Cliente(
                    cliente.nome(),
                    cliente.email(),
                    cliente.documento()
                ),
                itensDoDominio
            );
        }
    }

    record FaturaJson(
        Long id,
        Long pedidoId,
        ClienteJson cliente,
        List<ItemJson> itens,
        BigDecimal valorTotal,
        LocalDateTime criadaEm,
        String status,
        String codigoBarras,
        String chaveNfe,
        String hashEmail
    ) {
        static FaturaJson de(Fatura f) {
            return new FaturaJson(
                f.getId(),
                f.getPedidoId(),
                new ClienteJson(
                    f.getCliente().nome(),
                    f.getCliente().email(),
                    f.getCliente().documento()
                ),
                f
                    .getItens()
                    .stream()
                    .map(i ->
                        new ItemJson(
                            i.descricao(),
                            i.quantidade(),
                            i.valorUnitario()
                        )
                    )
                    .toList(),
                f.getValorTotal(),
                f.getCriadaEm(),
                f.getStatus().name(),
                f.getCodigoBarras(),
                f.getChaveNfe(),
                f.getHashEmail()
            );
        }
    }
}
