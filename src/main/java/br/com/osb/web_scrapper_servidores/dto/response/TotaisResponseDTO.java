package br.com.osb.web_scrapper_servidores.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TotaisResponseDTO (
    BigDecimal QUANTIDADE,
    BigDecimal VALOR
) {

    /**
     * O serviço externo pode omitir {@code QUANTIDADE}/{@code VALOR} para categorias sem
     * movimentação no mês — o próprio arquivo de referência tem células vazias. Normalizar para
     * zero na construção evita {@code NullPointerException} nas somas e na geração da planilha.
     */
    public TotaisResponseDTO {
        QUANTIDADE = QUANTIDADE == null ? BigDecimal.ZERO : QUANTIDADE;
        VALOR = VALOR == null ? BigDecimal.ZERO : VALOR;
    }

    public static TotaisResponseDTO zerado() {
        return new TotaisResponseDTO(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
