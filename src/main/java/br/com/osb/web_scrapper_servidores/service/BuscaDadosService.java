package br.com.osb.web_scrapper_servidores.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.osb.web_scrapper_servidores.client.ApiExternaClient;
import br.com.osb.web_scrapper_servidores.config.ApiExternaProperties;
import br.com.osb.web_scrapper_servidores.dto.request.ApiRequestDTO;
import br.com.osb.web_scrapper_servidores.dto.response.BuscaDadosResponseDTO;
import br.com.osb.web_scrapper_servidores.dto.response.TotaisResponseDTO;

/**
 * Consulta as categorias de um único mês.
 *
 * <p>Esta é a operação reutilizável de "consultar um período": recebe a data de referência via
 * {@link ApiRequestDTO} e dispara as 10 categorias em paralelo. A consulta anual
 * ({@link BuscaAnualService}) apenas invoca este serviço uma vez por mês completo, sem duplicar a
 * lógica de montagem da requisição nem de paralelismo.</p>
 */
@Service
public class BuscaDadosService {

    private static final Logger log = LoggerFactory.getLogger(BuscaDadosService.class);

    private static final String SERVIDORES = "servidores";
    private static final String EFETIVOS = "efetivos";
    private static final String COMISSIONADOS = "comissionados";
    private static final String CELETISTAS = "celetistas";
    private static final String APOSENTADOS = "aposentados";
    private static final String PENSIONISTAS = "pensionistas";
    private static final String ESTAGIARIOS = "estagiarios";
    private static final String CEDIDOS_RECEBIDOS = "cedidosRecebidos";
    private static final String TEMPORARIOS = "temporarios";
    private static final String AGENTE_POLITICO = "agentePolitico";

    private final ApiExternaClient client;
    private final Executor executor;
    private final ApiExternaProperties propriedades;

    public BuscaDadosService(ApiExternaClient client,
                             Executor executor,
                             ApiExternaProperties propriedades) {
        this.client = client;
        this.executor = executor;
        this.propriedades = propriedades;
    }

    private CompletableFuture<TotaisResponseDTO> consultarAsync(String categoria, ApiRequestDTO request) {
        String url = propriedades.urlDaCategoria(categoria, request.data().getYear());

        return CompletableFuture.supplyAsync(
            () -> client.consultarApiExterna(url, request).totais(),
            executor
        );
    }

    public BuscaDadosResponseDTO consultar(ApiRequestDTO request) {
        log.debug("Consultando as categorias da data de referência {}.", request.data());

        CompletableFuture<TotaisResponseDTO> servidores = consultarAsync(SERVIDORES, request);
        CompletableFuture<TotaisResponseDTO> efetivos = consultarAsync(EFETIVOS, request);
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
