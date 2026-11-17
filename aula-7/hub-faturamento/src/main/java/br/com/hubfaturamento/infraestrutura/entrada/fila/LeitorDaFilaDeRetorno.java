package br.com.hubfaturamento.infraestrutura.entrada.fila;

import br.com.hubfaturamento.dominio.porta.entrada.ReceberBoletoEmitido;
import br.com.hubfaturamento.infraestrutura.mensageria.FilasEmMemoria;
import br.com.hubfaturamento.infraestrutura.mensageria.FilasEmMemoria.BoletoPronto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeitorDaFilaDeRetorno {

    private static final Logger LOG = LoggerFactory.getLogger(
        LeitorDaFilaDeRetorno.class
    );

    private final FilasEmMemoria filas;
    private final ReceberBoletoEmitido receberBoleto;

    LeitorDaFilaDeRetorno(
        FilasEmMemoria filas,
        ReceberBoletoEmitido receberBoleto
    ) {
        this.filas = filas;
        this.receberBoleto = receberBoleto;
    }

    @Scheduled(
        fixedDelayString = "${hub.filas.intervalo-ms:2000}",
        initialDelayString = "${hub.filas.intervalo-ms:2000}"
    )
    public int consumirRetornos() {
        int consumidos = 0;
        BoletoPronto retorno;
        while ((retorno = filas.filaDeRetorno().poll()) != null) {
            try {
                receberBoleto.boletoEmitido(
                    retorno.faturaId(),
                    retorno.codigoBarras()
                );
                consumidos++;
            } catch (RuntimeException e) {
                LOG.warn(
                    "Retorno do boleto da fatura {} descartado: {}",
                    retorno.faturaId(),
                    e.getMessage()
                );
            }
        }
        return consumidos;
    }
}
