package br.com.buni.integration.core.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class HistoricoImportacao {

    private final String id;
    private final String tipo;
    private final String arquivo;
    private final String dataHora;
    private final String status;
    private final long processados;
    private final long sucesso;
    private final long erro;
    private final long duplicado;
    private final long tempoMs;
    private final String downloadUrl;
    private final String excelUrl;
}
