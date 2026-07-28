package br.com.osb.web_scrapper_servidores.controller;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.osb.web_scrapper_servidores.dto.request.ApiRequestDTO;
import br.com.osb.web_scrapper_servidores.dto.response.BuscaDadosResponseDTO;
import br.com.osb.web_scrapper_servidores.service.BuscaDadosService;
import br.com.osb.web_scrapper_servidores.service.PlanilhaService;

@RestController
@RequestMapping("/busca-mensal")
public class BuscaDadosController {

    private final BuscaDadosService service;
    private final PlanilhaService planilhaService;

    public BuscaDadosController(BuscaDadosService service, PlanilhaService planilhaService) {
        this.service = service;
        this.planilhaService = planilhaService;
    }
 
    @GetMapping
    public ResponseEntity<?> buscarDados(
            @RequestParam LocalDate data, 
            @RequestParam(defaultValue = "false") boolean gerarPlanilha) throws IOException {

        ApiRequestDTO apiRequest = new ApiRequestDTO(data);
        BuscaDadosResponseDTO dados = service.consultar(apiRequest);

        if(gerarPlanilha) {
            
            byte[] arquivo = planilhaService.gerarPlanilha(data, dados);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=QUADRO_DE_SERVIDORES_GERAL_MENSAL.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(arquivo);

        }

        return ResponseEntity.ok(dados);
    }

}
