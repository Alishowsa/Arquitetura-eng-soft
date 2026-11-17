package br.com.hubfaturamento.simulacao;

import br.com.hubfaturamento.dominio.fatura.DigitoVerificador;
import br.com.hubfaturamento.infraestrutura.mensageria.FilasEmMemoria;
import br.com.hubfaturamento.infraestrutura.mensageria.FilasEmMemoria.BoletoPronto;
import br.com.hubfaturamento.infraestrutura.mensageria.FilasEmMemoria.PedidoDeBoleto;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// não faz parte do hub: simula o banco emissor externo que lê a fila de pedidos e responde na de retorno
@Component
public class EmissorDeBoletoExterno {

    private static final Logger LOG = LoggerFactory.getLogger(
        EmissorDeBoletoExterno.class
    );
    private static final LocalDate BASE_FATOR_VENCIMENTO = LocalDate.of(
        1997,
        10,
        7
    );
    private static final int DIAS_PARA_VENCER = 7;

    private final FilasEmMemoria filas;

    public EmissorDeBoletoExterno(FilasEmMemoria filas) {
        this.filas = filas;
    }

    @Scheduled(fixedDelayString = "${hub.filas.intervalo-ms:2000}")
    public int processarPedidos() {
        int processados = 0;
        PedidoDeBoleto pedido;
        while ((pedido = filas.filaDePedidos().poll()) != null) {
            String codigo = gerarCodigoDeBarras(
                pedido,
                LocalDate.now().plusDays(DIAS_PARA_VENCER)
            );
            filas
                .filaDeRetorno()
                .add(new BoletoPronto(pedido.faturaId(), codigo));
            LOG.info(
                "[banco emissor] boleto da fatura {} gerado: {}",
                pedido.faturaId(),
                codigo
            );
            processados++;
        }
        return processados;
    }

    static String gerarCodigoDeBarras(
        PedidoDeBoleto pedido,
        LocalDate vencimento
    ) {
        String banco = "341";
        String moeda = "9";
        long fator =
            (ChronoUnit.DAYS.between(BASE_FATOR_VENCIMENTO, vencimento) %
                9000) +
            1000;
        String valor = "%010d".formatted(
            pedido.valor().movePointRight(2).longValueExact()
        );
        String campoLivre = "%025d".formatted(pedido.faturaId());
        String semDv = banco + moeda + fator + valor + campoLivre;
        int dv = DigitoVerificador.dvCodigoDeBarras(semDv);
        return semDv.substring(0, 4) + dv + semDv.substring(4);
    }
}
