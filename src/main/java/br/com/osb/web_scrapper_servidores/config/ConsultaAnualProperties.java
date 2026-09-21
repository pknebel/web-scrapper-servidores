package br.com.osb.web_scrapper_servidores.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da consulta anual.
 *
 * @param cacheTtlMinutos       validade do resultado consolidado em minutos. {@code 0} desativa o
 *                              cache. O cache evita que o endpoint de dados e o endpoint de
 *                              download refaçam as mesmas ~120 requisições ao serviço externo.
 * @param falharSeAlgumMesFalhar {@code false} (padrão) registra a falha do mês e segue para os
 *                              demais; {@code true} interrompe o processamento no primeiro erro.
 */
@ConfigurationProperties(prefix = "app.consulta-anual")
public record ConsultaAnualProperties(

        Long cacheTtlMinutos,
        boolean falharSeAlgumMesFalhar) {

    private static final long CACHE_TTL_PADRAO_MINUTOS = 10L;

    public ConsultaAnualProperties {
        cacheTtlMinutos = (cacheTtlMinutos == null || cacheTtlMinutos < 0)
                ? CACHE_TTL_PADRAO_MINUTOS
                : cacheTtlMinutos;
    }

    public Duration cacheTtl() {
        return Duration.ofMinutes(cacheTtlMinutos);
    }

    public boolean cacheDesabilitado() {
        return cacheTtlMinutos == 0L;
    }
}
