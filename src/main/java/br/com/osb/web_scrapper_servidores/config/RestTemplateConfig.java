package br.com.osb.web_scrapper_servidores.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * {@code RestTemplate} com timeouts explícitos.
     *
     * <p>Sem timeout, um único mês travado no serviço externo seguraria a requisição
     * indefinidamente — problema que se agrava na consulta anual, que percorre vários meses.</p>
     */
    @Bean
    public RestTemplate restTemplate(ApiExternaProperties propriedades) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(propriedades.timeoutConexaoMs()));
        factory.setReadTimeout(Duration.ofMillis(propriedades.timeoutLeituraMs()));

        return new RestTemplate(factory);
    }
}
