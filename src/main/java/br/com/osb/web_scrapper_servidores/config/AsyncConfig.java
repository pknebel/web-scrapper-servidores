package br.com.osb.web_scrapper_servidores.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    /**
     * Pool usado para consultar as 10 categorias de um mês em paralelo.
     *
     * <p>Sem {@code @Bean} este método não registrava nada no contexto e o Spring injetava o
     * {@code applicationTaskExecutor} da auto-configuração, ignorando silenciosamente a
     * configuração abaixo.</p>
     *
     * <p>A consulta anual percorre os meses <b>sequencialmente</b> justamente para que o pico de
     * chamadas simultâneas continue sendo o mesmo de hoje (10) e para que tarefas-mês nunca fiquem
     * bloqueadas esperando tarefas-categoria do mesmo pool.</p>
     */
    @Bean
    @Primary
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("consulta-api-");
        executor.initialize();
        return executor;
    }
}
