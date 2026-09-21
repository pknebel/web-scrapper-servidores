package br.com.osb.web_scrapper_servidores.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.osb.web_scrapper_servidores.dto.response.BuscaAnualResponseDTO;
import br.com.osb.web_scrapper_servidores.service.BuscaAnualService;
import br.com.osb.web_scrapper_servidores.service.PlanilhaAnualService;

/**
 * Endpoints da consulta anual.
 *
 * <p>Não recebem parâmetros: o ano e os meses completos são determinados dinamicamente a partir da
 * data atual. O endpoint mensal {@code /busca-mensal} permanece inalterado.</p>
 */
@RestController
@RequestMapping("/busca-anual")
@CrossOrigin(origins = "${app.cors.origem:http://localhost:5173}")
public class BuscaAnualController {

    private static final String TIPO_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final Logger log = LoggerFactory.getLogger(BuscaAnualController.class);

    private final BuscaAnualService buscaAnualService;
    private final PlanilhaAnualService planilhaAnualService;

    public BuscaAnualController(BuscaAnualService buscaAnualService,
                                PlanilhaAnualService planilhaAnualService) {
        this.buscaAnualService = buscaAnualService;
        this.planilhaAnualService = planilhaAnualService;
    }

    /** Dados consolidados de todos os meses completos do ano corrente. */
    @GetMapping
    public ResponseEntity<BuscaAnualResponseDTO> buscarDadosAnuais() {
        BuscaAnualResponseDTO dados = buscaAnualService.consultarAnoCorrente();

        if (dados.possuiFalhas()) {
            log.warn("Consulta anual de {} devolvida com {} mês(es) em falha.",
                    dados.ano(), dados.falhas().size());
        }

        return ResponseEntity.ok(dados);
    }

    /**
     * Planilha completa do ano corrente para download.
     *
     * <p>Reutiliza a mesma consulta consolidada do endpoint de dados — que é servida do cache
     * quando o usuário já visualizou a tabela — em vez de refazer as consultas mensais.</p>
     */
    @GetMapping("/planilha")
    public ResponseEntity<byte[]> baixarPlanilhaAnual() throws IOException {
        BuscaAnualResponseDTO dados = buscaAnualService.consultarAnoCorrente();

        byte[] arquivo = planilhaAnualService.gerarPlanilha(dados);

        String nomeArquivo = "QUADRO_DE_SERVIDORES_GERAL_ANUAL_%d.xlsx".formatted(dados.ano());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(nomeArquivo).build().toString())
            .contentType(MediaType.parseMediaType(TIPO_XLSX))
            .body(arquivo);
    }
}
