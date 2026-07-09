package br.com.osb.web_scrapper_servidores.client;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import br.com.osb.web_scrapper_servidores.dto.request.ApiRequestDTO;
import br.com.osb.web_scrapper_servidores.dto.response.ApiResponseDTO;
import br.com.osb.web_scrapper_servidores.mapper.ConsultaApiMapper;

@Service
public class ApiExternaClient {

    private final RestTemplate restTemplate;
    private final ConsultaApiMapper mapper;

    public ApiExternaClient(RestTemplate restTemplate, ConsultaApiMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    public ApiResponseDTO consultarApiExterna(ApiRequestDTO request) {
        Map<String, Object> body = mapper.toApiRequest(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<ApiResponseDTO> response = restTemplate.exchange(
            "https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/servidores/listAll?exercicio=158",
            HttpMethod.POST,
            entity,
            ApiResponseDTO.class
        );

    return response.getBody();
            
    }

}
