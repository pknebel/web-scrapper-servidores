package br.com.osb.web_scrapper_servidores.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do serviço externo (portal da transparência).
 *
 * <p>O parâmetro {@code exercicio} da API externa identifica o exercício financeiro e, portanto,
 * está preso a um ano. Por isso ele é configurável por ano ({@code exercicio-por-ano}) com um
 * valor de fallback ({@code exercicio-padrao}), evitando que a consulta do "ano corrente" passe a
 * devolver dados de um ano anterior na virada do ano.</p>
 */
@ConfigurationProperties(prefix = "app.api-externa")
public record ApiExternaProperties(

        String baseUrl,
        Integer exercicioPadrao,
        Map<Integer, Integer> exercicioPorAno,
        int tentativas,
        long intervaloRetentativaMs,
        long timeoutConexaoMs,
        long timeoutLeituraMs) {

    private static final String BASE_URL_PADRAO =
            "https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal";

    private static final int EXERCICIO_PADRAO = 158;
    private static final int TENTATIVAS_PADRAO = 2;
    private static final long INTERVALO_RETENTATIVA_PADRAO_MS = 500L;
    private static final long TIMEOUT_CONEXAO_PADRAO_MS = 10_000L;
    private static final long TIMEOUT_LEITURA_PADRAO_MS = 30_000L;

    public ApiExternaProperties {
        baseUrl = normalizarBaseUrl(baseUrl);
        exercicioPadrao = exercicioPadrao == null ? EXERCICIO_PADRAO : exercicioPadrao;
        exercicioPorAno = exercicioPorAno == null ? Map.of() : Map.copyOf(exercicioPorAno);
        tentativas = tentativas < 1 ? TENTATIVAS_PADRAO : tentativas;
        intervaloRetentativaMs = intervaloRetentativaMs < 0
                ? INTERVALO_RETENTATIVA_PADRAO_MS
                : intervaloRetentativaMs;
        timeoutConexaoMs = timeoutConexaoMs <= 0 ? TIMEOUT_CONEXAO_PADRAO_MS : timeoutConexaoMs;
        timeoutLeituraMs = timeoutLeituraMs <= 0 ? TIMEOUT_LEITURA_PADRAO_MS : timeoutLeituraMs;
    }

    private static String normalizarBaseUrl(String valor) {
        if (valor == null || valor.isBlank()) {
            return BASE_URL_PADRAO;
        }

        String normalizada = valor.strip();

        while (normalizada.endsWith("/")) {
            normalizada = normalizada.substring(0, normalizada.length() - 1);
        }

        return normalizada;
    }

    /** Exercício configurado para o ano informado, ou o exercício padrão. */
    public int exercicioDoAno(int ano) {
        return exercicioPorAno.getOrDefault(ano, exercicioPadrao);
    }

    /** Monta a URL da categoria mantendo exatamente o padrão já utilizado no projeto. */
    public String urlDaCategoria(String categoria, int ano) {
        return "%s/%s/listAll?exercicio=%d".formatted(baseUrl, categoria, exercicioDoAno(ano));
    }
}
