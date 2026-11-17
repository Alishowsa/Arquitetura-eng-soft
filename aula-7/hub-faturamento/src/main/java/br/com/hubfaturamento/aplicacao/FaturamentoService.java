package br.com.hubfaturamento.aplicacao;

import br.com.hubfaturamento.dominio.fatura.Fatura;
import br.com.hubfaturamento.dominio.porta.entrada.FaturarPedido;
import br.com.hubfaturamento.dominio.porta.entrada.ReceberBoletoEmitido;
import br.com.hubfaturamento.dominio.porta.saida.EmissorIndisponivelException;
import br.com.hubfaturamento.dominio.porta.saida.EmitirBoleto;
import br.com.hubfaturamento.dominio.porta.saida.EmitirNotaFiscal;
import br.com.hubfaturamento.dominio.porta.saida.GuardarFatura;
import br.com.hubfaturamento.dominio.porta.saida.NotificarCliente;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

public class FaturamentoService implements FaturarPedido, ReceberBoletoEmitido {

    private final GuardarFatura faturas;
    private final EmitirBoleto boletos;
    private final EmitirNotaFiscal notas;
    private final NotificarCliente avisos;
    private final Clock relogio;

    public FaturamentoService(
        GuardarFatura faturas,
        EmitirBoleto boletos,
        EmitirNotaFiscal notas,
        NotificarCliente avisos,
        Clock relogio
    ) {
        this.faturas = faturas;
        this.boletos = boletos;
        this.notas = notas;
        this.avisos = avisos;
        this.relogio = relogio;
    }

    @Override
    public Resultado faturar(PedidoFechado pedido) {
        Optional<Fatura> existente = faturas.buscarPorPedido(pedido.pedidoId());
        if (existente.isPresent()) {
            return new Resultado(existente.get(), true);
        }

        Fatura fatura = faturas.salvar(
            Fatura.criar(
                pedido.pedidoId(),
                pedido.cliente(),
                pedido.itens(),
                LocalDateTime.now(relogio)
            )
        );

        try {
            fatura.registrarNotaFiscal(notas.emitir(fatura));
            fatura = faturas.salvar(fatura);
        } catch (EmissorIndisponivelException e) {
            // emissor de nota fora do ar não impede a fatura: ela segue sem a chave da NF-e
        }

        // o boleto é pedido por último: o retorno chega por outra thread e não pode ler a fatura antes deste save
        boletos.solicitar(fatura);
        return new Resultado(fatura, false);
    }

    @Override
    public Fatura consultar(Long faturaId) {
        return faturas
            .buscarPorId(faturaId)
            .orElseThrow(() -> new FaturaNaoEncontradaException(faturaId));
    }

    @Override
    public void boletoEmitido(Long faturaId, String codigoBarras) {
        Fatura fatura = consultar(faturaId);
        fatura.registrarBoleto(codigoBarras);
        fatura = faturas.salvar(fatura);
        notificarSePronta(fatura);
    }

    private void notificarSePronta(Fatura fatura) {
        if (!fatura.prontaParaNotificar()) {
            return;
        }
        try {
            fatura.registrarClienteNotificado(avisos.enviarBoletoENota(fatura));
            faturas.salvar(fatura);
        } catch (EmissorIndisponivelException e) {
            // servidor de e-mail fora do ar: a fatura continua FINALIZADA, sem NOTIFICADA
        }
    }
}
