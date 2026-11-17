package br.com.hubfaturamento.dominio.fatura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DigitoVerificadorTest {

    @Test
    void chaveDeAcessoDoManualDaNfe() {
        // exemplo do manual da NF-e: soma ponderada 644, resto 6, DV = 11 - 6 = 5
        String semDv = "5206043300991100250655012000000780026730161";
        assertThat(DigitoVerificador.dvChaveDeAcesso(semDv)).isEqualTo(5);
        assertThat(DigitoVerificador.chaveDeAcessoValida(semDv + "5")).isTrue();
        assertThat(
            DigitoVerificador.chaveDeAcessoValida(semDv + "6")
        ).isFalse();
    }

    @Test
    void dvDoCodigoDeBarrasNuncaEhZeroNemDez() {
        for (int i = 0; i < 2000; i++) {
            String semDv = "%043d".formatted(i * 7_919L);
            int dv = DigitoVerificador.dvCodigoDeBarras(semDv);
            assertThat(dv).isBetween(1, 9);
            String codigo = CodigosDeTeste.codigoDeBarras(semDv);
            assertThat(DigitoVerificador.codigoDeBarrasValido(codigo)).isTrue();
        }
    }

    @Test
    void tamanhoErradoOuLetrasSaoInvalidos() {
        assertThat(
            DigitoVerificador.codigoDeBarrasValido("1".repeat(43))
        ).isFalse();
        assertThat(
            DigitoVerificador.codigoDeBarrasValido("a".repeat(44))
        ).isFalse();
        assertThat(DigitoVerificador.chaveDeAcessoValida(null)).isFalse();
    }
}
