package br.com.hubfaturamento.dominio.fatura;

public class FaturaInvalidaException extends RuntimeException {

    public FaturaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
