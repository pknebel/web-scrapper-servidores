package br.com.osb.web_scrapper_servidores.service;

import java.time.Clock;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.osb.web_scrapper_servidores.dto.request.PeriodoMensalDTO;

/**
 * Determina, de forma dinâmica, quais meses do ano corrente já estão completos.
 *
 * <p>Regra: de janeiro do ano corrente até o mês anterior ao mês atual. O mês em andamento é
 * sempre excluído. Em 1º de janeiro não há nenhum mês completo no ano corrente e a lista retorna
 * vazia.</p>
 */
@Service
public class PeriodoService {

    private static final Logger log = LoggerFactory.getLogger(PeriodoService.class);

    private final Clock clock;

    public PeriodoService(Clock clock) {
        this.clock = clock;
    }

    /** Mês corrente segundo o fuso configurado na aplicação. */
    public YearMonth mesCorrente() {
        return YearMonth.now(clock);
    }

    public int anoCorrente() {
        return mesCorrente().getYear();
    }

    public List<PeriodoMensalDTO> mesesCompletosDoAnoCorrente() {
        return mesesCompletosAte(mesCorrente());
    }

    /**
     * Meses completos do ano de {@code mesAtual}, de janeiro até o mês imediatamente anterior.
     *
     * <p>Exposto separadamente para permitir teste determinístico sem depender da data real.</p>
     */
    public List<PeriodoMensalDTO> mesesCompletosAte(YearMonth mesAtual) {
        Objects.requireNonNull(mesAtual, "mesAtual é obrigatório");

        YearMonth janeiro = YearMonth.of(mesAtual.getYear(), Month.JANUARY);

        List<PeriodoMensalDTO> meses = Stream
            .iterate(janeiro, mes -> mes.isBefore(mesAtual), mes -> mes.plusMonths(1))
            .map(PeriodoMensalDTO::de)
            .toList();

        if (meses.isEmpty()) {
            log.warn("Nenhum mês completo disponível no ano {} — o mês corrente ({}) ainda está em andamento.",
                    mesAtual.getYear(), mesAtual);
        } else {
            log.debug("{} mês(es) completo(s) no ano {}: de {} até {}.",
                    meses.size(), mesAtual.getYear(),
                    meses.getFirst().primeiroDia(), meses.getLast().ultimoDia());
        }

        return meses;
    }
}
