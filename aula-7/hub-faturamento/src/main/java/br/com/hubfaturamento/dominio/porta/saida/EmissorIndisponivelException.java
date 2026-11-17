package br.com.hubfaturamento.dominio.porta.saida;

public class EmissorIndisponivelException extends Exception {

    public EmissorIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
