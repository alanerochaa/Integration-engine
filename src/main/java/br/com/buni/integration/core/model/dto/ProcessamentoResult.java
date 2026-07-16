package br.com.buni.integration.core.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@Builder
public class ProcessamentoResult {
    private final String importId;
    private final String statusGeral;
    private final long totalSucesso;
    private final long totalErro;
    private final long totalDuplicado;
    private final long tempoTotalMs;
    private final Path caminhoRelatorio;
    private final Path caminhoExcel;
}
