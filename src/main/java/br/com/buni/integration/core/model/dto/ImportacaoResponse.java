package br.com.buni.integration.core.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImportacaoResponse {

    private final String importId;
    private final String status;
    private final long processados;
    private final long sucesso;
    private final long erro;
    private final long duplicado;
    private final long tempoMs;
    private final String downloadUrl;
    private final String excelUrl;
}
