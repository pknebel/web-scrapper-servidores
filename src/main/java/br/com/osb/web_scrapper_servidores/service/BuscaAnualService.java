package br.com.osb.web_scrapper_servidores.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.osb.web_scrapper_servidores.config.ConsultaAnualProperties;
import br.com.osb.web_scrapper_servidores.dto.request.PeriodoMensalDTO;
import br.com.osb.web_scrapper_servidores.dto.response.BuscaAnualResponseDTO;
import br.com.osb.web_scrapper_servidores.dto.response.BuscaDadosResponseDTO;
import br.com.osb.web_scrapper_servidores.dto.response.FalhaMensalDTO;
import br.com.osb.web_scrapper_servidores.dto.response.MesConsultadoDTO;
import br.com.osb.web_scrapper_servidores.exception.ApiExternaException;

/**
 * Orquestra a consulta de todos os meses completos do ano corrente e consolida o resultado.
 *
 * <h2>Estratégia de execução</h2>
 * <p>Os meses são percorridos <b>sequencialmente</b> e, dentro de cada mês,
 * {@link BuscaDadosService} mantém as 10 categorias em paralelo — exatamente como já acontecia.
 * A escolha é deliberada:</p>
 * <ul>
 *   <li>o pico de chamadas simultâneas continua sendo 10, e não 120, respeitando um portal público
 *       cujo limite de requisições não é documentado;</li>
 *   <li>paralelizar os meses sobre o mesmo pool causaria <i>deadlock</i> (tarefas-mês bloqueadas
 *       esperando tarefas-categoria do mesmo pool) e estouro da fila de 20;</li>
 *   <li>o processamento sequencial produz log ordenado e isolamento natural de falha por mês.</li>
 * </ul>
 * <p>Se a latência vier a incomodar, o caminho é um pool <b>separado</b> para os meses com
 * concorrência baixa (2 ou 3) — nunca reaproveitar o pool das categorias.</p>
 */
@Service
public class BuscaAnualService {

    private static final Logger log = LoggerFactory.getLogger(BuscaAnualService.class);

    private final PeriodoService periodoService;
    private final BuscaDadosService buscaDadosService;
    private final ConsultaAnualCache cache;
    private final ConsultaAnualProperties propriedades;

    public BuscaAnualService(PeriodoService periodoService,
                             BuscaDadosService buscaDadosService,
                             ConsultaAnualCache cache,
                             ConsultaAnualProperties propriedades) {
        this.periodoService = periodoService;
        this.buscaDadosService = buscaDadosService;
        this.cache = cache;
        this.propriedades = propriedades;
    }

    /**
     * Consulta e consolida todos os meses completos do ano corrente.
     *
     * <p>Quando nenhum mês está completo (execução em janeiro), devolve um resultado vazio — não é
     * um erro, é o estado legítimo do início do ano.</p>
     */
    public BuscaAnualResponseDTO consultarAnoCorrente() {
        int ano = periodoService.anoCorrente();
        List<PeriodoMensalDTO> meses = periodoService.mesesCompletosDoAnoCorrente();

        if (meses.isEmpty()) {
            return BuscaAnualResponseDTO.vazio(ano);
        }

        String chave = chaveDeCache(ano, meses.size());

        return cache.buscar(chave)
                .orElseGet(() -> consultarEArmazenar(chave, ano, meses));
    }

    private BuscaAnualResponseDTO consultarEArmazenar(String chave, int ano, List<PeriodoMensalDTO> meses) {
        BuscaAnualResponseDTO resultado = consultarMeses(ano, meses);

        // Um resultado parcial não é cacheado: o usuário deve poder tentar de novo e obter o
        // mês que falhou, em vez de receber a mesma falha por toda a validade do cache.
        if (!resultado.possuiFalhas()) {
            cache.armazenar(chave, resultado);
        }

        return resultado;
    }

    private BuscaAnualResponseDTO consultarMeses(int ano, List<PeriodoMensalDTO> meses) {
        List<MesConsultadoDTO> consultados = new ArrayList<>(meses.size());
        List<FalhaMensalDTO> falhas = new ArrayList<>();

        log.info("Iniciando consulta anual de {}: {} mês(es) completo(s) a consultar.", ano, meses.size());

        for (PeriodoMensalDTO periodo : meses) {
            try {
                log.info("Consultando {}/{} ({} a {}).",
                        periodo.nomeMes(), ano, periodo.primeiroDia(), periodo.ultimoDia());

                BuscaDadosResponseDTO dados = buscaDadosService.consultar(periodo.comoApiRequest());
                consultados.add(MesConsultadoDTO.de(periodo, dados));
            } catch (RuntimeException e) {
                tratarFalha(ano, periodo, e, falhas);
            }
        }

        log.info("Consulta anual de {} concluída: {} mês(es) com sucesso, {} com falha.",
                ano, consultados.size(), falhas.size());

        return new BuscaAnualResponseDTO(ano, consultados, falhas);
    }

    private void tratarFalha(int ano, PeriodoMensalDTO periodo, RuntimeException e, List<FalhaMensalDTO> falhas) {
        Throwable causa = causaRaiz(e);
        String motivo = causa.getMessage() != null ? causa.getMessage() : causa.toString();

        log.error("Falha ao consultar {}/{} ({} a {}): {}",
                periodo.nomeMes(), ano, periodo.primeiroDia(), periodo.ultimoDia(), motivo, causa);

        if (propriedades.falharSeAlgumMesFalhar()) {
            throw new ApiExternaException(
                "Falha ao consultar %s/%d: %s".formatted(periodo.nomeMes(), ano, motivo), causa);
        }

        falhas.add(new FalhaMensalDTO(periodo.numeroMes(), periodo.nomeMes(), motivo));
    }

    /** {@code CompletableFuture.join()} embrulha a falha real em {@link CompletionException}. */
    private Throwable causaRaiz(RuntimeException e) {
        return (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
    }

    private String chaveDeCache(int ano, int quantidadeDeMeses) {
        return "%d:%d".formatted(ano, quantidadeDeMeses);
    }
}
