package br.com.osb.web_scrapper_servidores.dto.request;

import java.time.LocalDate;

public record ApiRequestDTO(
    LocalDate data
) {
}
