package br.com.hubfaturamento.infraestrutura.mensageria;

import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Component;

@Component
public class FilasEmMemoria {

    public record PedidoDeBoleto(Long faturaId, BigDecimal valor) {}

    public record BoletoPronto(Long faturaId, String codigoBarras) {}

    private final BlockingQueue<PedidoDeBoleto> filaDePedidos =
        new LinkedBlockingQueue<>();
    private final BlockingQueue<BoletoPronto> filaDeRetorno =
        new LinkedBlockingQueue<>();

    public BlockingQueue<PedidoDeBoleto> filaDePedidos() {
        return filaDePedidos;
    }

    public BlockingQueue<BoletoPronto> filaDeRetorno() {
        return filaDeRetorno;
    }
}
