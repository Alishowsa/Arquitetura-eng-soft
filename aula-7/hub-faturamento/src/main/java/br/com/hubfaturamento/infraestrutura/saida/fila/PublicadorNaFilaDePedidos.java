package br.com.hubfaturamento.infraestrutura.saida.fila;

import br.com.hubfaturamento.dominio.fatura.Fatura;
import br.com.hubfaturamento.dominio.porta.saida.EmitirBoleto;
import br.com.hubfaturamento.infraestrutura.mensageria.FilasEmMemoria;
import br.com.hubfaturamento.infraestrutura.mensageria.FilasEmMemoria.PedidoDeBoleto;
import org.springframework.stereotype.Component;

@Component
class PublicadorNaFilaDePedidos implements EmitirBoleto {

    private final FilasEmMemoria filas;

    PublicadorNaFilaDePedidos(FilasEmMemoria filas) {
        this.filas = filas;
    }

    @Override
    public void solicitar(Fatura fatura) {
        filas
            .filaDePedidos()
            .add(new PedidoDeBoleto(fatura.getId(), fatura.getValorTotal()));
    }
}
