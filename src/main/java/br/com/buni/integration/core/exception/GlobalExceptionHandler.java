package br.com.buni.integration.core.exception;

import br.com.buni.integration.core.model.dto.ApiErrorResponse;
import br.com.buni.integration.core.util.DateUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest req) {
        int status = ex.getStatusCode().value();
        return build(status, codigoErro(status), limparMensagem(ex.getReason()), req.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUpload(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(413, "ARQUIVO_MUITO_GRANDE",
                "O arquivo enviado excede o limite permitido (50MB).", req.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(
            MissingServletRequestPartException ex, HttpServletRequest req) {
        return build(400, "ARQUIVO_NAO_INFORMADO",
                "O arquivo não foi incluído na requisição. Parâmetro esperado: file.", req.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest req) {
        return build(404, "NAO_ENCONTRADO",
                "Recurso não encontrado: " + req.getRequestURI(), req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Erro não tratado [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        return build(500, "ERRO_INTERNO",
                "Erro interno do servidor. Tente novamente ou contate o suporte.", req.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> build(int status, String erro, String mensagem, String path) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(DateUtils.agora())
                .status(status)
                .erro(erro)
                .mensagem(mensagem != null ? mensagem : erro)
                .path(path)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private String codigoErro(int status) {
        return switch (status) {
            case 400 -> "REQUISICAO_INVALIDA";
            case 401 -> "NAO_AUTORIZADO";
            case 403 -> "ACESSO_NEGADO";
            case 404 -> "NAO_ENCONTRADO";
            case 422 -> "FORMATO_INVALIDO";
            default  -> "ERRO";
        };
    }

    private String limparMensagem(String msg) {
        return (msg != null && !msg.isBlank()) ? msg : "Requisição inválida";
    }
}
