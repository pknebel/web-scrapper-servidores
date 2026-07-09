package br.com.osb.web_scrapper_servidores.mapper;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.osb.web_scrapper_servidores.dto.request.ApiRequestDTO;

@Component
public class ConsultaApiMapper {

    public Map<String, Object> toApiRequest(ApiRequestDTO request) {

        Map<String, Object> body = new HashMap<>();

        Map<String, Object> searchBean = new HashMap<>();
        Map<String, Object> searchProperties = new HashMap<>();

        Map<String, Object> filtro = new HashMap<>();
        
        filtro.put("separator", Map.of(
            "name", "Filtrar por",
            "hide", true
        ));

        filtro.put("property", "listFolha.data");
        filtro.put("key", "listFolha.data");
        filtro.put("searchRestrictionType", "DATE");

        filtro.put("condition", Map.of(
            "value", "IGUAL",
            "desc", "Igual"
        ));

        filtro.put("params", Map.of(
            "defaultValueCompare", request.data().toString(),
            "nestedName", "listFolha",
            "defaultValue", request.data().toString()
        ));

        filtro.put("value", request.data().toString());
        filtro.put("valueCompare", request.data().toString());

        searchProperties.put("Filtrar porlistFolha.data", filtro);
        searchBean.put("searchProperties", searchProperties);

        body.put("searchBean", searchBean);
        body
        .put("pagination", Map.of(
            "page", 1,
            "paginate", true
        ));

        return body;
    }

}
