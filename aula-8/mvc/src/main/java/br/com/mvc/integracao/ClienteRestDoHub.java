package br.com.mvc.integracao;

import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ClienteRestDoHub implements ClienteDoHub {

    private static final Logger LOG = LoggerFactory.getLogger(
        ClienteRestDoHub.class
    );

    private final RestClient http;
    private final long timeoutMs;

    public ClienteRestDoHub(
        @Value("${hub.url:http://127.0.0.1:8090}") String urlDoHub,
        @Value("${hub.timeout-ms:3000}") long timeoutMs
    ) {
        // java.net.http em vez de HttpURLConnection, que reenvia o POST sozinho quando a conexão cai
        HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(timeoutMs))
            .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
            cliente
        );
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.http = RestClient.builder()
            .baseUrl(urlDoHub)
            .requestFactory(fabrica)
            .build();
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Long faturar(PedidoParaFaturar pedido) throws FalhaNoHubException {
        try {
            RespostaDoHub resposta = http
                .post()
                .uri("/faturas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(pedido)
                .retrieve()
                .body(RespostaDoHub.class);
            if (resposta == null || resposta.id() == null) {
                throw new FalhaNoHubException(
                    "O hub respondeu sem o ID da fatura.",
                    null
                );
            }
            LOG.info(
                "Pedido {} faturado no hub: fatura {}",
                pedido.pedidoId(),
                resposta.id()
            );
            return resposta.id();
        } catch (ResourceAccessException e) {
            throw new FalhaNoHubException(
                "Hub fora do ar ou sem resposta em " +
                    timeoutMs +
                    " ms: " +
                    e.getMostSpecificCause().getMessage(),
                e
            );
        } catch (RestClientResponseException e) {
            throw new FalhaNoHubException(
                "Hub recusou o pedido (HTTP " +
                    e.getStatusCode().value() +
                    "): " +
                    e.getResponseBodyAsString(),
                e
            );
        } catch (RestClientException e) {
            throw new FalhaNoHubException(
                "Resposta inesperada do hub: " + e.getMessage(),
                e
            );
        }
    }

    record RespostaDoHub(Long id, String status) {}
}
