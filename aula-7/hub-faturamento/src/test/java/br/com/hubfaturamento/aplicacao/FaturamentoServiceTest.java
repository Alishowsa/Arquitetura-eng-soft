package br.com.hubfaturamento.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.hubfaturamento.dominio.fatura.Fatura;
import br.com.hubfaturamento.dominio.fatura.StatusFatura;
import br.com.hubfaturamento.dominio.porta.entrada.FaturarPedido.PedidoFechado;
import br.com.hubfaturamento.dominio.porta.entrada.FaturarPedido.Resultado;
import br.com.hubfaturamento.dominio.porta.saida.EmissorIndisponivelException;
import br.com.hubfaturamento.dominio.porta.saida.EmitirBoleto;
import br.com.hubfaturamento.dominio.porta.saida.EmitirNotaFiscal;
import br.com.hubfaturamento.dominio.porta.saida.GuardarFatura;
import br.com.hubfaturamento.dominio.porta.saida.NotificarCliente;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FaturamentoServiceTest {

    private static final String BOLETO = FaturaTestDados.BOLETO;

    private BancoEmMemoria banco;
    private FilaQueGuarda fila;
    private EmissorDeNotaFalso emissorNota;
    private EmailQueGuarda email;
    private FaturamentoService service;

    @BeforeEach
    void montar() {
        banco = new BancoEmMemoria();
        fila = new FilaQueGuarda();
        emissorNota = new EmissorDeNotaFalso();
        email = new EmailQueGuarda();
        Clock relogio = Clock.fixed(
            Instant.parse("2026-11-17T13:00:00Z"),
            ZoneOffset.UTC
        );
        service = new FaturamentoService(
            banco,
            fila,
            emissorNota,
            email,
            relogio
        );
    }

    private Resultado faturar(long pedidoId) {
        return service.faturar(
            new PedidoFechado(
                pedidoId,
                FaturaTestDados.CLIENTE,
                FaturaTestDados.ITENS
            )
        );
    }

    @Test
    void faturarSalvaEmiteNotaEPedeBoleto() {
        Fatura f = faturar(31).fatura();

        assertThat(f.getId()).isNotNull();
        assertThat(f.getStatus()).isEqualTo(StatusFatura.COM_NOTA);
        assertThat(f.getChaveNfe()).isEqualTo(FaturaTestDados.CHAVE);
        assertThat(fila.pedidos).containsExactly(f.getId());
        assertThat(email.enviados).isEmpty();
    }

    @Test
    void retornoDoBoletoFinalizaENotificaOCliente() {
        Fatura f = faturar(31).fatura();

        service.boletoEmitido(f.getId(), BOLETO);

        Fatura depois = service.consultar(f.getId());
        assertThat(depois.getStatus()).isEqualTo(StatusFatura.NOTIFICADA);
        assertThat(depois.getHashEmail()).isEqualTo(FaturaTestDados.HASH);
        assertThat(email.enviados).containsExactly(f.getId());
    }

    @Test
    void retornoDuplicadoNaFilaNaoEnviaDoisEmails() {
        Fatura f = faturar(31).fatura();

        service.boletoEmitido(f.getId(), BOLETO);
        service.boletoEmitido(f.getId(), BOLETO);

        assertThat(email.enviados).hasSize(1);
    }

    @Test
    void mesmoPedidoDuasVezesDevolveAMesmaFatura() {
        Resultado primeiro = faturar(31);
        Resultado segundo = faturar(31);

        assertThat(primeiro.jaExistia()).isFalse();
        assertThat(segundo.jaExistia()).isTrue();
        assertThat(segundo.fatura().getId()).isEqualTo(
            primeiro.fatura().getId()
        );
        assertThat(fila.pedidos).hasSize(1);
    }

    @Test
    void emissorDeNotaForaDoArNaoImpedeAFatura() {
        emissorNota.foraDoAr = true;

        Fatura f = faturar(31).fatura();
        service.boletoEmitido(f.getId(), BOLETO);

        Fatura depois = service.consultar(f.getId());
        assertThat(depois.getStatus()).isEqualTo(StatusFatura.COM_BOLETO);
        assertThat(depois.getChaveNfe()).isNull();
        assertThat(email.enviados).isEmpty();
    }

    @Test
    void servidorDeEmailForaDoArDeixaAFaturaFinalizada() {
        email.foraDoAr = true;
        Fatura f = faturar(31).fatura();

        service.boletoEmitido(f.getId(), BOLETO);

        assertThat(service.consultar(f.getId()).getStatus()).isEqualTo(
            StatusFatura.FINALIZADA
        );
    }

    @Test
    void consultarFaturaInexistenteFalha() {
        assertThatThrownBy(() -> service.consultar(404L)).isInstanceOf(
            FaturaNaoEncontradaException.class
        );
    }

    static class BancoEmMemoria implements GuardarFatura {

        private final Map<Long, Fatura> linhas = new HashMap<>();
        private long proximo = 1;

        @Override
        public Fatura salvar(Fatura f) {
            Fatura salva = f.getId() == null ? f.comId(proximo++) : f;
            linhas.put(salva.getId(), salva);
            return salva;
        }

        @Override
        public Optional<Fatura> buscarPorId(Long id) {
            return Optional.ofNullable(linhas.get(id));
        }

        @Override
        public Optional<Fatura> buscarPorPedido(Long pedidoId) {
            return linhas
                .values()
                .stream()
                .filter(f -> f.getPedidoId().equals(pedidoId))
                .findFirst();
        }
    }

    static class FilaQueGuarda implements EmitirBoleto {

        final List<Long> pedidos = new ArrayList<>();

        @Override
        public void solicitar(Fatura f) {
            pedidos.add(f.getId());
        }
    }

    static class EmissorDeNotaFalso implements EmitirNotaFiscal {

        boolean foraDoAr;

        @Override
        public String emitir(Fatura f) throws EmissorIndisponivelException {
            if (foraDoAr) {
                throw new EmissorIndisponivelException("fora do ar");
            }
            return FaturaTestDados.CHAVE;
        }
    }

    static class EmailQueGuarda implements NotificarCliente {

        final List<Long> enviados = new ArrayList<>();
        boolean foraDoAr;

        @Override
        public String enviarBoletoENota(
            Fatura f
        ) throws EmissorIndisponivelException {
            if (foraDoAr) {
                throw new EmissorIndisponivelException("SMTP fora do ar");
            }
            enviados.add(f.getId());
            return FaturaTestDados.HASH;
        }
    }
}
