package br.com.hubfaturamento.infraestrutura.configuracao;

import br.com.hubfaturamento.aplicacao.FaturamentoService;
import br.com.hubfaturamento.dominio.porta.saida.EmitirBoleto;
import br.com.hubfaturamento.dominio.porta.saida.EmitirNotaFiscal;
import br.com.hubfaturamento.dominio.porta.saida.GuardarFatura;
import br.com.hubfaturamento.dominio.porta.saida.NotificarCliente;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
class ConfiguracaoDoHub {

    @Bean
    Clock relogio() {
        return Clock.systemDefaultZone();
    }

    @Bean
    FaturamentoService faturamentoService(
        GuardarFatura faturas,
        EmitirBoleto boletos,
        EmitirNotaFiscal notas,
        NotificarCliente avisos,
        Clock relogio
    ) {
        return new FaturamentoService(faturas, boletos, notas, avisos, relogio);
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(
        name = "hub.filas.agendamento-ativo",
        havingValue = "true",
        matchIfMissing = true
    )
    static class AgendamentoDasFilas {}
}
