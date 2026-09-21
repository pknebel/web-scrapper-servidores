package br.com.osb.web_scrapper_servidores.dto.response;

/**
 * Mês que não pôde ser consultado.
 *
 * <p>Permite que o frontend informe ao usuário exatamente quais meses ficaram de fora, em vez de
 * apresentar uma planilha silenciosamente incompleta.</p>
 */
public record FalhaMensalDTO(
    int mes,
    String nomeMes,
    String motivo
) {
}
