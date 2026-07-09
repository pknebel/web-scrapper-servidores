package br.com.osb.web_scrapper_servidores.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.osb.web_scrapper_servidores.dto.request.ApiRequestDTO;
import br.com.osb.web_scrapper_servidores.dto.response.TotaisResponseDTO;
import br.com.osb.web_scrapper_servidores.service.BuscaDadosService;

@RestController
@RequestMapping("/busca-dados")
public class BuscaDadosController {

    private final BuscaDadosService service;

    public BuscaDadosController(BuscaDadosService service) {
        this.service = service;
    }
 
    @GetMapping
    public TotaisResponseDTO buscarDados(@RequestParam LocalDate request) {

        ApiRequestDTO apiRequest = new ApiRequestDTO(request);

        return service.consultar(apiRequest);
    }

}
