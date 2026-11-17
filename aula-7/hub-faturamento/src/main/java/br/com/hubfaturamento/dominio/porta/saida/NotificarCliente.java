package br.com.hubfaturamento.dominio.porta.saida;

import br.com.hubfaturamento.dominio.fatura.Fatura;

public interface NotificarCliente {
    String enviarBoletoENota(Fatura fatura) throws EmissorIndisponivelException;
}
