package br.com.buni.integration.core.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiErrorResponse {

    private final String timestamp;
    private final int status;
    private final String erro;
    private final String mensagem;
    private final String path;
}
