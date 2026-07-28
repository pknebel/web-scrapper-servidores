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

    public ApiResponseDTO consultarApiExterna(String url, ApiRequestDTO request) {
        Map<String, Object> body = mapper.toApiRequest(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<ApiResponseDTO> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            ApiResponseDTO.class
        );

    return response.getBody();
            
    }

}

/*
URLs que precisam ser consultadas:
https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/efetivos/listAll?exercicio=158
https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/comissionados/listAll?exercicio=158
https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/celetistas/listAll?exercicio=158
https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/aposentados/listAll?exercicio=158
https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/pensionistas/listAll?exercicio=158
https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/estagiarios/listAll?exercicio=158
https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/cedidosRecebidos/listAll?exercicio=158
https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/temporarios/listAll?exercicio=158
https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal/agentePolitico/listAll?exercicio=158

*/