package br.com.hubfaturamento.aplicacao;

public class FaturaNaoEncontradaException extends RuntimeException {

    public FaturaNaoEncontradaException(Long faturaId) {
        super("Fatura " + faturaId + " não encontrada.");
    }
}
