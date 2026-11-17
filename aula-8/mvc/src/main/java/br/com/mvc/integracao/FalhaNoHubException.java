package br.com.mvc.integracao;

public class FalhaNoHubException extends Exception {

    public FalhaNoHubException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
