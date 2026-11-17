package br.com.hubfaturamento.dominio.fatura;

public final class DigitoVerificador {

    private DigitoVerificador() {}

    public static boolean codigoDeBarrasValido(String codigo) {
        if (codigo == null || !codigo.matches("\\d{44}")) {
            return false;
        }
        String semDv = codigo.substring(0, 4) + codigo.substring(5);
        return dvCodigoDeBarras(semDv) == codigo.charAt(4) - '0';
    }

    public static int dvCodigoDeBarras(String quarentaETresDigitos) {
        int resto = somaPonderada(quarentaETresDigitos) % 11;
        int dv = 11 - resto;
        return dv == 0 || dv == 10 || dv == 11 ? 1 : dv;
    }

    public static boolean chaveDeAcessoValida(String chave) {
        if (chave == null || !chave.matches("\\d{44}")) {
            return false;
        }
        return (
            dvChaveDeAcesso(chave.substring(0, 43)) == chave.charAt(43) - '0'
        );
    }

    public static int dvChaveDeAcesso(String quarentaETresDigitos) {
        int resto = somaPonderada(quarentaETresDigitos) % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int somaPonderada(String digitos) {
        int soma = 0;
        int peso = 2;
        for (int i = digitos.length() - 1; i >= 0; i--) {
            soma += (digitos.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        return soma;
    }
}
