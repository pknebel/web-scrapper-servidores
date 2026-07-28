package br.com.osb.web_scrapper_servidores.dto.response;

public record BuscaDadosResponseDTO (

    TotaisResponseDTO servidores,
    TotaisResponseDTO efetivos,
    TotaisResponseDTO comissionados,
    TotaisResponseDTO celetistas,   
    TotaisResponseDTO aposentados,
    TotaisResponseDTO pensionistas,
    TotaisResponseDTO estagiarios,
    TotaisResponseDTO cedidosRecebidos,
    TotaisResponseDTO temporarios,
    TotaisResponseDTO agentePolitico
) {
}