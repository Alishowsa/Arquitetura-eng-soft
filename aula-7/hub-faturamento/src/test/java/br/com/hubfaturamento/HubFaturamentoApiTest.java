package br.com.hubfaturamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.hubfaturamento.dominio.fatura.DigitoVerificador;
import br.com.hubfaturamento.infraestrutura.entrada.fila.LeitorDaFilaDeRetorno;
import br.com.hubfaturamento.infraestrutura.mensageria.FilasEmMemoria;
import br.com.hubfaturamento.simulacao.EmissorDeBoletoExterno;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "hub.filas.agendamento-ativo=false")
@AutoConfigureMockMvc
class HubFaturamentoApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    EmissorDeBoletoExterno bancoEmissor;

    @Autowired
    LeitorDaFilaDeRetorno leitorDeRetorno;

    @Autowired
    FilasEmMemoria filas;

    @BeforeEach
    void esvaziarFilas() {
        filas.filaDePedidos().clear();
        filas.filaDeRetorno().clear();
    }

    private static String pedido(long pedidoId) {
        return """
        {
          "pedidoId": %d,
          "cliente": { "nome": "Ana Souza", "email": "ana@loja.com", "documento": "123.456.789-09" },
          "itens": [
            { "descricao": "Caderno", "quantidade": 2, "valorUnitario": 19.90 },
            { "descricao": "Caneta",  "quantidade": 10, "valorUnitario": 2.50 }
          ]
        }
        """.formatted(pedidoId);
    }

    @Test
    void pedidoFechadoViraFaturaNotificada() throws Exception {
        String resposta = mvc
            .perform(
                post("/faturas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pedido(501))
            )
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("/faturas/")))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.valorTotal").value(64.80))
            .andExpect(jsonPath("$.status").value("COM_NOTA"))
            .andExpect(jsonPath("$.chaveNfe").value(matchesPattern("\\d{44}")))
            .andExpect(jsonPath("$.codigoBarras").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String chave = JsonPath.read(resposta, "$.chaveNfe");
        assertThat(DigitoVerificador.chaveDeAcessoValida(chave)).isTrue();
        int id = JsonPath.read(resposta, "$.id");

        assertThat(bancoEmissor.processarPedidos()).isEqualTo(1);
        assertThat(leitorDeRetorno.consumirRetornos()).isEqualTo(1);

        String depois = mvc
            .perform(get("/faturas/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NOTIFICADA"))
            .andExpect(
                jsonPath("$.codigoBarras").value(matchesPattern("\\d{44}"))
            )
            .andExpect(
                jsonPath("$.hashEmail").value(matchesPattern("[0-9a-f]{64}"))
            )
            .andExpect(jsonPath("$.itens.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String codigo = JsonPath.read(depois, "$.codigoBarras");
        assertThat(DigitoVerificador.codigoDeBarrasValido(codigo)).isTrue();
        assertThat(codigo).contains("0000006480");
    }

    @Test
    void mesmoPedidoDeNovoDevolve200ComAMesmaFatura() throws Exception {
        String primeiro = mvc
            .perform(
                post("/faturas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pedido(502))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        int id = JsonPath.read(primeiro, "$.id");

        mvc.perform(
            post("/faturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pedido(502))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id));
        assertThat(filas.filaDePedidos()).hasSize(1);
    }

    @Test
    void pedidoInvalidoRetorna422EFaturaInexistente404() throws Exception {
        mvc.perform(
            post("/faturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"pedidoId\": 503, \"cliente\": {\"nome\": \"Ana\", \"email\": \"ana@loja.com\", \"documento\": \"12345678909\"}, \"itens\": []}"
                )
        )
            .andExpect(status().isUnprocessableContent())
            .andExpect(
                jsonPath("$.detail").value(
                    startsWith("A fatura precisa de pelo menos um item")
                )
            );

        mvc.perform(
            post("/faturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"pedidoId\": 504, \"cliente\": {\"nome\": \"Ana\", \"email\": \"x\", \"documento\": \"1\"}, \"itens\": []}"
                )
        ).andExpect(status().isUnprocessableContent());

        mvc.perform(get("/faturas/999999")).andExpect(status().isNotFound());

        mvc.perform(
            post("/faturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoId\": \"abc\"}")
        ).andExpect(status().isBadRequest());
    }
}
