package br.com.osb.web_scrapper_servidores.service;

import org.springframework.stereotype.Service;

import br.com.osb.web_scrapper_servidores.client.ApiExternaClient;
import br.com.osb.web_scrapper_servidores.dto.request.ApiRequestDTO;
import br.com.osb.web_scrapper_servidores.dto.response.ApiResponseDTO;
import br.com.osb.web_scrapper_servidores.dto.response.TotaisResponseDTO;

@Service
public class BuscaDadosService {

    private final ApiExternaClient client;

    public BuscaDadosService(ApiExternaClient client) {
        this.client = client;
    }

    public TotaisResponseDTO consultar(ApiRequestDTO request) {
        ApiResponseDTO response = client.consultarApiExterna(request);
        return response.totais();
    }

}
