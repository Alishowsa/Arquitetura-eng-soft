package br.com.hubfaturamento.dominio.porta.saida;

import br.com.hubfaturamento.dominio.fatura.Fatura;

public interface EmitirNotaFiscal {
    String emitir(Fatura fatura) throws EmissorIndisponivelException;
}
