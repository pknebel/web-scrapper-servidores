package br.com.osb.web_scrapper_servidores.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.osb.web_scrapper_servidores.config.ConsultaAnualProperties;
import br.com.osb.web_scrapper_servidores.dto.response.BuscaAnualResponseDTO;

/**
 * Cache curto do resultado anual consolidado.
 *
 * <p>Motivação: a tela do frontend faz "Buscar" e depois "Gerar Planilha". Sem cache, cada clique
 * dispararia a consulta completa do ano — cerca de 120 requisições ao serviço externo por clique.
 * Com o cache, o download reaproveita exatamente os dados já exibidos na tela.</p>
 *
 * <p>Guarda apenas uma entrada: a chave inclui o ano e a quantidade de meses completos, de modo que
 * o resultado é naturalmente invalidado quando um novo mês fecha.</p>
 */
@Component
public class ConsultaAnualCache {

    private static final Logger log = LoggerFactory.getLogger(ConsultaAnualCache.class);

    private final ConsultaAnualProperties propriedades;
    private final Clock clock;
    private final AtomicReference<Entrada> entrada = new AtomicReference<>();

    public ConsultaAnualCache(ConsultaAnualProperties propriedades, Clock clock) {
        this.propriedades = propriedades;
        this.clock = clock;
    }

    public Optional<BuscaAnualResponseDTO> buscar(String chave) {
        if (propriedades.cacheDesabilitado()) {
            return Optional.empty();
        }

        Entrada atual = entrada.get();

        if (atual == null || !atual.chave().equals(chave)) {
            return Optional.empty();
        }

        if (Instant.now(clock).isAfter(atual.expiraEm())) {
            entrada.compareAndSet(atual, null);
            log.debug("Cache da consulta anual expirado (chave {}).", chave);
            return Optional.empty();
        }

        log.info("Resultado anual servido do cache (chave {}).", chave);
        return Optional.of(atual.resultado());
    }

    public void armazenar(String chave, BuscaAnualResponseDTO resultado) {
        if (propriedades.cacheDesabilitado()) {
            return;
        }

        entrada.set(new Entrada(chave, resultado, Instant.now(clock).plus(propriedades.cacheTtl())));
        log.debug("Resultado anual armazenado em cache (chave {}, validade {}).", chave, propriedades.cacheTtl());
    }

    public void limpar() {
        entrada.set(null);
    }

    private record Entrada(String chave, BuscaAnualResponseDTO resultado, Instant expiraEm) {
    }
}
