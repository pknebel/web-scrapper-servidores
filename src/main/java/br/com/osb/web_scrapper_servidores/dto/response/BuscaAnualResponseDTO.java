package br.com.osb.web_scrapper_servidores.dto.response;

import java.util.List;
import java.util.Optional;

/**
 * Resultado consolidado de todos os meses completos do ano corrente.
 *
 * @param ano    ano corrente considerado na consulta.
 * @param meses  meses consultados com sucesso, em ordem cronológica.
 * @param falhas meses que falharam. Vazio quando todos foram consultados com sucesso.
 */
public record BuscaAnualResponseDTO(
    int ano,
    List<MesConsultadoDTO> meses,
    List<FalhaMensalDTO> falhas
) {

    public BuscaAnualResponseDTO {
        meses = meses == null ? List.of() : List.copyOf(meses);
        falhas = falhas == null ? List.of() : List.copyOf(falhas);
    }

    public static BuscaAnualResponseDTO vazio(int ano) {
        return new BuscaAnualResponseDTO(ano, List.of(), List.of());
    }

    /** Busca os dados de um mês pelo número (1 a 12). */
    public Optional<MesConsultadoDTO> mes(int numeroMes) {
        return meses.stream()
                .filter(mesConsultado -> mesConsultado.mes() == numeroMes)
                .findFirst();
    }

    public boolean possuiFalhas() {
        return !falhas.isEmpty();
    }
}
