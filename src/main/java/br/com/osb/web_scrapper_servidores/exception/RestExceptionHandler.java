package br.com.osb.web_scrapper_servidores.exception;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tratamento centralizado de erros.
 *
 * <p>O projeto não tinha nenhum tratamento: qualquer falha do serviço externo virava um
 * {@code 500} genérico, sem log. Aqui as falhas de integração passam a devolver
 * {@code 502 Bad Gateway} — semanticamente correto, já que o erro é do serviço a montante — em
 * formato {@code ProblemDetail} (RFC 7807), que é o padrão do Spring.</p>
 *
 * <p>Só são tratadas as exceções conhecidas; as demais continuam seguindo o comportamento padrão do
 * Spring Boot, para não alterar contratos existentes.</p>
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ApiExternaException.class)
    public ResponseEntity<ProblemDetail> tratarFalhaNaApiExterna(ApiExternaException e) {
        log.error("Falha na comunicação com o serviço externo: {}", e.getMessage(), e);

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY,
            "Não foi possível obter os dados do serviço externo."
        );

        problema.setTitle("Falha na consulta ao serviço externo");
        problema.setProperty("motivo", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problema);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ProblemDetail> tratarFalhaNaGeracaoDaPlanilha(IOException e) {
        log.error("Falha ao gerar a planilha: {}", e.getMessage(), e);

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Não foi possível gerar a planilha."
        );

        problema.setTitle("Falha na geração da planilha");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema);
    }
}
