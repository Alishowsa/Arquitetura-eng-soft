package br.com.hubfaturamento.infraestrutura.saida.soap;

import br.com.hubfaturamento.dominio.fatura.DigitoVerificador;
import br.com.hubfaturamento.dominio.fatura.Fatura;
import br.com.hubfaturamento.dominio.porta.saida.EmissorIndisponivelException;
import br.com.hubfaturamento.dominio.porta.saida.EmitirNotaFiscal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class EmissorDeNotaSoapSimulado implements EmitirNotaFiscal {

    private static final Logger LOG = LoggerFactory.getLogger(
        EmissorDeNotaSoapSimulado.class
    );
    private static final Pattern CHAVE = Pattern.compile(
        "<chNFe>(\\d{44})</chNFe>"
    );
    private static final String CNPJ_EMITENTE = "11222333000181";
    private static final String UF_SP = "35";

    private final boolean foraDoAr;

    EmissorDeNotaSoapSimulado(
        @Value("${hub.emissor-nota.fora-do-ar:false}") boolean foraDoAr
    ) {
        this.foraDoAr = foraDoAr;
    }

    @Override
    public String emitir(Fatura fatura) throws EmissorIndisponivelException {
        if (foraDoAr) {
            throw new EmissorIndisponivelException(
                "Emissor de nota fiscal não respondeu."
            );
        }
        String requisicao = """
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
          <soap:Body>
            <nfeAutorizacao>
              <numero>%d</numero>
              <destinatario documento="%s">%s</destinatario>
              <valorTotal>%s</valorTotal>
            </nfeAutorizacao>
          </soap:Body>
        </soap:Envelope>""".formatted(
            fatura.getId(),
            fatura.getCliente().documento(),
            fatura.getCliente().nome(),
            fatura.getValorTotal()
        );
        LOG.debug("[SOAP] requisição:\n{}", requisicao);

        String resposta = responder(fatura.getId());
        Matcher m = CHAVE.matcher(resposta);
        if (!m.find()) {
            throw new EmissorIndisponivelException(
                "Resposta SOAP sem chave de acesso."
            );
        }
        LOG.info(
            "[SOAP] nota da fatura {} autorizada, chave {}",
            fatura.getId(),
            m.group(1)
        );
        return m.group(1);
    }

    private static String responder(Long numeroNota) {
        String aamm = LocalDate.now().format(
            DateTimeFormatter.ofPattern("yyMM")
        );
        String semDv =
            UF_SP +
            aamm +
            CNPJ_EMITENTE +
            "55" +
            "001" +
            "%09d".formatted(numeroNota) +
            "1" +
            "%08d".formatted((numeroNota * 7919) % 100_000_000);
        String chave = semDv + DigitoVerificador.dvChaveDeAcesso(semDv);
        return """
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
          <soap:Body><retAutorizacao><cStat>100</cStat><chNFe>%s</chNFe></retAutorizacao></soap:Body>
        </soap:Envelope>""".formatted(chave);
    }
}
