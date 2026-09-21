package br.com.osb.web_scrapper_servidores.dto.response;

import java.time.LocalDate;

import br.com.osb.web_scrapper_servidores.dto.request.PeriodoMensalDTO;

/**
 * Dados de um mês consultado com sucesso.
 *
 * <p>Reutiliza {@link BuscaDadosResponseDTO} — exatamente a mesma estrutura de colunas devolvida
 * hoje por {@code /busca-mensal} — para que o frontend não precise de um segundo modelo de
 * dados.</p>
 */
public record MesConsultadoDTO(
    int mes,
    String nomeMes,
    LocalDate primeiroDia,
    LocalDate ultimoDia,
    BuscaDadosResponseDTO dados
) {

    public static MesConsultadoDTO de(PeriodoMensalDTO periodo, BuscaDadosResponseDTO dados) {
        return new MesConsultadoDTO(
            periodo.numeroMes(),
            periodo.nomeMes(),
            periodo.primeiroDia(),
            periodo.ultimoDia(),
            dados
        );
    }
}
