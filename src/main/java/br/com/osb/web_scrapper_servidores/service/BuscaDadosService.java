package br.com.osb.web_scrapper_servidores.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.stereotype.Service;

import br.com.osb.web_scrapper_servidores.client.ApiExternaClient;
import br.com.osb.web_scrapper_servidores.dto.request.ApiRequestDTO;
import br.com.osb.web_scrapper_servidores.dto.response.BuscaDadosResponseDTO;
import br.com.osb.web_scrapper_servidores.dto.response.TotaisResponseDTO;

@Service
public class BuscaDadosService {

    private final ApiExternaClient client;
    private final Executor executor;

    public BuscaDadosService(ApiExternaClient client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    private CompletableFuture<TotaisResponseDTO> consultarAsync(String url, ApiRequestDTO request) {
        return CompletableFuture.supplyAsync(
            () -> client.consultarApiExterna(url, request).totais(),
            executor
        );
    }

    private static final String baseUrl = "https://transparencia.e-publica.net/epublica-portal/rest/chapeco/gestaoDePessoal";

    private static final String SERVIDORES = baseUrl + "/servidores/listAll?exercicio=158";
    private static final String EFEITIVOS = baseUrl + "/efetivos/listAll?exercicio=158";
    private static final String COMISSIONADOS = baseUrl + "/comissionados/listAll?exercicio=158";
    private static final String CELETISTAS = baseUrl + "/celetistas/listAll?exercicio=158";
    private static final String APOSENTADOS = baseUrl + "/aposentados/listAll?exercicio=158";
    private static final String PENSIONISTAS = baseUrl + "/pensionistas/listAll?exercicio=158";
    private static final String ESTAGIARIOS = baseUrl + "/estagiarios/listAll?exercicio=158";
    private static final String CEDIDOS_RECEBIDOS = baseUrl + "/cedidosRecebidos/listAll?exercicio=158";
    private static final String TEMPORARIOS = baseUrl + "/temporarios/listAll?exercicio=158";
    private static final String AGENTE_POLITICO = baseUrl + "/agentePolitico/listAll?exercicio=158";

    public BuscaDadosResponseDTO consultar(ApiRequestDTO request) {
        CompletableFuture<TotaisResponseDTO> servidores = consultarAsync(SERVIDORES, request);
        CompletableFuture<TotaisResponseDTO> efetivos = consultarAsync(EFEITIVOS, request);
        CompletableFuture<TotaisResponseDTO> comissionados = consultarAsync(COMISSIONADOS, request);
        CompletableFuture<TotaisResponseDTO> celetistas = consultarAsync(CELETISTAS, request);
        CompletableFuture<TotaisResponseDTO> aposentados = consultarAsync(APOSENTADOS, request);
        CompletableFuture<TotaisResponseDTO> pensionistas = consultarAsync(PENSIONISTAS, request);
        CompletableFuture<TotaisResponseDTO> estagiarios = consultarAsync(ESTAGIARIOS, request);
        CompletableFuture<TotaisResponseDTO> cedidosRecebidos = consultarAsync(CEDIDOS_RECEBIDOS, request);
        CompletableFuture<TotaisResponseDTO> temporarios = consultarAsync(TEMPORARIOS, request);
        CompletableFuture<TotaisResponseDTO> agentePolitico = consultarAsync(AGENTE_POLITICO, request);

        CompletableFuture.allOf(
            servidores,
            efetivos,
            comissionados,
            celetistas,
            aposentados,
            pensionistas,
            estagiarios,
            cedidosRecebidos,
            temporarios,
            agentePolitico
        ).join();

        return new BuscaDadosResponseDTO(
            servidores.join(),
            efetivos.join(),
            comissionados.join(),
            celetistas.join(),
            aposentados.join(),
            pensionistas.join(),
            estagiarios.join(),
            cedidosRecebidos.join(),
            temporarios.join(),
            agentePolitico.join()
        );
    }

}