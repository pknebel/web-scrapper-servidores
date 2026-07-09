package br.com.osb.web_scrapper_servidores.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiResponseDTO (
    TotaisResponseDTO totais
) {

}
