package br.com.osb.web_scrapper_servidores.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expõe o relógio da aplicação como bean.
 *
 * <p>A regra "meses completos do ano corrente" depende da data atual. Fixar o fuso horário aqui
 * evita que a aplicação, rodando em um servidor em UTC, considere um mês a mais ou a menos na
 * virada do mês. Injetar {@link Clock} também torna a regra determinística nos testes.</p>
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:America/Sao_Paulo}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
