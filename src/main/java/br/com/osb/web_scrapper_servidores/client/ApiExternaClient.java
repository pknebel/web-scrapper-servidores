package br.com.osb.web_scrapper_servidores.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import br.com.osb.web_scrapper_servidores.config.ApiExternaProperties;
import br.com.osb.web_scrapper_servidores.dto.request.ApiRequestDTO;
import br.com.osb.web_scrapper_servidores.dto.response.ApiResponseDTO;
import br.com.osb.web_scrapper_servidores.exception.ApiExternaException;
import br.com.osb.web_scrapper_servidores.mapper.ConsultaApiMapper;

@Service
public class ApiExternaClient {

    private static final Logger log = LoggerFactory.getLogger(ApiExternaClient.class);

    private final RestTemplate restTemplate;
    private final ConsultaApiMapper mapper;
    private final ApiExternaProperties propriedades;

    public ApiExternaClient(RestTemplate restTemplate,
                            ConsultaApiMapper mapper,
                            ApiExternaProperties propriedades) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.propriedades = propriedades;
    }

    /**
     * Executa a consulta ao serviço externo mantendo o mesmo padrão de requisição já utilizado
     * (POST com o {@code searchBean} montado pelo {@link ConsultaApiMapper}).
     *
     * <p>Acrescenta apenas duas garantias que faltavam: validação da resposta (corpo ou
     * {@code totais} nulos viravam {@code NullPointerException}) e uma nova tentativa em caso de
     * falha de rede, já que o portal é um serviço público sujeito a instabilidade.</p>
     *
     * @throws ApiExternaException quando todas as tentativas falharem.
     */
    public ApiResponseDTO consultarApiExterna(String url, ApiRequestDTO request) {
        Map<String, Object> body = mapper.toApiRequest(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        int tentativas = propriedades.tentativas();
        ApiExternaException ultimaFalha = null;

        for (int tentativa = 1; tentativa <= tentativas; tentativa++) {
            try {
                return executar(url, entity, request);
            } catch (RestClientException | ApiExternaException e) {
                ultimaFalha = comoFalhaDeIntegracao(url, request, e);

                log.warn("Tentativa {}/{} falhou para a data {} em {}: {}",
                        tentativa, tentativas, request.data(), url, e.toString());

                aguardarAntesDeNovaTentativa(tentativa, tentativas);
            }
        }

        throw ultimaFalha != null
                ? ultimaFalha
                : new ApiExternaException("Nenhuma tentativa executada para %s".formatted(url));
    }

    private ApiResponseDTO executar(String url, HttpEntity<Map<String, Object>> entity, ApiRequestDTO request) {
        ResponseEntity<ApiResponseDTO> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            ApiResponseDTO.class
        );

        ApiResponseDTO corpo = response.getBody();

        if (corpo == null || corpo.totais() == null) {
            throw new ApiExternaException(
                "Resposta sem 'totais' para a data %s em %s".formatted(request.data(), url));
        }

        return corpo;
    }

    private ApiExternaException comoFalhaDeIntegracao(String url, ApiRequestDTO request, RuntimeException e) {
        if (e instanceof ApiExternaException falha) {
            return falha;
        }

        return new ApiExternaException(
            "Falha ao consultar %s para a data %s: %s".formatted(url, request.data(), e.getMessage()), e);
    }

    private void aguardarAntesDeNovaTentativa(int tentativa, int tentativas) {
        long intervalo = propriedades.intervaloRetentativaMs();

        if (tentativa >= tentativas || intervalo <= 0) {
            return;
        }

        try {
            Thread.sleep(intervalo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiExternaException("Consulta interrompida durante a espera entre tentativas.", e);
        }
    }
}
