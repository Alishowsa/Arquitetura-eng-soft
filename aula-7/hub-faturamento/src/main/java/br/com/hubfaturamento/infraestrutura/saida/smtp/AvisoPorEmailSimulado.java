package br.com.hubfaturamento.infraestrutura.saida.smtp;

import br.com.hubfaturamento.dominio.fatura.Fatura;
import br.com.hubfaturamento.dominio.porta.saida.NotificarCliente;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class AvisoPorEmailSimulado implements NotificarCliente {

    private static final Logger LOG = LoggerFactory.getLogger(
        AvisoPorEmailSimulado.class
    );

    @Override
    public String enviarBoletoENota(Fatura fatura) {
        String mensagem = """
        MAIL FROM:<faturamento@empresa.com.br>
        RCPT TO:<%s>
        DATA
        Subject: Fatura %d do pedido %d
        Content-Type: text/plain; charset=UTF-8

        Olá, %s.
        Valor total: R$ %s
        Código de barras do boleto: %s
        Chave de acesso da NF-e: %s
        .""".formatted(
            fatura.getCliente().email(),
            fatura.getId(),
            fatura.getPedidoId(),
            fatura.getCliente().nome(),
            fatura.getValorTotal(),
            fatura.getCodigoBarras(),
            fatura.getChaveNfe()
        );
        LOG.info("[SMTP simulado]\n{}", mensagem);
        return sha256(mensagem);
    }

    static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                texto.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
