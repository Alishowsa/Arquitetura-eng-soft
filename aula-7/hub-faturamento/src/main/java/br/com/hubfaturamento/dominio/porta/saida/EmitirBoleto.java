package br.com.hubfaturamento.dominio.porta.saida;

import br.com.hubfaturamento.dominio.fatura.Fatura;

public interface EmitirBoleto {
    void solicitar(Fatura fatura);
}
