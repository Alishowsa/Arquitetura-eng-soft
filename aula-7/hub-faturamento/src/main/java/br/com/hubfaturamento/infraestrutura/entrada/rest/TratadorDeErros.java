package br.com.hubfaturamento.infraestrutura.entrada.rest;

import br.com.hubfaturamento.aplicacao.FaturaNaoEncontradaException;
import br.com.hubfaturamento.dominio.fatura.FaturaInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(FaturaInvalidaException.class)
    ProblemDetail faturaInvalida(FaturaInvalidaException e) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_CONTENT,
            e.getMessage()
        );
    }

    @ExceptionHandler(FaturaNaoEncontradaException.class)
    ProblemDetail naoEncontrada(FaturaNaoEncontradaException e) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            e.getMessage()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail jsonInvalido() {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "JSON inválido. Envie pedidoId (número), valor (número) e emailCliente (texto)."
        );
    }
}
