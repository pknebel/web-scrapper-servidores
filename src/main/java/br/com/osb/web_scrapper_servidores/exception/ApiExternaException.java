package br.com.osb.web_scrapper_servidores.exception;

/**
 * Falha na comunicação com o serviço externo (portal da transparência).
 *
 * <p>Antes esta classe era uma casca vazia que não estendia {@code Exception} e, por isso, não era
 * usada em lugar nenhum. Agora ela é a exceção padrão do projeto para erros de integração e é
 * traduzida para {@code 502 Bad Gateway} pelo {@code RestExceptionHandler}.</p>
 */
public class ApiExternaException extends RuntimeException {

    public ApiExternaException(String mensagem) {
        super(mensagem);
    }

    public ApiExternaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
