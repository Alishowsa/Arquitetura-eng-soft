package br.com.hubfaturamento.dominio.porta.entrada;

public interface ReceberBoletoEmitido {
    void boletoEmitido(Long faturaId, String codigoBarras);
}
