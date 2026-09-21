package br.com.osb.web_scrapper_servidores.dto.request;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * Período de um mês fechado.
 *
 * <p>O primeiro e o último dia são derivados de {@link YearMonth}, que já trata corretamente meses
 * de 28, 29, 30 e 31 dias e anos bissextos — nenhum cálculo manual de calendário é feito aqui.</p>
 *
 * <p>Observação sobre a integração: o serviço externo filtra a folha por uma <b>data de referência
 * única</b> ({@code listFolha.data} com condição {@code IGUAL}), que corresponde ao primeiro dia do
 * mês. O {@code ultimoDia} é mantido no modelo para logs, resposta da API e validação, mas a
 * requisição continua sendo montada com {@code primeiroDia}, preservando o contrato atual.</p>
 */
public record PeriodoMensalDTO(
    YearMonth mes,
    LocalDate primeiroDia,
    LocalDate ultimoDia
) {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    public PeriodoMensalDTO {
        Objects.requireNonNull(mes, "mes é obrigatório");
        Objects.requireNonNull(primeiroDia, "primeiroDia é obrigatório");
        Objects.requireNonNull(ultimoDia, "ultimoDia é obrigatório");
    }

    /** Cria o período a partir do mês informado. Entrada preferencial desta classe. */
    public static PeriodoMensalDTO de(YearMonth mes) {
        Objects.requireNonNull(mes, "mes é obrigatório");

        return new PeriodoMensalDTO(mes, mes.atDay(1), mes.atEndOfMonth());
    }

    public static PeriodoMensalDTO de(int ano, int numeroMes) {
        return de(YearMonth.of(ano, numeroMes));
    }

    public int ano() {
        return mes.getYear();
    }

    public int numeroMes() {
        return mes.getMonthValue();
    }

    /** Nome do mês em português e caixa alta, no mesmo padrão do arquivo de referência. */
    public String nomeMes() {
        return mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR).toUpperCase(PT_BR);
    }

    /** Data de referência enviada ao serviço externo. */
    public ApiRequestDTO comoApiRequest() {
        return new ApiRequestDTO(primeiroDia);
    }
}
