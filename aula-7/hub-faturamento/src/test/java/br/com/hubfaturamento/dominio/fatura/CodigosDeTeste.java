package br.com.hubfaturamento.dominio.fatura;

public final class CodigosDeTeste {

    private CodigosDeTeste() {}

    public static String codigoDeBarras(String quarentaETresDigitos) {
        int dv = DigitoVerificador.dvCodigoDeBarras(quarentaETresDigitos);
        return (
            quarentaETresDigitos.substring(0, 4) +
            dv +
            quarentaETresDigitos.substring(4)
        );
    }

    public static String chaveDeAcesso(String quarentaETresDigitos) {
        return (
            quarentaETresDigitos +
            DigitoVerificador.dvChaveDeAcesso(quarentaETresDigitos)
        );
    }
}
