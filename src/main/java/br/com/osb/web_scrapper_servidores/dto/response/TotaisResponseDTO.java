package br.com.osb.web_scrapper_servidores.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TotaisResponseDTO (
    BigDecimal QUANTIDADE,
    BigDecimal VALOR
) {

}
